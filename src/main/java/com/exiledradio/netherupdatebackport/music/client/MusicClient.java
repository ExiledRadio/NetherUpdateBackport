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
 * Client side of stream M: hands 1.16's music choice to Backport Core's music owner ({@link Music116Step}, Core's
 * ticker plays it) and runs the nether biomes' ambience ({@link BiomeSounds}).
 */
public final class MusicClient {

    private BiomeSounds ambience;
    private int lastDimension;
    private boolean inWorld;

    private MusicClient() {
    }

    public static void init() {
        if (MusicContent.enabled()) {
            MinecraftForge.EVENT_BUS.register(new MusicClient());
            if (ModConfig.music.biomeMusic) {
                com.exiledradio.backportcore.presentation.client.GameMusic.add("1.16", "nether biomes, gentler switching", new Music116Step());
            }
        }
    }

    /** 1.16's {@code ClientPlayNetHandler.handleRespawn}: a new dimension stops the music (and 5 s more to wait). */
    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !ModConfig.music.biomeMusic) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            int dimension = mc.player.dimension;
            if (this.inWorld && dimension != this.lastDimension
                    && mc.getMusicTicker() instanceof com.exiledradio.backportcore.presentation.client.GameMusic.CoreTicker) {
                ((com.exiledradio.backportcore.presentation.client.GameMusic.CoreTicker) mc.getMusicTicker()).stopMusic();
            }
            this.lastDimension = dimension;
            this.inWorld = true;
        } else {
            this.inWorld = false;
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
