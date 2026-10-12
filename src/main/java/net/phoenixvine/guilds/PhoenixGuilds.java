package net.phoenixvine.guilds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.phoenixvine.guilds.client.GuildsKeybinds;
import net.phoenixvine.guilds.client.PhoenixGuildsClient;
import net.phoenixvine.guilds.content.flag.GuildFlagBlocks;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PhoenixGuilds.MOD_ID)
public class PhoenixGuilds {

    public static final String MOD_ID = "phoenix_guilds";
    public static final Logger LOGGER = LogManager.getLogger();

    public PhoenixGuilds(IEventBus modEventBus, ModContainer modContainer) {
        GuildFlagBlocks.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Safe client initialization on NeoForge 1.21.1
        if (FMLEnvironment.dist.isClient()) {
            PhoenixGuildsClient.init(modEventBus);
            modEventBus.register(GuildsKeybinds.class);
        }

    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Hello from common setup! Found a {}!", Items.DIAMOND);
        });
    }
}