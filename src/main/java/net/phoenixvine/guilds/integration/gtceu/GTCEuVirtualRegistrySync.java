package net.phoenixvine.guilds.integration.gtceu;

import net.phoenixvine.guilds.PhoenixGuilds;

import java.util.UUID;

public final class GTCEuVirtualRegistrySync {

    private GTCEuVirtualRegistrySync() {}

    public static void migrateToGuild(UUID from, UUID to) {
        if (from.equals(to)) return;
        String name = "net.phoenixvine.guilds.integration.gtceu." + (GuildsGTCEuIntegration.usesNewApi() ?
                "VirtualRegistryMigrator8" : "VirtualRegistryMigrator7");
        try {
            ((VirtualRegistryMigrator) Class.forName(name).getDeclaredConstructor().newInstance()).migrate(from, to);
        } catch (Throwable t) {
            PhoenixGuilds.LOGGER.error("Failed to migrate GTCEu virtual registry entries from {} to {}. Any" +
                    " existing Ender Link Cover networks may need manual reconfiguring.", from, to, t);
        }
    }
}

