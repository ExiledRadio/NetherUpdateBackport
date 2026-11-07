package com.exiledradio.netherupdatebackport.stone;

import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Gilded blackstone: blackstone with gold in it.
 *
 * <p>Drops (current Java's loot table): Silk Touch keeps the block; otherwise 2-5 gold nuggets with a
 * chance of 10%, 14.3%, 25% and 100% at Fortune 0 to III, and the block itself the rest of the time.
 * Silk Touch is vanilla's own path ({@code canSilkHarvest} is true for a plain full block).
 */
public final class BlockGildedBlackstone extends Shapes.Plain {

    /** Fortune 0, I, II, III. */
    static final float[] NUGGET_CHANCE = {0.1F, 0.14285715F, 0.25F, 1.0F};

    BlockGildedBlackstone(SoundType sound) {
        super(Material.ROCK, MapColor.BLACK, 1.5F, 6.0F, sound);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : RANDOM;
        float chance = NUGGET_CHANCE[Math.max(0, Math.min(fortune, NUGGET_CHANCE.length - 1))];
        if (random.nextFloat() < chance) {
            drops.add(new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4)));
        } else {
            drops.add(new ItemStack(this));
        }
    }
}
