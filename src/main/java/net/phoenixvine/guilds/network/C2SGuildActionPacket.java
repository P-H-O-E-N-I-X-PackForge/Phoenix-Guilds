package net.phoenixvine.guilds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenixvine.guilds.PhoenixGuilds;
import net.phoenixvine.guilds.data.GuildAction;
import net.phoenixvine.guilds.data.GuildManager;

import java.util.Objects;

import static net.phoenixvine.guilds.event.GuildActions.*;

public record C2SGuildActionPacket(GuildAction action, String arg) implements CustomPacketPayload {

    public static final Type<C2SGuildActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "guild_action")
    );

    public static final StreamCodec<ByteBuf, C2SGuildActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.map(i -> GuildAction.values()[i], GuildAction::ordinal), C2SGuildActionPacket::action,
            ByteBufCodecs.stringUtf8(GuildNetworkLimits.ACTION_ARG_MAX), C2SGuildActionPacket::arg,
            C2SGuildActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SGuildActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            GuildManager mgr = GuildManager.get(Objects.requireNonNull(player.getServer()).overworld());
            switch (packet.action()) {
                case CREATE -> handleCreate(player, mgr, packet.arg());
                case INVITE -> handleInvite(player, mgr, packet.arg());
                case REMOVE -> handleRemove(player, mgr, packet.arg());
                case LEAVE -> handleLeave(player, mgr);
                case DISBAND -> handleDisband(player, mgr);
                case PROMOTE -> handlePromote(player, mgr, packet.arg());
                case DEMOTE -> handleDemote(player, mgr, packet.arg());
                case TRANSFER -> handleTransfer(player, mgr, packet.arg());
                case SET_MOTD -> handleSetMotd(player, mgr, packet.arg());
                case SET_DESC -> handleSetDesc(player, mgr, packet.arg());
                case TOGGLE_FF -> handleToggleFF(player, mgr);
                case SET_HOME -> handleSetHome(player, mgr);
                case HOME -> handleHome(player, mgr);
                case ALLY_REQUEST -> handleAllyRequest(player, mgr, packet.arg());
                case ALLY_ACCEPT -> handleAllyAccept(player, mgr, packet.arg());
                case ALLY_DECLINE -> handleAllyDecline(player, mgr, packet.arg());
                case ALLY_BREAK -> handleAllyBreak(player, mgr, packet.arg());
                case GUILD_CHAT -> handleGuildChat(player, mgr, packet.arg());
                case ALLY_CHAT -> handleAllyChat(player, mgr, packet.arg());
                case WIKI_SET -> handleWikiSet(player, mgr, packet.arg());
                case WIKI_DELETE -> handleWikiDelete(player, mgr, packet.arg());
            }
        });
    }
}