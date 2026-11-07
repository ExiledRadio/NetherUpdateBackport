package com.exiledradio.netherupdatebackport.fungus;

import net.minecraft.block.material.MapColor;

/** The two nether woods and everything 1.16 names after them. */
public enum NetherWood {
    // Map colors: 1.16's CRIMSON_STEM / WARPED_STEM, CRIMSON_HYPHAE / WARPED_HYPHAE and the nylium colors have
    // no 1.12.2 equivalents; these are the nearest of 1.12.2's palette.
    CRIMSON("crimson", MapColor.MAGENTA_STAINED_HARDENED_CLAY, MapColor.RED_STAINED_HARDENED_CLAY, MapColor.NETHERRACK),
    WARPED("warped", MapColor.CYAN, MapColor.PURPLE_STAINED_HARDENED_CLAY, MapColor.CYAN);

    public final String name;
    public final MapColor stem;
    public final MapColor hyphae;
    public final MapColor plant;

    NetherWood(String name, MapColor stem, MapColor hyphae, MapColor plant) {
        this.name = name;
        this.stem = stem;
        this.hyphae = hyphae;
        this.plant = plant;
    }
}
