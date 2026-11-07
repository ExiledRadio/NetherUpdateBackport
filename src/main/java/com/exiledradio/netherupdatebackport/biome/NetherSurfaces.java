package com.exiledradio.netherupdatebackport.biome;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorHell;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * The nether's surface, per column by biome.
 *
 * <p>1.12.2's {@code ChunkGeneratorHell.buildSurfaces} knows one biome. This replaces it (through Forge's
 * {@code ReplaceBiomeBlocks} event, answered DENY) with:
 * <ul>
 *   <li>vanilla's own algorithm, run with the generator's own noise and random, so nether wastes columns are
 *       exactly what 1.12.2 makes, and its bedrock everywhere;</li>
 *   <li>for the other four biomes, 1.16.5's surface builders ({@code NetherForestsSurfaceBuilder},
 *       {@code SoulSandValleySurfaceBuilder}, {@code BasaltDeltasSurfaceBuilder}, both through
 *       {@code ValleySurfaceBuilder}) on the raw terrain, with their own noises seeded from the world seed and
 *       1.16's surface depth noise and per-chunk random, then vanilla's bedrock on top.</li>
 * </ul>
 * The blocks of other streams (nylium, warped wart block, soul soil, basalt, blackstone) are looked up by name;
 * when a stream is off, its block falls back to the nearest vanilla one.
 */
public final class NetherSurfaces {

    private static final int SEA_LEVEL = 32;

    private static Field randField;
    private static Field slowsandField;
    private static Field exclusivityField;

    private final long seed;
    private final Noise.Octaves surfaceNoise;
    private final Noise.Octaves forestNoise;
    private final Noise.Octaves[] soulFloor;
    private final Noise.Octaves[] soulCeiling;
    private final Noise.Octaves soulPatch;
    private final Noise.Octaves[] deltaFloor;
    private final Noise.Octaves[] deltaCeiling;
    private final Noise.Octaves deltaPatch;

    private static NetherSurfaces cached;

    private NetherSurfaces(long seed) {
        this.seed = seed;
        // NoiseChunkGenerator: three terrain noises come first off the same random, then the surface noise.
        Random r = new Random(seed);
        Noise.Octaves.ofOctaves(r, range(-15, 0));
        Noise.Octaves.ofOctaves(r, range(-15, 0));
        Noise.Octaves.ofOctaves(r, range(-7, 0));
        this.surfaceNoise = Noise.Octaves.ofOctaves(r, range(-3, 0));
        this.forestNoise = Noise.Octaves.ofOctaves(new Random(seed), 0);
        // ValleySurfaceBuilder.initNoise: floor noises, then ceiling, then the patch noise, on consecutive seeds.
        this.soulFloor = new Noise.Octaves[] {valley(seed), valley(seed + 1)};
        this.soulCeiling = new Noise.Octaves[] {valley(seed + 2), valley(seed + 3)};
        this.soulPatch = Noise.Octaves.ofOctaves(new Random(seed + 4), 0);
        this.deltaFloor = new Noise.Octaves[] {valley(seed), valley(seed + 1)};
        this.deltaCeiling = new Noise.Octaves[] {valley(seed + 2)};
        this.deltaPatch = Noise.Octaves.ofOctaves(new Random(seed + 3), 0);
    }

    private static Noise.Octaves valley(long seed) {
        return Noise.Octaves.ofOctaves(new Random(seed), -4);
    }

    private static int[] range(int from, int to) {
        int[] out = new int[to - from + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = from + i;
        }
        return out;
    }

    static synchronized NetherSurfaces forSeed(long seed) {
        if (cached == null || cached.seed != seed) {
            cached = new NetherSurfaces(seed);
        }
        return cached;
    }

    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void replaceBiomeBlocks(ChunkGeneratorEvent.ReplaceBiomeBlocks event) {
        if (event.getResult() == Event.Result.DENY || event.getWorld() == null || !(event.getGen() instanceof ChunkGeneratorHell)
                || !(event.getWorld().getBiomeProvider() instanceof NetherBiomeSource)) {
            return;
        }
        NetherBiomeSource source = (NetherBiomeSource) event.getWorld().getBiomeProvider();
        Biome[] biomes = source.chunk(event.getX(), event.getZ());
        boolean any = false;
        for (Biome b : biomes) {
            any |= b instanceof NetherBiome;
        }
        if (!any) {
            return; // all nether wastes: vanilla's own surface, untouched
        }
        try {
            build((ChunkGeneratorHell) event.getGen(), event.getWorld().getSeaLevel(), event.getX(), event.getZ(), event.getPrimer(), biomes,
                    forSeed(source.seed()));
            event.setResult(Event.Result.DENY);
        } catch (ReflectiveOperationException | RuntimeException e) {
            NetherUpdateBackport.LOGGER.error("Nether biome surfaces failed; this chunk keeps vanilla's surface", e);
        }
    }

