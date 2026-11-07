package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;

import net.minecraftforge.common.MinecraftForge;

/**
 * Stream C: 1.16's smaller changes to things 1.12.2 already has.
 *
 * <ul>
 *   <li>Hoes are a real tool ({@link HoeTool}).</li>
 *   <li>Fire Resistance lets you see farther in lava: client only, {@code changes.client.LavaFog}.</li>
 * </ul>
 * 1.16's nether-tab advancements all belong to a stream (see {@code docs/streams/changes.md}); vanilla's own
 * nether advancements did not change in 1.16, so none is added here.
 */
public final class ChangesContent {

    private static boolean hoes;

    private ChangesContent() {
    }

    /** From init: every mod's blocks and items are registered by then. */
    public static void init() {
        if (!ModConfig.changes.enabled || !ModConfig.changes.hoes) {
            return;
        }
        HoeTool.init();
        MinecraftForge.EVENT_BUS.register(new HoeTool());
        hoes = true;
    }

    /** @return true if hoes were made a tool at startup */
    public static boolean hoes() {
        return hoes;
    }
}
