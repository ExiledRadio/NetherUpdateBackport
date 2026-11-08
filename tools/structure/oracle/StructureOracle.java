import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

/**
 * Nether Update Backport, stream X: what vanilla 1.16.5's own code decides about bastions, fortresses, ruined
 * portals and nether fossils, for the self-test to compare with. Run against 1.16.5's server jar remapped to
 * Mojang's names (Backport Core's tools/vanilla-oracle explains the remapping), on Java 16 or later:
 *
 *   javac -cp server-mapped.jar -d classes StructureOracle.java
 *   java -cp "server-mapped.jar;classes" StructureOracle out-dir saves-dir
 *
 * Everything is vanilla's real code path: the nether's multi-noise biome source and noise generator, the overworld's
 * layered biome source, ChunkGenerator.createStructures' biome check (the noise biome at the chunk's middle quart),
 * ConfiguredStructureFeature.generate, and the pieces' own template placement into an empty world.
 *
 * <p>And the nether's biomes themselves (biomes_1165.txt.gz), for stream V's biome source: for each seed, every 4 blocks
 * over 2048 x 2048 blocks, the noise biome ({@code MultiNoiseBiomeSource.getNoiseBiome}, what structures read) and the
 * block biome at y 64 ({@code BiomeManager.getBiome} with the nether dimension's own zoomer and obfuscated seed, what
 * World.getBiome gives and a 1.12.2 column stores).
 */
public final class StructureOracle {
    static final long[] SEEDS = {0L, 12345L, -7346534567L, 8675309L, 3257840388504953787L};
    /** The biome grid: quart cells from -256 to 255 on both axes, so 2048 x 2048 blocks around 0, 0. */
    static final int BIOME_Q0 = -256;
    static final int BIOME_N = 512;

    static RegistryAccess.RegistryHolder access;
    static Registry<Biome> biomes;
    static StructureManager manager;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        Bootstrap.bootStrap();
        access = RegistryAccess.builtin();
        biomes = access.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<NoiseGeneratorSettings> noise = access.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(PackType.SERVER_DATA);
        resources.add(new VanillaPackResources("minecraft"));
        LevelStorageSource.LevelStorageAccess storage = LevelStorageSource.createDefault(Paths.get(args[1])).createAccess("oracle");
        manager = new StructureManager(resources, storage, DataFixers.getDataFixer());
        // Block tags (stairs, slabs, walls) as a server binds them: the block_age processor reads them.
        java.util.concurrent.Executor now = Runnable::run;
        net.minecraft.tags.TagLoader<net.minecraft.world.level.block.Block> blockTags =
                new net.minecraft.tags.TagLoader<>(Registry.BLOCK::getOptional, "tags/blocks", "block");
        net.minecraft.tags.TagLoader<net.minecraft.world.item.Item> itemTags =
                new net.minecraft.tags.TagLoader<>(Registry.ITEM::getOptional, "tags/items", "item");
        net.minecraft.tags.TagLoader<net.minecraft.world.level.material.Fluid> fluidTags =
                new net.minecraft.tags.TagLoader<>(Registry.FLUID::getOptional, "tags/fluids", "fluid");
        net.minecraft.tags.TagLoader<net.minecraft.world.entity.EntityType<?>> entityTags =
                new net.minecraft.tags.TagLoader<>(Registry.ENTITY_TYPE::getOptional, "tags/entity_types", "entity_type");
        net.minecraft.tags.TagContainer tags = net.minecraft.tags.TagContainer.of(
                blockTags.load(blockTags.prepare(resources, now).get()), itemTags.load(itemTags.prepare(resources, now).get()),
                fluidTags.load(fluidTags.prepare(resources, now).get()), entityTags.load(entityTags.prepare(resources, now).get()));
        tags.bindToGlobal();

        // Which ruined portal each biome lists, by vanilla id: the variant table.
        try (PrintWriter w = writer(new File(out, "portal_types_1165.txt"))) {
            w.println("# biomeId biomeName portalType (none: the biome lists no ruined portal)");
            for (Biome b : biomes) {
                String type = "none";
                for (Supplier<ConfiguredStructureFeature<?, ?>> s : b.getGenerationSettings().structures()) {
                    ConfiguredStructureFeature<?, ?> c = s.get();
                    if (c.feature == StructureFeature.RUINED_PORTAL) {
                        type = ((RuinedPortalConfiguration) c.config).portalType.getSerializedName();
                    }
                }
                w.println(BuiltinRegistries.BIOME.getId(BuiltinRegistries.BIOME.get(biomes.getKey(b))) + " " + biomes.getKey(b) + " " + type);
            }
        }

