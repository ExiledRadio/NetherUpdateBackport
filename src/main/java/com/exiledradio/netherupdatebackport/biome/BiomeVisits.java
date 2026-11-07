package com.exiledradio.netherupdatebackport.biome;

import com.exiledradio.netherupdatebackport.common.Triggers;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Fires {@code visit_<biome>} for <i>Hot Tourist Destinations</i> (written by stream P) when a player stands in
 * one of the four new nether biomes. Nether wastes is vanilla's {@code minecraft:hell}, which the advancement
 * checks with a plain location criterion. The four are events rather than location criteria because a location
 * criterion naming a biome that isn't registered (biomes switched off) fails the whole advancement.
 */
@Mod.EventBusSubscriber(modid = com.exiledradio.netherupdatebackport.NetherUpdateBackport.MOD_ID)
public final class BiomeVisits {

    private BiomeVisits() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)
                || event.player.dimension != -1 || event.player.ticksExisted % 20 != 0) {
            return;
        }
        NetherBiomes.Kind kind = NetherBiomes.at(event.player.world, new BlockPos(event.player));
        if (kind != null && kind != NetherBiomes.Kind.NETHER_WASTES) {
            Triggers.fire(event.player, "visit_" + kind.id);
        }
    }
}
