package com.exiledradio.netherupdatebackport.soul;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The soul lantern: a lantern giving light 10, standing on a block or hanging under one.
 *
 * <p>1.16's {@code LanternBlock}: placed hanging when put against a ceiling (or when nothing below holds it),
 * standing otherwise; it needs a centre to rest on or hang from (a full face, a fence or wall post, a chain's
 * end) and breaks when that goes. Its own block, not Village &amp; Pillage's lantern (that is 1.14's).
 */
public final class BlockSoulLantern extends Block implements Waterloggable {

    public static final PropertyBool HANGING = PropertyBool.create("hanging");

    private static final AxisAlignedBB STANDING = new AxisAlignedBB(5 / 16.0, 0, 5 / 16.0, 11 / 16.0, 9 / 16.0, 11 / 16.0);
    private static final AxisAlignedBB HUNG = new AxisAlignedBB(5 / 16.0, 1 / 16.0, 5 / 16.0, 11 / 16.0, 10 / 16.0, 11 / 16.0);

    BlockSoulLantern(SoundType sound) {
        super(Material.IRON, MapColor.IRON);
        setHardness(3.5F);
        setResistance(3.5F * 5.0F / 3.0F);
        setSoundType(sound);
        setLightLevel(10 / 15.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(HANGING, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HANGING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HANGING, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HANGING) ? 1 : 0;
    }

    /** @return true if the block on that side gives a lantern something to rest on or hang from */
    static boolean supports(IBlockAccess world, BlockPos pos, EnumFacing towards) {
        BlockPos at = pos.offset(towards);
        BlockFaceShape shape = world.getBlockState(at).getBlockFaceShape(world, at, towards.getOpposite());
        return shape == BlockFaceShape.SOLID || shape == BlockFaceShape.CENTER || shape == BlockFaceShape.CENTER_BIG
                || shape == BlockFaceShape.CENTER_SMALL || shape == BlockFaceShape.MIDDLE_POLE_THICK
                || shape == BlockFaceShape.MIDDLE_POLE || shape == BlockFaceShape.MIDDLE_POLE_THIN;
    }

    static boolean canStay(IBlockAccess world, BlockPos pos, boolean hanging) {
        return supports(world, pos, hanging ? EnumFacing.UP : EnumFacing.DOWN);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && (canStay(world, pos, false) || canStay(world, pos, true));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        boolean hanging = facing == EnumFacing.DOWN ? canStay(world, pos, true) || !canStay(world, pos, false) : !canStay(world, pos, false);
        return getDefaultState().withProperty(HANGING, hanging);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!canStay(world, pos, state.getValue(HANGING))) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(HANGING) ? HUNG : STANDING;
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
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
