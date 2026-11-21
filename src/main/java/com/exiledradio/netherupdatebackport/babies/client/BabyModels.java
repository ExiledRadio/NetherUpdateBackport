package com.exiledradio.netherupdatebackport.babies.client;

import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;

/**
 * When this mod's babies take 26.1's baby models (Game Drops Backport's request 1, stream 1): with Game Drops
 * Backport installed, as its {@code babies.enabled} and {@code babies.babyModels} settings say (read by name every
 * frame, so its settings screen applies at once); without it, when Backport Core's look is 26.1 or later. Otherwise
 * babies are 1.16's shrunken adults.
 */
public final class BabyModels {

    private static final GameVersion TINY_TAKEOVER = GameVersion.parse("26.1");

    private static boolean resolved;
    private static boolean gameDrops;
    private static Object babies;
    private static Field enabled;
    private static Field babyModels;

    private BabyModels() {
    }

    /** Set only by the client self-test, to draw both looks without Game Drops Backport installed. */
    private static volatile Boolean forced;

    /** Self-tests only: true or false forces the models on or off, null goes back to the settings. */
    public static void force(Boolean on) {
        forced = on;
    }

    /** @return true while babies draw with 26.1's models */
    public static boolean active() {
        Boolean f = forced;
        if (f != null) {
            return f;
        }
        if (!resolved) {
            resolve();
        }
        if (gameDrops) {
            return read(enabled) && read(babyModels);
        }
        return Presentation.newest().isAtLeast(TINY_TAKEOVER);
    }

    private static boolean read(Field f) {
        if (f == null) {
            return true;
        }
        try {
            return f.getBoolean(babies);
        } catch (IllegalAccessException e) {
            return true;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        gameDrops = Loader.isModLoaded(Owners.GAME_DROPS);
        if (gameDrops) {
            try {
                Field section = Class.forName("com.exiledradio.gamedropsbackport.ModConfig").getField("babies");
                babies = section.get(null);
                enabled = field(babies, "enabled");
                babyModels = field(babies, "babyModels");
            } catch (ReflectiveOperationException | RuntimeException e) {
                // A Game Drops Backport without a babies section: it is installed, so the models show.
                babies = null;
            }
        }
        resolved = true;
    }

    private static Field field(Object section, String name) {
        try {
            return section.getClass().getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