        PrintWriter slots = writer(new File(out, "slots_1165.txt"));
        slots.println("# seed chunkX chunkZ biome result   (one line per 27-chunk region's candidate: bastion, fortress or none)");
        PrintWriter portals = writer(new File(out, "portals_1165.txt"));
        portals.println("# seed dim chunkX chunkZ biome portalType result [template rotation mirror placement airPocket cold mossiness"
                + " overgrown vines blackstone tpx tpy tpz x0 y0 z0 x1 y1 z1]");
        PrintWriter fossils = writer(new File(out, "fossils_1165.txt"));
        fossils.println("# seed chunkX chunkZ biome result [template rotation tpx tpy tpz]");
        List<String[]> wholeBastions = new ArrayList<>();
        java.util.Set<String> kinds = new java.util.HashSet<>();
        List<String[]> wholePortals = new ArrayList<>();
        Map<String, StructureStart<?>> starts = new HashMap<>();
        Map<String, NoiseBasedChunkGenerator> gens = new HashMap<>();

        for (long seed : SEEDS) {
            BiomeSource netherBiomes = MultiNoiseBiomeSource.Preset.NETHER.biomeSource(biomes, seed);
            NoiseBasedChunkGenerator nether = new NoiseBasedChunkGenerator(netherBiomes, seed, () -> noise.getOrThrow(NoiseGeneratorSettings.NETHER));
            BiomeSource overBiomes = new OverworldBiomeSource(seed, false, false, biomes);
            NoiseBasedChunkGenerator over = new NoiseBasedChunkGenerator(overBiomes, seed, () -> noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD));
            gens.put(seed + " nether", nether);
            gens.put(seed + " overworld", over);
            StructureSettings ns = nether.getSettings();
            StructureSettings os = over.getSettings();

            // Bastions and fortresses: one candidate per 27-chunk region, then createStructures' per-structure check.
            StructureFeatureConfiguration bc = ns.getConfig(StructureFeature.BASTION_REMNANT);
            for (int rx = -20; rx < 20; rx++) {
                for (int rz = -20; rz < 20; rz++) {
                    ChunkPos c = StructureFeature.BASTION_REMNANT.getPotentialFeatureChunk(bc, seed, new WorldgenRandom(), rx * bc.spacing(), rz * bc.spacing());
                    Biome biome = netherBiomes.getNoiseBiome((c.x << 2) + 2, 0, (c.z << 2) + 2);
                    String result = "none";
                    for (Supplier<ConfiguredStructureFeature<?, ?>> s : biome.getGenerationSettings().structures()) {
                        ConfiguredStructureFeature<?, ?> csf = s.get();
                        if (csf.feature != StructureFeature.BASTION_REMNANT && csf.feature != StructureFeature.NETHER_BRIDGE) {
                            continue;
                        }
                        StructureStart<?> start = csf.generate(access, nether, netherBiomes, manager, seed, c, biome, 0, ns.getConfig(csf.feature));
                        if (start.isValid()) {
                            result = csf.feature == StructureFeature.BASTION_REMNANT ? "bastion" : "fortress";
                            String kind = result.equals("bastion") ? startKind(start) : "";
                            if (result.equals("bastion") && seed == 12345L && Math.abs(c.x) < 80 && Math.abs(c.z) < 80 && kinds.add(kind)) {
                                wholeBastions.add(new String[] {String.valueOf(seed), String.valueOf(c.x), String.valueOf(c.z)});
                                starts.put("bastion " + seed + " " + c.x + " " + c.z, start);
                            }
                        }
                    }
                    slots.println(seed + " " + c.x + " " + c.z + " " + biomes.getKey(biome) + " " + result);
                }
            }

