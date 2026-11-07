package com.exiledradio.netherupdatebackport.stone;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The basalt generator: lava over soul soil, touching blue ice, turns to basalt (1.16).
 *
 * <p>1.16's rule, from {@code FlowingFluidBlock.shouldSpreadLiquid}: a lava block (source or flowing)
 * whose block below is soul soil and which has blue ice above it or on any side becomes basalt, with
 * lava's fizz. Vanilla checks it when the lava is placed and when a neighbor changes; here the same two
 * moments come from Forge's neighbor-notify event (fired for the changed block, so the lava itself
 * and every block beside it are checked) without touching vanilla's liquid code.
 *
 * <p>Positions are collected during the tick and converted at its end, never inside the event: that
 * event fires from inside another block change, and replacing the block that is busy notifying would
 * hand vanilla a stale state.
 *
 * <p>Needs soul soil (stream L, looked up by name) and blue ice (whichever mod provides it; see
 * {@link Owners#BLUE_ICE}). Without either it does nothing.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class BasaltGenerator {

    private static final Map<World, Set<BlockPos>> PENDING = new HashMap<World, Set<BlockPos>>();

    /** The sides blue ice may be on: the opposites of 1.16's flow directions (up and the four horizontals). */
    private static final EnumFacing[] SIDES = {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST};

    private static Block soulSoil;
    private static Block blueIce;
    private static boolean looked;

    private BasaltGenerator() {
    }

    @SubscribeEvent
    public static void neighborNotify(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote || StoneContent.basalt == null || !ModConfig.stone.basaltGenerator) {
            return;
        }
        Set<BlockPos> set = PENDING.get(world);
        if (set == null) {
            set = new LinkedHashSet<BlockPos>();
            PENDING.put(world, set);
        }
        BlockPos pos = event.getPos().toImmutable();
        set.add(pos);
        for (EnumFacing side : EnumFacing.values()) {
            set.add(pos.offset(side));
        }
    }

    @SubscribeEvent
    public static void unload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        PENDING.remove(event.getWorld());
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        Set<BlockPos> set = PENDING.remove(event.world);
        if (set == null || !lookUp()) {
            return;
        }
        for (BlockPos pos : set) {
            if (event.world.isBlockLoaded(pos) && converts(event.world, pos)) {
                event.world.setBlockState(pos, StoneContent.basalt.getDefaultState());
                fizz(event.world, pos);
            }
        }
    }

    /** @return true if the block at pos is lava that 1.16 would turn to basalt */
    static boolean converts(World world, BlockPos pos) {
        if (!lookUp() || world.getBlockState(pos).getMaterial() != Material.LAVA
                || world.getBlockState(pos.down()).getBlock() != soulSoil) {
            return false;
        }
        for (EnumFacing side : SIDES) {
            if (world.getBlockState(pos.offset(side)).getBlock() == blueIce) {
                return true;
            }
        }
        return false;
    }

    private static boolean lookUp() {
        if (!looked) {
            soulSoil = Owners.ourBlock("soul_soil");
            blueIce = Owners.first(Owners.BLUE_ICE);
            looked = true;
        }
        return soulSoil != null && blueIce != null;
    }

    /** Vanilla's lava fizz: the sound and eight large smoke particles. */
    private static void fizz(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + 0.5, pos.getY() + 1.0,
                    pos.getZ() + 0.5, 8, 0.25, 0.0, 0.25, 0.0);
        }
    }
}
