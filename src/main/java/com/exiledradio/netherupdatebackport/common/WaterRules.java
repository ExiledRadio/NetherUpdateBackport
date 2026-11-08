package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import git.jbredwards.fluidlogged_api.api.event.FluidloggableEvent;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.16.5's waterlogging rules for this mod's blocks, over Fluidlogged API's own.
 *
 * <p>The blocks 1.16 made waterloggable (chain, soul lantern, soul campfire, the slabs, stairs and walls, the crimson
 * and warped fences, trapdoors and signs) implement Backport Core's {@link Waterloggable}. Fluidlogged API also
 * waterlogs by its own list: every block extending a vanilla stair, slab, wall, fence, trapdoor, sign, fence gate,
 * door, button or pressure plate, with any fluid, and with flowing fluid too (its {@code nonSourceFluidlogging}).
 * That list is asked before a block's own answers, so for this mod's blocks this handler, asked first, puts 1.16.5's
 * rules back:
 * <ul>
 *   <li>Only water, and only a water <i>source</i> (1.16.5's {@code IWaterLoggable.canPlaceLiquid}: {@code fluid ==
 *       Fluids.WATER}, never {@code FLOWING_WATER}). Flowing water does not run into a fence or a stair; a block
 *       placed into flowing water replaces it; a bucket or a source still waterlogs.</li>
 *   <li>Nothing else of ours holds water: fence gates, doors, buttons, pressure plates, the torch, plants, fire.</li>
 * </ul>
 * Everything else (sources, buckets, flow out of a waterlogged block, drowning, boats) is Fluidlogged API's.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class WaterRules {

    private WaterRules() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluidloggable(FluidloggableEvent event) {
        if (!ours(event.state)) {
            return;
        }
        if (!allowed(event.state, event.fluidState)) {
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    /** @return true for a block registered by this mod */
    static boolean ours(IBlockState state) {
        ResourceLocation name = state.getBlock().getRegistryName();
        return name != null && NetherUpdateBackport.MOD_ID.equals(name.getNamespace());
    }

    /**
     * @return whether 1.16.5 lets this fluid into this state of one of our blocks; the block's own {@link
     * Waterloggable#canHoldWater} (a double slab) is asked afterwards by Fluidlogged API as usual
     */
    public static boolean allowed(IBlockState state, FluidState fluid) {
        Block block = state.getBlock();
        if (!(block instanceof Waterloggable)) {
            return false;
        }
        return fluid.isEmpty() || fluid.isSource() && FluidloggedUtils.isCompatibleFluid(fluid.getFluid(), FluidRegistry.WATER);
    }
}
