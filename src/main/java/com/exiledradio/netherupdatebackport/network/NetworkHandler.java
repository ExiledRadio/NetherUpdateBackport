package com.exiledradio.netherupdatebackport.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * The mod's one network channel.
 *
 * <h2>Packet ids</h2>
 * Each stream owns a block of five ids (see {@code docs/PLAN.md}) and registers its packets here, inside
 * its own block, so two streams can never pick the same number and merges stay mechanical:
 *
 * <pre>
 *   0-4   common          25-29 netherite     50-54 music
 *   5-9   stone           30-34 hoglins       55-59 changes
 *   10-14 blocks          35-39 piglins       60-64 biomes
 *   15-19 fungus          40-44 striders      65-69 structures
 *   20-24 soul            45-49 mobs
 * </pre>
 *
 * <p>The channel name is short because Forge caps channel names at 20 characters, one fewer than the
 * mod id.
 */
public final class NetworkHandler {

    public static final String CHANNEL = "nubackport";

    public static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        // Streams register their packets below, in id order.
    }
}
