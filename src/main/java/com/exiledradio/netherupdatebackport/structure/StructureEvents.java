package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.netherupdatebackport.common.Triggers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The bastion advancements' events (stream P's JSON): {@code enter_bastion} for <i>Those Were the Days</i>, and the
 * {@code loot_bastion_*} events for <i>War Pigs</i>, fired by the loot tables' {@code advancement_event} condition.
 *
 * <p>1.16 tests its location criterion once a second ({@code ServerPlayerEntity.tick}, every 20 ticks): inside a
 * piece of a bastion, not just its outer box ({@code LocationPredicate}'s {@code feature} asks
 * {@code getStructureAt(pos, true, ...)}). Same here.
 */
public final class StructureEvents {

    /** The last different events fired, newest last, for the self-test. */
    static final List<String> FIRED = Collections.synchronizedList(new ArrayList<String>());

    /** Registered by {@link #init}; public for the self-test's own tick. */
    public StructureEvents() {
    }

    /** @return the last events fired ("player event"), for the self-test */
    public static List<String> firedForTest() {
        synchronized (FIRED) {
            return new ArrayList<String>(FIRED);
        }
    }

    static void init() {
        if (StructureContent.bastions()) {
            MinecraftForge.EVENT_BUS.register(new StructureEvents());
        }
    }

    static void fire(EntityPlayer player, String event) {
        String key = player.getName() + " " + event;
        synchronized (FIRED) {
            FIRED.remove(key);
            FIRED.add(key);
            if (FIRED.size() > 64) {
                FIRED.remove(0);
            }
        }
        Triggers.fire(player, event);
    }

    /** @return true if the position is inside one of the pieces of a bastion laid out in this world */
    public static boolean inBastion(net.minecraft.world.World world, BlockPos pos) {
        ScatteredStructure bastions = ScatteredStructure.of(world, Structures.BASTION_REMNANT);
        return bastions != null && bastions.isInsideStructure(pos);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || event.player.ticksExisted % 20 != 0
                || event.player.world.provider.getDimension() != -1) {
            return;
        }
        if (inBastion(event.player.world, new BlockPos(event.player))) {
            fire(event.player, "enter_bastion");
        }
    }
}