            // Ruined portals: the nether's own spacing (25, 10), the overworld's (40, 15).
            for (int dim = 0; dim < 2; dim++) {
                BiomeSource source = dim == 0 ? overBiomes : netherBiomes;
                NoiseBasedChunkGenerator gen = dim == 0 ? over : nether;
                StructureSettings settings = dim == 0 ? os : ns;
                StructureFeatureConfiguration pc = settings.getConfig(StructureFeature.RUINED_PORTAL);
                int range = dim == 0 ? 10 : 16;
                for (int rx = -range; rx < range; rx++) {
                    for (int rz = -range; rz < range; rz++) {
                        ChunkPos c = StructureFeature.RUINED_PORTAL.getPotentialFeatureChunk(pc, seed, new WorldgenRandom(), rx * pc.spacing(), rz * pc.spacing());
                        Biome biome = source.getNoiseBiome((c.x << 2) + 2, 0, (c.z << 2) + 2);
                        String line = seed + " " + (dim == 0 ? "overworld" : "nether") + " " + c.x + " " + c.z + " " + biomes.getKey(biome);
                        ConfiguredStructureFeature<?, ?> csf = null;
                        for (Supplier<ConfiguredStructureFeature<?, ?>> s : biome.getGenerationSettings().structures()) {
                            if (s.get().feature == StructureFeature.RUINED_PORTAL) {
                                csf = s.get();
                            }
                        }
                        if (csf == null) {
                            portals.println(line + " none none");
                            continue;
                        }
                        String type = ((RuinedPortalConfiguration) csf.config).portalType.getSerializedName();
                        StructureStart<?> start = csf.generate(access, gen, source, manager, seed, c, biome, 0, pc);
                        if (!start.isValid()) {
                            portals.println(line + " " + type + " invalid");
                            continue;
                        }
                        CompoundTag t = start.getPieces().get(0).createTag();
                        CompoundTag p = t.getCompound("Properties");
                        BoundingBox b = start.getPieces().get(0).getBoundingBox();
                        portals.println(line + " " + type + " valid " + t.getString("Template") + " " + t.getString("Rotation") + " " + t.getString("Mirror")
                                + " " + t.getString("VerticalPlacement") + " " + p.getBoolean("air_pocket") + " " + p.getBoolean("cold") + " "
                                + p.getFloat("mossiness") + " " + p.getBoolean("overgrown") + " " + p.getBoolean("vines") + " "
                                + p.getBoolean("replace_with_blackstone") + " " + t.getInt("TPX") + " " + t.getInt("TPY") + " " + t.getInt("TPZ") + " "
                                + b.x0 + " " + b.y0 + " " + b.z0 + " " + b.x1 + " " + b.y1 + " " + b.z1);
                        String key = "portal " + seed + " " + (dim == 0 ? "overworld" : "nether") + " " + c.x + " " + c.z;
                        if (seed == 12345L && Math.abs(rx) <= 1 && Math.abs(rz) <= 1) {
                            wholePortals.add(new String[] {String.valueOf(seed), dim == 0 ? "overworld" : "nether", String.valueOf(c.x), String.valueOf(c.z)});
                            starts.put(key, start);
                        }
                    }
                }
            }

