package net.phoenixvine.guilds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenixvine.guilds.PhoenixGuilds;
import net.phoenixvine.guilds.data.Guild;
import net.phoenixvine.guilds.data.GuildManager;
import net.phoenixvine.guilds.event.GuildActions;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record C2SSetGuildFlagPacket(
        Optional<UUID> targetGuildId,
        boolean useDrawing,
        String iconId,
        String pixelData,
        int width,
        int height
) implements CustomPacketPayload {

    public static final Type<C2SSetGuildFlagPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "set_guild_flag")
    );

    public static final StreamCodec<ByteBuf, C2SSetGuildFlagPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), C2SSetGuildFlagPacket::targetGuildId,
            ByteBufCodecs.BOOL, C2SSetGuildFlagPacket::useDrawing,
            ByteBufCodecs.stringUtf8(GuildNetworkLimits.ICON_ID_MAX), C2SSetGuildFlagPacket::iconId,
            ByteBufCodecs.stringUtf8(Guild.FLAG_PIXEL_DATA_LENGTH), C2SSetGuildFlagPacket::pixelData,
            ByteBufCodecs.VAR_INT, C2SSetGuildFlagPacket::width,
            ByteBufCodecs.VAR_INT, C2SSetGuildFlagPacket::height,
            C2SSetGuildFlagPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SSetGuildFlagPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            GuildManager mgr = GuildManager.get(Objects.requireNonNull(player.getServer()).overworld());
            GuildActions.handleSetFlag(
                    player,
                    mgr,
                    packet.targetGuildId().orElse(null),
                    packet.useDrawing(),
                    packet.iconId(),
                    packet.pixelData(),
                    packet.width(),
                    packet.height()
            );
        });
    }
}