package com.exiledradio.netherupdatebackport.changes;

/**
 * How far you see in lava: today's {@code LavaFogEnvironment} (1.16 brought it in with 3 blocks for Fire
 * Resistance; current Java has 5).
 *
 * <p>Linear fog, from {@link #start} to {@link #end} blocks: a spectator sees from -8 to half the render
 * distance, Fire Resistance 0 to 5, anyone else 0.25 to 1. 1.12.2 used exponential fog of density 2 for
 * everyone. Kept out of the client package so the numbers can be tested on a server; the client's fog event
 * is {@code changes.client.LavaFog}.
 */
public final class LavaVision {

    private LavaVision() {
    }

    public static float start(boolean spectator, boolean fireResistant, float renderDistance) {
        return spectator ? -8.0F : fireResistant ? 0.0F : 0.25F;
    }

    public static float end(boolean spectator, boolean fireResistant, float renderDistance) {
        return spectator ? renderDistance * 0.5F : fireResistant ? 5.0F : 1.0F;
    }
}
