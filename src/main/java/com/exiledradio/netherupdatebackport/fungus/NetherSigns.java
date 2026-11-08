package com.exiledradio.netherupdatebackport.fungus;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSign;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Crimson and warped signs, built the family's way (Village &amp; Pillage's wood signs).
 *
 * <ul>
 *   <li>The blocks extend vanilla's standing and wall signs, so placement, popping off, rotation and the
 *       outline behave as an oak sign's; they drop and are picked as their own item.</li>
 *   <li>The tile is vanilla's sign under its own registry name ({@link Tile}): everything that works on signs
 *       checks {@code instanceof TileEntitySign} (the editing screen and its packets, commands in sign text,
 *       other mods' tools). The separate class exists only so a renderer can draw the right wood.</li>
 *   <li>The item is vanilla's {@link ItemSign} with its two blocks swapped. Stacks to 16.</li>
 * </ul>
 * Hardness 1, wood sounds; unlike oak signs these never burn (nether wood).
 */
public final class NetherSigns {

    private NetherSigns() {
    }

    /** The tile behind every nether sign; the wood to draw is read from its block. */
    public static class Tile extends TileEntitySign {
    }

    public static final class Standing extends BlockStandingSign implements Waterloggable {
        public final NetherWood wood;

        Standing(NetherWood wood) {
            this.wood = wood;
            setHardness(1.0F);
            setSoundType(SoundType.WOOD);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new Tile();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return FungusContent.signItem(this.wood);
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(FungusContent.signItem(this.wood));
        }
    }

    public static final class Wall extends BlockWallSign implements Waterloggable {
        public final NetherWood wood;

        Wall(NetherWood wood) {
            this.wood = wood;
            setHardness(1.0F);
            setSoundType(SoundType.WOOD);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new Tile();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return FungusContent.signItem(this.wood);
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(FungusContent.signItem(this.wood));
        }
    }

    /** @return the wood of a nether sign block, or null */
    public static NetherWood woodOf(net.minecraft.block.Block block) {
        if (block instanceof Standing) {
            return ((Standing) block).wood;
        }
        if (block instanceof Wall) {
            return ((Wall) block).wood;
        }
        return null;
    }

    /** The held sign: on top of a block it stands facing the player, on a side it hangs; then the editor opens. */
    public static final class SignItem extends ItemSign {
        final NetherWood wood;

        SignItem(NetherWood wood) {
            this.wood = wood;
        }

        @Override
        public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                          EnumFacing facing, float hitX, float hitY, float hitZ) {
            IBlockState clicked = world.getBlockState(pos);
            boolean replaceable = clicked.getBlock().isReplaceable(world, pos);
            if (facing == EnumFacing.DOWN || !(clicked.getMaterial().isSolid() || replaceable)
                    || (replaceable && facing != EnumFacing.UP)) {
                return EnumActionResult.FAIL;
            }
            pos = pos.offset(facing);
            ItemStack stack = player.getHeldItem(hand);
            Standing standing = FungusContent.standingSign(this.wood);
            Wall wall = FungusContent.wallSign(this.wood);
            if (!player.canPlayerEdit(pos, facing, stack) || !standing.canPlaceBlockAt(world, pos)) {
                return EnumActionResult.FAIL;
            }
            if (world.isRemote) {
                return EnumActionResult.SUCCESS;
            }
            pos = replaceable ? pos.down() : pos;
            if (facing == EnumFacing.UP) {
                int rotation = MathHelper.floor((double) ((player.rotationYaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
                world.setBlockState(pos, standing.getDefaultState().withProperty(BlockStandingSign.ROTATION, rotation), 11);
            } else {
                world.setBlockState(pos, wall.getDefaultState().withProperty(BlockWallSign.FACING, facing), 11);
            }
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntitySign && !ItemBlock.setTileEntityNBT(world, player, pos, stack)) {
                player.openEditSign((TileEntitySign) tile);
            }
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
            }
            stack.shrink(1);
            return EnumActionResult.SUCCESS;
        }
    }
}
