package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A chain: a thin pillar along an axis (1.16's 3x3-pixel box), iron, with 1.16's chain sounds.
 *
 * <p>Its ends report a small centre, so lanterns and other blocks that hang from or stand on a thin centre
 * (vanilla's {@code CENTER_SMALL}) attach to it.
 */
public final class BlockChain extends Shapes.Pillar implements Waterloggable {

    private static final AxisAlignedBB Y = new AxisAlignedBB(6.5 / 16, 0, 6.5 / 16, 9.5 / 16, 1, 9.5 / 16);
    private static final AxisAlignedBB Z = new AxisAlignedBB(6.5 / 16, 6.5 / 16, 0, 9.5 / 16, 9.5 / 16, 1);
    private static final AxisAlignedBB X = new AxisAlignedBB(0, 6.5 / 16, 6.5 / 16, 1, 9.5 / 16, 9.5 / 16);

    BlockChain(SoundType sound) {
        super(Material.IRON, MapColor.AIR, 5.0F, 6.0F, sound);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(AXIS)) {
            case X:
                return X;
            case Z:
                return Z;
            default:
                return Y;
        }
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
        return face.getAxis() == state.getValue(AXIS) ? BlockFaceShape.CENTER_SMALL : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
