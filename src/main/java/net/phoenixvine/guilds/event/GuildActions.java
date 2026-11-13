package net.phoenixvine.guilds.event;

import net.phoenixvine.guilds.utils.GuildsLang;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenixvine.guilds.data.Guild;
import net.phoenixvine.guilds.data.GuildManager;
import net.phoenixvine.guilds.data.GuildRank;
import net.phoenixvine.guilds.utils.GuildContextAction;
import net.phoenixvine.guilds.utils.GuildPacketUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.phoenixvine.guilds.utils.GuildPacketUtils.*;
import static net.phoenixvine.guilds.utils.GuildUtils.*;

public class GuildActions {

    private static final Map<UUID, Long> HOME_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long HOME_COOLDOWN_MS = 5 * 60 * 1000L;

    public static void handleCreate(ServerPlayer player, GuildManager mgr, String name) {
        if (name.isBlank()) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_name_cannot_be_empty"));
            return;
        }
        if (mgr.isInGuild(player.getUUID())) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.you_are_already_in_a"));
            return;
        }
        if (mgr.getGuildByName(name).isPresent()) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.a_guild_named_already_exists", name));
            return;
        }
        var guild = mgr.createGuild(name, player.getUUID());
        send(player, GuildsLang.tr("phoenix_guilds.guild_actions.created_guild", guild.getName()));
        
        syncToPlayer(player, mgr);
        broadcastAllGuildList(Objects.requireNonNull(player.getServer()), mgr);
    }

    public static void broadcastAllGuildList(MinecraftServer server, GuildManager mgr) {
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (!mgr.isInGuild(p.getUUID())) syncToPlayer(p, mgr);
    }

    public static void handleInvite(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            ServerPlayer target = Objects.requireNonNull(player.getServer()).getPlayerList()
                    .getPlayerByName(targetName);
            if (target == null) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.player_is_not_online", targetName));
                return;
            }

            String result = mgr.sendInvite(guild.getId(), player.getUUID(), target.getUUID());
            switch (result) {
                case "sent" -> {
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.invitation_sent_to", targetName));
                    MutableComponent msg = GuildsLang.tr("phoenix_guilds.guild_actions.you_have_been_invited_to", guild.getName())
                            .append(GuildsLang.tr("phoenix_guilds.guild_actions.accept")
                                    .withStyle(s -> s
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                    "/guilds accept " + guild.getName()))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                    GuildsLang.tr("phoenix_guilds.guild_actions.click_to_accept")))))
                            .append(" ")
                            .append(GuildsLang.tr("phoenix_guilds.guild_actions.decline")
                                    .withStyle(s -> s
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                    "/guilds decline " + guild.getName()))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                    GuildsLang.tr("phoenix_guilds.guild_actions.click_to_decline")))));
                    target.sendSystemMessage(msg);
                }
                case "no_permission" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_invite"));
                case "already_in_guild" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.is_already_in_a_guild", targetName));
                case "guild_full" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.your_guild_is_full_members", Guild.MAX_MEMBERS));
                case "already_invited" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.has_already_been_invited", targetName));
                default -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.could_not_send_invite", result));
            }
        });
    }

    public static void handleInviteAccept(ServerPlayer player, GuildManager mgr, String guildName) {
        String result = mgr.acceptInvite(player.getUUID(), guildName);
        switch (result) {
            case "accepted" -> {
                Optional<Guild> guildOpt = mgr.getGuildByName(guildName);
                if (guildOpt.isPresent()) {
                    Guild guild = guildOpt.get();
                    guild.addLog(player.getName().getString() + " joined the Guild.");
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.you_have_joined", guild.getName()));
                    
                    syncToGuild(guild, player.getServer(), mgr);
                    broadcastAllGuildList(player.getServer(), mgr);
                }
            }
            case "already_in_guild" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.you_are_already_in_a"));
            case "guild_not_found" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_not_found", guildName));
            case "no_invite" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.you_have_no_pending_invitation", guildName));
            case "guild_full" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.that_guild_is_full"));
            default -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.could_not_accept_invite", result));
        }
    }

    public static void handleInviteDecline(ServerPlayer player, GuildManager mgr, String guildName) {
        String result = mgr.declineInvite(player.getUUID(), guildName);
        switch (result) {
            case "declined" -> {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.declined_invitation_from", guildName));
                mgr.getGuildByName(guildName).ifPresent(guild -> {
                    guild.addLog(player.getName().getString() + " declined the invitation.");
                    syncToGuild(guild, player.getServer(), mgr);
                });
            }
            case "guild_not_found" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_not_found", guildName));
            case "no_invite" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.you_have_no_pending_invitation", guildName));
            default -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.could_not_decline_invite", result));
        }
    }

    public static void handleHome(ServerPlayer player, GuildManager mgr) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.isHomeSet()) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_home_has_not_been"));
                return;
            }

            long now = System.currentTimeMillis();
            Long expires = HOME_COOLDOWNS.get(player.getUUID());
            if (expires != null && now < expires) {
                long secsLeft = (expires - now) / 1000;
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_home_is_on_cooldown", secsLeft));
                return;
            }
            HOME_COOLDOWNS.put(player.getUUID(), now + HOME_COOLDOWN_MS);

            ServerLevel targetLevel = Objects.requireNonNull(player.getServer()).getLevel(
                    ResourceKey.create(Registries.DIMENSION, guild.getHomeDimension()));
            if (targetLevel == null) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_home_dimension_is_no"));
                return;
            }
            if (player.level() == targetLevel) {
                player.connection.teleport(guild.getHomeX(), guild.getHomeY(), guild.getHomeZ(), guild.getHomeYaw(),
                        guild.getHomePitch());
            } else {
                player.teleportTo(targetLevel, guild.getHomeX(), guild.getHomeY(), guild.getHomeZ(), guild.getHomeYaw(),
                        guild.getHomePitch());
            }
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.teleported_to_s_home", guild.getName()));
        });
    }

    public static void handlePromote(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            ServerPlayer target = Objects.requireNonNull(player.getServer()).getPlayerList()
                    .getPlayerByName(targetName);
            if (target == null || !guild.isMember(target.getUUID())) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.is_not_in_your_guild", targetName));
                return;
            }
            String result = mgr.promotePlayer(guild.getId(), player.getUUID(), target.getUUID());
            switch (result) {
                case "ok" -> {
                    GuildRank newRank = guild.getRank(target.getUUID());
                    guild.addLog(
                            player.getName().getString() + " promoted " + targetName + " to " + newRank.label() + ".");
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.promoted_to", targetName, newRank.label()));
                    target.sendSystemMessage(GuildsLang.tr("phoenix_guilds.guild_actions.you_were_promoted_to_in", newRank.label(), guild.getName()));
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "no_permission" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.only_the_guild_owner_can_4"));
                case "already_owner" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.use_guilds_transfer_to_make"));
                default -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.could_not_promote", result));
            }
        });
    }

    public static void handleDemote(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            ServerPlayer target = Objects.requireNonNull(player.getServer()).getPlayerList()
                    .getPlayerByName(targetName);
            if (target == null || !guild.isMember(target.getUUID())) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.is_not_in_your_guild", targetName));
                return;
            }
            String result = mgr.demotePlayer(guild.getId(), player.getUUID(), target.getUUID());
            switch (result) {
                case "ok" -> {
                    guild.addLog(player.getName().getString() + " demoted " + targetName + " to Member.");
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.demoted_to_member", targetName));
                    target.sendSystemMessage(
                            GuildsLang.tr("phoenix_guilds.guild_actions.you_were_demoted_to_member", guild.getName()));
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "no_permission" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.only_the_guild_owner_can_3"));
                case "cant_demote_owner" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.cannot_demote_the_owner"));
                case "already_member" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.is_already_a_member", targetName));
                default -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.could_not_demote", result));
            }
        });
    }

    public static void handleTransfer(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            ServerPlayer target = Objects.requireNonNull(player.getServer()).getPlayerList()
                    .getPlayerByName(targetName);
            if (target == null || !guild.isMember(target.getUUID())) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.is_not_in_your_guild", targetName));
                return;
            }
            String result = mgr.transferOwnership(guild.getId(), player.getUUID(), target.getUUID());
            if ("ok".equals(result)) {
                guild.addLog(player.getName().getString() + " transferred ownership to " + targetName + ".");
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.transferred_ownership_of_to", guild.getName(), targetName));
                target.sendSystemMessage(
                        GuildsLang.tr("phoenix_guilds.guild_actions.you_are_now_the_owner", guild.getName()));
                syncToGuild(guild, player.getServer(), mgr);
            } else {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.only_the_guild_owner_can_2"));
            }
        });
    }

    public static void handleRemove(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.hasRank(player.getUUID(), GuildRank.OFFICER)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_remove"));
                return;
            }
            ServerPlayer target = Objects.requireNonNull(player.getServer()).getPlayerList()
                    .getPlayerByName(targetName);
            if (target == null || !guild.isMember(target.getUUID())) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.is_not_in_your_guild", targetName));
                return;
            }
            if (guild.getRank(target.getUUID()).ordinal() >= guild.getRank(player.getUUID()).ordinal()) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.you_cannot_remove_someone_of"));
                return;
            }
            mgr.removeMember(guild.getId(), target.getUUID());
            guild.addLog(player.getName().getString() + " removed " + targetName + ".");
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.removed_from", targetName, guild.getName()));
            target.sendSystemMessage(GuildsLang.tr("phoenix_guilds.guild_actions.you_were_removed_from_guild", guild.getName()));
            syncToGuild(guild, player.getServer(), mgr);
            syncToPlayer(target, mgr);
            GuildActions.broadcastAllGuildList(player.getServer(), mgr);
        });
    }

    public static void handleLeave(ServerPlayer player, GuildManager mgr) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            String name = guild.getName();
            UUID gid = guild.getId();
            guild.addLog(player.getName().getString() + " left the Guild.");
            mgr.removeMember(gid, player.getUUID());
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.left_guild", name));
            syncToPlayer(player, mgr);
            mgr.getGuildById(gid).ifPresent(s -> syncToGuild(s, player.getServer(), mgr));
            GuildActions.broadcastAllGuildList(Objects.requireNonNull(player.getServer()), mgr);
        });
    }

    public static void handleDisband(ServerPlayer player, GuildManager mgr) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.getOwner().equals(player.getUUID())) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.only_the_owner_can_delete"));
                return;
            }
            String name = guild.getName();
            List<ServerPlayer> allOnline = onlineMembers(guild, player.getServer());
            for (ServerPlayer mem : allOnline)
                if (!mem.getUUID().equals(player.getUUID()))
                    mem.sendSystemMessage(GuildsLang.tr("phoenix_guilds.guild_actions.guild_was_deleted", name));
            mgr.disbandGuild(guild.getId());
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.deleted_guild", name));
            for (ServerPlayer mem : allOnline) syncToPlayer(mem, mgr);
            GuildActions.broadcastAllGuildList(Objects.requireNonNull(player.getServer()), mgr);
        });
    }

    public static void handleSetMotd(ServerPlayer player, GuildManager mgr, String text) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!mgr.setMotd(guild.getId(), player.getUUID(), text)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_set_3"));
                return;
            }
            guild.addLog(player.getName().getString() + " updated the MOTD.");
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_motd_updated"));
            syncToGuild(guild, player.getServer(), mgr);
            GuildActions.broadcastAllGuildList(Objects.requireNonNull(player.getServer()), mgr);
        });
    }

    public static void handleSetDesc(ServerPlayer player, GuildManager mgr, String text) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!mgr.setDescription(guild.getId(), player.getUUID(), text)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_set_2"));
                return;
            }
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_description_updated"));
            syncToGuild(guild, player.getServer(), mgr);
            GuildActions.broadcastAllGuildList(Objects.requireNonNull(player.getServer()), mgr);
        });
    }

    public static void handleToggleFF(ServerPlayer player, GuildManager mgr) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!mgr.toggleFriendlyFire(guild.getId(), player.getUUID())) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.only_the_guild_owner_can"));
                return;
            }
            boolean nowOn = guild.isFriendlyFire();
            guild.addLog(player.getName().getString() + " turned friendly fire " + (nowOn ? "ON" : "OFF") + ".");
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.friendly_fire_is_now", (nowOn ? "§aON" : "§cOFF")));
            syncToGuild(guild, player.getServer(), mgr);
        });
    }

    public static void handleSetHome(ServerPlayer player, GuildManager mgr) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            ResourceLocation dim = player.level().dimension().location();
            boolean ok = mgr.setHome(guild.getId(), player.getUUID(), dim,
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
            if (!ok) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_set"));
                return;
            }
            guild.addLog(player.getName().getString() + " set Guild home.");
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_home_set_to_your"));
            syncToGuild(guild, player.getServer(), mgr);
        });
    }

    public static void handleGuildChat(ServerPlayer player, GuildManager mgr, String message) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            Component msg = GuildsLang.tr("phoenix_guilds.guild_actions.guild_chat", player.getName().getString(),
                    message);
            for (UUID uuid : guild.getMembers()) {
                ServerPlayer member = Objects.requireNonNull(player.getServer()).getPlayerList().getPlayer(uuid);
                if (member != null) member.sendSystemMessage(msg);
            }
        });
    }

    public static void handleAllyChat(ServerPlayer player, GuildManager mgr, String message) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            Component msg = GuildsLang.tr("phoenix_guilds.guild_actions.ally_chat", player.getName().getString(),
                    guild.getName(), message);

            for (UUID uuid : guild.getMembers()) {
                ServerPlayer member = Objects.requireNonNull(player.getServer()).getPlayerList().getPlayer(uuid);
                if (member != null) member.sendSystemMessage(msg);
            }

            for (UUID allyId : guild.getAllies()) {
                mgr.getGuildById(allyId).ifPresent(ally -> {
                    for (UUID uuid : ally.getMembers()) {
                        ServerPlayer member = Objects.requireNonNull(player.getServer()).getPlayerList()
                                .getPlayer(uuid);
                        if (member != null) member.sendSystemMessage(msg);
                    }
                });
            }
        });
    }

    public static void handleAllyRequest(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.hasRank(player.getUUID(), GuildRank.OFFICER)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_send"));
                return;
            }
            String result = mgr.sendAllyRequest(guild.getId(), targetName);
            switch (result) {
                case "sent" -> {
                    sendStatus(player, "Alliance request sent to " + targetName + ".", false);
                    mgr.getGuildByName(targetName).ifPresent(target -> {
                        ServerPlayer to = Objects.requireNonNull(player.getServer()).getPlayerList()
                                .getPlayer(target.getOwner());
                        if (to != null) to.sendSystemMessage(GuildsLang.tr("phoenix_guilds.guild_actions.guild_has_sent_your_guild", guild.getName(), guild.getName()));
                        syncToGuild(target, player.getServer(), mgr);
                    });
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "accepted" -> {
                    sendStatus(player, "Alliance formed with " + targetName + "!", false);
                    mgr.getGuildByName(targetName).ifPresent(t -> syncToGuild(t, player.getServer(), mgr));
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "already_ally" -> sendStatus(player, "Already allied with " + targetName + ".", true);
                case "already_sent" -> sendStatus(player, "Request already pending.", true);
                case "self" -> sendStatus(player, "You can't ally with your own Guild.", true);
                case "guild_not_found" -> sendStatus(player, "Guild '" + targetName + "' not found.", true);
            }
        });
    }

    public static void handleAllyAccept(ServerPlayer player, GuildManager mgr, String senderName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.hasRank(player.getUUID(), GuildRank.OFFICER)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_accept"));
                return;
            }
            String result = mgr.acceptAllyRequest(guild.getId(), senderName);
            switch (result) {
                case "accepted" -> {
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.alliance_formed_with", senderName));
                    mgr.getGuildByName(senderName).ifPresent(s -> {
                        syncToGuild(s, player.getServer(), mgr);
                        ServerPlayer so = Objects.requireNonNull(player.getServer()).getPlayerList()
                                .getPlayer(s.getOwner());
                        if (so != null) so.sendSystemMessage(
                                GuildsLang.tr("phoenix_guilds.guild_actions.guild_accepted_your_alliance_request", guild.getName()));
                    });
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "no_request" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.no_pending_request_from", senderName));
                case "guild_not_found" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_not_found", senderName));
            }
        });
    }

    public static void handleAllyDecline(ServerPlayer player, GuildManager mgr, String senderName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.hasRank(player.getUUID(), GuildRank.OFFICER)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_decline"));
                return;
            }
            String result = mgr.declineAllyRequest(guild.getId(), senderName);
            switch (result) {
                case "declined" -> {
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.declined_request_from", senderName));
                    mgr.getGuildByName(senderName).ifPresent(s -> syncToGuild(s, player.getServer(), mgr));
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "no_request" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.no_pending_request_from", senderName));
                case "guild_not_found" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_not_found", senderName));
            }
        });
    }

    public static void handleAllyBreak(ServerPlayer player, GuildManager mgr, String targetName) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            if (!guild.hasRank(player.getUUID(), GuildRank.OFFICER)) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_break"));
                return;
            }
            String result = mgr.breakAlliance(guild.getId(), targetName);
            switch (result) {
                case "broken" -> {
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.broke_alliance_with", targetName));
                    mgr.getGuildByName(targetName).ifPresent(t -> {
                        syncToGuild(t, player.getServer(), mgr);
                        ServerPlayer to = Objects.requireNonNull(player.getServer()).getPlayerList()
                                .getPlayer(t.getOwner());
                        if (to != null) to.sendSystemMessage(
                                GuildsLang.tr("phoenix_guilds.guild_actions.guild_broke_the_alliance", guild.getName()));
                    });
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "not_ally" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.not_allied_with", targetName));
                case "guild_not_found" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_not_found", targetName));
            }
        });
    }

    public static void handleWikiSet(ServerPlayer player, GuildManager mgr, String arg) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            int sep = arg.indexOf(' ');
            if (sep < 0) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.invalid_wiki_data"));
                return;
            }
            String title = arg.substring(0, sep).trim();
            String content = arg.substring(sep + 1);
            String result = mgr.setWikiPage(guild.getId(), player.getUUID(), title, content);
            switch (result) {
                case "ok" -> {
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.wiki_page_saved", title));
                    guild.addLog(player.getName().getString() + " updated wiki page \"" + title + "\".");
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "no_permission" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_edit_2"));
                case "wiki_full" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.wiki_is_full_20_pages"));
                case "empty_title" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.page_title_cannot_be_empty"));
            }
        });
    }

    public static void handleSetFlag(ServerPlayer player, GuildManager mgr, UUID targetGuildId, boolean useDrawing,
                                     String iconId, String pixelData, int width, int height) {
        Optional<Guild> currentGuild = mgr.getGuildFor(player.getUUID());
        if (targetGuildId == null) {
            if (currentGuild.isEmpty()) {
                send(player, GuildsLang.tr("phoenix_guilds.guilds_commands.you_are_not_in_a"));
                return;
            }
        } else if (mgr.getGuildById(targetGuildId).isEmpty()) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_flag_block.that_flag_s_guild_no"));
            return;
        } else if (currentGuild.isEmpty() || !currentGuild.get().getId().equals(targetGuildId)) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_flag_block.you_re_not_a_member"));
            return;
        }
        Guild g = targetGuildId == null ? currentGuild.get() : mgr.getGuildById(targetGuildId).get();
        if (useDrawing) {
            if (pixelData == null || pixelData.length() != Guild.FLAG_PIXEL_DATA_LENGTH ||
                    !pixelData.matches("[0-9a-f]+")) {
                send(player, GuildsLang.tr("phoenix_guilds.guild_actions.invalid_flag_drawing_data"));
                return;
            }
        } else if (!isValidFlagIcon(iconId)) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.that_item_or_block_doesn"));
            return;
        }
        if (!mgr.setFlag(g.getId(), player.getUUID(), useDrawing, iconId, pixelData, width, height)) {
            send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_edit"));
            return;
        }
        g.addLog(player.getName().getString() + " updated the Guild flag.");
        send(player, GuildsLang.tr("phoenix_guilds.guild_actions.guild_flag_updated"));
        syncToGuild(g, player.getServer(), mgr);
        GuildPacketUtils.pushFlagToOnlineMembers(g, player.getServer());
    }

    public static void handleWikiDelete(ServerPlayer player, GuildManager mgr, String title) {
        GuildActions.runWithGuild(player, mgr, (guild) -> {
            String result = mgr.deleteWikiPage(guild.getId(), player.getUUID(), title);
            switch (result) {
                case "ok" -> {
                    send(player, GuildsLang.tr("phoenix_guilds.guild_actions.deleted_wiki_page", title));
                    guild.addLog(player.getName().getString() + " deleted wiki page \"" + title + "\".");
                    syncToGuild(guild, player.getServer(), mgr);
                }
                case "no_permission" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.officers_and_above_can_delete"));
                case "not_found" -> send(player, GuildsLang.tr("phoenix_guilds.guild_actions.no_wiki_page_named", title));
            }
        });
    }

    public static void runWithGuild(ServerPlayer player, GuildManager mgr, GuildContextAction action) {
        mgr.getGuildFor(player.getUUID()).ifPresentOrElse(
                action::execute,
                () -> send(player, GuildsLang.tr("phoenix_guilds.guilds_commands.you_are_not_in_a")));
    }
}
