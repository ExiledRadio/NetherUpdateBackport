package com.exiledradio.netherupdatebackport.fungus;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Huge crimson and warped fungi: 1.16.5's {@code HugeFungusFeature}, step for step.
 *
 * <p>A stem 4-13 tall (doubled one time in twelve), a hat of wart blocks with shroomlights in it, and for
 * crimson, weeping vines hanging from the hat. "Planted" fungi (grown with bone meal) push through plants
 * and break what is in the way; generated ones (stream V's forests) can be thick (3x3 stems, 6%) and never
 * grow past the top of the world.
 */
public final class HugeFungus {

    private HugeFungus() {
    }

    /** @return true if a fungus was grown; the base below pos must be the wood's nylium */
    public static boolean place(World world, Random rand, BlockPos pos, NetherWood wood, boolean planted) {
        IBlockState stem = FungusContent.stem(wood).getDefaultState();
        IBlockState hat = wood == NetherWood.CRIMSON ? Blocks.NETHER_WART_BLOCK.getDefaultState() : FungusContent.warpedWartBlock.getDefaultState();
        IBlockState decor = FungusContent.shroomlight.getDefaultState();
        if (world.getBlockState(pos.down()).getBlock() != FungusContent.nylium(wood)) {
            return false;
        }
        int height = MathHelper.getInt(rand, 4, 13);
        if (rand.nextInt(12) == 0) {
            height *= 2;
        }
        if (!planted && pos.getY() + height + 1 >= world.getActualHeight()) {
            return false;
        }
        boolean thick = !planted && rand.nextFloat() < 0.06F;
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 4);
        placeStem(world, rand, pos, stem, height, thick, planted);
        placeHat(world, rand, pos, hat, decor, height, thick, planted);
        return true;
    }

    private static boolean replaceable(World world, BlockPos pos, boolean plantsToo) {
        Material m = world.getBlockState(pos).getMaterial();
        return m.isReplaceable() || plantsToo && m == Material.PLANTS;
    }

    private static void placeStem(World world, Random rand, BlockPos base, IBlockState stem, int height, boolean thick, boolean planted) {
        int r = thick ? 1 : 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                boolean corner = thick && Math.abs(dx) == r && Math.abs(dz) == r;
                for (int y = 0; y < height; y++) {
                    BlockPos p = base.add(dx, y, dz);
                    if (!replaceable(world, p, true)) {
                        continue;
                    }
                    if (planted) {
                        if (!world.isAirBlock(p.down())) {
                            world.destroyBlock(p, true);
                        }
                        world.setBlockState(p, stem, 3);
                    } else if (corner) {
                        if (rand.nextFloat() < 0.1F) {
                            world.setBlockState(p, stem, 2);
                        }
                    } else {
                        world.setBlockState(p, stem, 2);
                    }
                }
            }
        }
    }

    private static void placeHat(World world, Random rand, BlockPos base, IBlockState hat, IBlockState decor, int height,
                                 boolean thick, boolean planted) {
        boolean crimson = hat.getBlock() == Blocks.NETHER_WART_BLOCK;
        int hatHeight = Math.min(rand.nextInt(1 + height / 3) + 5, height);
        int start = height - hatHeight;
        for (int y = start; y <= height; y++) {
            int r = y < height - rand.nextInt(3) ? 2 : 1;
            if (hatHeight > 8 && y < start + 4) {
                r = 3;
            }
            if (thick) {
                r++;
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean edgeX = dx == -r || dx == r;
                    boolean edgeZ = dz == -r || dz == r;
                    boolean inside = !edgeX && !edgeZ && y != height;
                    boolean corner = edgeX && edgeZ;
                    boolean lower = y < start + 3;
                    BlockPos p = base.add(dx, y, dz);
                    if (!replaceable(world, p, false)) {
                        continue;
                    }
                    if (planted && !world.isAirBlock(p.down())) {
                        world.destroyBlock(p, true);
                    }
                    if (lower) {
                        if (!inside) {
                            hatDrop(world, rand, p, hat, crimson);
                        }
                    } else if (inside) {
                        hatBlock(world, rand, p, hat, decor, 0.1F, 0.2F, crimson ? 0.1F : 0.0F);
                    } else if (corner) {
                        hatBlock(world, rand, p, hat, decor, 0.01F, 0.7F, crimson ? 0.083F : 0.0F);
                    } else {
                        hatBlock(world, rand, p, hat, decor, 5.0E-4F, 0.98F, crimson ? 0.07F : 0.0F);
                    }
                }
            }
        }
    }

    private static void hatBlock(World world, Random rand, BlockPos p, IBlockState hat, IBlockState decor,
                                 float decorChance, float hatChance, float vineChance) {
        if (rand.nextFloat() < decorChance) {
            world.setBlockState(p, decor, 2);
        } else if (rand.nextFloat() < hatChance) {
            world.setBlockState(p, hat, 2);
            if (rand.nextFloat() < vineChance) {
                weepingVines(world, rand, p);
            }
        }
    }

    private static void hatDrop(World world, Random rand, BlockPos p, IBlockState hat, boolean crimson) {
        if (world.getBlockState(p.down()).getBlock() == hat.getBlock()) {
            world.setBlockState(p, hat, 2);
        } else if (rand.nextFloat() < 0.15) {
            world.setBlockState(p, hat, 2);
            if (crimson && rand.nextInt(11) == 0) {
                weepingVines(world, rand, p);
            }
        }
    }

    private static void weepingVines(World world, Random rand, BlockPos from) {
        BlockPos below = from.down();
        if (world.isAirBlock(below)) {
            int length = MathHelper.getInt(rand, 1, 5);
            if (rand.nextInt(7) == 0) {
                length *= 2;
            }
            NetherVegetation.weepingColumn(world, rand, below, length, 23, 25);
        }
    }
}
