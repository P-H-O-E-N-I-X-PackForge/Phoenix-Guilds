package net.phoenixvine.guilds.common;

import net.minecraft.server.level.ServerPlayer;
import net.phoenixvine.guilds.data.GuildAction;
import net.phoenixvine.wiki.api.gate.SuiteGates;

/**
 * What a player may do with guilds, as features of the shared gate API ({@link SuiteGates}): another mod binds them to
 * research or anything else, e.g. {@code SuiteGates.bind(GuildGates.CREATE, "*", "conflux:node/foo")}. A feature that
 * nothing is bound to is allowed. Managing a guild you are already in (ranks, invites, leaving) is never gated, so a
 * gate
 * cannot trap a player in a guild.
 */
public final class GuildGates {

    /** Founding a guild. */
    public static final String CREATE = "guilds:create";
    /** Setting and using the guild home. */
    public static final String HOME = "guilds:home";
    /** Asking for, accepting and breaking alliances. */
    public static final String ALLIANCES = "guilds:alliances";
    /** Guild and ally chat. */
    public static final String CHAT = "guilds:chat";
    /** Editing the guild wiki. */
    public static final String WIKI = "guilds:wiki";

    private GuildGates() {}

    /** The feature an action needs, or null if the action is not gated. */
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

    /** Whether the player may do the action; tells them when not. */
    public static boolean check(ServerPlayer player, GuildAction action) {
        String feature = featureFor(action);
        if (feature == null || allowed(player, feature)) return true;
        player.displayClientMessage(SuiteGates.denial(feature, SuiteGates.ANY_SCOPE), false);
        return false;
    }
}
