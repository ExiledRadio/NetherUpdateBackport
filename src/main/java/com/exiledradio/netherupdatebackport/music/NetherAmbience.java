package com.exiledradio.netherupdatebackport.music;

import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The five nether biomes' ambience, with 1.16.5's numbers ({@code BiomeMaker}'s {@code ambientLoopSound},
 * {@code ambientMoodSound} and {@code ambientAdditionsSound}), and the mood arithmetic of 1.16.5's
 * {@code BiomeSoundHandler}. The client plays them ({@code music.client.BiomeSounds}); this class has no client types.
 */
public final class NetherAmbience {

    /** {@code MoodSoundAmbience}: ticks of darkness before a mood sound, for every nether biome. */
    public static final int MOOD_TICK_DELAY = 6000;
    /** {@code MoodSoundAmbience}: how far from the player a mood sound's position is looked for. */
    public static final int MOOD_SEARCH_EXTENT = 8;
    /** {@code MoodSoundAmbience}: how much farther than that position the sound is played. */
    public static final double MOOD_OFFSET = 2.0D;
    /** {@code SoundAdditionsAmbience}: the chance per tick of an addition, for every nether biome. */
    public static final double ADDITIONS_CHANCE = 0.0111D;

    /** One biome's ambience. */
    public static final class Settings {
        public final NetherBiomes.Kind kind;
        public final SoundEvent loop;
        public final SoundEvent mood;
        public final int moodTickDelay;
        public final int moodSearchExtent;
        public final double moodOffset;
        public final SoundEvent additions;
        public final double additionsChance;

        Settings(NetherBiomes.Kind kind, SoundEvent loop, SoundEvent mood, SoundEvent additions) {
            this.kind = kind;
            this.loop = loop;
            this.mood = mood;
            this.moodTickDelay = MOOD_TICK_DELAY;
            this.moodSearchExtent = MOOD_SEARCH_EXTENT;
            this.moodOffset = MOOD_OFFSET;
            this.additions = additions;
            this.additionsChance = ADDITIONS_CHANCE;
        }
    }

    private static final Map<NetherBiomes.Kind, Settings> SETTINGS = new EnumMap<NetherBiomes.Kind, Settings>(NetherBiomes.Kind.class);

    private NetherAmbience() {
    }

    /** Makes the fifteen sound events ({@code ambient.<biome>.loop/mood/additions}); called while items register. */
    static void makeEvents() {
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            String p = "ambient." + kind.id + ".";
            SETTINGS.put(kind, new Settings(kind, Sounds.event(p + "loop"), Sounds.event(p + "mood"), Sounds.event(p + "additions")));
        }
    }

    /** @return a nether biome's ambience, or null (another biome, or the stream switched off) */
    @Nullable
    public static Settings of(@Nullable NetherBiomes.Kind kind) {
        return kind == null ? null : SETTINGS.get(kind);
    }

    /**
     * One tick of 1.16's mood: sky light at the sampled position lowers it a little; without sky light, block light
     * above 0 lowers it and darkness (block light 0) raises it by {@code 1 / tickDelay}. The caller plays the mood
     * sound and resets to 0 at 1 or more, and clamps it at 0 otherwise.
     */
    public static float mood(float moodiness, int skyLight, int blockLight, int tickDelay) {
        if (skyLight > 0) {
            return moodiness - (float) skyLight / 15.0F * 0.001F;
        }
        return moodiness - (float) (blockLight - 1) / (float) tickDelay;
    }
}
