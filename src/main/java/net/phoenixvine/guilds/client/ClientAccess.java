package net.phoenixvine.guilds.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenixvine.guilds.client.screen.GuildFlagEditorScreen;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class ClientAccess {

    public static void openFlagEditor(UUID guildId) {
        Minecraft.getInstance().setScreen(new GuildFlagEditorScreen(null, guildId));
    }
}
