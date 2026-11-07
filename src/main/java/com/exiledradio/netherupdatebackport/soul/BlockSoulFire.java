package com.exiledradio.netherupdatebackport.soul;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Soul fire: the blue fire that burns on soul sand and soul soil.
 *
 * <p>1.16's {@code SoulFireBlock}: it stands only on a soul fire base block and goes out the moment that is
 * gone; light 10; 2 fire damage to what is in it (normal fire's is 1) and sets it alight; it never spreads
 * and never burns anything (it has none of {@code BlockFire}'s spreading, which this block does not extend).
 * Vanilla fire lit or spread onto soul sand or soul soil becomes this ({@link SoulContent}).
 */
public final class BlockSoulFire extends Block {

    BlockSoulFire() {
        super(Material.FIRE, MapColor.LIGHT_BLUE);
        setLightLevel(10 / 15.0F);
        setHardness(0.0F);
        disableStats();
    }

    /** @return true if soul fire can stand on this block (1.16's {@code soul_fire_base_blocks}) */
    public static boolean isBase(Block block) {
        return block == Blocks.SOUL_SAND || block != null && block == SoulContent.soulSoil;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && isBase(world.getBlockState(pos.down()).getBlock());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!isBase(world.getBlockState(pos.down()).getBlock())) {
            world.setBlockToAir(pos);
        }
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!entity.isImmuneToFire()) {
            entity.setFire(8);
            entity.attackEntityFrom(DamageSource.IN_FIRE, 2.0F);
        }
    }

    /** Puts it out, with vanilla fire's hiss (as punching a fire out does). */
    public static void extinguish(World world, BlockPos pos) {
        world.setBlockToAir(pos);
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        // 1.16's AbstractFireBlock.animateTick: the fire's ambient sound one tick in 24.
        if (rand.nextInt(24) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT,
                    SoundCategory.BLOCKS, 1.0F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F, false);
        }
    }
}
