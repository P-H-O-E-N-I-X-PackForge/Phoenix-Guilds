package net.phoenixvine.guilds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenixvine.guilds.PhoenixGuilds;
import net.phoenixvine.guilds.data.Guild;
import net.phoenixvine.guilds.data.GuildManager;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record C2SRequestGuildFlagPacket(UUID guildId) implements CustomPacketPayload {

    public static final Type<C2SRequestGuildFlagPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "c2s_request_guild_flag")
    );

    public static final StreamCodec<ByteBuf, C2SRequestGuildFlagPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, C2SRequestGuildFlagPacket::guildId,
            C2SRequestGuildFlagPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SRequestGuildFlagPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            GuildManager mgr = GuildManager.get(Objects.requireNonNull(player.getServer()).overworld());
            Optional<Guild> opt = mgr.getGuildById(payload.guildId());

            S2CGuildFlagPacket reply = opt.map(g -> new S2CGuildFlagPacket(payload.guildId(), g.getFlagIconId(),
                            g.getFlagPixelData(), g.isFlagUseDrawing(), g.getFlagWidth(), g.getFlagHeight()))
                    .orElseGet(() -> new S2CGuildFlagPacket(payload.guildId(), "", "", false, Guild.DEFAULT_FLAG_SIZE,
                            Guild.DEFAULT_FLAG_SIZE));

            PacketDistributor.sendToPlayer(player, reply);
        });
    }
}