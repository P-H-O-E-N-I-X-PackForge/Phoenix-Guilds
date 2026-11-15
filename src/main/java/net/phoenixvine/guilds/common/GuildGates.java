package net.phoenixvine.guilds.common;

import net.minecraft.server.level.ServerPlayer;
import net.phoenixvine.guilds.data.GuildAction;
import net.phoenixvine.wiki.api.gate.SuiteGates;

public final class GuildGates {

    public static final String CREATE = "guilds:create";

    public static final String HOME = "guilds:home";

    public static final String ALLIANCES = "guilds:alliances";

    public static final String CHAT = "guilds:chat";

    public static final String WIKI = "guilds:wiki";

    private GuildGates() {}

    public static String featureFor(GuildAction action) {
        return switch (action) {
            case CREATE -> CREATE;
            case SET_HOME, HOME -> HOME;
            case ALLY_REQUEST, ALLY_ACCEPT -> ALLIANCES;
            case GUILD_CHAT, ALLY_CHAT -> CHAT;
            case WIKI_SET, WIKI_DELETE -> WIKI;
            default -> null;
        };
    }

    public static boolean allowed(ServerPlayer player, String feature) {
        return SuiteGates.allowed(player, feature, SuiteGates.ANY_SCOPE);
    }

    public static boolean check(ServerPlayer player, GuildAction action) {
        String feature = featureFor(action);
        if (feature == null || allowed(player, feature)) return true;
        player.displayClientMessage(SuiteGates.denial(feature, SuiteGates.ANY_SCOPE), false);
        return false;
    }
}
