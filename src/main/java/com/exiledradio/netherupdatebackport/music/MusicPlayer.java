package com.exiledradio.netherupdatebackport.music;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.16.5's {@code MusicTicker}: when the next track starts, and when a playing one is cut off.
 *
 * <p>The game's side (starting and stopping a sound) is a {@link Speaker}, so this runs without a client: the client's
 * ticker ({@code music.client.Music116Ticker}) is a speaker over the sound handler, the self-test a fake one.
 *
 * <p>What changed from 1.12.2's ticker: a different track cuts off the playing one only if it {@link
 * MusicChoice.Track#replaceCurrent replaces}; otherwise the playing one finishes and the new one follows after its
 * delay. So switching game modes (20w17a) or walking into another nether biome no longer stops the music.
 */
public final class MusicPlayer {

    /** Plays music for a {@link MusicPlayer}. */
    public interface Speaker {
        /**
         * Starts a track and returns its handle. An event with no sounds (warped forest's) returns a handle that never
         * plays, as 1.16 does with its empty sound.
         */
        Object play(SoundEvent event);

        void stop(Object handle);

        boolean isPlaying(Object handle);
    }

    private final Random random;
    private final Speaker speaker;
    @Nullable
    private Object current;
    @Nullable
    private ResourceLocation currentName;
    private int nextSongDelay = 100;

    public MusicPlayer(Random random, Speaker speaker) {
        this.random = random;
        this.speaker = speaker;
    }

    /** One game tick (1.16's {@code tick()}), with the music the situation asks for. */
    public void tick(MusicChoice.Track track) {
        if (this.current != null) {
            if (!same(track.name(), this.currentName) && track.replaceCurrent) {
                this.speaker.stop(this.current);
                this.nextSongDelay = nextInt(0, track.minDelay / 2);
            }
            if (!this.speaker.isPlaying(this.current)) {
                this.current = null;
                this.currentName = null;
                this.nextSongDelay = Math.min(this.nextSongDelay, nextInt(track.minDelay, track.maxDelay));
            }
        }
        this.nextSongDelay = Math.min(this.nextSongDelay, track.maxDelay);
        if (this.current == null && this.nextSongDelay-- <= 0) {
            start(track);
        }
    }

    /** Starts a track now (1.16's {@code startPlaying}). */
    public void start(MusicChoice.Track track) {
        this.currentName = track.name();
        this.current = this.speaker.play(track.event);
        this.nextSongDelay = Integer.MAX_VALUE;
    }

    /** Stops the music (1.16's {@code stopPlaying}, which changing dimension calls) and waits 5 s longer for the next. */
    public void stop() {
        if (this.current != null) {
            this.speaker.stop(this.current);
            this.current = null;
            this.currentName = null;
        }
        this.nextSongDelay += 100;
    }

    /** @return true if this track is the one playing (1.16's {@code isPlayingMusic}) */
    public boolean isPlaying(MusicChoice.Track track) {
        return this.current != null && same(track.name(), this.currentName);
    }

    /** @return the handle of the track playing, or null */
    @Nullable
    public Object current() {
        return this.current;
    }

    /** @return ticks until the next track may start */
    public int delay() {
        return this.nextSongDelay;
    }

    private int nextInt(int min, int max) {
        return min >= max ? min : this.random.nextInt(max - min + 1) + min;
    }

    private static boolean same(@Nullable ResourceLocation a, @Nullable ResourceLocation b) {
        return a == null ? b == null : a.equals(b);
    }
}
