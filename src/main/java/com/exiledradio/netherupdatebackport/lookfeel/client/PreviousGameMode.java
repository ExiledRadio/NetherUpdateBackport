package com.exiledradio.netherupdatebackport.lookfeel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The client's previous game mode, as 1.16's {@code MultiPlayerGameMode.previousLocalPlayerMode}: the mode the
 * player had before the last change. 1.16 also gets it from the server on login (the player's saved
 * {@code previousPlayerGameType}); 1.12.2's server sends nothing, so it is known from the first change seen in
 * this session on, and unknown ({@code null}) before.
 */
@SideOnly(Side.CLIENT)
public final class PreviousGameMode {

    @Nullable
    private static GameType current;
    @Nullable
    private static GameType previous;

    @Nullable
    public static GameType get() {
        return previous;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.playerController == null) {
            current = null;
            previous = null;
            return;
        }
        GameType now = mc.playerController.getCurrentGameType();
        if (current != null && now != current) {
            previous = current;
        }
        current = now;
    }
}
