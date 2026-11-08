package com.exiledradio.netherupdatebackport.netherite;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Ancient debris: 1.16.5's {@code ore_debris_large} and {@code ore_debris_small}, both "no exposed ore"
 * features.
 *
 * <ul>
 *   <li>Large: one try per chunk of up to 3, at a depth-average height (16 give or take 8);</li>
 *   <li>small: one try of up to 2, anywhere from y 8 to 119.</li>
 * </ul>
 * 1.16's {@code NoExposedOreFeature}: a random count up to the size, each block offset from the centre by at
 * most its index (capped at 7) on each axis, placed only into netherrack, basalt or blackstone and only where
 * no face touches air, so debris is never seen until it is dug to. Decoration only.
 */
final class AncientDebrisGeneration implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        // With another mod's debris kept visible (compat.duplicates THEIRS), only that one generates.
        if (!ModConfig.netherite.generate || world.provider.getDimensionType() != DimensionType.NETHER
                || DuplicateBlocks.isHidden(NetheriteContent.ancientDebris)) {
            return;
        }
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        // DepthAverageConfig(16, 8): rand(8) + rand(8) + 16 - 8.
        place(world, random, new BlockPos(x + random.nextInt(16), random.nextInt(8) + random.nextInt(8) + 8, z + random.nextInt(16)), 3);
        // TopSolidRangeConfig(8, 16, 128): 8 + rand(112).
        place(world, random, new BlockPos(x + random.nextInt(16), 8 + random.nextInt(112), z + random.nextInt(16)), 2);
    }

    static void place(World world, Random random, BlockPos centre, int size) {
        int count = random.nextInt(size + 1);
        for (int i = 0; i < count; i++) {
            int spread = Math.min(i, 7);
            BlockPos p = centre.add(offset(random, spread), offset(random, spread), offset(random, spread));
            if (replaceable(world.getBlockState(p).getBlock()) && !facesAir(world, p)) {
                world.setBlockState(p, NetheriteContent.ancientDebris.getDefaultState(), 2);
            }
        }
    }

    private static int offset(Random random, int spread) {
        return Math.round((random.nextFloat() - random.nextFloat()) * spread);
    }

    /** 1.16's {@code NETHER_ORE_REPLACEABLES}: netherrack, basalt, blackstone. */
    private static boolean replaceable(Block block) {
        return block == Blocks.NETHERRACK || block == Owners.ourBlock("basalt") && block != null
                || block == Owners.ourBlock("blackstone") && block != null;
    }

    private static boolean facesAir(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            if (world.isAirBlock(pos.offset(face))) {
                return true;
            }
        }
        return false;
    }
}
