package com.exiledradio.netherupdatebackport.soul;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.IPlantable;

/**
 * Soul soil: a plain block, but for 1.16.5's {@code WitherRoseBlock.mayPlaceOn}, which takes soul soil besides
 * netherrack and soul sand (20w06a). The wither rose is Village &amp; Pillage's (1.14); its bush asks the soil, so the
 * soil answers yes for it, found by name. Its own checks cover netherrack and soul sand, and a wither's kill places
 * one wherever {@code canPlaceBlockAt} says yes, so this is all it takes.
 */
public class SoulSoil extends Shapes.Plain {

    private static final ResourceLocation WITHER_ROSE = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "wither_rose");

    public SoulSoil(SoundType sound) {
        super(Material.GROUND, MapColor.BROWN, 0.5F, 0.5F, sound);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
        if (direction == EnumFacing.UP && plantable instanceof Block && WITHER_ROSE.equals(((Block) plantable).getRegistryName())
                && ModConfig.changes.enabled && ModConfig.changes.witherRoseSoulSoil) {
            return true;
        }
        return super.canSustainPlant(state, world, pos, direction, plantable);
    }
}
