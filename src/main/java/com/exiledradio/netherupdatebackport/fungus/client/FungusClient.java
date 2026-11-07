package com.exiledradio.netherupdatebackport.fungus.client;

import com.exiledradio.netherupdatebackport.fungus.NetherSigns;

import net.minecraftforge.fml.client.registry.ClientRegistry;

/** Client side of stream F: the sign renderer. */
public final class FungusClient {

    private FungusClient() {
    }

    public static void init() {
        ClientRegistry.bindTileEntitySpecialRenderer(NetherSigns.Tile.class, new NetherSignRenderer());
    }
}
