package net.phoenixvine.guilds.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.phoenixvine.guilds.PhoenixGuilds;

@EventBusSubscriber(modid = PhoenixGuilds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GuildNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // --- C2S (Serverbound) Packets ---
        registrar.playToServer(
                C2SGuildActionPacket.TYPE,
                C2SGuildActionPacket.STREAM_CODEC,
                C2SGuildActionPacket::handle
        );

        registrar.playToServer(
                C2SSetGuildFlagPacket.TYPE,
                C2SSetGuildFlagPacket.STREAM_CODEC,
                C2SSetGuildFlagPacket::handle
        );

        registrar.playToServer(
                C2SRequestGuildFlagPacket.TYPE,
                C2SRequestGuildFlagPacket.STREAM_CODEC,
                C2SRequestGuildFlagPacket::handle
        );

        // --- S2C (Clientbound) Packets ---
        registrar.playToClient(
                S2CGuildSyncPacket.TYPE,
                S2CGuildSyncPacket.STREAM_CODEC,
                GuildPacketHandlerClient::handleSyncPacket
        );

        registrar.playToClient(
                S2COpenGuildScreenPacket.TYPE,
                S2COpenGuildScreenPacket.STREAM_CODEC,
                GuildPacketHandlerClient::handleOpenScreenPacket
        );

        registrar.playToClient(
                S2CGuildFlagPacket.TYPE,
                S2CGuildFlagPacket.STREAM_CODEC,
                GuildPacketHandlerClient::handleClientFlagPacket
        );

        registrar.playToClient(
                S2CGuildStatusPacket.TYPE,
                S2CGuildStatusPacket.STREAM_CODEC,
                GuildPacketHandlerClient::handleStatusPacket
        );
    }
}