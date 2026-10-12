package net.phoenixvine.guilds.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.phoenixvine.guilds.PhoenixGuilds;
import net.phoenixvine.guilds.client.screen.GuildScreen;
import net.phoenixvine.guilds.content.flag.GuildFlagBlockEntityRenderer;
import net.phoenixvine.guilds.content.flag.GuildFlagBlocks;
import net.phoenixvine.guilds.content.flag.GuildFlagItemRenderer;
import net.phoenixvine.wiki.client.suite.SuiteHudBar;
import net.phoenixvine.wiki.theme.PhoenixTheme;

public class PhoenixGuildsClient {

    public static void init(IEventBus modEventBus) {
        modEventBus.register(GuildsKeybinds.class);

        modEventBus.addListener(PhoenixGuildsClient::clientSetup);
        modEventBus.addListener(PhoenixGuildsClient::registerRenderers);
        modEventBus.addListener(PhoenixGuildsClient::registerClientExtensions);

        PhoenixTheme.registerMod("net.phoenixvine.guilds", PhoenixGuilds.MOD_ID);
        PhoenixTheme.addChangeListener(GuildThemeUtils::refreshCache);
    }

    private static void clientSetup(final FMLClientSetupEvent event) {
        PhoenixGuilds.LOGGER.info("Hey, we're on Minecraft version {}!", Minecraft.getInstance().getLaunchedVersion());

        event.enqueueWork(() -> {
            // Block entity renderer handles the flag visuals
        });

        SuiteHudBar.register(PhoenixGuilds.MOD_ID, SuiteHudBar.PRIORITY_GUILDS,
                ResourceLocation.fromNamespaceAndPath(PhoenixGuilds.MOD_ID, "textures/gui/suite_bar_icon.png"),
                Component.literal("§fOpen Guild Menu"),
                () -> Minecraft.getInstance().setScreen(
                        new GuildScreen(Minecraft.getInstance().screen)));
    }

    private static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GuildFlagBlocks.GUILD_FLAG_ENTITY.get(), GuildFlagBlockEntityRenderer::new);
    }

    private static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new GuildFlagItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, GuildFlagBlocks.GUILD_FLAG_ITEM.get());
    }
}