package com.exiledradio.netherupdatebackport.fungus;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The crimson and warped forests' small growth, from 1.16.5's features: {@code NetherVegetationFeature} (roots,
 * fungi, sprouts scattered over nylium), {@code TwistingVineFeature} and the vine columns. Bone meal on nylium
 * uses them now; stream V's forests will place them in generation.
 */
public final class NetherVegetation {

    /** A weighted choice of plants: 1.16's {@code WeightedBlockStateProvider}. */
    public static final class Mix {
        final String[] names;
        final int[] weights;
        final int total;

        Mix(String[] names, int[] weights) {
            this.names = names;
            this.weights = weights;
            int t = 0;
            for (int w : weights) {
                t += w;
            }
            this.total = t;
        }

        Block pick(Random rand) {
            int r = rand.nextInt(this.total);
            for (int i = 0; i < this.names.length; i++) {
                r -= this.weights[i];
                if (r < 0) {
                    return FungusContent.block(this.names[i]);
                }
            }
            return FungusContent.block(this.names[0]);
        }
    }

    public static final Mix CRIMSON_FOREST = new Mix(new String[] {"crimson_roots", "crimson_fungus", "warped_fungus"}, new int[] {87, 11, 1});
    public static final Mix WARPED_FOREST = new Mix(new String[] {"warped_roots", "crimson_roots", "warped_fungus", "crimson_fungus"},
            new int[] {85, 1, 13, 1});
    public static final Mix SPROUTS = new Mix(new String[] {"nether_sprouts"}, new int[] {1});

    private NetherVegetation() {
    }

    /** 1.16's {@code NetherVegetationFeature.place}: spread x spread tries within the box, on nylium only. */
    public static boolean place(World world, Random rand, BlockPos pos, Mix mix, int spread, int height) {
        if (!(world.getBlockState(pos.down()).getBlock() instanceof BlockNylium) || pos.getY() < 1 || pos.getY() + 1 >= 256) {
            return false;
        }
        int placed = 0;
        for (int i = 0; i < spread * spread; i++) {
            BlockPos p = pos.add(rand.nextInt(spread) - rand.nextInt(spread), rand.nextInt(height) - rand.nextInt(height),
                    rand.nextInt(spread) - rand.nextInt(spread));
            Block plant = mix.pick(rand);
            if (plant != null && world.isAirBlock(p) && p.getY() > 0 && ((BlockNetherPlant) plant).canBlockStay(world, p, plant.getDefaultState())) {
                world.setBlockState(p, plant.getDefaultState(), 2);
                placed++;
            }
        }
        return placed > 0;
    }

    /** 1.16's {@code TwistingVineFeature.place} with its spread, height and length. */
    public static boolean twistingVines(World world, Random rand, BlockPos pos, int spread, int height, int maxLength) {
        if (invalidTwisting(world, pos)) {
            return false;
        }
        for (int i = 0; i < spread * spread; i++) {
            BlockPos p = pos.add(MathHelper.getInt(rand, -spread, spread), MathHelper.getInt(rand, -height, height),
                    MathHelper.getInt(rand, -spread, spread));
            // Down to the first non-air block, then one up.
            while (p.getY() > 0 && world.isAirBlock(p)) {
                p = p.down();
            }
            if (p.getY() <= 0) {
                continue;
            }
            p = p.up();
            if (invalidTwisting(world, p)) {
                continue;
            }
            int length = MathHelper.getInt(rand, 1, maxLength);
            if (rand.nextInt(6) == 0) {
                length *= 2;
            }
            if (rand.nextInt(5) == 0) {
                length = 1;
            }
            twistingColumn(world, rand, p, length, 17, 25);
        }
        return true;
    }

    private static boolean invalidTwisting(World world, BlockPos pos) {
        if (!world.isAirBlock(pos)) {
            return true;
        }
        Block below = world.getBlockState(pos.down()).getBlock();
        return below != Blocks.NETHERRACK && below != FungusContent.nylium(NetherWood.WARPED) && below != FungusContent.warpedWartBlock;
    }

    /** A twisting vine column of a length, its tip at a 1.16 age from min to max (folded to ours). */
    public static void twistingColumn(World world, Random rand, BlockPos from, int length, int minAge, int maxAge) {
        BlockNetherVines.Head head = FungusContent.twistingVines;
        BlockPos p = from;
        for (int i = 1; i <= length; i++) {
            if (world.isAirBlock(p)) {
                if (i == length || !world.isAirBlock(p.up())) {
                    world.setBlockState(p, tip(head, rand, minAge, maxAge), 2);
                    break;
                }
                world.setBlockState(p, head.body.getDefaultState(), 2);
            }
            p = p.up();
        }
    }

    /** A weeping vine column hanging down from a block. */
    public static void weepingColumn(World world, Random rand, BlockPos from, int length, int minAge, int maxAge) {
        BlockNetherVines.Head head = FungusContent.weepingVines;
        BlockPos p = from;
        for (int i = 0; i <= length; i++) {
            if (world.isAirBlock(p)) {
                if (i == length || !world.isAirBlock(p.down())) {
                    world.setBlockState(p, tip(head, rand, minAge, maxAge), 2);
                    break;
                }
                world.setBlockState(p, head.body.getDefaultState(), 2);
            }
            p = p.down();
        }
    }

    private static IBlockState tip(BlockNetherVines.Head head, Random rand, int minAge, int maxAge) {
        int age25 = MathHelper.getInt(rand, minAge, maxAge);
        return head.getDefaultState().withProperty(BlockNetherVines.AGE, Math.min(BlockNetherVines.MAX_AGE, age25 * (BlockNetherVines.MAX_AGE + 1) / 25));
    }
}
