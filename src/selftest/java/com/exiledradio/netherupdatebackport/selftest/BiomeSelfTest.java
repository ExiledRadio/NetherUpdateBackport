package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.biome.BiomeContent;
import com.exiledradio.netherupdatebackport.biome.NetherBiome;
import com.exiledradio.netherupdatebackport.biome.NetherBiomeSource;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.biome.NetherDecorator;
import com.exiledradio.netherupdatebackport.biome.Noise;
import com.exiledradio.netherupdatebackport.biome.WorldProviderNether;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.NoiseGeneratorImproved;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Stream V: the four nether biomes. */
public final class BiomeSelfTest implements SelfTest {

    @Override
    public String name() {
        return "biome";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        noise(c);
        parity(c);
        dumps(c);
        features(c);
        generation(c);
    }

    // ------------------------------------------------------------------------------------------

    private static void registration(Check c) throws Exception {
        Field name = ReflectionHelper.findField(Biome.class, "biomeName", "field_76791_y");
        String[][] expected = {{"crimson_forest", "Crimson Forest"}, {"warped_forest", "Warped Forest"},
                {"soul_sand_valley", "Soul Sand Valley"}, {"basalt_deltas", "Basalt Deltas"}};
        for (String[] e : expected) {
            Biome b = ForgeRegistries.BIOMES.getValue(new ResourceLocation(NetherUpdateBackport.MOD_ID, e[0]));
            if (!c.ok(b instanceof NetherBiome, e[0] + " is registered")) {
                continue;
            }
            c.eq(e[1], name.get(b), e[0] + "'s name");
            c.ok(BiomeDictionary.hasType(b, BiomeDictionary.Type.NETHER) && BiomeDictionary.hasType(b, BiomeDictionary.Type.HOT)
                    && BiomeDictionary.hasType(b, BiomeDictionary.Type.DRY), e[0] + " is NETHER, HOT, DRY");
            c.ok(!b.canRain(), e[0] + " has no rain");
            c.eq(NetherBiomes.Kind.byId(e[0]), NetherBiomes.of(b), e[0] + " looks up as itself");
        }
        c.eq("Nether Wastes", name.get(Biomes.HELL), "vanilla's hell is named Nether Wastes");
        c.eq(NetherBiomes.Kind.NETHER_WASTES, NetherBiomes.of(Biomes.HELL), "hell looks up as nether wastes");
        c.eq(null, NetherBiomes.of(Biomes.PLAINS), "plains is no nether biome");

        // Spawns: 1.16.5's BiomeMaker lists, this mod's mobs by name.
        Biome crimson = NetherBiomes.biome(NetherBiomes.Kind.CRIMSON_FOREST);
        Biome warped = NetherBiomes.biome(NetherBiomes.Kind.WARPED_FOREST);
        Biome valley = NetherBiomes.biome(NetherBiomes.Kind.SOUL_SAND_VALLEY);
        Biome deltas = NetherBiomes.biome(NetherBiomes.Kind.BASALT_DELTAS);
        spawn(c, crimson, EnumCreatureType.MONSTER, "minecraft:zombie_pigman", 1, 2, 4);
        spawn(c, crimson, EnumCreatureType.MONSTER, "netherupdatebackport:hoglin", 9, 3, 4);
        spawn(c, valley, EnumCreatureType.MONSTER, "minecraft:skeleton", 20, 5, 5);
        spawn(c, valley, EnumCreatureType.MONSTER, "minecraft:ghast", 50, 4, 4);
        spawn(c, valley, EnumCreatureType.MONSTER, "minecraft:enderman", 1, 4, 4);
        spawn(c, deltas, EnumCreatureType.MONSTER, "minecraft:ghast", 40, 1, 1);
        spawn(c, deltas, EnumCreatureType.MONSTER, "minecraft:magma_cube", 100, 2, 5);
        spawn(c, warped, EnumCreatureType.MONSTER, "minecraft:enderman", 1, 4, 4);
        c.eq(1, warped.getSpawnableList(EnumCreatureType.MONSTER).size(), "warped forest's only monster is the enderman");
        for (Biome b : new Biome[] {crimson, warped, valley, deltas}) {
            spawn(c, b, EnumCreatureType.CREATURE, "netherupdatebackport:strider", 60, 1, 2);
        }
        boolean piglins = ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(NetherUpdateBackport.MOD_ID, "piglin"));
        if (piglins) {
            spawn(c, crimson, EnumCreatureType.MONSTER, "netherupdatebackport:piglin", 5, 3, 4);
            spawn(c, Biomes.HELL, EnumCreatureType.MONSTER, "netherupdatebackport:piglin", 15, 4, 4);
        } else {
            c.ok(true, "no piglin entity in this build: no piglin spawns, and nothing failed");
        }

