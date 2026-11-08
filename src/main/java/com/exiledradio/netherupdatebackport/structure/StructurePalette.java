package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Where the 1.16 blocks and mobs of the bastion, ruined portal and fossil files come from, through Backport Core's
 * {@link ModernPalette}.
 *
 * <ul>
 *   <li>This mod's blocks are named as 1.16 names them, so listing it as a source serves most: the blackstone and
 *       basalt families (S), crying obsidian and nether gold ore (U), and so on.</li>
 *   <li>Chain: Core reads a file's {@code minecraft:chain} by today's name, {@code minecraft:iron_chain}; U's chain.</li>
 *   <li>Slabs: Core 1.1.0 reads {@code type} into the family's 1.12.2 slabs (a {@code _slab} with {@code half}, a
 *       separate {@code _double_slab}) and a slab named without a type as its default, a bottom half. So this mod's
 *       blackstone slabs (S), Village &amp; Pillage's smooth quartz, mossy stone brick and 1.14 stone slabs (or this
 *       mod's copies without it) and 1.12.2's own stone and stone brick slabs need no mapping here.</li>
 *   <li>Lanterns and stone brick walls are Village &amp; Pillage's (already a Core source), or this mod's copies.</li>
 *   <li>Mobs: hoglins (H) and piglins and brutes (P), by name, only once they are registered.</li>
 * </ul>
 */
final class StructurePalette {

    private StructurePalette() {
    }

    static void preInit() {
        ModernPalette.map("minecraft:iron_chain", () -> Owners.ourBlock("chain"));
    }

    static void init() {
        entity("minecraft:hoglin", "hoglin");
        entity("minecraft:piglin", "piglin");
        entity("minecraft:piglin_brute", "piglin_brute");
    }

    private static void entity(String modern, String ours) {
        ResourceLocation id = new ResourceLocation(NetherUpdateBackport.MOD_ID, ours);
        if (ForgeRegistries.ENTITIES.containsKey(id)) {
            ModernPalette.entity(modern, id);
        }
    }
}
