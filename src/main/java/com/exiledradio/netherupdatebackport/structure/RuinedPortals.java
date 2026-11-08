package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.VanillaPlacements;
import com.exiledradio.backportcore.terrain.NetherHeights;

import net.minecraft.init.Biomes;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 1.16.5's ruined portals ({@code RuinedPortalStructure}), in the overworld and the nether, ported line for line.
 *
 * <ul>
 *   <li><b>Placement</b>: one candidate chunk per region, salt 34222645; 40 chunks apart (15 apart at least) in the
 *       overworld and, as 1.16.5's nether settings change it ({@code DimensionSettings.nether}), 25 and 10 in the
 *       nether.</li>
 *   <li><b>Variant by biome</b>: the noise biome at the chunk's middle quart lists one ruined portal type
 *       ({@link Type}); in the nether every biome lists the nether one.</li>
 *   <li><b>The start</b> (1.16's {@code setLargeFeatureSeed} random): the type's placement and settings, then giant
 *       (one in twenty) or normal template, rotation, mirror; the height from the ground at the box's middle and
 *       {@code findSuitableY}'s test of the four corners against 1.16's base column (bare noise, through Core's
 *       {@link NetherHeights#baseForWorld}); coldness from the biome's temperature there.</li>
 * </ul>
 */
public final class RuinedPortals extends ScatteredStructure {

    /** 1.16.5's {@code DimensionSettings.nether}: ruined portals 25 apart, 10 at least, in the nether. */
    public static final ScatteredPlacement NETHER_PLACEMENT = VanillaPlacements.V1_16_5.RUINED_PORTAL_NETHER;

    static final String[] PORTALS = {"ruined_portal/portal_1", "ruined_portal/portal_2", "ruined_portal/portal_3",
            "ruined_portal/portal_4", "ruined_portal/portal_5", "ruined_portal/portal_6", "ruined_portal/portal_7",
            "ruined_portal/portal_8", "ruined_portal/portal_9", "ruined_portal/portal_10"};
    static final String[] GIANT_PORTALS = {"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2",
            "ruined_portal/giant_portal_3"};

    /** 1.16's {@code RuinedPortalStructure.Location}: which configured ruined portal a biome lists. */
    public enum Type {
        STANDARD, DESERT, JUNGLE, SWAMP, MOUNTAIN, OCEAN, NETHER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** 1.16's {@code RuinedPortalPiece.Location}. */
    public enum Placement {
        ON_LAND_SURFACE, PARTLY_BURIED, ON_OCEAN_FLOOR, IN_MOUNTAIN, UNDERGROUND, IN_NETHER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** {@code RuinedPortalPiece.getHeightMapType}: true for {@code OCEAN_FLOOR_WG}, false for {@code WORLD_SURFACE_WG}. */
        boolean oceanFloor() {
            return this == ON_OCEAN_FLOOR;
        }
    }

    /** Each 1.16.5 biome's ruined portal, by numeric id (1.12.2's biomes keep vanilla's ids), from the oracle. */
    private static final Map<Integer, Type> BY_ID = new HashMap<Integer, Type>();

    static {
        String table = "0 O,1 S,2 D,3 M,4 S,5 S,6 W,7 S,10 O,11 S,12 S,13 M,14 S,15 S,16 S,17 D,18 S,19 S,20 M,21 J,22 J,23 J,24 O,"
                + "25 M,26 S,27 S,28 S,29 S,30 S,31 S,32 S,33 S,34 M,35 S,36 M,37 S,38 M,39 M,44 O,45 O,46 O,47 O,48 O,49 O,50 O,"
                + "129 S,130 D,131 M,132 S,133 M,134 W,140 S,149 J,151 J,155 S,156 S,157 S,158 M,160 S,161 S,162 M,163 M,164 M,"
                + "165 M,166 M,167 M,168 J,169 J";
        for (String entry : table.split(",")) {
            String[] p = entry.split(" ");
            Type t;
            switch (p[1]) {
                case "O": t = Type.OCEAN; break;
                case "D": t = Type.DESERT; break;
                case "M": t = Type.MOUNTAIN; break;
                case "J": t = Type.JUNGLE; break;
                case "W": t = Type.SWAMP; break;
                default: t = Type.STANDARD;
            }
            BY_ID.put(Integer.parseInt(p[0]), t);
        }
    }

    final boolean nether;

    RuinedPortals(boolean nether) {
        super("NUB_RuinedPortal", nether ? NETHER_PLACEMENT : VanillaPlacements.V1_16_5.RUINED_PORTAL);
        this.nether = nether;
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return type(world, chunkX, chunkZ) != null;
    }

    /** @return the ruined portal the biome at the chunk's middle quart lists, or null for none (the end, the void) */
    @Nullable
    Type type(World world, int chunkX, int chunkZ) {
        if (this.nether) {
            return Type.NETHER;
        }
        return typeOf(quartBiome(world, chunkX, chunkZ));
    }

    /** The noise biome at the chunk's middle quart: 1.12.2's biomes at 1:4, as 1.16's {@code getNoiseBiome} reads. */
    static Biome quartBiome(World world, int chunkX, int chunkZ) {
        Biome[] b = world.getBiomeProvider().getBiomesForGeneration(null, (chunkX << 2) + 2, (chunkZ << 2) + 2, 1, 1);
        return b == null || b.length == 0 || b[0] == null ? Biomes.PLAINS : b[0];
    }

    /** @return a biome's ruined portal: 1.16.5's for vanilla's biomes; for other mods' biomes, by their dictionary types */
    @Nullable
    public static Type typeOf(Biome biome) {
        int id = Biome.getIdForBiome(biome);
        boolean vanilla = biome.getRegistryName() != null && "minecraft".equals(biome.getRegistryName().getNamespace());
        Type known = BY_ID.get(id);
        if (vanilla) {
            return known;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER)) {
            return Type.NETHER;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.END) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.VOID)) {
            return null;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
            return Type.OCEAN;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE)) {
            return Type.JUNGLE;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)) {
            return Type.SWAMP;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY) && BiomeDictionary.hasType(biome, BiomeDictionary.Type.HOT)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA)) {
            return Type.DESERT;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.MOUNTAIN)) {
            return Type.MOUNTAIN;
        }
        return Type.STANDARD;
    }

    /**
     * Seed maps tell ruined portals apart by their type (the biome's configured ruined portal): Backport Core's
     * seed-parity dump names starts {@code ruined_portal_<type>} with it.
     */
    @Override
    @Nullable
    public String variantAt(World world, int chunkX, int chunkZ) {
        Type type = type(world, chunkX, chunkZ);
        return type == null ? null : type.id();
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        Type type = type(this.world, chunkX, chunkZ);
        return new Start(this.world, chunkX, chunkZ, type == null ? Type.STANDARD : type);
    }

    public static final class Start extends StructureStart {
        /** For loading. */
        public Start() {
        }

        Start(World world, int chunkX, int chunkZ, Type type) {
            super(chunkX, chunkZ);
            Layout layout = layout(world, world.getSeed(), chunkX, chunkZ, type, NetherHeights.baseForWorld(world));
            this.components.add(layout.piece());
            updateBoundingBox();
        }
    }

    /** What 1.16.5's start decides, before it becomes a piece: for the piece and the self-test. */
    public static final class Layout {
        public final Type type;
        public final Placement placement;
        public boolean airPocket;
        public float mossiness = 0.2F;
        public boolean overgrown;
        public boolean vines;
        public boolean blackstone;
        public boolean cold;
        public String template;
        public Rotation rotation;
        public Mirror mirror;
        public BlockPos pivot;
        public BlockPos position;
        /** {@code findSuitableY}'s height before the corner test (what the random decides alone). */
        public int firstY;
        public StructureBoundingBox box;
        final long seed;

        Layout(Type type, Placement placement, long seed) {
            this.type = type;
            this.placement = placement;
            this.seed = seed;
        }

        public RuinedPortalPiece piece() {
            return new RuinedPortalPiece(this, new Random(this.seed));
        }
    }

    /** A layout given whole, as another game decided it: for comparing with vanilla's own pieces. */
    public static Layout given(Type type, Placement placement, String template, Rotation rotation, Mirror mirror, BlockPos position,
                               boolean airPocket, boolean cold, float mossiness, boolean overgrown, boolean vines, boolean blackstone) {
        Layout l = new Layout(type, placement, position.toLong());
        l.template = template;
        l.rotation = rotation;
        l.mirror = mirror;
        ModernTemplate t = ModernTemplate.load(templateId(template));
        l.pivot = new BlockPos(t.size().getX() / 2, 0, t.size().getZ() / 2);
        l.position = position;
        l.airPocket = airPocket;
        l.cold = cold;
        l.mossiness = mossiness;
        l.overgrown = overgrown;
        l.vines = vines;
        l.blackstone = blackstone;
        l.box = t.boundingBox(position, TemplatePlacement.of(rotation, mirror).pivot(l.pivot));
        return l;
    }

    /**
     * {@code RuinedPortalStructure.Start.generatePieces}, line for line.
     *
     * @param base 1.16's base columns ({@link NetherHeights#baseForWorld}); null or unknown columns fall back to the
     *             sea level
     */
    public static Layout layout(World world, long seed, int chunkX, int chunkZ, Type type, @Nullable NetherHeights base) {
        Random random = StructureRandoms.start(seed, chunkX, chunkZ);
        Layout l;
        switch (type) {
            case DESERT:
                l = new Layout(type, Placement.PARTLY_BURIED, seed);
                l.airPocket = false;
                l.mossiness = 0.0F;
                break;
            case JUNGLE:
                l = new Layout(type, Placement.ON_LAND_SURFACE, seed);
                l.airPocket = random.nextFloat() < 0.5F;
                l.mossiness = 0.8F;
                l.overgrown = true;
                l.vines = true;
                break;
            case SWAMP:
                l = new Layout(type, Placement.ON_OCEAN_FLOOR, seed);
                l.airPocket = false;
                l.mossiness = 0.5F;
                l.vines = true;
                break;
            case MOUNTAIN: {
                boolean inMountain = random.nextFloat() < 0.5F;
                l = new Layout(type, inMountain ? Placement.IN_MOUNTAIN : Placement.ON_LAND_SURFACE, seed);
                l.airPocket = inMountain || random.nextFloat() < 0.5F;
                break;
            }
            case OCEAN:
                l = new Layout(type, Placement.ON_OCEAN_FLOOR, seed);
                l.airPocket = false;
                l.mossiness = 0.8F;
                break;
            case NETHER:
                l = new Layout(type, Placement.IN_NETHER, seed);
                l.airPocket = random.nextFloat() < 0.5F;
                l.mossiness = 0.0F;
                l.blackstone = true;
                break;
            default: {
                boolean underground = random.nextFloat() < 0.5F;
                l = new Layout(type, underground ? Placement.UNDERGROUND : Placement.ON_LAND_SURFACE, seed);
                l.airPocket = underground || random.nextFloat() < 0.5F;
            }
        }

        if (random.nextFloat() < 0.05F) {
            l.template = GIANT_PORTALS[random.nextInt(GIANT_PORTALS.length)];
        } else {
            l.template = PORTALS[random.nextInt(PORTALS.length)];
        }
        ModernTemplate template = ModernTemplate.load(templateId(l.template));
        l.rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        l.mirror = random.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
        l.pivot = new BlockPos(template.size().getX() / 2, 0, template.size().getZ() / 2);
        BlockPos corner = new BlockPos(chunkX << 4, 0, chunkZ << 4);
        TemplatePlacement p = TemplatePlacement.of(l.rotation, l.mirror).pivot(l.pivot);
        StructureBoundingBox box = template.boundingBox(corner, p);
        // MutableBoundingBox.getCenter
        int cx = box.minX + (box.maxX - box.minX + 1) / 2;
        int cz = box.minZ + (box.maxZ - box.minZ + 1) / 2;
        int k = baseHeight(world, base, cx, cz, l.placement.oceanFloor()) - 1;
        int y = findSuitableY(random, base, l, k, box.getYSize(), box);
        l.position = new BlockPos(corner.getX(), y, corner.getZ());
        if (type == Type.MOUNTAIN || type == Type.OCEAN || type == Type.STANDARD) {
            l.cold = quartBiome(world, chunkX, chunkZ).getTemperature(l.position) < 0.15F;
        }
        l.box = template.boundingBox(l.position, p);
        return l;
    }

    static ResourceLocation templateId(String path) {
        return JigsawPools.templateId(new ResourceLocation("minecraft", path));
    }

    /** {@code ChunkGenerator.getBaseHeight}: one above the top block that is not air, or above the top solid one. */
    private static int baseHeight(World world, @Nullable NetherHeights base, int x, int z, boolean oceanFloor) {
        int h = base == null ? NetherHeights.UNKNOWN : oceanFloor ? base.floor(x, z) : base.height(x, z);
        return h == NetherHeights.UNKNOWN ? world.getSeaLevel() + 1 : h;
    }

    private static int findSuitableY(Random random, @Nullable NetherHeights base, Layout l, int k, int ySpan, StructureBoundingBox box) {
        int i;
        switch (l.placement) {
            case IN_NETHER:
                if (l.airPocket) {
                    i = randomIntInclusive(random, 32, 100);
                } else if (random.nextFloat() < 0.5F) {
                    i = randomIntInclusive(random, 27, 29);
                } else {
                    i = randomIntInclusive(random, 29, 100);
                }
                break;
            case IN_MOUNTAIN:
                i = randomWithinInterval(random, 70, k - ySpan);
                break;
            case UNDERGROUND:
                i = randomWithinInterval(random, 15, k - ySpan);
                break;
            case PARTLY_BURIED:
                i = k - ySpan + randomIntInclusive(random, 2, 8);
                break;
            default:
                i = k;
        }
        l.firstY = i;
        int[][] corners = {{box.minX, box.minZ}, {box.maxX, box.minZ}, {box.minX, box.maxZ}, {box.maxX, box.maxZ}};
        boolean oceanFloor = l.placement.oceanFloor();
        int y;
        for (y = i; y > 15; --y) {
            int opaque = 0;
            for (int[] c : corners) {
                if (opaqueAt(base, c[0], y, c[1], oceanFloor)) {
                    ++opaque;
                    if (opaque == 3) {
                        return y;
                    }
                }
            }
        }
        return y;
    }

    /**
     * The heightmap's test on the base column: {@code WORLD_SURFACE_WG} counts anything but air, {@code OCEAN_FLOOR_WG}
     * only what blocks motion. A column that cannot be known counts, so the portal stays where the random put it.
     */
    private static boolean opaqueAt(@Nullable NetherHeights base, int x, int y, int z, boolean oceanFloor) {
        if (base == null || !base.known(x, z)) {
            return true;
        }
        return oceanFloor ? base.isSolid(x, y, z) : !base.isAir(x, y, z);
    }

    private static int randomIntInclusive(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private static int randomWithinInterval(Random random, int min, int max) {
        return min < max ? randomIntInclusive(random, min, max) : max;
    }
}
