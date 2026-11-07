package com.exiledradio.netherupdatebackport.fungus;

import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Crimson or warped nylium: netherrack with a fungal top.
 *
 * <p>1.16's {@code NyliumBlock}: a random tick turns it back to netherrack when the block above stops all
 * light; it drops netherrack, or itself with Silk Touch. Bone meal grows the forest's plants around it
 * ({@link NetherVegetation}): crimson roots and fungi on crimson, warped roots, fungi and sprouts on warped,
 * with a one-in-eight chance of twisting vines.
 */
public final class BlockNylium extends Shapes.Plain implements IGrowable {

    final NetherWood wood;

    BlockNylium(NetherWood wood, SoundType sound) {
        super(Material.ROCK, wood.plant, 0.4F, 0.4F, sound);
        this.wood = wood;
        setTickRandomly(true);
    }

    static boolean canStayNylium(World world, BlockPos pos) {
        BlockPos up = pos.up();
        IBlockState above = world.getBlockState(up);
        return above.getLightOpacity(world, up) < 15;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!world.isRemote && !canStayNylium(world, pos)) {
            world.setBlockState(pos, Blocks.NETHERRACK.getDefaultState());
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(Blocks.NETHERRACK);
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(pos.up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        BlockPos up = pos.up();
        if (this.wood == NetherWood.CRIMSON) {
            NetherVegetation.place(world, rand, up, NetherVegetation.CRIMSON_FOREST, 3, 1);
        } else {
            NetherVegetation.place(world, rand, up, NetherVegetation.WARPED_FOREST, 3, 1);
            NetherVegetation.place(world, rand, up, NetherVegetation.SPROUTS, 3, 1);
            if (rand.nextInt(8) == 0) {
                NetherVegetation.twistingVines(world, rand, up, 3, 1, 2);
            }
        }
    }
}
