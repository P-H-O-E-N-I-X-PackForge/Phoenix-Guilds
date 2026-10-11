package net.phoenixvine.guilds.integration.gtceu;

import java.util.UUID;

/**
 * Moves GregTech's ender-link networks from one owner to another. GregTech's virtual registry changed between 7.x and
 * 8.x, so each version has its own implementation ({@link VirtualRegistryMigrator7}, {@code VirtualRegistryMigrator8});
 * this interface has no GregTech types and is always safe to load.
 */
public interface VirtualRegistryMigrator {

    void migrate(UUID from, UUID to);
}
