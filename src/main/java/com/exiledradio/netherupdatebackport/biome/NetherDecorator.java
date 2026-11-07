package com.exiledradio.netherupdatebackport.biome;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.fungus.BlockNetherVines;
import com.exiledradio.netherupdatebackport.fungus.HugeFungus;
import com.exiledradio.netherupdatebackport.fungus.NetherVegetation;
import com.exiledradio.netherupdatebackport.fungus.NetherWood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBush;
import net.minecraft.world.gen.feature.WorldGenHellLava;
import net.minecraft.world.gen.feature.WorldGenMinable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The four biomes' features, and nether wastes' 1.16 additions, from 1.16.5's {@code BiomeMaker},
 * {@code Features} and the feature classes. 1.12.2's own nether decoration (fire, glowstone, nether mushrooms,
 * quartz, magma, open and closed lava springs) already matches 1.16's for every biome and keeps running, except
 * where {@link BiomeContent} turns a piece of it off for a biome that 1.16 leaves it out of.
 *
 * <h2>Never cascading</h2>
 * Population of chunk (x, z) may touch only the four chunks (x..x+1, z..z+1). Every feature starts at the chunk
 * origin + 8 + 0..15, and every block read or written goes through {@link Region}, which reads outside the four
 * chunks as bedrock (what every feature here treats as "leave alone") and drops writes there. The few 1.16
 * features wider than 8 blocks (large basalt columns, wide deltas) are clipped at that edge. Placement uses flag
 * 2: no neighbor updates, so nothing reaches past the edge either.
 */
public final class NetherDecorator {

    private NetherDecorator() {
    }

    /** The four chunks a population may touch, x and z from {@code min} (inclusive) to {@code min + 32}. */
    public static final class Region {
        final World world;
        final int minX;
        final int minZ;
        private final IBlockState outside = Blocks.BEDROCK.getDefaultState();

        public Region(World world, int chunkX, int chunkZ) {
            this.world = world;
            this.minX = chunkX << 4;
            this.minZ = chunkZ << 4;
        }

        public boolean inside(BlockPos p) {
            return p.getX() >= this.minX && p.getX() < this.minX + 32 && p.getZ() >= this.minZ && p.getZ() < this.minZ + 32
                    && p.getY() >= 0 && p.getY() < 256;
        }

        public IBlockState get(BlockPos p) {
            return inside(p) ? this.world.getBlockState(p) : this.outside;
        }

        public Block block(BlockPos p) {
            return get(p).getBlock();
        }

        public boolean isAir(BlockPos p) {
            return inside(p) && this.world.isAirBlock(p);
        }

        /**
         * @return true if a block may be placed here: inside, and not on the region's outer ring, since placing a
         * block reads its neighbors (observers, liquids mixing, fire) and those must be inside too
         */
        public boolean writable(BlockPos p) {
            return p.getX() > this.minX && p.getX() < this.minX + 31 && p.getZ() > this.minZ && p.getZ() < this.minZ + 31
                    && p.getY() >= 0 && p.getY() < 256;
        }

        /** Places a block without neighbor or observer updates (flags 2 | 16), or drops it where not writable. */
        public void set(BlockPos p, IBlockState state) {
            if (writable(p)) {
                this.world.setBlockState(p, state, 2 | 16);
            }
        }

        /** World.getHeight: the top block's y + 1 in the column (the nether's bedrock roof: 128). */
        public int height(int x, int z) {
            return this.world.getHeight(x, z);
        }
    }

    /** A feature placed at one position; returns whether it placed anything (as 1.16's {@code Feature.place}). */
    public interface Feature {
        boolean place(Region r, Random rand, BlockPos pos);
    }

