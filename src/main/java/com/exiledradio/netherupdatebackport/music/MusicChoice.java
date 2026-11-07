package com.exiledradio.netherupdatebackport.music;

import com.exiledradio.netherupdatebackport.biome.NetherBiomes;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Which music the game asks for: 1.16.5's {@code Minecraft.getSituationalMusic}, {@code BackgroundMusicTracks} and
 * the nether biomes' {@code backgroundMusic}, in one place.
 *
 * <p>This is the music-choosing logic of the mod and nothing else: it reads no game state (the client fills in the
 * situation) and plays nothing ({@link MusicPlayer} does). The family's presentation plan moves the music pool and
 * the shared music hooks into Backport Core later; this class is the part that moves. Its API:
 * <ul>
 *   <li>{@link Track}: what a situation asks for (1.16's {@code BackgroundMusicSelector}).</li>
 *   <li>{@link #MENU}, {@link #CREDITS}: the two situations outside a world.</li>
 *   <li>{@link #inWorld}: the rules inside a world.</li>
 *   <li>{@link #nether}: a nether biome's track.</li>
 * </ul>
 *
 * <p>Safe on a dedicated server (no client types), so the self-test checks the rules there.
 */
public final class MusicChoice {

    /** 1.16's {@code BackgroundMusicSelector}: a sound event, the wait before it, and whether it cuts off other music. */
    public static final class Track {
        public final SoundEvent event;
        /** Ticks to wait, at least, after a track ends before this one starts. */
        public final int minDelay;
        /** Ticks to wait, at most. */
        public final int maxDelay;
        /** True if this track stops a different one that is playing (menu, credits, the End); false lets it finish. */
        public final boolean replaceCurrent;

        public Track(SoundEvent event, int minDelay, int maxDelay, boolean replaceCurrent) {
            this.event = event;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
            this.replaceCurrent = replaceCurrent;
        }

        /** @return the event's name, which is how 1.16 tells two tracks apart */
        @Nullable
        public ResourceLocation name() {
            return this.event.getRegistryName();
        }

        @Override
        public String toString() {
            return name() + "[" + this.minDelay + "-" + this.maxDelay + (this.replaceCurrent ? ", replaces" : "") + "]";
        }
    }

    /** Where the player is, as far as music cares. */
    public enum Dimension {
        NETHER, END, OTHER
    }

    // 1.16.5's BackgroundMusicTracks. The events are 1.12.2's own, which play the same tracks.
    public static final Track MENU = new Track(SoundEvents.MUSIC_MENU, 20, 600, true);
    /** 20w17a: creative music no longer cuts off the survival track (12000-24000, was 1200-3600 and replacing). */
    public static final Track CREATIVE = game(SoundEvents.MUSIC_CREATIVE);
    public static final Track CREDITS = new Track(SoundEvents.MUSIC_CREDITS, 0, 0, true);
    public static final Track END_BOSS = new Track(SoundEvents.MUSIC_DRAGON, 0, 0, true);
    public static final Track END = new Track(SoundEvents.MUSIC_END, 6000, 24000, true);
    public static final Track GAME = game(SoundEvents.MUSIC_GAME);

    private static final Map<NetherBiomes.Kind, Track> NETHER = new EnumMap<NetherBiomes.Kind, Track>(NetherBiomes.Kind.class);

    private MusicChoice() {
    }

    /** 1.16's {@code BackgroundMusicTracks.createGameMusic}: 10 to 20 minutes apart, never cutting off another track. */
    public static Track game(SoundEvent event) {
        return new Track(event, 12000, 24000, false);
    }

    /** A track a dimension of another mod asks for (Forge's {@code WorldProvider.getMusicType}), with 1.12.2's rule: it replaces. */
    public static Track legacy(SoundEvent event, int minDelay, int maxDelay) {
        return new Track(event, minDelay, maxDelay, true);
    }

    /** Called by {@link MusicContent} once the nether's music events exist. */
    static void setNether(NetherBiomes.Kind kind, SoundEvent event) {
        NETHER.put(kind, game(event));
    }

    /**
     * @return a nether biome's track (1.16: {@code music.nether.<biome>}, game-music delays), or null while the
     * stream's sound events are not registered. Warped forest's event has no sounds in 1.16, so it stays silent.
     */
    @Nullable
    public static Track nether(NetherBiomes.Kind kind) {
        return NETHER.get(kind);
    }

    /**
     * The music for a player in a world (1.16.5's {@code getSituationalMusic} past the credits and menu cases).
     *
     * @param dimensionOwn   what the dimension itself asks for (another mod's dimension), or null; it wins, as in
     *                       Forge 1.12.2
     * @param dimension      the player's dimension
     * @param endBoss        the boss bar asks for the dragon music (1.12.2's {@code shouldPlayEndBossMusic})
     * @param creativeFlying creative mode with flying allowed
     * @param biome          the nether biome at the player's feet ({@link NetherBiomes#at}), or null for any other
     *                       biome
     */
    public static Track inWorld(@Nullable Track dimensionOwn, Dimension dimension, boolean endBoss, boolean creativeFlying,
                                @Nullable NetherBiomes.Kind biome) {
        if (dimensionOwn != null) {
            return dimensionOwn;
        }
        if (dimension == Dimension.END) {
            return endBoss ? END_BOSS : END;
        }
        // 1.16: in the nether the biome always decides; elsewhere creative flying gets the creative tracks first.
        if (dimension != Dimension.NETHER && creativeFlying) {
            return CREATIVE;
        }
        if (biome == null && dimension == Dimension.NETHER) {
            // A nether biome of another mod: 1.16 would play its own music or the overworld's; the nether's
            // default biome's music is the closer answer in a 1.12.2 pack.
            biome = NetherBiomes.Kind.NETHER_WASTES;
        }
        Track track = biome == null ? null : nether(biome);
        return track == null ? GAME : track;
    }
}