            // Nether fossils: soul sand valleys.
            StructureFeatureConfiguration fc = ns.getConfig(StructureFeature.NETHER_FOSSIL);
            for (int rx = -60; rx < 60; rx++) {
                for (int rz = -60; rz < 60; rz++) {
                    ChunkPos c = StructureFeature.NETHER_FOSSIL.getPotentialFeatureChunk(fc, seed, new WorldgenRandom(), rx * fc.spacing(), rz * fc.spacing());
                    Biome biome = netherBiomes.getNoiseBiome((c.x << 2) + 2, 0, (c.z << 2) + 2);
                    ConfiguredStructureFeature<?, ?> csf = null;
                    for (Supplier<ConfiguredStructureFeature<?, ?>> s : biome.getGenerationSettings().structures()) {
                        if (s.get().feature == StructureFeature.NETHER_FOSSIL) {
                            csf = s.get();
                        }
                    }
                    String line = seed + " " + c.x + " " + c.z + " " + biomes.getKey(biome);
                    if (csf == null) {
                        continue;
                    }
                    StructureStart<?> start = csf.generate(access, nether, netherBiomes, manager, seed, c, biome, 0, fc);
                    if (!start.isValid()) {
                        fossils.println(line + " invalid");
                        continue;
                    }
                    CompoundTag t = start.getPieces().get(0).createTag();
                    fossils.println(line + " valid " + t.getString("Template") + " " + t.getString("Rot") + " " + t.getInt("TPX") + " " + t.getInt("TPY")
                            + " " + t.getInt("TPZ"));
                }
            }
        }
        slots.close();
        portals.close();
        fossils.close();

        // Whole structures, block for block: every block the pieces place into an empty world.
        try (PrintWriter w = gzWriter(new File(out, "blocks_1165.txt.gz"))) {
            for (String[] b : wholeBastions) {
                StructureStart<?> start = starts.get("bastion " + b[0] + " " + b[1] + " " + b[2]);
                Map<BlockPos, BlockState> world = new HashMap<>();
                WorldGenLevel level = fake(world, Long.parseLong(b[0]), true);
                BoundingBox all = start.getBoundingBox();
                start.placeInChunk(level, null, gens.get(b[0] + " nether"), new Random(0L), all, new ChunkPos(Integer.parseInt(b[1]), Integer.parseInt(b[2])));
                dump(w, "bastion " + b[0] + " " + b[1] + " " + b[2] + " " + start.getPieces().size() + " " + startKind(start), world);
            }
            for (String[] p : wholePortals) {
                StructureStart<?> start = starts.get("portal " + p[0] + " " + p[1] + " " + p[2] + " " + p[3]);
                Map<BlockPos, BlockState> world = new HashMap<>();
                WorldGenLevel level = fake(world, Long.parseLong(p[0]), p[1].equals("nether"));
                // The template alone, as TemplateStructurePiece places it; the netherrack and vines that follow read the
                // terrain around, which a backport's terrain does not share.
                TemplateStructurePiece piece = (TemplateStructurePiece) start.getPieces().get(0);
                StructureTemplate template = (StructureTemplate) field(TemplateStructurePiece.class, "template").get(piece);
                StructurePlaceSettings settings = (StructurePlaceSettings) field(TemplateStructurePiece.class, "placeSettings").get(piece);
                BlockPos at = (BlockPos) field(TemplateStructurePiece.class, "templatePosition").get(piece);
                BoundingBox box = piece.getBoundingBox();
                settings.setBoundingBox(box);
                template.placeInWorld(level, at, new BlockPos(box.getCenter().getX(), box.y0, box.getCenter().getZ()), settings, new Random(0L), 2);
                dump(w, "portal " + p[0] + " " + p[1] + " " + p[2] + " " + p[3] + " 1", world);
            }
        }
        storage.close();
        biomes(out);
        System.out.println("oracle written: " + wholeBastions.size() + " bastions, " + wholePortals.size() + " portals block for block");
    }

    /** One letter per nether biome: n(ether wastes), s(oul sand valley), c(rimson), w(arped), b(asalt deltas). */
    static char letter(Biome biome) {
        String path = biomes.getKey(biome).getPath();
        switch (path) {
            case "nether_wastes":
                return 'n';
            case "soul_sand_valley":
                return 's';
            case "crimson_forest":
                return 'c';
            case "warped_forest":
                return 'w';
            case "basalt_deltas":
                return 'b';
            default:
                throw new IllegalStateException("not a nether biome: " + path);
        }
    }

    static void biomes(File out) throws Exception {
        net.minecraft.world.level.biome.BiomeZoomer zoomer = access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
                .get(net.minecraft.world.level.dimension.DimensionType.NETHER_LOCATION).getBiomeZoomer();
        try (PrintWriter w = gzWriter(new File(out, "biomes_1165.txt.gz"))) {
            w.println("# nether biomes of 1.16.5, one letter per 4 blocks (n s c w b: wastes, soul sand valley, crimson, warped, deltas)");
            w.println("# '= seed mode q0 n' then n rows (z) of n letters (x); cell (i, j) is quart (q0 + i, q0 + j), block (4 * that)");
            w.println("# mode noise: MultiNoiseBiomeSource.getNoiseBiome(qx, 0, qz); mode block64: BiomeManager.getBiome(4qx, 64, 4qz)");
            for (long seed : SEEDS) {
                BiomeSource source = MultiNoiseBiomeSource.Preset.NETHER.biomeSource(biomes, seed);
                net.minecraft.world.level.biome.BiomeManager manager = new net.minecraft.world.level.biome.BiomeManager(source,
                        net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(seed), zoomer);
                StringBuilder row = new StringBuilder(BIOME_N);
                w.println("= " + seed + " noise " + BIOME_Q0 + " " + BIOME_N);
                for (int j = 0; j < BIOME_N; j++) {
                    row.setLength(0);
                    for (int i = 0; i < BIOME_N; i++) {
                        row.append(letter(source.getNoiseBiome(BIOME_Q0 + i, 0, BIOME_Q0 + j)));
                    }
                    w.println(row);
                }
                w.println("= " + seed + " block64 " + BIOME_Q0 + " " + BIOME_N);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int j = 0; j < BIOME_N; j++) {
                    row.setLength(0);
                    for (int i = 0; i < BIOME_N; i++) {
                        row.append(letter(manager.getBiome(pos.set((BIOME_Q0 + i) * 4, 64, (BIOME_Q0 + j) * 4))));
                    }
                    w.println(row);
                }
            }
        }
    }

    /** The start piece's template: one of the four bastion kinds. */
    static String startKind(StructureStart<?> start) {
        String e = ((PoolElementStructurePiece) start.getPieces().get(0)).getElement().toString();
        return e.substring(e.indexOf("[Left[") + 6, e.indexOf("]]"));
    }

    static int countOf(List<String[]> list, String dim) {
        int n = 0;
        for (String[] s : list) {
            if (s[1].equals(dim)) {
                n++;
            }
        }
        return n;
    }

    static void dump(PrintWriter w, String header, Map<BlockPos, BlockState> world) {
        w.println("= " + header + " " + world.size());
        TreeMap<Long, String> sorted = new TreeMap<>();
        for (Map.Entry<BlockPos, BlockState> e : world.entrySet()) {
            sorted.put(e.getKey().asLong(), e.getKey().getX() + " " + e.getKey().getY() + " " + e.getKey().getZ() + " " + text(e.getValue()));
        }
        for (String line : sorted.values()) {
            w.println(line);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static String text(BlockState state) {
        StringBuilder b = new StringBuilder(Registry.BLOCK.getKey(state.getBlock()).toString());
        TreeMap<String, String> props = new TreeMap<>();
        for (Property p : state.getProperties()) {
            props.put(p.getName(), p.getName(state.getValue(p)));
        }
        if (!props.isEmpty()) {
            b.append('[');
            boolean first = true;
            for (Map.Entry<String, String> e : props.entrySet()) {
                if (!first) {
                    b.append(',');
                }
                first = false;
                b.append(e.getKey()).append('=').append(e.getValue());
            }
            b.append(']');
        }
        return b.toString();
    }

    static Field field(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /** A world of air that remembers what is placed: getBlockState, setBlock, fluids and heights; no tiles or entities. */
    static WorldGenLevel fake(Map<BlockPos, BlockState> world, long seed, boolean nether) {
        Object dimension = access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(nether
                ? net.minecraft.world.level.dimension.DimensionType.NETHER_LOCATION : net.minecraft.world.level.dimension.DimensionType.OVERWORLD_LOCATION);
        InvocationHandler h = (proxy, m, a) -> {
            switch (m.getName()) {
                case "dimensionType":
                    return dimension;
                case "getBlockTicks":
                case "getLiquidTicks":
                    return net.minecraft.world.level.EmptyTickList.empty();
                case "getBlockState":
                    return world.getOrDefault(((BlockPos) a[0]).immutable(), Blocks.AIR.defaultBlockState());
                case "getFluidState":
                    return world.getOrDefault(((BlockPos) a[0]).immutable(), Blocks.AIR.defaultBlockState()).getFluidState();
                case "setBlock":
                    world.put(((BlockPos) a[0]).immutable(), (BlockState) a[1]);
                    return true;
                case "removeBlock":
                    world.put(((BlockPos) a[0]).immutable(), Blocks.AIR.defaultBlockState());
                    return true;
                case "isStateAtPosition":
                    return ((java.util.function.Predicate<BlockState>) a[1]).test(world.getOrDefault(((BlockPos) a[0]).immutable(), Blocks.AIR.defaultBlockState()));
                case "getBlockEntity":
                case "getLevel":
                case "getServer":
                    return null;
                case "getSeed":
                    return seed;
                case "getRandom":
                    return new Random(0L);
                case "getHeight":
                    if (a != null && a.length == 3) {
                        int top = 0;
                        for (Map.Entry<BlockPos, BlockState> e : world.entrySet()) {
                            if (e.getKey().getX() == (Integer) a[1] && e.getKey().getZ() == (Integer) a[2] && !e.getValue().isAir()) {
                                top = Math.max(top, e.getKey().getY() + 1);
                            }
                        }
                        return top;
                    }
                    return 256;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == a[0];
                case "toString":
                    return "oracle world";
                default:
                    if (m.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, m, a);
                    }
                    Class<?> r = m.getReturnType();
                    if (r == boolean.class) {
                        return false;
                    }
                    if (r == int.class || r == long.class || r == float.class || r == double.class || r == short.class || r == byte.class) {
                        return r == int.class ? (Object) 0 : r == long.class ? (Object) 0L : r == float.class ? (Object) 0F : r == double.class ? (Object) 0D
                                : r == short.class ? (Object) (short) 0 : (Object) (byte) 0;
                    }
                    return null;
            }
        };
        return (WorldGenLevel) Proxy.newProxyInstance(StructureOracle.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class}, h);
    }

    static PrintWriter writer(File f) throws Exception {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
    }

    static PrintWriter gzWriter(File f) throws Exception {
        return new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8));
    }
}
