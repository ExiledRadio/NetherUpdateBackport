package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.netherupdatebackport.common.Triggers;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The respawn anchor: charged with glowstone (up to 4), it sets the player's respawn point in the nether;
 * used anywhere else, it explodes.
 *
 * <p>1.16.5's {@code RespawnAnchorBlock.use}, in order: glowstone in the hand charges it (glowstone in the off
 * hand while the main hand holds something else charges it too); an empty anchor does nothing; outside the
 * nether a charged one explodes (power 5, with fire; with water beside it or above it the blast breaks no
 * blocks, as 1.16's water-resistance trick works out to); in the nether it sets the respawn point
 * ({@link RespawnAnchors}). Light 0, 3, 7, 11, 15 and comparator output the same, by charge.
 */
public final class BlockRespawnAnchor extends Shapes.Plain {

    public static final int MAX = 4;
    public static final PropertyInteger CHARGE = PropertyInteger.create("charges", 0, MAX);

    BlockRespawnAnchor() {
        super(Material.ROCK, MapColor.BLACK, 50.0F, 1200.0F, SoundType.STONE);
        setDefaultState(this.blockState.getBaseState().withProperty(CHARGE, 0));
    }

    /** 1.16's {@code getScaledChargeLevel(state, 15)}. */
    public static int scaled(int charge) {
        return (int) Math.floor((charge / (float) MAX) * 15.0F);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CHARGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(CHARGE, Math.min(meta & 7, MAX));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(CHARGE);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        return here.getBlock() == this ? scaled(here.getValue(CHARGE)) : scaled(state.getValue(CHARGE));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return scaled(state.getValue(CHARGE));
    }

    static boolean isFuel(ItemStack stack) {
        return stack.getItem() == Item.getItemFromBlock(Blocks.GLOWSTONE);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        int charge = state.getValue(CHARGE);
        if (hand == EnumHand.MAIN_HAND && !isFuel(held) && isFuel(player.getHeldItemOffhand()) && charge < MAX) {
            return false; // the off hand's glowstone charges it next
        }
        if (isFuel(held) && charge < MAX) {
            if (!world.isRemote) {
                charge(world, pos, state);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                if (charge + 1 == MAX) {
                    Triggers.fire(player, "charge_respawn_anchor_max");
                }
            }
            return true;
        }
        if (charge == 0) {
            return false;
        }
        if (!canSetSpawn(world)) {
            if (!world.isRemote) {
                explode(world, pos);
            }
            return true;
        }
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            if (RespawnAnchors.set((EntityPlayerMP) player, pos)) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BlocksContent.ANCHOR_SET_SPAWN,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
        return true;
    }

    /** @return true in a world where an anchor sets the spawn point (the nether, as 1.16's dimension types) */
    public static boolean canSetSpawn(World world) {
        return world.provider.getDimensionType() == DimensionType.NETHER;
    }

    public static void charge(World world, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, state.withProperty(CHARGE, state.getValue(CHARGE) + 1), 3);
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BlocksContent.ANCHOR_CHARGE,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    /** 1.16's explosion: the anchor goes, then a power-5 fiery blast, harmless to blocks beside water. */
    static void explode(World world, BlockPos pos) {
        world.setBlockToAir(pos);
        boolean water = world.getBlockState(pos.up()).getMaterial() == Material.WATER;
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            water |= wouldFlow(world, pos.offset(side));
        }
        world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5.0F, !water, !water);
    }

    /** 1.16's {@code isWaterThatWouldFlow}: a water source, or flowing water of level 2+ with no water under it. */
    private static boolean wouldFlow(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() != Material.WATER) {
            return false;
        }
        if (!state.getPropertyKeys().contains(net.minecraft.block.BlockLiquid.LEVEL)) {
            return true;
        }
        int level = state.getValue(net.minecraft.block.BlockLiquid.LEVEL);
        if (level == 0) {
            return true;
        }
        // 1.12.2 counts flowing water's level down from 8 (1 = nearest the source); 1.16's "amount" is 8 - level.
        return 8 - (level & 7) >= 2 && world.getBlockState(pos.down()).getMaterial() != Material.WATER;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (state.getValue(CHARGE) == 0) {
            return;
        }
        if (rand.nextInt(100) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BlocksContent.ANCHOR_AMBIENT,
                    SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        }
        double x = pos.getX() + 0.5 + (0.5 - rand.nextDouble());
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5 + (0.5 - rand.nextDouble());
        double vy = rand.nextFloat() * 0.04;
        world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0.0, vy, 0.0);
    }
}