        // The provider ---------------------------------------------------------------------------------
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        c.ok(BiomeContent.providerReplaced(), "dimension -1's provider was replaced");
        c.ok(nether.provider instanceof WorldProviderNether, "the nether's provider is ours: " + nether.provider.getClass().getName());
        c.eq(DimensionType.NETHER, nether.provider.getDimensionType(), "it still says it is the nether");
        c.ok(nether.provider.isNether() && nether.provider.doesWaterVaporize(), "it is still the nether (no water)");
        c.ok(nether.getBiomeProvider() instanceof NetherBiomeSource, "the nether's biome source is 1.16's");
        if (nether.getBiomeProvider() instanceof NetherBiomeSource) {
            c.eq(nether.getSeed(), ((NetherBiomeSource) nether.getBiomeProvider()).seed(), "seeded from the world");
        }
        c.ok(BiomeContent.netherIsVanilla(DimensionType.NETHER), "vanilla's nether type counts as vanilla");
        c.ok(!BiomeContent.netherIsVanilla(DimensionType.OVERWORLD), "another provider would make ours step aside");
        c.ok(!BiomeContent.netherIsVanilla(DimensionManager.getProviderType(-1)), "ours is not vanilla's (no double replacement)");
        c.ok(NetherBiomes.active(nether) && !NetherBiomes.active(c.world), "active in the nether only");
        c.ok(BiomeContent.weights().isEmpty() || allOnes(BiomeContent.weights()), "default weights are all 1");
    }

    private static boolean allOnes(Map<NetherBiomes.Kind, Float> w) {
        for (Float f : w.values()) {
            if (f != 1.0F) {
                return false;
            }
        }
        return true;
    }

    private static void spawn(Check c, Biome biome, EnumCreatureType type, String entity, int weight, int min, int max) {
        Class<?> cls = ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(entity))
                ? ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entity)).getEntityClass() : null;
        if (!c.ok(cls != null, entity + " exists")) {
            return;
        }
        for (Biome.SpawnListEntry e : biome.getSpawnableList(type)) {
            if (e.entityClass == cls) {
                c.ok(e.itemWeight == weight && e.minGroupCount == min && e.maxGroupCount == max,
                        biome.getRegistryName() + " spawns " + entity + " " + weight + " " + min + "-" + max + ": got " + e);
                return;
            }
        }
        c.fail(biome.getRegistryName() + " has no " + type + " spawn of " + entity);
    }

    // ------------------------------------------------------------------------------------------

    private static void noise(Check c) {
        // 1.16's ImprovedNoiseGenerator against 1.12.2's NoiseGeneratorImproved: same random draws, same gradients.
        Random r = new Random(99);
        double worst = 0.0D;
        for (int i = 0; i < 200; i++) {
            long seed = r.nextLong();
            double x = r.nextDouble() * 400 - 200;
            double y = r.nextDouble() * 400 - 200;
            double z = r.nextDouble() * 400 - 200;
            double[] old = new double[2];
            new NoiseGeneratorImproved(new Random(seed)).populateNoiseArray(old, x, y, z, 1, 2, 1, 1.0D, 1.0D, 1.0D, 1.0D);
            worst = Math.max(worst, Math.abs(old[0] - Noise.improved(seed, x, y, z)));
        }
        c.ok(worst < 1.0E-9, "1.16 improved noise matches 1.12.2's own (largest difference " + worst + ")");

        // The biome source: deterministic, all five biomes, 1.16's sizes.
        NetherBiomeSource a = new NetherBiomeSource(12345L, Collections.<NetherBiomes.Kind, Float>emptyMap());
        NetherBiomeSource b = new NetherBiomeSource(12345L, Collections.<NetherBiomes.Kind, Float>emptyMap());
        Map<NetherBiomes.Kind, Integer> counts = new EnumMap<NetherBiomes.Kind, Integer>(NetherBiomes.Kind.class);
        boolean same = true;
        int n = 400;
        for (int qx = 0; qx < n; qx++) {
            for (int qz = 0; qz < n; qz++) {
                NetherBiomes.Kind k = a.noiseKind(qx * 2, 0, qz * 2);
                same &= k == b.noiseKind(qx * 2, 0, qz * 2);
                counts.put(k, (counts.containsKey(k) ? counts.get(k) : 0) + 1);
            }
        }
        c.ok(same, "the same seed gives the same biomes");
        for (NetherBiomes.Kind k : NetherBiomes.Kind.values()) {
            int got = counts.containsKey(k) ? counts.get(k) : 0;
            c.ok(got > n * n / 50, k + " covers more than 2% of a 3200-block square: " + got * 100 / (n * n) + "%");
        }
        NetherBiomes.Kind first = a.noiseKind(0, 0, 0);
        NetherBiomes.Kind other = new NetherBiomeSource(54321L, Collections.<NetherBiomes.Kind, Float>emptyMap()).noiseKind(0, 0, 0);
        int differ = 0;
        NetherBiomeSource s2 = new NetherBiomeSource(54321L, Collections.<NetherBiomes.Kind, Float>emptyMap());
        for (int i = 0; i < 1000; i++) {
            if (a.noiseKind(i * 7, 0, i * 13) != s2.noiseKind(i * 7, 0, i * 13)) {
                differ++;
            }
        }
        c.ok(differ > 300, "another seed gives another map (" + differ + " of 1000 cells differ; " + first + "/" + other + ")");
        // y never matters to the noise biome (1.16's useY is false).
        c.eq(a.noiseKind(37, 0, 91), a.noiseKind(37, 30, 91), "the noise biome ignores y");

        // Block biomes: the fuzzed magnifier only moves borders.
        int agree = 0;
        int total = 0;
        for (int x = 0; x < 512; x += 3) {
            for (int z = 0; z < 512; z += 5) {
                total++;
                if (a.blockKind(x, 64, z) == a.noiseKind(x >> 2, 16, z >> 2)) {
                    agree++;
                }
            }
        }
        c.ok(agree > total * 8 / 10, "the fuzzed block biome is mostly its own cell's (" + agree + "/" + total + ")");
        Biome[] chunk = a.chunk(3, -7);
        boolean columnsMatch = true;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                columnsMatch &= NetherBiomes.of(chunk[z << 4 | x]) == a.blockKind(48 + x, NetherBiomeSource.COLUMN_Y, -112 + z);
            }
        }
        c.ok(columnsMatch, "a chunk's column biomes are the fuzzed biomes at y " + NetherBiomeSource.COLUMN_Y);

        // Weights: 0 drops a biome, more than 1 grows one.
        Map<NetherBiomes.Kind, Float> noCrimson = new HashMap<NetherBiomes.Kind, Float>();
        noCrimson.put(NetherBiomes.Kind.CRIMSON_FOREST, 0.0F);
        Map<NetherBiomes.Kind, Float> bigWarped = new HashMap<NetherBiomes.Kind, Float>();
        bigWarped.put(NetherBiomes.Kind.WARPED_FOREST, 3.0F);
        NetherBiomeSource dropped = new NetherBiomeSource(12345L, noCrimson);
        NetherBiomeSource grown = new NetherBiomeSource(12345L, bigWarped);
        int crimson = 0;
        int warped = 0;
        int warpedBefore = 0;
        for (int qx = 0; qx < 200; qx++) {
            for (int qz = 0; qz < 200; qz++) {
                crimson += dropped.noiseKind(qx * 4, 0, qz * 4) == NetherBiomes.Kind.CRIMSON_FOREST ? 1 : 0;
                warped += grown.noiseKind(qx * 4, 0, qz * 4) == NetherBiomes.Kind.WARPED_FOREST ? 1 : 0;
                warpedBefore += a.noiseKind(qx * 4, 0, qz * 4) == NetherBiomes.Kind.WARPED_FOREST ? 1 : 0;
            }
        }
        c.eq(0, crimson, "weight 0 removes crimson forests");
        c.ok(warped > warpedBefore * 3 / 2, "weight 3 grows warped forests: " + warpedBefore + " -> " + warped);
    }

    /** The oracle's letter for a biome (tools/structure/oracle, StructureOracle.letter). */
    private static char letter(NetherBiomes.Kind kind) {
        switch (kind) {
            case SOUL_SAND_VALLEY:
                return 's';
            case CRIMSON_FOREST:
                return 'c';
            case WARPED_FOREST:
                return 'w';
            case BASALT_DELTAS:
                return 'b';
            default:
                return 'n';
        }
    }

    /**
     * Against vanilla 1.16.5 itself: biomes_1165.txt.gz is what Mojang's own MultiNoiseBiomeSource (NETHER preset) and
     * BiomeManager (the nether's zoomer) give, every 4 blocks over 2048 x 2048 blocks, for each oracle seed. Compared
     * three ways: the noise biome ({@code noiseKind}, what structures read), the block biome at y 64 ({@code
     * blockKind}), and the biome a chunk actually stores ({@code chunk()}, the cached path chunk generation uses).
     */
    private static void parity(Check c) throws Exception {
        java.io.InputStream in = BiomeSelfTest.class.getResourceAsStream("/assets/" + NetherUpdateBackport.MOD_ID + "/oracle/biomes_1165.txt.gz");
        if (!c.ok(in != null, "the 1.16.5 biome oracle is in this test build")) {
            return;
        }
        long cells = 0;
        long noiseWrong = 0;
        long blockWrong = 0;
        long storedWrong = 0;
        int seeds = 0;
        String first = "";
        Map<Character, Integer> tally = new HashMap<Character, Integer>();
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(in),
                java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            NetherBiomeSource src = null;
            long seed = 0;
            String mode = "";
            int q0 = 0;
            int row = 0;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("=")) {
                    String[] h = line.split(" ");
                    long s = Long.parseLong(h[1]);
                    if (src == null || s != seed) {
                        seed = s;
                        src = new NetherBiomeSource(seed, Collections.<NetherBiomes.Kind, Float>emptyMap(), false);
                        seeds++;
                    }
                    mode = h[2];
                    q0 = Integer.parseInt(h[3]);
                    row = 0;
                    continue;
                }
                int qz = q0 + row;
                for (int i = 0; i < line.length(); i++) {
                    int qx = q0 + i;
                    char want = line.charAt(i);
                    if (mode.equals("noise")) {
                        cells++;
                        tally.merge(want, 1, Integer::sum);
                        if (letter(src.noiseKind(qx, 0, qz)) != want) {
                            if (noiseWrong++ == 0 && first.isEmpty()) {
                                first = seed + " noise quart " + qx + "," + qz + " want " + want + " got " + letter(src.noiseKind(qx, 0, qz));
                            }
                        }
                    } else {
                        int x = qx * 4;
                        int z = qz * 4;
                        if (letter(src.blockKind(x, 64, z)) != want) {
                            if (blockWrong++ == 0 && first.isEmpty()) {
                                first = seed + " block " + x + ",64," + z + " want " + want + " got " + letter(src.blockKind(x, 64, z));
                            }
                        }
                        NetherBiomes.Kind stored = NetherBiomes.of(src.chunk(x >> 4, z >> 4)[(z & 15) << 4 | (x & 15)]);
                        if (letter(stored == null ? NetherBiomes.Kind.NETHER_WASTES : stored) != want) {
                            if (storedWrong++ == 0 && first.isEmpty()) {
                                first = seed + " stored " + x + "," + z + " want " + want + " got " + stored;
                            }
                        }
                    }
                }
                row++;
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST INFO biome parity with 1.16.5: {} seeds, {} cells of 4 blocks each way ({} x {} blocks per seed), "
                + "noise {} differ, block (y 64) {} differ, stored {} differ; oracle letters {} {}", seeds, cells, 2048, 2048, noiseWrong, blockWrong,
                storedWrong, tally, first);
        c.ok(seeds >= 5 && cells == (long) seeds * 512 * 512, "the oracle covers " + seeds + " seeds of 512 x 512 cells: " + cells);
        c.ok(tally.size() == 5, "all five nether biomes occur in the oracle: " + tally);
        c.eq(0L, noiseWrong, "seed parity: the nether's noise biomes are 1.16.5's in every cell " + first);
        c.eq(0L, blockWrong, "seed parity: the nether's block biomes (y 64) are 1.16.5's every 4 blocks " + first);
        c.eq(0L, storedWrong, "seed parity: the biome a nether chunk stores is 1.16.5's at y 64, every 4 blocks " + first);
    }

    /** Grids for the seed check against seed tools (cubiomes at scale 4): eclipse/selftest-nether-biomes-<seed>.txt. */
    private static void dumps(Check c) throws Exception {
        long[] seeds = {c.world.getSeed(), 0L, 1L, 12345L, -4172144997902289642L, 3257840388504953787L};
        for (long seed : seeds) {
            PrintWriter out = new PrintWriter(c.world.getMinecraftServer().getFile("selftest-nether-biomes-" + seed + ".txt"), "UTF-8");
            NetherBiomeSource.dump(seed, -128, -128, 256, out);
            out.close();
        }
        c.ok(true, "wrote " + seeds.length + " nether biome grids (256 x 256 cells, 4 blocks each)");
    }

    // ------------------------------------------------------------------------------------------

    private static void features(Check c) {
        WorldServer w = c.world;
        IBlockState rack = Blocks.NETHERRACK.getDefaultState();
        Block basaltBlock = Owners.ourBlock("basalt");
        IBlockState basalt = basaltBlock == null ? Blocks.STONE.getDefaultState() : basaltBlock.getDefaultState();
        Random rand = new Random(4);

        // Region: the four chunks of a population, nothing outside.
        BlockPos area = c.area();
        NetherDecorator.Region r = new NetherDecorator.Region(w, area.getX() >> 4, area.getZ() >> 4);
        c.ok(r.inside(area) && r.inside(area.add(31, 0, 31)) && !r.inside(area.add(32, 0, 0)) && !r.inside(area.add(0, 0, -1)),
                "a region is the 32 x 32 blocks of its four chunks");
        c.eq(Blocks.BEDROCK, r.block(area.add(-1, 0, 0)), "outside reads as bedrock");
        BlockPos out = area.add(-1, 5, -1);
        IBlockState before = w.getBlockState(out);
        r.set(out, rack);
        c.eq(before, w.getBlockState(out), "writes outside are dropped");

        // Basalt pillar: ceiling at 110, floor (stone) at 99.
        c.fill(area.add(0, 10, 0), area.add(31, 10, 31), rack);
        BlockPos top = area.add(16, 9, 16);
        c.ok(NetherDecorator.basaltPillar(r, rand, top, basalt), "a basalt pillar hangs from the ceiling");
        boolean column = true;
        for (int y = 100; y <= 109; y++) {
            column &= w.getBlockState(new BlockPos(top.getX(), y, top.getZ())).getBlock() == basalt.getBlock();
        }
        c.ok(column, "the pillar reaches the floor");
        c.ok(!NetherDecorator.basaltPillar(r, rand, area.add(5, 5, 5), basalt), "no pillar without a ceiling right above");

        // Multilayer: floors at 100 (on the stone) and 111 (on the ceiling), both found.
        final List<BlockPos> found = new ArrayList<BlockPos>();
        NetherDecorator.multilayer(r, new Random(1), area.add(8, 0, 8), 4, (rr, rd, p) -> found.add(p));
        boolean low = false;
        boolean high = false;
        for (BlockPos p : found) {
            low |= p.getY() == 100;
            high |= p.getY() == 111;
        }
        c.ok(high && low, "multilayer placement finds the floor on top and the one below: " + found.size());

        // Delta on a netherrack floor, near the region's edge: lava and magma, nothing outside.
        BlockPos a2 = c.area();
        NetherDecorator.Region r2 = new NetherDecorator.Region(w, a2.getX() >> 4, a2.getZ() >> 4);
        c.fill(a2.add(-4, -2, -4), a2.add(35, -1, 35), rack);
        int lava = 0;
        for (int i = 0; i < 40; i++) {
            NetherDecorator.delta(r2, rand, a2.add(1 + rand.nextInt(30), -1, 1 + rand.nextInt(30)));
        }
        boolean outsideClean = true;
        for (int x = -4; x < 36; x++) {
            for (int z = -4; z < 36; z++) {
                IBlockState s = w.getBlockState(a2.add(x, -1, z));
                boolean in = x >= 0 && x < 32 && z >= 0 && z < 32;
                if (s.getBlock() == Blocks.LAVA || s.getBlock() == Blocks.MAGMA) {
                    lava += in ? 1 : 0;
                    outsideClean &= in;
                }
            }
        }
        c.ok(lava > 20, "deltas pour lava and magma into the floor: " + lava);
        c.ok(outsideClean, "and never outside the region");

        // Basalt columns and blobs.
        BlockPos a3 = c.area();
        NetherDecorator.Region r3 = new NetherDecorator.Region(w, a3.getX() >> 4, a3.getZ() >> 4);
        c.fill(a3.add(0, -1, 0), a3.add(31, -1, 31), rack);
        boolean columns = false;
        for (int i = 0; i < 5; i++) {
            columns |= NetherDecorator.basaltColumns(r3, rand, a3.add(16, 0, 16), 2, 1, 5, 5, basalt);
        }
        c.ok(columns && w.getBlockState(a3.add(16, 0, 16)).getBlock() == basalt.getBlock(), "large basalt columns rise from the floor");
        BlockPos a4 = c.area();
        NetherDecorator.Region r4 = new NetherDecorator.Region(w, a4.getX() >> 4, a4.getZ() >> 4);
        c.fill(a4.add(0, 0, 0), a4.add(31, 20, 31), rack);
        c.ok(NetherDecorator.replaceBlob(r4, rand, a4.add(16, 25, 16), basalt), "a blob finds netherrack below it");
        int blob = 0;
        boolean inRadius = true;
        for (BlockPos p : BlockPos.getAllInBox(a4, a4.add(31, 20, 31))) {
            if (w.getBlockState(p).getBlock() == basalt.getBlock()) {
                blob++;
                inRadius &= Math.abs(p.getX() - a4.getX() - 16) + Math.abs(p.getY() - a4.getY() - 20) + Math.abs(p.getZ() - a4.getZ() - 16) <= 7;
            }
        }
        c.ok(blob >= 25 && inRadius, "the blob is a diamond of radius 3-7 from the top netherrack: " + blob);
        c.eq(27, NetherDecorator.withinManhattan(BlockPos.ORIGIN, 1, 1, 1).size(), "withinManhattan(1,1,1) covers the 3x3x3 box");
        List<BlockPos> wm = NetherDecorator.withinManhattan(BlockPos.ORIGIN, 1, 1, 1);
        c.eq(BlockPos.ORIGIN, wm.get(0), "withinManhattan starts at the center");
        c.eq(7, countWithin(wm, 1), "withinManhattan(1,1,1) has the 7 cells within distance 1 first");
    }

    private static int countWithin(List<BlockPos> list, int d) {
        int n = 0;
        for (BlockPos p : list) {
            if (Math.abs(p.getX()) + Math.abs(p.getY()) + Math.abs(p.getZ()) <= d) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------------------------------

    /** Counts chunk loads during population: each would be cascading worldgen. */
    public static final class CascadeWatch {
        int depth;
        int loads;
        final List<String> where = new ArrayList<String>();

        @SubscribeEvent
        public void pre(PopulateChunkEvent.Pre e) {
            if (e.getWorld().provider.getDimension() == -1) {
                this.depth++;
            }
        }

        @SubscribeEvent
        public void post(PopulateChunkEvent.Post e) {
            if (e.getWorld().provider.getDimension() == -1) {
                this.depth--;
            }
        }

        @SubscribeEvent
        public void load(ChunkEvent.Load e) {
            if (e.getWorld().provider.getDimension() != -1) {
                return;
            }
            Object populating = null;
            try {
                Field f = Chunk.class.getDeclaredField("populating");
                f.setAccessible(true);
                populating = f.get(null);
            } catch (ReflectiveOperationException ignored) {
            }
            if (this.depth > 0 || populating != null) {
                this.loads++;
                String source = "vanilla";
                for (StackTraceElement f : new Throwable().getStackTrace()) {
                    if (f.getClassName().startsWith("com.exiledradio") && !f.getClassName().contains(".selftest.")) {
                        source = f.getClassName() + "." + f.getMethodName();
                        break;
                    }
                }
                this.where.add(e.getChunk().x + "," + e.getChunk().z + " by " + source);
                if (this.loads <= 3) {
                    NetherUpdateBackport.LOGGER.error("SELFTEST cascade: chunk " + e.getChunk().x + "," + e.getChunk().z
                            + " loaded while populating " + populating, new Throwable("here"));
                }
            }
        }
    }

    private static void generation(Check c) {
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        if (!(nether.getBiomeProvider() instanceof NetherBiomeSource)) {
            c.fail("the nether has no 1.16 biome source; generation not tested");
            return;
        }
        NetherBiomeSource source = (NetherBiomeSource) nether.getBiomeProvider();
        CascadeWatch watch = new CascadeWatch();
        MinecraftForge.EVENT_BUS.register(watch);
        // Streams S and U's ore generators reach past the populated area (WorldGenMinable adds 8 to their own +8):
        // reported to the integrator, and off here so this test sees this stream's generation alone.
        boolean stone = com.exiledradio.netherupdatebackport.ModConfig.stone.generate;
        boolean gold = com.exiledradio.netherupdatebackport.ModConfig.blocks.generateGold;
        com.exiledradio.netherupdatebackport.ModConfig.stone.generate = false;
        com.exiledradio.netherupdatebackport.ModConfig.blocks.generateGold = false;
        try {
            for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
                BlockPos center = find(source, kind);
                if (!c.ok(center != null, "found a large " + kind + " to generate")) {
                    continue;
                }
                int cx = center.getX() >> 4;
                int cz = center.getZ() >> 4;
                for (int x = -4; x <= 3; x++) {
                    for (int z = -4; z <= 3; z++) {
                        nether.getChunkProvider().provideChunk(cx + x, cz + z);
                    }
                }
                inspect(c, nether, kind, cx, cz);
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(watch);
            com.exiledradio.netherupdatebackport.ModConfig.stone.generate = stone;
            com.exiledradio.netherupdatebackport.ModConfig.blocks.generateGold = gold;
        }
        c.eq(0, watch.loads, "no chunk loads while populating the nether (cascading worldgen) " + watch.where);
    }

    /** @return a block in the middle of a 140 x 140 block square of one noise biome, or null */
    private static BlockPos find(NetherBiomeSource s, NetherBiomes.Kind kind) {
        for (int qx = 6000; qx < 9000; qx += 7) {
            for (int qz = 6000; qz < 7200; qz += 7) {
                if (s.noiseKind(qx, 0, qz) != kind) {
                    continue;
                }
                boolean all = true;
                // The inspected 6 x 6 chunks reach 63 blocks from the center; 17 cells (68 blocks) cover that and the fuzz.
                for (int dx = -17; dx <= 17 && all; dx++) {
                    for (int dz = -17; dz <= 17 && all; dz++) {
                        all = s.noiseKind(qx + dx, 0, qz + dz) == kind;
                    }
                }
                if (all) {
                    return new BlockPos(qx << 2, 64, qz << 2);
                }
            }
        }
        return null;
    }

    private static void inspect(Check c, WorldServer nether, NetherBiomes.Kind kind, int cx, int cz) {
        Map<Block, Integer> count = new HashMap<Block, Integer>();
        boolean bedrock = true;
        boolean biomes = true;
        boolean populated = true;
        Biome want = NetherBiomes.biome(kind);
        for (int ox = -3; ox <= 2; ox++) {
            for (int oz = -3; oz <= 2; oz++) {
                Chunk chunk = nether.getChunk(cx + ox, cz + oz);
                populated &= chunk.isTerrainPopulated();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        biomes &= chunk.getBiome(new BlockPos(x, 64, z), nether.getBiomeProvider()) == want;
                        bedrock &= chunk.getBlockState(x, 0, z).getBlock() == Blocks.BEDROCK && chunk.getBlockState(x, 127, z).getBlock() == Blocks.BEDROCK;
                        for (int y = 1; y < 127; y++) {
                            Block b = chunk.getBlockState(x, y, z).getBlock();
                            count.put(b, (count.containsKey(b) ? count.get(b) : 0) + 1);
                        }
                    }
                }
            }
        }
        String k = kind.id;
        c.ok(populated, k + ": the 36 chunks are populated");
        c.ok(biomes, k + ": every column stores the biome");
        c.ok(bedrock, k + ": bedrock floor and roof in every column");
        c.eq(kind, NetherBiomes.at(nether, new BlockPos(cx << 4, 64, cz << 4).add(-8, 0, -8)), k + ": the lookup answers it");
        int crimsonNylium = n(count, "crimson_nylium");
        int warpedNylium = n(count, "warped_nylium");
        int soul = n(count, Blocks.SOUL_SAND) + n(count, "soul_soil");
        int basalt = n(count, "basalt");
        NetherUpdateBackport.LOGGER.info("SELFTEST biome {} at chunk {},{}: {}", k, cx, cz, summary(count));
        switch (kind) {
            case CRIMSON_FOREST:
                c.ok(crimsonNylium > 500, k + ": crimson nylium floors: " + crimsonNylium);
                c.ok(n(count, "crimson_stem") > 0, k + ": huge crimson fungi: " + n(count, "crimson_stem"));
                c.ok(n(count, "crimson_roots") + n(count, "crimson_fungus") > 0, k + ": roots and fungi");
                c.ok(n(count, Blocks.NETHER_WART_BLOCK) > 0, k + ": nether wart blocks");
                c.eq(0, warpedNylium + basalt, k + ": no warped nylium or basalt");
                break;
            case WARPED_FOREST:
                c.ok(warpedNylium > 500, k + ": warped nylium floors: " + warpedNylium);
                c.ok(n(count, "warped_stem") > 0, k + ": huge warped fungi: " + n(count, "warped_stem"));
                c.ok(n(count, "warped_roots") + n(count, "nether_sprouts") + n(count, "twisting_vines") + n(count, "twisting_vines_plant") > 0,
                        k + ": roots, sprouts and twisting vines");
                c.ok(n(count, "warped_wart_block") > 0, k + ": warped wart blocks");
                c.eq(0, crimsonNylium + basalt, k + ": no crimson nylium or basalt");
                break;
            case SOUL_SAND_VALLEY:
                c.ok(soul > 200, k + ": soul sand and soul soil floors: " + soul);
                c.ok(n(count, "soul_soil") > 0, k + ": some of it soul soil: " + n(count, "soul_soil"));
                c.eq(0, crimsonNylium + warpedNylium, k + ": no nylium");
                c.eq(0, n(count, Blocks.BROWN_MUSHROOM) + n(count, Blocks.RED_MUSHROOM), k + ": no nether mushrooms");
                break;
            case BASALT_DELTAS:
                c.ok(basalt > 200, k + ": basalt: " + basalt);
                c.ok(n(count, "blackstone") > 50, k + ": blackstone: " + n(count, "blackstone"));
                c.ok(n(count, Blocks.MAGMA) > 0, k + ": magma: " + n(count, Blocks.MAGMA));
                c.eq(0, crimsonNylium + warpedNylium, k + ": no nylium");
                break;
            case NETHER_WASTES:
                c.eq(0, crimsonNylium + warpedNylium + basalt + n(count, "soul_soil"), k + ": no nylium, basalt or soul soil");
                c.ok(n(count, Blocks.NETHERRACK) > 10000, k + ": netherrack");
                break;
            default:
                break;
        }
    }

    private static int n(Map<Block, Integer> count, String ours) {
        Block b = Owners.ourBlock(ours);
        return b == null ? 0 : n(count, b);
    }

    private static int n(Map<Block, Integer> count, Block b) {
        Integer v = count.get(b);
        return v == null ? 0 : v;
    }

    private static String summary(Map<Block, Integer> count) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Block, Integer> e : count.entrySet()) {
            if (e.getKey() != Blocks.AIR && e.getKey() != Blocks.NETHERRACK && e.getKey() != Blocks.LAVA) {
                s.append(e.getKey().getRegistryName().getPath()).append('=').append(e.getValue()).append(' ');
            }
        }
        return s.toString();
    }
}
