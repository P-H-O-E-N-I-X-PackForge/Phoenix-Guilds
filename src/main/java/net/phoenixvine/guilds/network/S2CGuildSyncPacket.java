package net.phoenixvine.guilds.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.guilds.PhoenixGuilds;
import net.phoenixvine.guilds.data.Guild;

import java.util.List;
import java.util.UUID;

public record S2CGuildSyncPacket(
        String guildName,
        UUID ownerUUID,
        String motd,
        String description,
        boolean friendlyFire,
        boolean homeSet,
        String flagIconId,
        String flagPixelData,
        boolean flagUseDrawing,
        int flagWidth,
        int flagHeight,
        List<MemberEntry> members,
        List<AllyEntry> allies,
        List<PendingEntry> pendingOutgoing,
        List<PendingEntry> pendingIncoming,
        List<LogEntry> logEntries,
        List<WikiPage> wikiPages,
        List<GuildSummary> allGuilds
) implements CustomPacketPayload {

    public static final Type<S2CGuildSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "s2c_guild_sync")
    );

    public record MemberEntry(UUID uuid, String name, boolean isOnline, String rank) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MemberEntry> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, MemberEntry::uuid,
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.MEMBER_NAME_MAX), MemberEntry::name,
                ByteBufCodecs.BOOL, MemberEntry::isOnline,
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.RANK_MAX), MemberEntry::rank,
                MemberEntry::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<MemberEntry>> LIST_STREAM_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    public record GuildSummary(UUID id, String name, int memberCount, int onlineCount, String description) {
        public static final StreamCodec<RegistryFriendlyByteBuf, GuildSummary> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, GuildSummary::id,
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.NAME_MAX), GuildSummary::name,
                ByteBufCodecs.VAR_INT, GuildSummary::memberCount,
                ByteBufCodecs.VAR_INT, GuildSummary::onlineCount,
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.DESCRIPTION_MAX), GuildSummary::description,
                GuildSummary::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<GuildSummary>> LIST_STREAM_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    public record AllyEntry(String name, int memberCount, int onlineCount) {
        public static final StreamCodec<RegistryFriendlyByteBuf, AllyEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.NAME_MAX), AllyEntry::name,
                ByteBufCodecs.VAR_INT, AllyEntry::memberCount,
                ByteBufCodecs.VAR_INT, AllyEntry::onlineCount,
                AllyEntry::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<AllyEntry>> LIST_STREAM_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    public record PendingEntry(String guildName) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PendingEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.NAME_MAX), PendingEntry::guildName,
                PendingEntry::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<PendingEntry>> LIST_STREAM_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    public record LogEntry(long timestamp, String message) {
        public static final StreamCodec<RegistryFriendlyByteBuf, LogEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, LogEntry::timestamp,
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.LOG_MESSAGE_MAX), LogEntry::message,
                LogEntry::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<LogEntry>> LIST_STREAM_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    public record WikiPage(String title, String content) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WikiPage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.WIKI_TITLE_MAX), WikiPage::title,
                ByteBufCodecs.stringUtf8(GuildNetworkLimits.WIKI_CONTENT_MAX), WikiPage::content,
                WikiPage::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<WikiPage>> LIST_STREAM_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CGuildSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                boolean inGuild = packet.guildName() != null;
                buf.writeBoolean(inGuild);
                if (inGuild) {
                    ByteBufCodecs.stringUtf8(GuildNetworkLimits.NAME_MAX).encode(buf, packet.guildName());
                    UUIDUtil.STREAM_CODEC.encode(buf, packet.ownerUUID());
                    ByteBufCodecs.stringUtf8(GuildNetworkLimits.MOTD_MAX).encode(buf, packet.motd());
                    ByteBufCodecs.stringUtf8(GuildNetworkLimits.DESCRIPTION_MAX).encode(buf, packet.description());
                    ByteBufCodecs.BOOL.encode(buf, packet.friendlyFire());
                    ByteBufCodecs.BOOL.encode(buf, packet.homeSet());
                    ByteBufCodecs.stringUtf8(GuildNetworkLimits.ICON_ID_MAX).encode(buf, packet.flagIconId());
                    ByteBufCodecs.stringUtf8(Guild.FLAG_PIXEL_DATA_LENGTH).encode(buf, packet.flagPixelData());
                    ByteBufCodecs.BOOL.encode(buf, packet.flagUseDrawing());
                    ByteBufCodecs.VAR_INT.encode(buf, packet.flagWidth());
                    ByteBufCodecs.VAR_INT.encode(buf, packet.flagHeight());
                    MemberEntry.LIST_STREAM_CODEC.encode(buf, packet.members());
                    AllyEntry.LIST_STREAM_CODEC.encode(buf, packet.allies());
                    PendingEntry.LIST_STREAM_CODEC.encode(buf, packet.pendingOutgoing());
                    PendingEntry.LIST_STREAM_CODEC.encode(buf, packet.pendingIncoming());
                    LogEntry.LIST_STREAM_CODEC.encode(buf, packet.logEntries());
                    WikiPage.LIST_STREAM_CODEC.encode(buf, packet.wikiPages());
                }
                GuildSummary.LIST_STREAM_CODEC.encode(buf, packet.allGuilds());
            },
            buf -> {
                boolean inGuild = buf.readBoolean();
                String guildName;
                UUID ownerUUID;
                String motd;
                String description;
                boolean friendlyFire;
                boolean homeSet;
                String flagIconId;
                String flagPixelData;
                boolean flagUseDrawing;
                int flagWidth;
                int flagHeight;
                List<MemberEntry> members;
                List<AllyEntry> allies;
                List<PendingEntry> pendingOutgoing;
                List<PendingEntry> pendingIncoming;
                List<LogEntry> logEntries;
                List<WikiPage> wikiPages;

                if (inGuild) {
                    guildName = ByteBufCodecs.stringUtf8(GuildNetworkLimits.NAME_MAX).decode(buf);
                    ownerUUID = UUIDUtil.STREAM_CODEC.decode(buf);
                    motd = ByteBufCodecs.stringUtf8(GuildNetworkLimits.MOTD_MAX).decode(buf);
                    description = ByteBufCodecs.stringUtf8(GuildNetworkLimits.DESCRIPTION_MAX).decode(buf);
                    friendlyFire = ByteBufCodecs.BOOL.decode(buf);
                    homeSet = ByteBufCodecs.BOOL.decode(buf);
                    flagIconId = ByteBufCodecs.stringUtf8(GuildNetworkLimits.ICON_ID_MAX).decode(buf);
                    flagPixelData = ByteBufCodecs.stringUtf8(Guild.FLAG_PIXEL_DATA_LENGTH).decode(buf);
                    flagUseDrawing = ByteBufCodecs.BOOL.decode(buf);
                    flagWidth = ByteBufCodecs.VAR_INT.decode(buf);
                    flagHeight = ByteBufCodecs.VAR_INT.decode(buf);
                    members = MemberEntry.LIST_STREAM_CODEC.decode(buf);
                    allies = AllyEntry.LIST_STREAM_CODEC.decode(buf);
                    pendingOutgoing = PendingEntry.LIST_STREAM_CODEC.decode(buf);
                    pendingIncoming = PendingEntry.LIST_STREAM_CODEC.decode(buf);
                    logEntries = LogEntry.LIST_STREAM_CODEC.decode(buf);
                    wikiPages = WikiPage.LIST_STREAM_CODEC.decode(buf);
                } else {
                    guildName = null;
                    ownerUUID = null;
                    motd = "";
                    description = "";
                    friendlyFire = false;
                    homeSet = false;
                    flagIconId = "";
                    flagPixelData = "0".repeat(Guild.FLAG_PIXEL_DATA_LENGTH);
                    flagUseDrawing = false;
                    flagWidth = Guild.DEFAULT_FLAG_SIZE;
                    flagHeight = Guild.DEFAULT_FLAG_SIZE;
                    members = List.of();
                    allies = List.of();
                    pendingOutgoing = List.of();
                    pendingIncoming = List.of();
                    logEntries = List.of();
                    wikiPages = List.of();
                }

                List<GuildSummary> allGuilds = GuildSummary.LIST_STREAM_CODEC.decode(buf);

                return new S2CGuildSyncPacket(guildName, ownerUUID, motd, description, friendlyFire, homeSet,
                        flagIconId, flagPixelData, flagUseDrawing, flagWidth, flagHeight,
                        members, allies, pendingOutgoing, pendingIncoming, logEntries, wikiPages, allGuilds);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}