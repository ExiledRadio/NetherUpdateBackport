package com.exiledradio.netherupdatebackport.biome;

import com.google.common.hash.Hashing;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The nether's biome source: 1.16.5's {@code NetherBiomeProvider} with its {@code NETHER} preset, bit for bit, so a
 * seed gives the same nether biomes as vanilla 1.16 (and as seed maps show it).
 *
 * <ul>
 *   <li><b>Noise biomes</b> ({@link #noiseKind}): one biome per 4x4x4 cell ("quart" coordinates), the parameter
 *       point nearest to four noises at the cell (temperature, humidity, altitude, weirdness; y is ignored, as
 *       vanilla's {@code useY} is false). Each noise is a {@code MaxMinNoiseMixer} with first octave -7 and two
 *       octaves of amplitude 1, seeded from the world seed plus 0, 1, 2 and 3.</li>
 *   <li><b>Block biomes</b> ({@link #getBiome}): 1.16's {@code FuzzedBiomeMagnifier} over those cells, with the
 *       SHA-256-hashed world seed, as {@code World.getBiome} reads them. 1.12.2 stores one biome per column, so a
 *       column takes the biome at {@link #COLUMN_Y}.</li>
 * </ul>
 * With {@code biomes.weights} left at 1, nothing differs from vanilla. A weight scales a point's distance (a
 * larger weight grows its biome); 0 removes it.
 */
public final class NetherBiomeSource extends BiomeProvider {

    /** The y whose fuzzed biome a 1.12.2 column stores: the middle of the nether's 128 blocks. */
    public static final int COLUMN_Y = 64;

    /** One parameter point of the preset, as {@code Biome.Attributes}. */
    static final class Point {
        final NetherBiomes.Kind kind;
        final float temperature;
        final float humidity;
        final float altitude;
        final float weirdness;
        final float offset;
        final float weight;

        Point(NetherBiomes.Kind kind, float temperature, float humidity, float altitude, float weirdness, float offset, float weight) {
            this.kind = kind;
            this.temperature = temperature;
            this.humidity = humidity;
            this.altitude = altitude;
            this.weirdness = weirdness;
            this.offset = offset;
            this.weight = weight;
        }

        /** {@code Biome.Attributes.fitness}, in float as vanilla computes it; the sampled offset is always 0. */
        float fitness(float t, float h, float a, float w) {
            float f = (this.temperature - t) * (this.temperature - t) + (this.humidity - h) * (this.humidity - h)
                    + (this.altitude - a) * (this.altitude - a) + (this.weirdness - w) * (this.weirdness - w)
                    + (this.offset - 0.0F) * (this.offset - 0.0F);
            return this.weight == 1.0F ? f : f / this.weight;
        }
    }

    /** {@code NetherBiomeProvider.Preset.NETHER}'s points, in vanilla's order (the first wins a tie). */
    static List<Point> points(Map<NetherBiomes.Kind, Float> weights, boolean registeredOnly) {
        List<Point> all = Arrays.asList(
                new Point(NetherBiomes.Kind.NETHER_WASTES, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, weight(weights, NetherBiomes.Kind.NETHER_WASTES)),
                new Point(NetherBiomes.Kind.SOUL_SAND_VALLEY, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, weight(weights, NetherBiomes.Kind.SOUL_SAND_VALLEY)),
                new Point(NetherBiomes.Kind.CRIMSON_FOREST, 0.4F, 0.0F, 0.0F, 0.0F, 0.0F, weight(weights, NetherBiomes.Kind.CRIMSON_FOREST)),
                new Point(NetherBiomes.Kind.WARPED_FOREST, 0.0F, 0.5F, 0.0F, 0.0F, 0.375F, weight(weights, NetherBiomes.Kind.WARPED_FOREST)),
                new Point(NetherBiomes.Kind.BASALT_DELTAS, -0.5F, 0.0F, 0.0F, 0.0F, 0.175F, weight(weights, NetherBiomes.Kind.BASALT_DELTAS)));
        List<Point> kept = new ArrayList<Point>();
        for (Point p : all) {
            if (p.weight > 0.0F && (!registeredOnly || NetherBiomes.biome(p.kind) != null)) {
                kept.add(p);
            }
        }
        return kept;
    }

    private static float weight(Map<NetherBiomes.Kind, Float> weights, NetherBiomes.Kind kind) {
        Float w = weights.get(kind);
        return w == null ? 1.0F : w;
    }

    private static final double[] AMPLITUDES = {1.0D, 1.0D};

    private final long seed;
    private final long zoomSeed;
    private final Noise.MaxMin temperature;
    private final Noise.MaxMin humidity;
    private final Noise.MaxMin altitude;
    private final Noise.MaxMin weirdness;
    private final List<Point> points;

    /** chunk (as a long) -> its 256 column biomes, most recently used last */
    private final Map<Long, Biome[]> columns = new LinkedHashMap<Long, Biome[]>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Biome[]> eldest) {
            return size() > 1024;
        }
    };

    public NetherBiomeSource(long seed, Map<NetherBiomes.Kind, Float> weights) {
        this(seed, weights, true);
    }

    /** @param registeredOnly leave out biomes that are not registered (false: always vanilla's five, for dumps) */
    public NetherBiomeSource(long seed, Map<NetherBiomes.Kind, Float> weights, boolean registeredOnly) {
        this.seed = seed;
        this.zoomSeed = Hashing.sha256().hashLong(seed).asLong();
        this.temperature = new Noise.MaxMin(new Random(seed), -7, AMPLITUDES);
        this.humidity = new Noise.MaxMin(new Random(seed + 1L), -7, AMPLITUDES);
        this.altitude = new Noise.MaxMin(new Random(seed + 2L), -7, AMPLITUDES);
        this.weirdness = new Noise.MaxMin(new Random(seed + 3L), -7, AMPLITUDES);
        this.points = points(weights, registeredOnly);
    }

    public long seed() {
        return this.seed;
    }

    /** {@code NetherBiomeProvider.getNoiseBiome} at quart coordinates (block coordinates divided by 4). */
    public NetherBiomes.Kind noiseKind(int qx, int qy, int qz) {
        float t = (float) this.temperature.getValue(qx, 0.0D, qz);
        float h = (float) this.humidity.getValue(qx, 0.0D, qz);
        float a = (float) this.altitude.getValue(qx, 0.0D, qz);
        float w = (float) this.weirdness.getValue(qx, 0.0D, qz);
        Point best = null;
        float bestFit = Float.MAX_VALUE;
        for (Point p : this.points) {
            float f = p.fitness(t, h, a, w);
            if (best == null || f < bestFit) {
                best = p;
                bestFit = f;
            }
        }
        return best == null ? NetherBiomes.Kind.NETHER_WASTES : best.kind;
    }

    /** The biome at a block, through 1.16's {@code FuzzedBiomeMagnifier}. */
    public NetherBiomes.Kind blockKind(int x, int y, int z) {
        int i = x - 2;
        int j = y - 2;
        int k = z - 2;
        int l = i >> 2;
        int i1 = j >> 2;
        int j1 = k >> 2;
        double d0 = (double) (i & 3) / 4.0D;
        double d1 = (double) (j & 3) / 4.0D;
        double d2 = (double) (k & 3) / 4.0D;
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int c = 0; c < 8; ++c) {
            boolean fx = (c & 4) == 0;
            boolean fy = (c & 2) == 0;
            boolean fz = (c & 1) == 0;
            double dist = fiddledDistance(this.zoomSeed, fx ? l : l + 1, fy ? i1 : i1 + 1, fz ? j1 : j1 + 1,
                    fx ? d0 : d0 - 1.0D, fy ? d1 : d1 - 1.0D, fz ? d2 : d2 - 1.0D);
            if (c == 0 || bestDist > dist) {
                best = c;
                bestDist = dist;
            }
        }
        return noiseKind((best & 4) == 0 ? l : l + 1, (best & 2) == 0 ? i1 : i1 + 1, (best & 1) == 0 ? j1 : j1 + 1);
    }

    private static long fastNext(long seed, long salt) {
        seed = seed * (seed * 6364136223846793005L + 1442695040888963407L);
        return seed + salt;
    }

    private static double fiddledDistance(long seed, int x, int y, int z, double dx, double dy, double dz) {
        long r = fastNext(seed, x);
        r = fastNext(r, y);
        r = fastNext(r, z);
        r = fastNext(r, x);
        r = fastNext(r, y);
        r = fastNext(r, z);
        double fx = fiddle(r);
        r = fastNext(r, seed);
        double fy = fiddle(r);
        r = fastNext(r, seed);
        double fz = fiddle(r);
        return sqr(dz + fz) + sqr(dy + fy) + sqr(dx + fx);
    }

    private static double fiddle(long r) {
        double d = (double) ((int) Math.floorMod(r >> 24, 1024L)) / 1024.0D;
        return (d - 0.5D) * 0.9D;
    }

    private static double sqr(double d) {
        return d * d;
    }

    // ------------------------------------------------------------------------------------------
    // 1.12.2's BiomeProvider: one biome per column.
    // ------------------------------------------------------------------------------------------

    /** @return the 256 column biomes of a chunk, indexed {@code z << 4 | x} as 1.12.2's biome arrays are */
    public Biome[] chunk(int cx, int cz) {
        Long key = (long) cx & 0xFFFFFFFFL | ((long) cz & 0xFFFFFFFFL) << 32;
        synchronized (this.columns) {
            Biome[] cached = this.columns.get(key);
            if (cached != null) {
                return cached;
            }
        }
        Biome[] out = new Biome[256];
        // The fuzz only ever picks a cell next to the block's own (offset by -2): a 6x6 cell window covers the chunk.
        int qx0 = ((cx << 4) - 2 >> 2);
        int qz0 = ((cz << 4) - 2 >> 2);
        NetherBiomes.Kind[] cells = new NetherBiomes.Kind[6 * 6];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                out[z << 4 | x] = NetherBiomes.biomeOrHell(blockKindCached(cells, qx0, qz0, (cx << 4) + x, COLUMN_Y, (cz << 4) + z));
            }
        }
        synchronized (this.columns) {
            this.columns.put(key, out);
        }
        return out;
    }

    /** {@link #blockKind} with noise cells memoized over a chunk (noise ignores y, so cells are columns). */
    private NetherBiomes.Kind blockKindCached(NetherBiomes.Kind[] cells, int qx0, int qz0, int x, int y, int z) {
        int i = x - 2;
        int j = y - 2;
        int k = z - 2;
        int l = i >> 2;
        int i1 = j >> 2;
        int j1 = k >> 2;
        double d0 = (double) (i & 3) / 4.0D;
        double d1 = (double) (j & 3) / 4.0D;
        double d2 = (double) (k & 3) / 4.0D;
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int c = 0; c < 8; ++c) {
            boolean fx = (c & 4) == 0;
            boolean fy = (c & 2) == 0;
            boolean fz = (c & 1) == 0;
            double dist = fiddledDistance(this.zoomSeed, fx ? l : l + 1, fy ? i1 : i1 + 1, fz ? j1 : j1 + 1,
                    fx ? d0 : d0 - 1.0D, fy ? d1 : d1 - 1.0D, fz ? d2 : d2 - 1.0D);
            if (c == 0 || bestDist > dist) {
                best = c;
                bestDist = dist;
            }
        }
        int qx = (best & 4) == 0 ? l : l + 1;
        int qz = (best & 1) == 0 ? j1 : j1 + 1;
        int index = (qx - qx0) * 6 + (qz - qz0);
        if (index < 0 || index >= cells.length || qx - qx0 >= 6 || qz - qz0 >= 6) {
            return noiseKind(qx, 0, qz);
        }
        NetherBiomes.Kind kind = cells[index];
        if (kind == null) {
            kind = noiseKind(qx, 0, qz);
            cells[index] = kind;
        }
        return kind;
    }

    /**
     * Writes the noise biomes of an n x n area at 4-block resolution (quart x from qx0, z from qz0; y is ignored) as
     * 1.16.5's numeric biome ids, one row per z, for comparison with seed tools at scale 4.
     */
    public static void dump(long seed, int qx0, int qz0, int n, java.io.PrintWriter out) {
        NetherBiomeSource source = new NetherBiomeSource(seed, java.util.Collections.<NetherBiomes.Kind, Float>emptyMap(), false);
        out.println("# nether noise biomes, Minecraft 1.16.5 ids: seed=" + seed + " qx0=" + qx0 + " qz0=" + qz0 + " n=" + n + " scale=4 y=0");
        StringBuilder row = new StringBuilder();
        for (int z = 0; z < n; z++) {
            row.setLength(0);
            for (int x = 0; x < n; x++) {
                if (x > 0) {
                    row.append(' ');
                }
                row.append(source.noiseKind(qx0 + x, 0, qz0 + z).vanillaId);
            }
            out.println(row);
        }
    }

    @Override
    public List<Biome> getBiomesToSpawnIn() {
        return Collections.emptyList();
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return getBiome(pos, Biomes.HELL);
    }

    @Override
    public Biome getBiome(BlockPos pos, @Nullable Biome defaultBiome) {
        return chunk(pos.getX() >> 4, pos.getZ() >> 4)[(pos.getZ() & 15) << 4 | (pos.getX() & 15)];
    }

    @Override
    public float getTemperatureAtHeight(float temperature, int height) {
        return temperature;
    }

    @Override
    public Biome[] getBiomesForGeneration(@Nullable Biome[] biomes, int x, int z, int width, int height) {
        // 1.12.2 asks at 1:4 here, as its own layers work: the noise biome of each cell.
        if (biomes == null || biomes.length < width * height) {
            biomes = new Biome[width * height];
        }
        for (int dz = 0; dz < height; dz++) {
            for (int dx = 0; dx < width; dx++) {
                biomes[dz * width + dx] = NetherBiomes.biomeOrHell(noiseKind(x + dx, 0, z + dz));
            }
        }
        return biomes;
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] oldBiomeList, int x, int z, int width, int depth) {
        return getBiomes(oldBiomeList, x, z, width, depth, true);
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
        if (listToReuse == null || listToReuse.length < width * length) {
            listToReuse = new Biome[width * length];
        }
        if (width == 16 && length == 16 && (x & 15) == 0 && (z & 15) == 0) {
            System.arraycopy(chunk(x >> 4, z >> 4), 0, listToReuse, 0, 256);
            return listToReuse;
        }
        for (int dz = 0; dz < length; dz++) {
            for (int dx = 0; dx < width; dx++) {
                listToReuse[dz * width + dx] = getBiome(new BlockPos(x + dx, 0, z + dz));
            }
        }
        return listToReuse;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        int x0 = x - radius >> 2;
        int z0 = z - radius >> 2;
        int x1 = x + radius >> 2;
        int z1 = z + radius >> 2;
        for (int qx = x0; qx <= x1; qx++) {
            for (int qz = z0; qz <= z1; qz++) {
                if (!allowed.contains(NetherBiomes.biomeOrHell(noiseKind(qx, 0, qz)))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    @Nullable
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
        int x0 = x - range >> 2;
        int z0 = z - range >> 2;
        int x1 = x + range >> 2;
        int z1 = z + range >> 2;
        BlockPos found = null;
        int count = 0;
        for (int qz = z0; qz <= z1; qz++) {
            for (int qx = x0; qx <= x1; qx++) {
                if (biomes.contains(NetherBiomes.biomeOrHell(noiseKind(qx, 0, qz))) && (found == null || random.nextInt(count + 1) == 0)) {
                    found = new BlockPos(qx << 2, 0, qz << 2);
                    count++;
                }
            }
        }
        return found;
    }

    @Override
    public boolean isFixedBiome() {
        return false;
    }

    @Override
    public Biome getFixedBiome() {
        return Biomes.HELL;
    }

    @Override
    public void cleanupCache() {
    }
}
