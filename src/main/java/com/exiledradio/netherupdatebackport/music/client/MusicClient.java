package com.exiledradio.netherupdatebackport.music.client;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.music.MusicContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Client side of stream M: installs 1.16's music ticker ({@link Music116Ticker}) and runs the nether biomes'
 * ambience ({@link BiomeSounds}).
 */
public final class MusicClient {

    private boolean tickerTried;
    private BiomeSounds ambience;

    private MusicClient() {
    }

    public static void init() {
        if (MusicContent.enabled()) {
            MinecraftForge.EVENT_BUS.register(new MusicClient());
        }
    }

    /** The ticker goes in on the first client tick, after every mod has loaded (so one that replaced it is seen). */
    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || this.tickerTried) {
            return;
        }
        this.tickerTried = true;
        if (ModConfig.music.biomeMusic) {
            Music116Ticker.install(Minecraft.getMinecraft());
        }
    }

    /** 1.16 ticks the ambience at the end of the client player's own tick. */
    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.END || event.player != mc.player) {
            return;
        }
        if (this.ambience == null || this.ambience.player() != event.player) {
            if (this.ambience != null) {
                this.ambience.retire();
            }
            this.ambience = new BiomeSounds(event.player, mc.getSoundHandler());
        }
        this.ambience.tick(ModConfig.music.biomeAmbience);
    }

    /**
     * 1.16's nether biomes have their own mood sounds instead of the cave sounds 1.12.2's client world plays anywhere
     * dark: those are dropped while a nether biome's ambience plays.
     */
    @SubscribeEvent
    public void playSound(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound == null || this.ambience == null || this.ambience.settings() == null
                || this.ambience.player() != Minecraft.getMinecraft().player) {
            return;
        }
        if (sound.getCategory() == SoundCategory.AMBIENT && SoundEvents.AMBIENT_CAVE.getSoundName().equals(sound.getSoundLocation())) {
            event.setResultSound(null);
        }
    }
}
