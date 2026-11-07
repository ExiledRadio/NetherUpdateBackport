package com.exiledradio.netherupdatebackport.biome;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * "Which nether biome is here": the lookup other streams use (music and ambience, the <i>Hot Tourist
 * Destinations</i> advancement), and the five biomes' 1.16 numbers.
 *
 * <p>Nether wastes is vanilla's {@code minecraft:hell} (renamed in the language file), so chunks generated before
 * this mod, and worlds with the new biomes switched off, are nether wastes everywhere. The other four are this
 * mod's {@code netherupdatebackport:crimson_forest} and so on.
 *
 * <p>Safe on both sides and with stream V switched off (then only nether wastes is ever answered).
 */
public final class NetherBiomes {

    /** The five 1.16 nether biomes, with their 1.16.5 ids, fog colors and ambient particles. */
    public enum Kind {
        NETHER_WASTES("nether_wastes", 8, 0x330808, null, 0.0F),
        SOUL_SAND_VALLEY("soul_sand_valley", 170, 0x1B4745, Particle.ASH, 0.00625F),
        CRIMSON_FOREST("crimson_forest", 171, 0x330303, Particle.CRIMSON_SPORE, 0.025F),
        WARPED_FOREST("warped_forest", 172, 0x1A051A, Particle.WARPED_SPORE, 0.01428F),
        BASALT_DELTAS("basalt_deltas", 173, 0x685F70, Particle.WHITE_ASH, 0.118093334F);

        /** 1.16's registry path ({@code minecraft:<id>} there; ours, but for nether wastes, here). */
        public final String id;
        /** 1.16.5's numeric biome id, as seed tools number them. */
        public final int vanillaId;
        /** {@code BiomeAmbience.fogColor}, 0xRRGGBB. */
        public final int fogColor;
        /** The ambient particle, or null. */
        @Nullable
        public final Particle particle;
        /** {@code ParticleEffectAmbience.probability}: the chance per sampled position per tick. */
        public final float particleChance;

        Kind(String id, int vanillaId, int fogColor, @Nullable Particle particle, float particleChance) {
            this.id = id;
            this.vanillaId = vanillaId;
            this.fogColor = fogColor;
            this.particle = particle;
            this.particleChance = particleChance;
        }

        @Nullable
        public static Kind byId(String id) {
            for (Kind k : values()) {
                if (k.id.equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }

    /** The four ambient particles 1.16's nether biomes give off. */
    public enum Particle {
        ASH, WHITE_ASH, CRIMSON_SPORE, WARPED_SPORE
    }

    private static final Map<Kind, Biome> BIOMES = new EnumMap<Kind, Biome>(Kind.class);

    private NetherBiomes() {
    }

    static void put(Kind kind, Biome biome) {
        BIOMES.put(kind, biome);
    }

    /** @return the biome of a kind: vanilla's hell for nether wastes, null for one of ours not registered */
    @Nullable
    public static Biome biome(Kind kind) {
        return kind == Kind.NETHER_WASTES ? Biomes.HELL : BIOMES.get(kind);
    }

    static Biome biomeOrHell(Kind kind) {
        Biome b = biome(kind);
        return b == null ? Biomes.HELL : b;
    }

    /** @return the nether biome a biome is, or null for any other biome (another mod's nether biomes too) */
    @Nullable
    public static Kind of(@Nullable Biome biome) {
        if (biome == null) {
            return null;
        }
        if (biome == Biomes.HELL) {
            return Kind.NETHER_WASTES;
        }
        if (biome instanceof NetherBiome) {
            return ((NetherBiome) biome).kind;
        }
        return null;
    }

    /**
     * @return the nether biome at a position, or null where the biome is none of the five (outside the nether,
     * or another mod's biome). Reads the chunk's stored biome when it is loaded, as {@code World.getBiome} does.
     */
    @Nullable
    public static Kind at(World world, BlockPos pos) {
        return of(world.getBiome(pos));
    }

    /** @return true if this world's biomes come from this mod's 1.16 biome source (the nether, with V on) */
    public static boolean active(World world) {
        return world.getBiomeProvider() instanceof NetherBiomeSource;
    }
}
