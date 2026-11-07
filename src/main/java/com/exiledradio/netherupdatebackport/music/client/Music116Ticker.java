package com.exiledradio.netherupdatebackport.music.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.music.MusicChoice;
import com.exiledradio.netherupdatebackport.music.MusicPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * The game's music ticker, replaced with 1.16.5's: {@link MusicPlayer} decides when tracks start and stop,
 * {@link MusicChoice} which track the situation asks for; this class only reads the situation from the client and
 * plays through the sound handler.
 *
 * <p>Installed in place of vanilla's {@code Minecraft.musicTicker} once, on the first client tick, and only if that
 * field still holds vanilla's own class: a ticker another mod put there is left alone (logged).
 */
final class Music116Ticker extends MusicTicker implements MusicPlayer.Speaker {

    private final Minecraft mc;
    private final MusicPlayer player;
    private int lastDimension;
    private boolean inWorld;

    private Music116Ticker(Minecraft mc) {
        super(mc);
        this.mc = mc;
        this.player = new MusicPlayer(new Random(), this);
    }

    /** @return true if the ticker was installed */
    static boolean install(Minecraft mc) {
        try {
            Field field = ReflectionHelper.findField(Minecraft.class, "musicTicker", "mcMusicTicker", "field_147126_aw");
            Object old = field.get(mc);
            if (old == null || old.getClass() != MusicTicker.class) {
                NetherUpdateBackport.LOGGER.warn("Another mod has replaced the music ticker ({}); 1.16's music rules are not applied.",
                        old == null ? "null" : old.getClass().getName());
                return false;
            }
            field.set(mc, new Music116Ticker(mc));
            return true;
        } catch (Exception e) {
            NetherUpdateBackport.LOGGER.warn("Could not replace the music ticker; 1.16's music rules are not applied.", e);
            return false;
        }
    }

    @Override
    public void update() {
        // 1.16's ClientPlayNetHandler.handleRespawn: a new dimension stops the music (and 5 s more to wait).
        if (this.mc.player != null) {
            int dimension = this.mc.player.dimension;
            if (this.inWorld && dimension != this.lastDimension) {
                this.player.stop();
            }
            this.lastDimension = dimension;
            this.inWorld = true;
        } else {
            this.inWorld = false;
        }
        this.player.tick(situation());
    }

    /** Vanilla calls this for nothing in 1.12.2; another mod might, so it plays through the same player. */
    @Override
    public void playMusic(MusicTicker.MusicType type) {
        this.player.start(MusicChoice.legacy(type.getMusicLocation(), type.getMinDelay(), type.getMaxDelay()));
    }

    /** 1.16.5's {@code Minecraft.getSituationalMusic}, with Forge 1.12.2's per-dimension music first. */
    private MusicChoice.Track situation() {
        if (this.mc.currentScreen instanceof GuiWinGame) {
            return MusicChoice.CREDITS;
        }
        if (this.mc.player == null || this.mc.world == null) {
            return MusicChoice.MENU;
        }
        MusicTicker.MusicType own = this.mc.world.provider.getMusicType();
        MusicChoice.Track ownTrack = own == null ? null : MusicChoice.legacy(own.getMusicLocation(), own.getMinDelay(), own.getMaxDelay());
        WorldProvider provider = this.mc.player.world.provider;
        MusicChoice.Dimension dimension = provider instanceof WorldProviderHell || provider.getDimensionType() == DimensionType.NETHER
                ? MusicChoice.Dimension.NETHER
                : provider instanceof WorldProviderEnd ? MusicChoice.Dimension.END : MusicChoice.Dimension.OTHER;
        boolean endBoss = dimension == MusicChoice.Dimension.END && this.mc.ingameGUI.getBossOverlay().shouldPlayEndBossMusic();
        boolean creativeFlying = this.mc.player.capabilities.isCreativeMode && this.mc.player.capabilities.allowFlying;
        NetherBiomes.Kind biome = NetherBiomes.at(this.mc.world, new BlockPos(this.mc.player));
        return MusicChoice.inWorld(ownTrack, dimension, endBoss, creativeFlying, biome);
    }

    // ------------------------------------------------------------------------------------------
    // MusicPlayer.Speaker
    // ------------------------------------------------------------------------------------------

    @Override
    public Object play(SoundEvent event) {
        ISound sound = PositionedSoundRecord.getMusicRecord(event);
        SoundHandler handler = this.mc.getSoundHandler();
        SoundEventAccessor accessor = handler.getAccessor(event.getSoundName());
        // 1.16 plays nothing for an event without sounds (warped forest's), and says nothing about it.
        if (accessor != null && accessor.getWeight() > 0) {
            handler.playSound(sound);
        }
        return sound;
    }

    @Override
    public void stop(Object handle) {
        this.mc.getSoundHandler().stopSound((ISound) handle);
    }

    @Override
    public boolean isPlaying(Object handle) {
        return this.mc.getSoundHandler().isSoundPlaying((ISound) handle);
    }
}