    private static void fields() {
        if (randField == null) {
            randField = ReflectionHelper.findField(ChunkGeneratorHell.class, "rand", "field_185954_p");
            slowsandField = ReflectionHelper.findField(ChunkGeneratorHell.class, "slowsandGravelNoiseGen", "field_73177_m");
            exclusivityField = ReflectionHelper.findField(ChunkGeneratorHell.class, "netherrackExculsivityNoiseGen", "field_73174_n");
        }
    }

    static final IBlockState AIR = Blocks.AIR.getDefaultState();
    static final IBlockState NETHERRACK = Blocks.NETHERRACK.getDefaultState();
    static final IBlockState BEDROCK = Blocks.BEDROCK.getDefaultState();
    static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    static final IBlockState SOUL_SAND = Blocks.SOUL_SAND.getDefaultState();

    /** Everything after the event check of vanilla's {@code buildSurfaces}, per column. */
    private static void build(ChunkGeneratorHell gen, int seaLevel, int cx, int cz, ChunkPrimer primer, Biome[] biomes, NetherSurfaces s)
            throws ReflectiveOperationException {
        fields();
        Random rand = (Random) randField.get(gen);
        NoiseGeneratorOctaves slowsandGen = (NoiseGeneratorOctaves) slowsandField.get(gen);
        NoiseGeneratorOctaves exclusivityGen = (NoiseGeneratorOctaves) exclusivityField.get(gen);
        int i = seaLevel + 1;
        double[] slowsand = slowsandGen.generateNoiseOctaves(null, cx * 16, cz * 16, 0, 16, 16, 1, 0.03125D, 0.03125D, 1.0D);
        double[] gravel = slowsandGen.generateNoiseOctaves(null, cx * 16, 109, cz * 16, 16, 1, 16, 0.03125D, 1.0D, 0.03125D);
        double[] depth = exclusivityGen.generateNoiseOctaves(null, cx * 16, cz * 16, 0, 16, 16, 1, 0.0625D, 0.0625D, 0.0625D);

        Surfaces states = Surfaces.lookUp();
        IBlockState[] raw = new IBlockState[128];
        IBlockState[] col = new IBlockState[128];
        // Vanilla's loop order: j then k, with the primer read at (k, y, j) - so j is z and k is x.
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int y = 0; y < 128; y++) {
                    raw[y] = primer.getBlockState(k, y, j);
                    col[y] = raw[y];
                }
                vanillaColumn(rand, col, slowsand[j + k * 16], gravel[j + k * 16], depth[j + k * 16], i);
                Biome biome = biomes[j << 4 | k];
                if (biome instanceof NetherBiome) {
                    // 1.16 builds the surface on the raw terrain and lays bedrock afterwards.
                    for (int y = 0; y < 128; y++) {
                        boolean bedrock = col[y] == BEDROCK && raw[y] != BEDROCK;
                        col[y] = raw[y];
                        if (bedrock) {
                            raw[y] = null; // marks bedrock to lay over the surface below
                        }
                    }
                    IBlockState[] built = col;
                    s.modernColumn(((NetherBiome) biome).kind, states, built, cx, cz, k, j);
                    for (int y = 0; y < 128; y++) {
                        primer.setBlockState(k, y, j, raw[y] == null ? BEDROCK : built[y]);
                    }
                } else {
                    for (int y = 0; y < 128; y++) {
                        if (col[y] != raw[y]) {
                            primer.setBlockState(k, y, j, col[y]);
                        }
                    }
                }
            }
        }
    }

    /** One column of 1.12.2's {@code ChunkGeneratorHell.buildSurfaces}, same random calls in the same order. */
    private static void vanillaColumn(Random rand, IBlockState[] col, double slowsand, double gravel, double depth, int i) {
        boolean flag = slowsand + rand.nextDouble() * 0.2D > 0.0D;
        boolean flag1 = gravel + rand.nextDouble() * 0.2D > 0.0D;
        int l = (int) (depth / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int i1 = -1;
        IBlockState top = NETHERRACK;
        IBlockState under = NETHERRACK;
        for (int y = 127; y >= 0; --y) {
            if (y < 127 - rand.nextInt(5) && y > rand.nextInt(5)) {
                IBlockState here = col[y];
                if (here.getBlock() != null && here.getMaterial() != Material.AIR) {
                    if (here.getBlock() == Blocks.NETHERRACK) {
                        if (i1 == -1) {
                            if (l <= 0) {
                                top = AIR;
                                under = NETHERRACK;
                            } else if (y >= i - 4 && y <= i + 1) {
                                top = NETHERRACK;
                                under = NETHERRACK;
                                if (flag1) {
                                    top = GRAVEL;
                                    under = NETHERRACK;
                                }
                                if (flag) {
                                    top = SOUL_SAND;
                                    under = SOUL_SAND;
                                }
                            }
                            if (y < i && (top == null || top.getMaterial() == Material.AIR)) {
                                top = LAVA;
                            }
                            i1 = l;
                            col[y] = y >= i - 1 ? top : under;
                        } else if (i1 > 0) {
                            --i1;
                            col[y] = under;
                        }
                    }
                } else {
                    i1 = -1;
                }
            } else {
                col[y] = BEDROCK;
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // 1.16.5's surface builders. Their per-chunk random is NoiseChunkGenerator's: setBaseChunkSeed, x outer.
    // ------------------------------------------------------------------------------------------

    private int randChunkX = Integer.MIN_VALUE;
    private int randChunkZ;
    private final double[] chunkDoubles = new double[256 * 3];

    /** A column's share of 1.16's per-chunk surface random. */
    private static final class Doubles {
        private final double[] values;
        private int pos;

        Doubles(double[] values, int pos) {
            this.values = values;
            this.pos = pos;
        }

        double nextDouble() {
            return this.values[this.pos++];
        }
    }

    /**
     * Runs the 1.16 builder of a kind on one column. 1.16 visits columns x outer, z inner with one random for the
     * chunk ({@code setBaseChunkSeed}); every nether builder (nether, forests, valleys) takes exactly three doubles
     * from it per column, so a column's doubles are found by its place in that order.
     */
    private void modernColumn(NetherBiomes.Kind kind, Surfaces states, IBlockState[] col, int cx, int cz, int lx, int lz) {
        int x = (cx << 4) + lx;
        int z = (cz << 4) + lz;
        double noise = this.surfaceNoise.surface(x * 0.0625D, z * 0.0625D, 0.0625D, lx * 0.0625D) * 15.0D;
        if (this.randChunkX != cx || this.randChunkZ != cz) {
            Random r = new Random((long) cx * 341873128712L + (long) cz * 132897987541L);
            for (int i = 0; i < this.chunkDoubles.length; i++) {
                this.chunkDoubles[i] = r.nextDouble();
            }
            this.randChunkX = cx;
            this.randChunkZ = cz;
        }
        Doubles rand = new Doubles(this.chunkDoubles, (lx * 16 + lz) * 3);
        switch (kind) {
            case CRIMSON_FOREST:
                forest(rand, col, x, z, noise, states.crimsonNylium, NETHERRACK, Blocks.NETHER_WART_BLOCK.getDefaultState());
                break;
            case WARPED_FOREST:
                forest(rand, col, x, z, noise, states.warpedNylium, NETHERRACK, states.warpedWart);
                break;
            case SOUL_SAND_VALLEY:
                valley(rand, col, x, z, noise, new IBlockState[] {SOUL_SAND, states.soulSoil}, new IBlockState[] {SOUL_SAND, states.soulSoil},
                        this.soulFloor, this.soulCeiling, this.soulPatch);
                break;
            case BASALT_DELTAS:
                valley(rand, col, x, z, noise, new IBlockState[] {states.basalt, states.blackstone}, new IBlockState[] {states.basalt},
                        this.deltaFloor, this.deltaCeiling, this.deltaPatch);
                break;
            default:
                break;
        }
    }

    private static boolean isAir(IBlockState s) {
        return s.getMaterial() == Material.AIR;
    }

    /** {@code NetherForestsSurfaceBuilder.apply}: top = nylium, under = netherrack, "underwater" = wart block. */
    private void forest(Doubles rand, IBlockState[] col, int x, int z, double noise, IBlockState topMaterial, IBlockState underMaterial,
                        IBlockState wartMaterial) {
        int i = SEA_LEVEL;
        double d0 = this.forestNoise.getValue(x * 0.1D, SEA_LEVEL, z * 0.1D);
        boolean flag = d0 > 0.15D + rand.nextDouble() * 0.35D;
        double d1 = this.forestNoise.getValue(x * 0.1D, 109.0D, z * 0.1D);
        boolean flag1 = d1 > 0.25D + rand.nextDouble() * 0.9D;
        int l = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int i1 = -1;
        IBlockState under = underMaterial;
        for (int y = 127; y >= 0; --y) {
            IBlockState top = topMaterial;
            IBlockState here = col[y];
            if (isAir(here)) {
                i1 = -1;
            } else if (here.getBlock() == Blocks.NETHERRACK) {
                if (i1 == -1) {
                    boolean flag2 = false;
                    if (l <= 0) {
                        flag2 = true;
                        under = underMaterial;
                    }
                    if (flag) {
                        top = underMaterial;
                    } else if (flag1) {
                        top = wartMaterial;
                    }
                    if (y < i && flag2) {
                        top = LAVA;
                    }
                    i1 = l;
                    col[y] = y >= i - 1 ? top : under;
                } else if (i1 > 0) {
                    --i1;
                    col[y] = under;
                }
            }
        }
    }

    /** {@code ValleySurfaceBuilder.apply} for soul sand valleys and basalt deltas; gravel is the patch block of both. */
    private void valley(Doubles rand, IBlockState[] col, int x, int z, double noise, IBlockState[] floorStates, IBlockState[] ceilingStates,
                        Noise.Octaves[] floorNoises, Noise.Octaves[] ceilingNoises, Noise.Octaves patchNoise) {
        int i = SEA_LEVEL + 1;
        int l = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int i1 = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        boolean flag = patchNoise.getValue(x * 0.03125D, 109.0D, z * 0.03125D) * 75.0D + rand.nextDouble() > 0.0D;
        IBlockState ceiling = pick(ceilingStates, ceilingNoises, x, z);
        IBlockState floor = pick(floorStates, floorNoises, x, z);
        IBlockState above = AIR; // y=128 in a 1.12.2 nether primer
        for (int y = 127; y >= 0; --y) {
            IBlockState here = col[y];
            if (above.getBlock() == Blocks.NETHERRACK && (isAir(here) || here == LAVA)) {
                for (int k1 = 0, p = y; k1 < l; ++k1) {
                    p++;
                    if (p > 127 || col[p].getBlock() != Blocks.NETHERRACK) {
                        break;
                    }
                    col[p] = ceiling;
                }
            }
            if ((isAir(above) || above == LAVA) && here.getBlock() == Blocks.NETHERRACK) {
                for (int l1 = 0, p = y; l1 < i1 && p >= 0 && col[p].getBlock() == Blocks.NETHERRACK; ++l1, --p) {
                    col[p] = flag && y >= i - 4 && y <= i + 1 ? GRAVEL : floor;
                }
            }
            above = here;
        }
    }

    /** The state whose noise is highest at (x, 32, z): {@code stream().max(...)} over an ordered map. */
    private static IBlockState pick(IBlockState[] states, Noise.Octaves[] noises, int x, int z) {
        IBlockState best = states[0];
        double bestValue = noises[0].getValue(x, SEA_LEVEL, z);
        for (int n = 1; n < noises.length; n++) {
            double v = noises[n].getValue(x, SEA_LEVEL, z);
            if (v >= bestValue) { // Stream.max keeps the later of equal elements
                best = states[n];
                bestValue = v;
            }
        }
        return best;
    }

    /** Other streams' surface blocks by name, each with its vanilla fallback when that stream is off. */
    static final class Surfaces {
        IBlockState crimsonNylium;
        IBlockState warpedNylium;
        IBlockState warpedWart;
        IBlockState soulSoil;
        IBlockState basalt;
        IBlockState blackstone;

        static Surfaces lookUp() {
            Surfaces s = new Surfaces();
            s.crimsonNylium = state("crimson_nylium", NETHERRACK);
            s.warpedNylium = state("warped_nylium", NETHERRACK);
            s.warpedWart = state("warped_wart_block", Blocks.NETHER_WART_BLOCK.getDefaultState());
            s.soulSoil = state("soul_soil", SOUL_SAND);
            s.basalt = NetherDecorator.upright(state("basalt", NETHERRACK));
            s.blackstone = state("blackstone", NETHERRACK);
            return s;
        }

        private static IBlockState state(String name, IBlockState fallback) {
            Block b = Owners.ourBlock(name);
            return b == null ? fallback : b.getDefaultState();
        }
    }
}
