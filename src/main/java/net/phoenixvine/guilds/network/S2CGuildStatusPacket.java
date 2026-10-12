package net.phoenixvine.guilds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.guilds.PhoenixGuilds;

public record S2CGuildStatusPacket(String message, boolean error) implements CustomPacketPayload {

    public static final Type<S2CGuildStatusPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "s2c_guild_status")
    );

    public static final StreamCodec<ByteBuf, S2CGuildStatusPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(GuildNetworkLimits.LOG_MESSAGE_MAX), S2CGuildStatusPacket::message,
            ByteBufCodecs.BOOL, S2CGuildStatusPacket::error,
            S2CGuildStatusPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}