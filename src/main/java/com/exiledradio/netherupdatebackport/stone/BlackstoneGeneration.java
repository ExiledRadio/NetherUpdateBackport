package com.exiledradio.netherupdatebackport.stone;

import com.exiledradio.netherupdatebackport.ModConfig;

import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Blackstone blobs in the nether: 1.16's {@code ore_blackstone}, which every nether biome has.
 *
 * <p>Two blobs of up to 33 blocks per chunk, replacing netherrack, at a height from 5 to 31
 * ({@code TopSolidRangeConfig(5, 10, 37)}). Decoration only: nothing about the terrain changes, and
 * only newly generated chunks get it.
 */
final class BlackstoneGeneration implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!ModConfig.stone.generate || world.provider.getDimensionType() != DimensionType.NETHER) {
            return;
        }
        WorldGenMinable blob = new WorldGenMinable(StoneContent.blackstone.getDefaultState(), 33, BlockMatcher.forBlock(Blocks.NETHERRACK));
        // No +8 here: WorldGenMinable adds it itself (as vanilla's quartz and magma calls rely on), so the
        // blob lands in the middle of the 2x2 chunks that population guarantees. Adding it again pushed
        // blobs 16-31 blocks in plus their reach, into chunks not yet generated (cascading worldgen).
        int x = chunkX * 16;
        int z = chunkZ * 16;
        for (int i = 0; i < 2; i++) {
            blob.generate(world, random, new BlockPos(x + random.nextInt(16), 5 + random.nextInt(27), z + random.nextInt(16)));
        }
    }
}
