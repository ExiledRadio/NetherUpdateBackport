package com.exiledradio.netherupdatebackport.blocks;

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
 * Nether gold ore: 1.16's {@code ore_gold_nether}, 10 veins of up to 10 per chunk at y 10-117, replacing
 * netherrack, in every nether biome (basalt deltas get twice as many; that is stream V's). Decoration only.
 */
final class NetherGoldGeneration implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!ModConfig.blocks.generateGold || world.provider.getDimensionType() != DimensionType.NETHER) {
            return;
        }
        WorldGenMinable vein = new WorldGenMinable(BlocksContent.netherGoldOre.getDefaultState(), 10, BlockMatcher.forBlock(Blocks.NETHERRACK));
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        for (int i = 0; i < 10; i++) {
            // RANGE_10_20_ROOFED: TopSolidRangeConfig(10, 20, 128) -> 10 + rand(108).
            vein.generate(world, random, new BlockPos(x + random.nextInt(16), 10 + random.nextInt(108), z + random.nextInt(16)));
        }
    }
}
