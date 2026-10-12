package net.phoenixvine.guilds.integration.gtceu;
/*
import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEnderRegistry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;

import net.phoenixvine.guilds.PhoenixGuilds;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public final class VirtualRegistryMigrator7 implements VirtualRegistryMigrator {

    private static final EntryTypes<?>[] KNOWN_TYPES = {
            EntryTypes.ENDER_FLUID, EntryTypes.ENDER_ITEM, EntryTypes.ENDER_REDSTONE,
    };

    @Override
    public void migrate(UUID from, UUID to) {
        VirtualEnderRegistry registry = VirtualEnderRegistry.getInstance();
        for (EntryTypes<?> type : KNOWN_TYPES) migrateType(registry, from, to, type);
    }

    private static <T extends VirtualEntry> void migrateType(VirtualEnderRegistry registry, UUID from, UUID to,
                                                             EntryTypes<T> type) {
        Set<String> names = registry.getEntryNames(from, type);
        if (names == null || names.isEmpty()) return;

        for (String name : new ArrayList<>(names)) {
            if (registry.hasEntry(to, type, name)) {
                PhoenixGuilds.LOGGER.warn("GTCEu virtual entry '{}' already exists under {}. Leaving the one" +
                        " from {} in place; it'll need a manual rename to reconnect.", name, to, from);
                continue;
            }
            T entry = registry.getEntry(from, type, name);
            if (entry == null) continue;
            registry.addEntry(to, name, entry);
            registry.deleteEntry(from, type, name);
        }
    }
}



 */