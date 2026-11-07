package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Crying obsidian: obsidian's strength, light level 10, purple tears dripping from its open faces.
 *
 * <p>1.16's {@code CryingObsidianBlock.animateTick}: one tick in five, a random face other than the top;
 * if nothing solid covers it, a tear forms just outside it. The particle is drawn by a client class.
 */
public final class BlockCryingObsidian extends Shapes.Plain {

    BlockCryingObsidian() {
        super(Material.ROCK, MapColor.BLACK, 50.0F, 1200.0F, SoundType.STONE);
        setLightLevel(10 / 15.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(5) != 0) {
            return;
        }
        EnumFacing face = EnumFacing.random(rand);
        if (face == EnumFacing.UP) {
            return;
        }
        BlockPos next = pos.offset(face);
        if (world.getBlockState(next).isSideSolid(world, next, face.getOpposite())) {
            return;
        }
        double dx = face.getXOffset() == 0 ? rand.nextDouble() : 0.5 + face.getXOffset() * 0.6;
        double dy = face.getYOffset() == 0 ? rand.nextDouble() : 0.5 + face.getYOffset() * 0.6;
        double dz = face.getZOffset() == 0 ? rand.nextDouble() : 0.5 + face.getZOffset() * 0.6;
        com.exiledradio.netherupdatebackport.blocks.client.BlocksClient.tear(world, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
    }
}
