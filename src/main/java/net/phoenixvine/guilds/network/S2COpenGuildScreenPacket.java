package net.phoenixvine.guilds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.guilds.PhoenixGuilds;

public record S2COpenGuildScreenPacket() implements CustomPacketPayload {

    public static final Type<S2COpenGuildScreenPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "s2c_open_guild_screen")
    );

    public static final StreamCodec<ByteBuf, S2COpenGuildScreenPacket> STREAM_CODEC = StreamCodec.unit(new S2COpenGuildScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}