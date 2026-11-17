package com.exiledradio.netherupdatebackport.changes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.16's redstone dot and cross (20w18a, 20w21a).
 *
 * <p>In 1.16 an unconnected redstone wire is a cross, which powers all four sides (1.12.2's lone dot does
 * the same); using it makes a dot, which powers only the block below; using the dot makes a cross again. Any
 * redstone wire powers a side exactly when it points into it ({@code getSignal}: the side's connection, with
 * a lone connection drawn as a line through), so corners and T-junctions now power the blocks they point into.
 *
 * <p>1.12.2's wire keeps all 16 metadata values for power, so the dot is this block: a
 * {@code BlockRedstoneWire} of its own, which exists only while unconnected. It turns back into vanilla's wire
 * when it is used, when anything connects to it, or when the setting is off. Vanilla's wire stays
 * {@code minecraft:redstone_wire}, so no world, contraption or mod sees anything but power changes.
 *
 * <p>The sides rule and the cross drawing are patch P11 ({@code ChangeHooks.weakPower}, {@code actualState}).
 */
public final class RedstoneDot extends BlockRedstoneWire {

    static RedstoneDot dot;

    public RedstoneDot() {
        this.setHardness(0.0F);
        this.setSoundType(SoundType.STONE);
        this.disableStats();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        // A dot never shows a connection: one turns it back into vanilla's wire.
        return state;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote && this.canPlaceBlockAt(world, pos) && (!ChangeHooks.redstoneActive() || connected(world, pos) != 0)) {
            world.setBlockState(pos, Blocks.REDSTONE_WIRE.getDefaultState().withProperty(POWER, state.getValue(POWER)), 3);
            return;
        }
        super.neighborChanged(state, world, pos, block, fromPos);
    }

    // ---- 1.16's getSignal ------------------------------------------------------------------------------------

    /**
     * 1.16.5's {@code RedstoneWireBlock.getSignal}: up and the block below as before, the block above never, and
     * a side only when the wire points into it. A dot points nowhere.
     *
     * <p>No wire gives power while any wire works out its own: vanilla's wire switches off its single instance
     * for that, so the dot's instance is asked too, or power would pass wire, block, wire between the two.
     */
    public static int weakPower(BlockRedstoneWire wire, IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (!wire.canProvidePower(state) || !Blocks.REDSTONE_WIRE.canProvidePower(state) || dot != null && !dot.canProvidePower(state)) {
            return 0;
        }
        int power = state.getValue(POWER);
        if (power == 0 || side == EnumFacing.DOWN) {
            return 0;
        }
        if (side == EnumFacing.UP) {
            return power;
        }
        if (wire instanceof RedstoneDot) {
            return 0;
        }
        int c = expand(connected(world, pos));
        return (c & bit(side.getOpposite())) != 0 ? power : 0;
    }

    /** @return the horizontal sides a wire here connects to, 1.12.2's {@code isPowerSourceAt}, as bits */
    public static int connected(IBlockAccess world, BlockPos pos) {
        int bits = 0;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            if (isPowerSourceAt(world, pos, f)) {
                bits |= bit(f);
            }
        }
        return bits;
    }

    /**
     * 1.16.5's {@code getConnectionState}: with no north-south connection both east and west are added, with no
     * east-west one both north and south. A lone connection becomes a line; none, a cross.
     */
    static int expand(int bits) {
        int ns = bit(EnumFacing.NORTH) | bit(EnumFacing.SOUTH);
        int ew = bit(EnumFacing.EAST) | bit(EnumFacing.WEST);
        int out = bits;
        if ((bits & ns) == 0) {
            out |= ew;
        }
        if ((bits & ew) == 0) {
            out |= ns;
        }
        return out;
    }

    static int bit(EnumFacing f) {
        return 1 << f.getHorizontalIndex();
    }

    /** 1.12.2's private {@code BlockRedstoneWire.isPowerSourceAt}, unchanged. */
    private static boolean isPowerSourceAt(IBlockAccess world, BlockPos pos, EnumFacing side) {
        BlockPos at = pos.offset(side);
        IBlockState state = world.getBlockState(at);
        boolean solid = state.isNormalCube();
        boolean solidAbove = world.getBlockState(pos.up()).isNormalCube();
        if (!solidAbove && solid && canConnectUpwardsTo(world, at.up())) {
            return true;
        }
        if (canConnectTo(state, side, world, pos)) {
            return true;
        }
        if (state.getBlock() == Blocks.POWERED_REPEATER && state.getValue(BlockRedstoneDiode.FACING) == side) {
            return true;
        }
        return !solid && canConnectUpwardsTo(world, at.down());
    }

    /** An unconnected wire's actual state gets every side "side": the cross, as 1.16 draws (and boxes) it. */
    static IBlockState crossIfLone(IBlockState state) {
        if (state.getBlock() instanceof RedstoneDot
                || !"none".equals(name(state, NORTH)) || !"none".equals(name(state, EAST))
                || !"none".equals(name(state, SOUTH)) || !"none".equals(name(state, WEST))) {
            return state;
        }
        return named(named(named(named(state, NORTH, "side"), EAST, "side"), SOUTH, "side"), WEST, "side");
    }

    // 1.12.2's EnumAttachPosition is package-private: its values are reached by name.
    private static <T extends Comparable<T>> String name(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> IBlockState named(IBlockState state, IProperty<T> property, String name) {
        for (T value : property.getAllowedValues()) {
            if (property.getName(value).equals(name)) {
                return state.withProperty(property, value);
            }
        }
        return state;
    }

    // ---- using it --------------------------------------------------------------------------------------------

    /**
     * 1.16.5's {@code RedstoneWireBlock.use}: a player who may build turns an unconnected cross into a dot and a
     * dot into a cross. Block use comes before the held item, as for any block, unless the player sneaks with
     * something in hand.
     */
    public static final class Use {
        @SubscribeEvent
        public void use(PlayerInteractEvent.RightClickBlock event) {
            RedstoneDot.use(event);
        }
    }

    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!ChangeHooks.redstoneActive() || dot == null) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block != Blocks.REDSTONE_WIRE && block != dot) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        boolean holding = !player.getHeldItemMainhand().isEmpty() || !player.getHeldItemOffhand().isEmpty();
        if (!player.capabilities.allowEdit || player.isSneaking() && holding || connected(world, pos) != 0) {
            return;
        }
        if (!world.isRemote) {
            toggle(world, pos, state);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** Cross to dot or dot to cross, keeping the power; the blocks the cross pointed into hear of it. */
    public static void toggle(World world, BlockPos pos, IBlockState state) {
        Block to = state.getBlock() == dot ? Blocks.REDSTONE_WIRE : dot;
        world.setBlockState(pos, to.getDefaultState().withProperty(POWER, state.getValue(POWER)), 3);
        // updatesOnShapeChange: all four sides change between a cross and a dot.
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos side = pos.offset(f);
            if (world.getBlockState(side).isNormalCube()) {
                world.notifyNeighborsOfStateExcept(side, to, f.getOpposite());
            }
        }
    }

    public static Block block() {
        return dot;
    }
}
