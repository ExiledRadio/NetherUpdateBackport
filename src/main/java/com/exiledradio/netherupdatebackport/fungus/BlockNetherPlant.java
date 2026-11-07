package com.exiledradio.netherupdatebackport.fungus;

import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The small nether plants: crimson and warped fungi, crimson and warped roots, nether sprouts.
 *
 * <p>All grow on nylium, soul soil, mycelium and 1.12.2's dirt blocks (1.16's {@code mayPlaceOn}). Roots and
 * sprouts are replaceable like grass and fireproof (1.16's {@code REPLACEABLE_FIREPROOF_PLANT}); sprouts drop
 * only with shears. A fungus grows into a huge fungus with bone meal, 40% of the time, but only on its own
 * nylium ({@link HugeFungus}).
 */
public class BlockNetherPlant extends BlockBush {

    /** 1.16's {@code REPLACEABLE_FIREPROOF_PLANT}: not solid, replaceable, never burns. */
    public static final Material FIREPROOF_PLANT = new MaterialLogic(MapColor.FOLIAGE) {
        {
            setReplaceable();
        }
    };

    private final AxisAlignedBB box;

    BlockNetherPlant(Material material, MapColor color, SoundType sound, AxisAlignedBB box) {
        super(material, color);
        setSoundType(sound);
        setHardness(0.0F);
        this.box = box;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.box.offset(state.getOffset(world, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return this instanceof Fungus ? EnumOffsetType.NONE : EnumOffsetType.XZ;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockNylium || block == Blocks.MYCELIUM || block == Owners.ourBlock("soul_soil")
                || super.canSustainBush(state);
    }

    // Forge's bush asks the soil (canSustainPlant), and netherrack-like soil answers no for every plant
    // type; these plants decide themselves, by 1.16's list above.
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && canSustainBush(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return canSustainBush(world.getBlockState(pos.down()));
    }

    /** Roots: like grass, a block placed in their space replaces them; they drop themselves. */
    public static final class Roots extends BlockNetherPlant {
        Roots(MapColor color, SoundType sound) {
            super(FIREPROOF_PLANT, color, sound, new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 13 / 16.0, 14 / 16.0));
        }

        @Override
        public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
            return true;
        }
    }

    /** Nether sprouts: replaceable, dropped only with shears. */
    public static final class Sprouts extends BlockNetherPlant {
        Sprouts(MapColor color, SoundType sound) {
            super(FIREPROOF_PLANT, color, sound, new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 3 / 16.0, 14 / 16.0));
        }

        @Override
        public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
            return true;
        }

        @Override
        public net.minecraft.item.Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return Items.AIR;
        }

        @Override
        public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack tool) {
            if (!world.isRemote && tool.getItem() == Items.SHEARS) {
                spawnAsEntity(world, pos, new ItemStack(this));
            }
            super.harvestBlock(world, player, pos, state, te, tool);
        }
    }

    /** A fungus: grows into a huge fungus on its own nylium. */
    public static final class Fungus extends BlockNetherPlant implements IGrowable {
        final NetherWood wood;

        Fungus(NetherWood wood, MapColor color, SoundType sound) {
            super(Material.PLANTS, color, sound, new AxisAlignedBB(4 / 16.0, 0, 4 / 16.0, 12 / 16.0, 9 / 16.0, 12 / 16.0));
            this.wood = wood;
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
            Block below = world.getBlockState(pos.down()).getBlock();
            return below instanceof BlockNylium && ((BlockNylium) below).wood == this.wood;
        }

        @Override
        public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
            return rand.nextFloat() < 0.4F;
        }

        @Override
        public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
            HugeFungus.place(world, rand, pos, this.wood, true);
        }
    }
}
