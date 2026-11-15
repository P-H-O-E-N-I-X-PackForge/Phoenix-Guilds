package net.phoenixvine.guilds.integration.gtceu;

import java.util.UUID;

public interface VirtualRegistryMigrator {

    void migrate(UUID from, UUID to);
}
