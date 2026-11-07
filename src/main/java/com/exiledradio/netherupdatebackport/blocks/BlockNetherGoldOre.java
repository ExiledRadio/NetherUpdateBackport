package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Nether gold ore: 2-6 gold nuggets, more with Fortune, 0-1 experience; Silk Touch keeps the block.
 *
 * <p>Current Java's loot table: {@code set_count 2-6}, then Fortune's {@code ore_drops} bonus (the count
 * times {@code max(0, rand(fortune + 2) - 1) + 1}). Experience from 1.16's {@code OreBlock.xpOnDrop}.
 * Piglins that see it mined get angry (stream P reads this block by name).
 */
public final class BlockNetherGoldOre extends Shapes.Plain {

    BlockNetherGoldOre(SoundType sound) {
        super(Material.ROCK, MapColor.NETHERRACK, 3.0F, 3.0F, sound);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : RANDOM;
        int count = 2 + random.nextInt(5);
        if (fortune > 0) {
            count *= Math.max(0, random.nextInt(fortune + 2) - 1) + 1;
        }
        drops.add(new ItemStack(Items.GOLD_NUGGET, count));
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : new Random();
        return MathHelper.getInt(random, 0, 1);
    }
}
