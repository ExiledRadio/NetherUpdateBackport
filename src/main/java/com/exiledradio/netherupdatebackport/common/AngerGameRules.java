package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.ModConfig;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.16's two anger game rules (20w11a and 20w12a), {@code forgiveDeadPlayers} (on) and {@code universalAnger}
 * (off), added to every world that lacks them when it loads, so {@code /gamerule} lists and sets them.
 *
 * <p>Every stream reads them through here: with the setting {@code mobs.angerGameRules} off, or in a world not
 * loaded on this server yet, each acts as its 1.16 default.
 */
public final class AngerGameRules {

    public static final String FORGIVE_DEAD_PLAYERS = "forgiveDeadPlayers";
    public static final String UNIVERSAL_ANGER = "universalAnger";

    @SubscribeEvent
    public void load(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || !active()) {
            return;
        }
        GameRules rules = world.getGameRules();
        add(rules, FORGIVE_DEAD_PLAYERS, "true");
        add(rules, UNIVERSAL_ANGER, "false");
    }

    /**
     * Adds a rule at its default, or, for one a saved world brought back (1.12.2 reads a rule it does not know as
     * "any value"), makes it a true/false rule again with its saved value, so {@code /gamerule} offers true and false.
     */
    private static void add(GameRules rules, String rule, String fallback) {
        if (!rules.hasRule(rule)) {
            rules.addGameRule(rule, fallback, GameRules.ValueType.BOOLEAN_VALUE);
        } else if (!rules.areSameType(rule, GameRules.ValueType.BOOLEAN_VALUE)) {
            String saved = rules.getString(rule);
            rules.addGameRule(rule, "true".equals(saved) || "false".equals(saved) ? saved : fallback, GameRules.ValueType.BOOLEAN_VALUE);
        }
    }

    private static boolean active() {
        return ModConfig.mobs.enabled && ModConfig.mobs.angerGameRules;
    }

    /** @return the world's {@code forgiveDeadPlayers}, or true (its default) */
    public static boolean forgiveDeadPlayers(World world) {
        if (!active() || !world.getGameRules().hasRule(FORGIVE_DEAD_PLAYERS)) {
            return true;
        }
        return world.getGameRules().getBoolean(FORGIVE_DEAD_PLAYERS);
    }

    /** @return the world's {@code universalAnger}, or false (its default) */
    public static boolean universalAnger(World world) {
        return active() && world.getGameRules().hasRule(UNIVERSAL_ANGER) && world.getGameRules().getBoolean(UNIVERSAL_ANGER);
    }
}
