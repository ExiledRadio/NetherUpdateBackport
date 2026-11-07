package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Where the 1.16 blocks and mobs of the bastion, ruined portal and fossil files come from, through Backport Core's
 * {@link ModernPalette}.
 *
 * <ul>
 *   <li>This mod's blocks are named as 1.16 names them, so listing it as a source serves most: the blackstone and
 *       basalt families (S), crying obsidian and nether gold ore (U), and so on.</li>
 *   <li>Chain: Core reads a file's {@code minecraft:chain} by today's name, {@code minecraft:iron_chain}; U's chain.</li>
 *   <li>Slabs: 1.12.2 makes a double slab a block of its own, so {@code type} picks the half or the double. Smooth
 *       quartz slabs are Village &amp; Pillage's, or this mod's copy without it (stream Ow's {@code owned}).</li>
 *   <li>Lanterns and stone brick walls are Village &amp; Pillage's (already a Core source), or this mod's copies.</li>
 *   <li>Mobs: hoglins (H) and piglins and brutes (P), by name, only once they are registered.</li>
 * </ul>
 */
final class StructurePalette {

    private StructurePalette() {
    }

    static void preInit() {
        ModernPalette.map("minecraft:iron_chain", () -> Owners.ourBlock("chain"));
        slab("minecraft:blackstone_slab", NetherUpdateBackport.MOD_ID + ":blackstone_slab");
        slab("minecraft:polished_blackstone_slab", NetherUpdateBackport.MOD_ID + ":polished_blackstone_slab");
        slab("minecraft:polished_blackstone_brick_slab", NetherUpdateBackport.MOD_ID + ":polished_blackstone_brick_slab");
        slab("minecraft:smooth_quartz_slab", Owners.VILLAGE_AND_PILLAGE + ":smooth_quartz_slab", NetherUpdateBackport.MOD_ID + ":smooth_quartz_slab");
        slab("minecraft:mossy_stone_brick_slab", Owners.VILLAGE_AND_PILLAGE + ":mossy_stone_brick_slab", NetherUpdateBackport.MOD_ID + ":mossy_stone_brick_slab");
        // 1.12.2's own stone slabs, with 1.16's default (a bottom half) where a processor names one without a type:
        // block_age turns stairs into a plain "minecraft:stone_slab", which is Blocks.STONE_SLAB.defaultBlockState().
        vanillaSlab("minecraft:stone_brick_slab", BlockStoneSlab.EnumType.SMOOTHBRICK);
        vanillaSlab("minecraft:smooth_stone_slab", BlockStoneSlab.EnumType.STONE);
        if (Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE)) {
            // 1.14's plain stone slab is Village & Pillage's.
            slab("minecraft:stone_slab", Owners.VILLAGE_AND_PILLAGE + ":stone_slab");
        } else {
            // Without it, 1.12.2's (smooth) stone slab stands in, as Core's own stand-in has it.
            vanillaSlab("minecraft:stone_slab", BlockStoneSlab.EnumType.STONE);
        }
    }

    /** A 1.16 slab that 1.12.2 has as a variant of its stone slab and double stone slab. */
    static void vanillaSlab(String modern, BlockStoneSlab.EnumType variant) {
        ModernPalette.map(modern, (Map<String, String> props) -> {
            if ("double".equals(props.get("type"))) {
                return Blocks.DOUBLE_STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, variant);
            }
            return Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, variant).withProperty(BlockSlab.HALF,
                    "top".equals(props.get("type")) ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM);
        });
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

    /**
     * A 1.16 slab ({@code type=bottom|top|double}) as 1.12.2's half slab ({@code half}) or its double block
     * ({@code <name>_double_slab}), from the first of the given half slabs that is registered.
     */
    static void slab(String modern, String... halves) {
        ModernPalette.map(modern, (Map<String, String> props) -> {
            for (String id : halves) {
                Block half = registered(id);
                if (half == null) {
                    continue;
                }
                if ("double".equals(props.get("type"))) {
                    Block full = registered(id.substring(0, id.length() - "_slab".length()) + "_double_slab");
                    return full == null ? null : full.getDefaultState();
                }
                IBlockState s = half.getDefaultState();
                if (s.getPropertyKeys().contains(BlockSlab.HALF)) {
                    s = s.withProperty(BlockSlab.HALF, "top".equals(props.get("type")) ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM);
                }
                return s;
            }
            return null;
        });
    }

    @Nullable
    static Block registered(String name) {
        ResourceLocation id = new ResourceLocation(name);
        Block b = ForgeRegistries.BLOCKS.getValue(id);
        return b == null || b == Blocks.AIR || !id.equals(b.getRegistryName()) ? null : b;
    }
}