    /** @return a pillar block's state standing upright (1.12.2's {@code BlockRotatedPillar} defaults to X) */
    static IBlockState upright(IBlockState state) {
        return state.getPropertyKeys().contains(BlockRotatedPillar.AXIS) ? state.withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y) : state;
    }

    // ------------------------------------------------------------------------------------------

    /** Decorates the populated area of chunk (chunkX, chunkZ) as the given biome. */
    public static void decorate(NetherBiomes.Kind kind, World world, Random rand, int chunkX, int chunkZ) {
        if (!NetherBiomes.active(world)) {
            return;
        }
        Region r = new Region(world, chunkX, chunkZ);
        BlockPos origin = new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8);
        Blocks16 b = Blocks16.lookUp();
        switch (kind) {
            case NETHER_WASTES:
                defaultMushrooms(r, rand, origin);
                gravelOre(r, rand, origin);
                break;
            case SOUL_SAND_VALLEY:
                // LOCAL_MODIFICATIONS, then UNDERGROUND_DECORATION, as 1.16 orders them.
                if (b.basalt != null) {
                    ranged(r, rand, origin, 10, 128, (rr, rd, p) -> basaltPillar(rr, rd, p, b.basalt));
                }
                // PATCH_CRIMSON_ROOTS is only .range(128): one patch at the chunk's own x and z (here its middle).
                patch(r, rand, origin.up(rand.nextInt(128)), b.crimsonRoots, null);
                if (b.soulFire != null && b.soulSoil != null) {
                    firePatches(r, rand, origin, b.soulFire, b.soulSoil);
                }
                ore(r, rand, origin, Blocks.SOUL_SAND.getDefaultState(), 12, 12, 0, 32);
                gravelOre(r, rand, origin);
                break;
            case BASALT_DELTAS:
                // SURFACE_STRUCTURES: deltas, small and large basalt columns.
                multilayer(r, rand, origin, 40, NetherDecorator::delta);
                if (b.basalt != null) {
                    multilayer(r, rand, origin, 4, (rr, rd, p) -> basaltColumns(rr, rd, p, 1, 1, 1, 3, b.basalt));
                    multilayer(r, rand, origin, 2, (rr, rd, p) -> basaltColumns(rr, rd, p, 2, 1, 5, 5, b.basalt));
                    // UNDERGROUND_DECORATION
                    ranged(r, rand, origin, 75, 128, (rr, rd, p) -> replaceBlob(rr, rd, p, b.basalt));
                }
                if (b.blackstone != null) {
                    ranged(r, rand, origin, 25, 128, (rr, rd, p) -> replaceBlob(rr, rd, p, b.blackstone));
                }
                deltaSprings(r, rand, origin, b);
                // 1.16 doubles closed springs, quartz and gold here (SPRING_CLOSED_DOUBLE, ORE_QUARTZ_DELTAS,
                // ORE_GOLD_DELTAS): 1.12.2's population and stream U already place one helping each.
                WorldGenHellLava closed = new WorldGenHellLava(Blocks.FLOWING_LAVA, true);
                for (int i = 0; i < 16; i++) {
                    closed.generate(world, rand, squared(rand, origin, 10, 108));
                }
                ore(r, rand, origin, Blocks.QUARTZ_ORE.getDefaultState(), 14, 16, 10, 108);
                if (b.netherGoldOre != null && ModConfig.blocks.generateGold) {
                    ore(r, rand, origin, b.netherGoldOre, 10, 10, 10, 108);
                }
                break;
            case CRIMSON_FOREST:
                defaultMushrooms(r, rand, origin);
                gravelOre(r, rand, origin);
                // VEGETAL_DECORATION: weeping vines, crimson fungi, crimson forest vegetation.
                if (b.weepingVines) {
                    ranged(r, rand, origin, 10, 128, NetherDecorator::weepingVines);
                }
                if (b.crimsonNylium != null) {
                    multilayer(r, rand, origin, 8, (rr, rd, p) -> HugeFungus.place(rr.world, rd, p, NetherWood.CRIMSON, false));
                    multilayer(r, rand, origin, 6, (rr, rd, p) -> NetherVegetation.place(rr.world, rd, p, NetherVegetation.CRIMSON_FOREST, 8, 4));
                }
                break;
            case WARPED_FOREST:
                defaultMushrooms(r, rand, origin);
                gravelOre(r, rand, origin);
                if (b.warpedNylium != null) {
                    multilayer(r, rand, origin, 8, (rr, rd, p) -> HugeFungus.place(rr.world, rd, p, NetherWood.WARPED, false));
                    multilayer(r, rand, origin, 5, (rr, rd, p) -> NetherVegetation.place(rr.world, rd, p, NetherVegetation.WARPED_FOREST, 8, 4));
                    multilayer(r, rand, origin, 4, (rr, rd, p) -> NetherVegetation.place(rr.world, rd, p, NetherVegetation.SPROUTS, 8, 4));
                    if (b.twistingVines != null && b.twistingVinesPlant != null) {
                        ranged(r, rand, origin, 10, 128, (rr, rd, p) -> twistingVines(rr, rd, p, b));
                    }
                }
                break;
            default:
                break;
        }
    }

    /** Other streams' blocks, by name; null (or false) when that stream is switched off. */
    public static final class Blocks16 {
        IBlockState basalt;
        IBlockState blackstone;
        IBlockState soulSoil;
        IBlockState soulFire;
        IBlockState crimsonRoots;
        IBlockState crimsonNylium;
        IBlockState warpedNylium;
        IBlockState netherGoldOre;
        boolean weepingVines;
        IBlockState twistingVines;
        IBlockState twistingVinesPlant;

        static Blocks16 lookUp() {
            Blocks16 b = new Blocks16();
            b.basalt = state("basalt");
            if (b.basalt != null) {
                b.basalt = upright(b.basalt);
            }
            b.blackstone = state("blackstone");
            b.soulSoil = state("soul_soil");
            b.soulFire = state("soul_fire");
            b.crimsonRoots = state("crimson_roots");
            b.crimsonNylium = state("crimson_nylium");
            b.warpedNylium = state("warped_nylium");
            b.netherGoldOre = state("nether_gold_ore");
            b.weepingVines = Owners.ourBlock("weeping_vines") != null;
            b.twistingVines = state("twisting_vines");
            b.twistingVinesPlant = state("twisting_vines_plant");
            return b;
        }

        private static IBlockState state(String name) {
            Block block = Owners.ourBlock(name);
            return block == null ? null : block.getDefaultState();
        }
    }

    // ------------------------------------------------------------------------------------------
    // Placements
    // ------------------------------------------------------------------------------------------

    /** {@code .squared()} then a range: x and z in the chunk, then y from minY to minY + range - 1, in 1.16's order. */
    private static BlockPos squared(Random rand, BlockPos origin, int minY, int range) {
        int x = rand.nextInt(16);
        int z = rand.nextInt(16);
        return origin.add(x, minY + rand.nextInt(range), z);
    }

    /** {@code .range(height).squared().count(count)}: each try at x, z in the chunk and y from 0 to height - 1. */
    private static void ranged(Region r, Random rand, BlockPos origin, int count, int height, Feature f) {
        for (int i = 0; i < count; i++) {
            int x = rand.nextInt(16);
            int z = rand.nextInt(16);
            int y = rand.nextInt(height);
            f.place(r, rand, origin.add(x, y, z));
        }
    }

    /**
     * 1.16's {@code CountMultilayerPlacement}: count tries on the topmost floor of random columns, then count on the
     * floor below that, and so on until a layer places nothing.
     */
    public static void multilayer(Region r, Random rand, BlockPos origin, int count, Feature f) {
        List<BlockPos> list = new ArrayList<BlockPos>();
        int layer = 0;
        boolean found;
        do {
            found = false;
            for (int j = 0; j < count; ++j) {
                int x = rand.nextInt(16) + origin.getX();
                int z = rand.nextInt(16) + origin.getZ();
                int top = r.height(x, z);
                int y = floorAt(r, x, top, z, layer);
                if (y != Integer.MAX_VALUE) {
                    list.add(new BlockPos(x, y, z));
                    found = true;
                }
            }
            ++layer;
        } while (found);
        for (BlockPos p : list) {
            f.place(r, rand, p);
        }
    }

    private static int floorAt(Region r, int x, int top, int z, int layer) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
        int i = 0;
        IBlockState above = r.get(p);
        for (int y = top; y >= 1; --y) {
            p.setY(y - 1);
            IBlockState here = r.get(p);
            if (!isEmpty(here) && isEmpty(above) && here.getBlock() != Blocks.BEDROCK) {
                if (i == layer) {
                    return p.getY() + 1;
                }
                ++i;
            }
            above = here;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean isEmpty(IBlockState s) {
        Material m = s.getMaterial();
        return m == Material.AIR || m == Material.WATER || m == Material.LAVA;
    }

    /** WorldGenMinable over netherrack, positions as 1.12.2's own ores take them (it adds 8 itself). */
    private static void ore(Region r, Random rand, BlockPos origin, IBlockState state, int size, int count, int minY, int range) {
        WorldGenMinable gen = new WorldGenMinable(state, size, BlockMatcher.forBlock(Blocks.NETHERRACK));
        BlockPos corner = origin.add(-8, 0, -8);
        for (int i = 0; i < count; i++) {
            gen.generate(r.world, rand, squared(rand, corner, minY, range));
        }
    }

    /** {@code ORE_GRAVEL_NETHER}: 2 veins of 33 at y 5-41, in every biome but basalt deltas. */
    private static void gravelOre(Region r, Random rand, BlockPos origin) {
        ore(r, rand, origin, Blocks.GRAVEL.getDefaultState(), 33, 2, 5, 37);
    }

    /** {@code BROWN_MUSHROOM_NORMAL} (1 in 4) and {@code RED_MUSHROOM_NORMAL} (1 in 8), y up to twice the top. */
    private static void defaultMushrooms(Region r, Random rand, BlockPos origin) {
        mushroom(r, rand, origin, 4, new WorldGenBush(Blocks.BROWN_MUSHROOM));
        mushroom(r, rand, origin, 8, new WorldGenBush(Blocks.RED_MUSHROOM));
    }

    private static void mushroom(Region r, Random rand, BlockPos origin, int chance, WorldGenBush bush) {
        if (rand.nextFloat() < 1.0F / chance) {
            int x = rand.nextInt(16);
            int z = rand.nextInt(16);
            int top = r.height(origin.getX() + x, origin.getZ() + z);
            if (top > 0) {
                int y = rand.nextInt(top * 2);
                if (y < 255) {
                    bush.generate(r.world, rand, origin.add(x, y, z));
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Features
    // ------------------------------------------------------------------------------------------

    /** {@code RandomPatchFeature} without projection: 64 tries within 7 across and 3 up or down. */
    private static boolean patch(Region r, Random rand, BlockPos pos, IBlockState state, Block onlyOn) {
        if (state == null) {
            return false;
        }
        int placed = 0;
        for (int j = 0; j < 64; ++j) {
            BlockPos p = pos.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (r.isAir(p) && r.inside(p.down()) && (onlyOn == null || r.block(p.down()) == onlyOn) && canSurvive(r.world, p, state)) {
                r.set(p, state);
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean canSurvive(World world, BlockPos p, IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockBush ? ((BlockBush) block).canBlockStay(world, p, state) : block.canPlaceBlockAt(world, p);
    }

    /** {@code PATCH_SOUL_FIRE}: the fire placement (1 to 10 tries, y 4-123) of patches on soul soil only. */
    private static void firePatches(Region r, Random rand, BlockPos origin, IBlockState fire, IBlockState soil) {
        int n = rand.nextInt(rand.nextInt(10) + 1) + 1;
        for (int i = 0; i < n; ++i) {
            patch(r, rand, squared(rand, origin, 4, 120), fire, soil.getBlock());
        }
    }

    /** {@code SPRING_DELTA}, 16 per chunk at y 4-123, in place of the open springs 1.16 leaves out of deltas. */
    private static void deltaSprings(Region r, Random rand, BlockPos origin, Blocks16 b) {
        Set<Block> valid = new HashSet<Block>(Arrays.asList(Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.GRAVEL, Blocks.MAGMA));
        if (b.blackstone != null) {
            valid.add(b.blackstone.getBlock());
        }
        for (int i = 0; i < 16; i++) {
            spring(r, rand, squared(rand, origin, 4, 120), valid, true, 4, 1);
        }
    }

    /** 1.16's {@code SpringFeature} for lava. */
    public static boolean spring(Region r, Random rand, BlockPos pos, Set<Block> valid, boolean needsBlockBelow, int rockCount, int holeCount) {
        if (!valid.contains(r.block(pos.up()))) {
            return false;
        }
        if (needsBlockBelow && !valid.contains(r.block(pos.down()))) {
            return false;
        }
        IBlockState here = r.get(pos);
        if (here.getMaterial() != Material.AIR && !valid.contains(here.getBlock())) {
            return false;
        }
        int rock = 0;
        int hole = 0;
        for (EnumFacing f : new EnumFacing[] {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.DOWN}) {
            BlockPos n = pos.offset(f);
            if (valid.contains(r.block(n))) {
                rock++;
            }
            if (r.isAir(n)) {
                hole++;
            }
        }
        if (rock == rockCount && hole == holeCount && r.writable(pos)) {
            IBlockState lava = Blocks.FLOWING_LAVA.getDefaultState();
            r.world.setBlockState(pos, lava, 2 | 16);
            r.world.immediateBlockTick(pos, lava, rand);
            return true;
        }
        return false;
    }

    /** 1.16's {@code BasaltPillarFeature}: hangs from a ceiling down to the floor, with a ragged base. */
    public static boolean basaltPillar(Region r, Random rand, BlockPos pos, IBlockState basalt) {
        if (!r.isAir(pos) || r.isAir(pos.up())) {
            return false;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(pos);
        boolean n = true;
        boolean s = true;
        boolean w = true;
        boolean e = true;
        while (r.isAir(p)) {
            if (p.getY() < 0 || p.getY() >= 256) {
                return true;
            }
            r.set(p, basalt);
            n = n && hangOff(r, rand, p.north(), basalt);
            s = s && hangOff(r, rand, p.south(), basalt);
            w = w && hangOff(r, rand, p.west(), basalt);
            e = e && hangOff(r, rand, p.east(), basalt);
            p.setY(p.getY() - 1);
        }
        p.setY(p.getY() + 1);
        baseHangOff(r, rand, p.north(), basalt);
        baseHangOff(r, rand, p.south(), basalt);
        baseHangOff(r, rand, p.west(), basalt);
        baseHangOff(r, rand, p.east(), basalt);
        p.setY(p.getY() - 1);
        for (int i = -3; i < 4; ++i) {
            for (int j = -3; j < 4; ++j) {
                int k = Math.abs(i) * Math.abs(j);
                if (rand.nextInt(10) < 10 - k) {
                    BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos(p.getX() + i, p.getY(), p.getZ() + j);
                    int l = 3;
                    while (r.isAir(q.down())) {
                        q.setY(q.getY() - 1);
                        --l;
                        if (l <= 0) {
                            break;
                        }
                    }
                    if (!r.isAir(q.down())) {
                        r.set(q, basalt);
                    }
                }
            }
        }
        return true;
    }

    private static void baseHangOff(Region r, Random rand, BlockPos p, IBlockState basalt) {
        if (rand.nextBoolean()) {
            r.set(p, basalt);
        }
    }

    private static boolean hangOff(Region r, Random rand, BlockPos p, IBlockState basalt) {
        if (rand.nextInt(10) != 0) {
            r.set(p, basalt);
            return true;
        }
        return false;
    }

    /** {@code FeatureSpread.of(base, spread).sample}. */
    private static int spread(Random rand, int base, int spread) {
        return spread == 0 ? base : base + rand.nextInt(spread + 1);
    }

    private static final Set<Block> COLUMN_CANNOT_PLACE_ON = new HashSet<Block>(Arrays.asList(Blocks.LAVA, Blocks.FLOWING_LAVA, Blocks.BEDROCK,
            Blocks.MAGMA, Blocks.SOUL_SAND, Blocks.NETHER_BRICK, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART,
            Blocks.CHEST, Blocks.MOB_SPAWNER));

    /** 1.16's {@code BasaltColumnFeature}, reach and height as {@code FeatureSpread}s. */
    public static boolean basaltColumns(Region r, Random rand, BlockPos pos, int reachBase, int reachSpread, int heightBase, int heightSpread,
                                 IBlockState basalt) {
        int sea = 32;
        if (!canPlaceColumnAt(r, sea, pos)) {
            return false;
        }
        int j = spread(rand, heightBase, heightSpread);
        boolean flag = rand.nextFloat() < 0.9F;
        int k = Math.min(j, flag ? 5 : 8);
        int l = flag ? 50 : 15;
        boolean placed = false;
        int w = 2 * k + 1;
        for (int n = 0; n < l; n++) {
            BlockPos p = new BlockPos(pos.getX() - k + rand.nextInt(w), pos.getY() + rand.nextInt(1), pos.getZ() - k + rand.nextInt(w));
            int i1 = j - manhattan(p, pos);
            if (i1 >= 0) {
                placed |= placeColumn(r, sea, p, i1, spread(rand, reachBase, reachSpread), basalt);
            }
        }
        return placed;
    }

    private static boolean placeColumn(Region r, int sea, BlockPos pos, int height, int reach, IBlockState basalt) {
        boolean placed = false;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                BlockPos p = pos.add(dx, 0, dz);
                int i = Math.abs(dx) + Math.abs(dz);
                BlockPos start = airOrLavaOcean(r, sea, p) ? findSurface(r, sea, p, i) : findAir(r, p, i);
                if (start != null) {
                    BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(start);
                    for (int j = height - i / 2; j >= 0; --j) {
                        if (airOrLavaOcean(r, sea, m)) {
                            r.set(m, basalt);
                            m.setY(m.getY() + 1);
                            placed = true;
                        } else {
                            if (r.get(m).getBlock() != basalt.getBlock()) {
                                break;
                            }
                            m.setY(m.getY() + 1);
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static BlockPos findSurface(Region r, int sea, BlockPos start, int steps) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(start);
        while (p.getY() > 1 && steps > 0) {
            --steps;
            if (canPlaceColumnAt(r, sea, p)) {
                return p.toImmutable();
            }
            p.setY(p.getY() - 1);
        }
        return null;
    }

    private static boolean canPlaceColumnAt(Region r, int sea, BlockPos p) {
        if (!airOrLavaOcean(r, sea, p)) {
            return false;
        }
        IBlockState below = r.get(p.down());
        return below.getMaterial() != Material.AIR && !COLUMN_CANNOT_PLACE_ON.contains(below.getBlock());
    }

    private static BlockPos findAir(Region r, BlockPos start, int steps) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(start);
        while (p.getY() < 256 && steps > 0) {
            --steps;
            IBlockState s = r.get(p);
            if (COLUMN_CANNOT_PLACE_ON.contains(s.getBlock())) {
                return null;
            }
            if (s.getMaterial() == Material.AIR) {
                return p.toImmutable();
            }
            p.setY(p.getY() + 1);
        }
        return null;
    }

    private static boolean airOrLavaOcean(Region r, int sea, BlockPos p) {
        IBlockState s = r.get(p);
        return r.inside(p) && (s.getMaterial() == Material.AIR || s.getMaterial() == Material.LAVA && p.getY() <= sea);
    }

    private static int manhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    /**
     * 1.16's {@code BlockPos.withinManhattan}: every offset within the box, nearest (Manhattan) first, in vanilla's
     * order inside each distance.
     */
    public static List<BlockPos> withinManhattan(BlockPos c, int xs, int ys, int zs) {
        List<BlockPos> out = new ArrayList<BlockPos>();
        int max = xs + ys + zs;
        for (int depth = 0; depth <= max; depth++) {
            int maxX = Math.min(xs, depth);
            for (int x = -maxX; x <= maxX; x++) {
                int maxY = Math.min(ys, depth - Math.abs(x));
                for (int y = -maxY; y <= maxY; y++) {
                    int z = depth - Math.abs(x) - Math.abs(y);
                    if (z <= zs) {
                        out.add(c.add(x, y, z));
                        if (z != 0) {
                            out.add(c.add(x, y, -z));
                        }
                    }
                }
            }
        }
        return out;
    }

    private static final Set<Block> DELTA_CANNOT_REPLACE = new HashSet<Block>(Arrays.asList(Blocks.BEDROCK, Blocks.NETHER_BRICK,
            Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART, Blocks.CHEST, Blocks.MOB_SPAWNER));

    /** 1.16's {@code BasaltDeltasStructure} with {@code DELTA}'s config: lava pools (3-7 across) with magma rims (0-2). */
    public static boolean delta(Region r, Random rand, BlockPos pos) {
        IBlockState contents = Blocks.LAVA.getDefaultState();
        IBlockState rim = Blocks.MAGMA.getDefaultState();
        boolean placed = false;
        boolean withRim = rand.nextDouble() < 0.9D;
        int i = withRim ? spread(rand, 0, 2) : 0;
        int j = withRim ? spread(rand, 0, 2) : 0;
        boolean rimmed = withRim && i != 0 && j != 0;
        int k = spread(rand, 3, 4);
        int l = spread(rand, 3, 4);
        int max = Math.max(k, l);
        for (BlockPos p : withinManhattan(pos, k, 0, l)) {
            if (manhattan(p, pos) > max) {
                break;
            }
            if (deltaClear(r, p, contents)) {
                if (rimmed) {
                    placed = true;
                    r.set(p, rim);
                }
                BlockPos q = p.add(i, 0, j);
                if (deltaClear(r, q, contents)) {
                    placed = true;
                    r.set(q, contents);
                }
            }
        }
        return placed;
    }

    private static boolean deltaClear(Region r, BlockPos p, IBlockState contents) {
        IBlockState s = r.get(p);
        if (s.getBlock() == contents.getBlock() || DELTA_CANNOT_REPLACE.contains(s.getBlock()) || !r.inside(p)) {
            return false;
        }
        for (EnumFacing f : EnumFacing.values()) {
            boolean air = r.get(p.offset(f)).getMaterial() == Material.AIR;
            if (air && f != EnumFacing.UP || !air && f == EnumFacing.UP) {
                return false;
            }
        }
        return true;
    }

    /** 1.16's {@code NetherackBlobReplacementStructure}: radius 3-7 (Manhattan) of netherrack turned into a block. */
    public static boolean replaceBlob(Region r, Random rand, BlockPos pos, IBlockState with) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(pos.getX(), Math.max(1, Math.min(255, pos.getY())), pos.getZ());
        boolean found = false;
        while (p.getY() > 1) {
            if (r.block(p) == Blocks.NETHERRACK) {
                found = true;
                break;
            }
            p.setY(p.getY() - 1);
        }
        if (!found) {
            return false;
        }
        int radius = spread(rand, 3, 4);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            int ry = radius - Math.abs(dx);
            for (int dy = -ry; dy <= ry; dy++) {
                int rz = ry - Math.abs(dy);
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos q = p.add(dx, dy, dz);
                    if (r.block(q) == Blocks.NETHERRACK) {
                        r.set(q, with);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /**
     * 1.16's {@code TwistingVineFeature} (spread 8, height 4, length up to 8), through the region: its 8-block spread
     * reaches the region's edge, which stream F's version (written for bone meal) does not guard.
     */
    public static boolean twistingVines(Region r, Random rand, BlockPos pos, Blocks16 b) {
        if (invalidTwisting(r, pos, b)) {
            return false;
        }
        for (int i = 0; i < 8 * 8; ++i) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(pos.getX() + nextInt(rand, -8, 8), pos.getY() + nextInt(rand, -4, 4),
                    pos.getZ() + nextInt(rand, -8, 8));
            if (firstAirAboveGround(r, p) && !invalidTwisting(r, p, b)) {
                int length = nextInt(rand, 1, 8);
                if (rand.nextInt(6) == 0) {
                    length *= 2;
                }
                if (rand.nextInt(5) == 0) {
                    length = 1;
                }
                for (int j = 1; j <= length; ++j) {
                    if (r.isAir(p)) {
                        if (j == length || !r.isAir(p.up())) {
                            int age25 = nextInt(rand, 17, 25);
                            r.set(p, b.twistingVines.withProperty(BlockNetherVines.AGE,
                                    Math.min(BlockNetherVines.MAX_AGE, age25 * (BlockNetherVines.MAX_AGE + 1) / 25)));
                            break;
                        }
                        r.set(p, b.twistingVinesPlant);
                    }
                    p.setY(p.getY() + 1);
                }
            }
        }
        return true;
    }

    /** {@code MathHelper.nextInt(random, min, max)}. */
    private static int nextInt(Random rand, int min, int max) {
        return min >= max ? min : rand.nextInt(max - min + 1) + min;
    }

    private static boolean firstAirAboveGround(Region r, BlockPos.MutableBlockPos p) {
        do {
            p.setY(p.getY() - 1);
            if (p.getY() < 0 || p.getY() >= 256) {
                return false;
            }
        } while (r.isAir(p));
        p.setY(p.getY() + 1);
        return true;
    }

    private static boolean invalidTwisting(Region r, BlockPos p, Blocks16 b) {
        if (!r.isAir(p)) {
            return true;
        }
        Block below = r.block(p.down());
        return below != Blocks.NETHERRACK && below != Owners.ourBlock("warped_nylium") && below != Owners.ourBlock("warped_wart_block");
    }

    /** 1.16's {@code WeepingVineFeature}: a clump of nether wart on the ceiling, with weeping vines hanging from it. */
    public static boolean weepingVines(Region r, Random rand, BlockPos pos) {
        if (!r.isAir(pos)) {
            return false;
        }
        Block above = r.block(pos.up());
        if (above != Blocks.NETHERRACK && above != Blocks.NETHER_WART_BLOCK) {
            return false;
        }
        IBlockState wart = Blocks.NETHER_WART_BLOCK.getDefaultState();
        r.set(pos, wart);
        for (int i = 0; i < 200; ++i) {
            BlockPos p = pos.add(rand.nextInt(6) - rand.nextInt(6), rand.nextInt(2) - rand.nextInt(5), rand.nextInt(6) - rand.nextInt(6));
            if (r.isAir(p)) {
                int j = 0;
                for (EnumFacing f : EnumFacing.values()) {
                    Block n = r.block(p.offset(f));
                    if (n == Blocks.NETHERRACK || n == Blocks.NETHER_WART_BLOCK) {
                        ++j;
                    }
                    if (j > 1) {
                        break;
                    }
                }
                if (j == 1) {
                    r.set(p, wart);
                }
            }
        }
        for (int i = 0; i < 100; ++i) {
            BlockPos p = pos.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(2) - rand.nextInt(7), rand.nextInt(8) - rand.nextInt(8));
            if (r.isAir(p)) {
                Block up = r.block(p.up());
                if (up == Blocks.NETHERRACK || up == Blocks.NETHER_WART_BLOCK) {
                    int length = 1 + rand.nextInt(8);
                    if (rand.nextInt(6) == 0) {
                        length *= 2;
                    }
                    if (rand.nextInt(5) == 0) {
                        length = 1;
                    }
                    NetherVegetation.weepingColumn(r.world, rand, p, length, 17, 25);
                }
            }
        }
        return true;
    }
}
