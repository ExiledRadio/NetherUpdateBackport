package com.exiledradio.netherupdatebackport.soul;

import net.minecraft.block.BlockTorch;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The soul torch: a torch giving light 10 with a blue soul flame.
 *
 * <p>1.12.2's torch is one block for the floor and the wall (its {@code facing}), where 1.16 splits it into
 * {@code soul_torch} and {@code soul_wall_torch}; this follows 1.12.2's torch, so everything vanilla's torch
 * does (placement, popping off, the five facings) is the same. Particles at vanilla's torch positions.
 */
public final class BlockSoulTorch extends BlockTorch {

    BlockSoulTorch() {
        setLightLevel(10 / 15.0F);
        setHardness(0.0F);
        setSoundType(SoundType.WOOD);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        EnumFacing facing = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;
        if (facing.getAxis().isHorizontal()) {
            EnumFacing back = facing.getOpposite();
            x += 0.27 * back.getXOffset();
            y += 0.22;
            z += 0.27 * back.getZOffset();
        }
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0, 0.0, 0.0);
        com.exiledradio.netherupdatebackport.soul.client.SoulClient.soulFlame(world, x, y, z, 0.0, 0.0, 0.0);
    }
}
