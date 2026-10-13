package net.phoenixvine.guilds.integration.gtceu;

import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEnderRegistry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.phoenixvine.guilds.PhoenixGuilds;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public final class VirtualRegistryMigrator {

    private static final EntryTypes<?>[] KNOWN_TYPES = {
            EntryTypes.ENDER_FLUID, EntryTypes.ENDER_ITEM, EntryTypes.ENDER_REDSTONE,
    };

    public void migrate(UUID from, UUID to) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        VirtualEnderRegistry registry = VirtualEnderRegistry.get(server.overworld());
        for (EntryTypes<?> type : KNOWN_TYPES) migrateType(registry, from, to, type);
    }

    private static <T extends VirtualEntry> void migrateType(VirtualEnderRegistry registry, UUID from, UUID to,
                                                             EntryTypes<T> type) {
        Map<String, VirtualEntry> entries;
        try {
            entries = registry.getEntries(from, type);
        } catch (NullPointerException noRegistryForOwner) {
            return;
        }
        if (entries == null || entries.isEmpty()) return;

        for (Map.Entry<String, VirtualEntry> entry : new ArrayList<>(entries.entrySet())) {
            String name = entry.getKey();
            if (registry.hasEntry(to, type, name)) {
                PhoenixGuilds.LOGGER.warn("GTCEu virtual entry '{}' already exists under {}. Leaving the one" +
                        " from {} in place; it'll need a manual rename to reconnect.", name, to, from);
                continue;
            }
            registry.addEntry(to, name, entry.getValue());
            registry.forceDeleteEntry(from, type, name);
        }
    }
}
