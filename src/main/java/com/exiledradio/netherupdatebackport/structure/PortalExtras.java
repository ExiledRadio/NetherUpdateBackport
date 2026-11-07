package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.ModernProcessors;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplateBlock;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.TemplateProcessor;
import com.exiledradio.backportcore.terrain.NetherHeights;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * What 1.16.5's {@code RuinedPortalPiece.postProcess} does after the template: {@code spreadNetherrack},
 * {@code addNetherrackDripColumnsBelowPortal}, then vines and leaves over the piece's box, in 1.16's order and drawing
 * from 1.16's random: the start chunk's decoration seed with the ruined portal's feature index (5 in
 * {@code SURFACE_STRUCTURES}), after the template's chest took its loot seed from it.
 *
 * <p>1.16 reads the world as it stands when the start chunk is decorated. Here that is worked out ahead, without
 * loading a chunk: the terrain as it will generate (Core's {@link NetherHeights#forWorld}) under the portal's own blocks
 * (the template run through the piece's processors), then each write the extras make. Each chunk writes its share.
 */
final class PortalExtras {

    /** {@code GenerationStage.Decoration.SURFACE_STRUCTURES} and ruined portals' place in it (outposts, mansions, jungle temples, desert temples, igloos first). */
    static final int STEP = 4;
    static final int INDEX = 5;

    private static final float[] SPREAD = {1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.9F, 0.9F, 0.8F, 0.7F, 0.6F, 0.4F, 0.2F};
    /** {@code Direction.Plane.HORIZONTAL}'s order. */
    private static final EnumFacing[] HORIZONTAL = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

    final Map<BlockPos, String> writes = new LinkedHashMap<BlockPos, String>();
    final Map<BlockPos, Long> lootSeeds = new HashMap<BlockPos, Long>();

    private final Map<BlockPos, Cell> overlay = new HashMap<BlockPos, Cell>();
    private final Map<Long, Integer> overlayTop = new HashMap<Long, Integer>();
    @Nullable
    private final NetherHeights terrain;
    private final boolean nether;
    private final int seaLevel;
    private final RuinedPortalPiece piece;
    private final Random random;

    private PortalExtras(World world, RuinedPortalPiece piece) {
        this.piece = piece;
        this.nether = piece.verticalPlacement == RuinedPortals.Placement.IN_NETHER;
        this.terrain = NetherHeights.forWorld(world);
        this.seaLevel = world.getSeaLevel();
        BlockPos at = piece.position();
        this.random = new Random(featureSeed(decorationSeed(world.getSeed(), at.getX() >> 4 << 4, at.getZ() >> 4 << 4), INDEX, STEP));
    }

    /** 1.16's {@code WorldgenRandom.setDecorationSeed}. */
    static long decorationSeed(long worldSeed, int blockX, int blockZ) {
        Random r = new Random(worldSeed);
        long a = r.nextLong() | 1L;
        long b = r.nextLong() | 1L;
        return (long) blockX * a + (long) blockZ * b ^ worldSeed;
    }

    /** 1.16's {@code WorldgenRandom.setFeatureSeed}. */
    static long featureSeed(long decorationSeed, int index, int step) {
        return decorationSeed + (long) index + (long) (10000 * step);
    }

    static PortalExtras plan(World world, RuinedPortalPiece piece) {
        PortalExtras out = new PortalExtras(world, piece);
        out.template();
        out.spreadNetherrack();
        out.dripColumnsBelowPortal();
        if (piece.vines || piece.overgrown) {
            StructureBoundingBox b = piece.templateBox();
            for (int z = b.minZ; z <= b.maxZ; z++) {
                for (int y = b.minY; y <= b.maxY; y++) {
                    for (int x = b.minX; x <= b.maxX; x++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (piece.vines) {
                            out.maybeAddVines(pos);
                        }
                        if (piece.overgrown) {
                            out.maybeAddLeavesAbove(pos);
                        }
                    }
                }
            }
        }
        return out;
    }

    // ---- the template, through the piece's processors

    private void template() {
        ModernTemplate template = this.piece.loadTemplate();
        TemplatePlacement placement = this.piece.basePlacement();
        TemplateProcessor age = ModernProcessors.blockAge(this.piece.mossiness);
        TemplateProcessor black = ModernProcessors.blackstoneReplace();
        BlockPos origin = this.piece.position();
        int[] chests = {0};
        template.forEachBlock(this.piece.palette(), (local, block) -> {
            BlockPos pos = ModernTemplate.worldPos(origin, local, placement);
            if (!this.piece.airPocket && block.is("minecraft:air")) {
                return;
            }
            TemplateBlock b = rules(pos, block);
            b = age.process(null, pos, b, null);
            IBlockState s = b.state();
            if (cell(pos).is("minecraft:lava") && (s == null || !s.isFullCube())) {
                b = b.replacedBy("minecraft:lava");
            }
            if (this.piece.blackstone) {
                b = black.process(null, pos, b, null);
            }
            IBlockState placed = b.state() == null ? null : b.state().withMirror(this.piece.mirror()).withRotation(this.piece.rotation());
            set(pos, new Cell(b.name(), placed));
            if (b.state() != null && b.tile() != null && b.tile().hasKey("LootTable") && b.state().getBlock().hasTileEntity(b.state())) {
                TileEntity te = b.state().getBlock().createTileEntity(null, b.state());
                if (te instanceof net.minecraft.tileentity.TileEntityLockableLoot) {
                    chests[0]++;
                    this.lootSeeds.put(pos, null);
                }
            }
        });
        for (Map.Entry<BlockPos, Long> e : this.lootSeeds.entrySet()) {
            e.setValue(this.random.nextLong());
        }
    }

    /** The piece's rule processor, the way 1.16's draws: one random per position, first matching rule wins. */
    private TemplateBlock rules(BlockPos pos, TemplateBlock block) {
        Random r = new Random(positionSeed(pos));
        if (block.is("minecraft:gold_block") && r.nextFloat() < 0.3F) {
            return block.replacedBy("minecraft:air");
        }
        if (block.is("minecraft:lava")) {
            if (this.piece.verticalPlacement == RuinedPortals.Placement.ON_OCEAN_FLOOR) {
                return block.replacedBy("minecraft:magma_block");
            }
            if (this.piece.cold) {
                return block.replacedBy("minecraft:netherrack");
            }
            if (r.nextFloat() < 0.2F) {
                return block.replacedBy("minecraft:magma_block");
            }
        }
        if (!this.piece.cold && block.is("minecraft:netherrack") && r.nextFloat() < 0.07F) {
            return block.replacedBy("minecraft:magma_block");
        }
        return block;
    }

    /** {@code MathHelper.getSeed}: the processors' per-position random. */
    static long positionSeed(BlockPos pos) {
        long l = (long) (pos.getX() * 3129871) ^ (long) pos.getZ() * 116129781L ^ (long) pos.getY();
        l = l * l * 42317861L + l * 11L;
        return l >> 16;
    }

    // ---- RuinedPortalPiece's own steps

    private void spreadNetherrack() {
        boolean onSurface = this.piece.verticalPlacement == RuinedPortals.Placement.ON_LAND_SURFACE
                || this.piece.verticalPlacement == RuinedPortals.Placement.ON_OCEAN_FLOOR;
        StructureBoundingBox box = this.piece.templateBox();
        int i = box.minX + (box.maxX - box.minX + 1) / 2;
        int j = box.minZ + (box.maxZ - box.minZ + 1) / 2;
        int k = SPREAD.length;
        int l = (box.getXSize() + box.getZSize()) / 2;
        int i1 = this.random.nextInt(Math.max(1, 8 - l / 2));
        for (int x = i - k; x <= i + k; ++x) {
            for (int z = j - k; z <= j + k; ++z) {
                int i2 = Math.abs(x - i) + Math.abs(z - j);
                int j2 = Math.max(0, i2 + i1);
                if (j2 < k && this.random.nextDouble() < (double) SPREAD[j2]) {
                    int k2 = height(x, z) - 1;
                    int l2 = onSurface ? k2 : Math.min(box.minY, k2);
                    BlockPos pos = new BlockPos(x, l2, z);
                    if (Math.abs(l2 - box.minY) <= 3 && canBeReplacedByNetherrackOrMagma(pos)) {
                        placeNetherrackOrMagma(pos);
                        if (this.piece.overgrown) {
                            maybeAddLeavesAbove(pos);
                        }
                        dripColumn(pos.down());
                    }
                }
            }
        }
    }

    private void dripColumnsBelowPortal() {
        StructureBoundingBox box = this.piece.templateBox();
        for (int x = box.minX + 1; x < box.maxX; ++x) {
            for (int z = box.minZ + 1; z < box.maxZ; ++z) {
                BlockPos pos = new BlockPos(x, box.minY, z);
                if (cell(pos).is("minecraft:netherrack")) {
                    dripColumn(pos.down());
                }
            }
        }
    }

    private void dripColumn(BlockPos start) {
        BlockPos pos = start;
        placeNetherrackOrMagma(pos);
        int i = 8;
        while (i > 0 && this.random.nextFloat() < 0.5F) {
            pos = pos.down();
            --i;
            placeNetherrackOrMagma(pos);
        }
    }

    private boolean canBeReplacedByNetherrackOrMagma(BlockPos pos) {
        Cell c = cell(pos);
        return !c.is("minecraft:air") && !c.is("minecraft:obsidian") && !c.is("minecraft:chest")
                && (this.piece.verticalPlacement == RuinedPortals.Placement.IN_NETHER || !c.is("minecraft:lava"));
    }

    private void placeNetherrackOrMagma(BlockPos pos) {
        if (!this.piece.cold && this.random.nextFloat() < 0.07F) {
            write(pos, "minecraft:magma_block");
        } else {
            write(pos, "minecraft:netherrack");
        }
    }

    private void maybeAddVines(BlockPos pos) {
        Cell c = cell(pos);
        if (!c.isAir() && !c.is("minecraft:vine")) {
            EnumFacing direction = HORIZONTAL[this.random.nextInt(HORIZONTAL.length)];
            BlockPos next = pos.offset(direction);
            if (cell(next).isAir() && c.fullFace(direction)) {
                write(next, "minecraft:vine[" + direction.getOpposite().getName() + "=true]");
            }
        }
    }

    private void maybeAddLeavesAbove(BlockPos pos) {
        if (this.random.nextFloat() < 0.5F && cell(pos).is("minecraft:netherrack") && cell(pos.up()).isAir()) {
            write(pos.up(), "minecraft:jungle_leaves[persistent=true]");
        }
    }

    // ---- the world as 1.16 would see it

    private void write(BlockPos pos, String state) {
        this.writes.put(pos, state);
        set(pos, new Cell(state.indexOf('[') > 0 ? state.substring(0, state.indexOf('[')) : state, null));
    }

    private void set(BlockPos pos, Cell cell) {
        this.overlay.put(pos, cell);
        long key = (long) pos.getX() << 32 ^ (pos.getZ() & 0xFFFFFFFFL);
        Integer top = this.overlayTop.get(key);
        if (top == null || pos.getY() > top) {
            this.overlayTop.put(key, pos.getY());
        }
    }

    private Cell cell(BlockPos pos) {
        Cell c = this.overlay.get(pos);
        return c != null ? c : terrainCell(pos.getX(), pos.getY(), pos.getZ());
    }

    private Cell terrainCell(int x, int y, int z) {
        if (this.terrain == null || !this.terrain.known(x, z)) {
            int h = this.terrain == null ? NetherHeights.UNKNOWN : this.terrain.height(x, z);
            if (h == NetherHeights.UNKNOWN) {
                h = this.seaLevel + 1;
            }
            return y < h ? Cell.solid(this.nether) : Cell.AIR;
        }
        if (this.terrain.isAir(x, y, z)) {
            return Cell.AIR;
        }
        if (this.terrain.isLiquid(x, y, z)) {
            return this.nether ? Cell.LAVA : Cell.WATER;
        }
        if (this.terrain.isSolid(x, y, z)) {
            return Cell.solid(this.nether);
        }
        return Cell.PLANT;
    }

    /** {@code getHeight(WORLD_SURFACE_WG, x, z)}, or {@code OCEAN_FLOOR_WG} for an ocean floor portal. */
    private int height(int x, int z) {
        boolean floor = this.piece.verticalPlacement == RuinedPortals.Placement.ON_OCEAN_FLOOR;
        int top = this.terrain == null ? NetherHeights.UNKNOWN : floor ? this.terrain.floor(x, z) : this.terrain.height(x, z);
        if (top == NetherHeights.UNKNOWN) {
            top = this.seaLevel + 1;
        }
        Integer over = this.overlayTop.get((long) x << 32 ^ (z & 0xFFFFFFFFL));
        int y = Math.max(top - 1, over == null ? -1 : over);
        for (; y >= 0; y--) {
            Cell c = cell(new BlockPos(x, y, z));
            if (floor ? c.blocksMotion() : !c.isAir()) {
                return y + 1;
            }
        }
        return 0;
    }

    /** A block as the extras test it: its 1.16 name, and the 1.12.2 state where one is known. */
    private static final class Cell {
        static final Cell AIR = new Cell("minecraft:air", Blocks.AIR.getDefaultState());
        static final Cell LAVA = new Cell("minecraft:lava", Blocks.LAVA.getDefaultState());
        static final Cell WATER = new Cell("minecraft:water", Blocks.WATER.getDefaultState());
        static final Cell PLANT = new Cell("minecraft:grass", Blocks.TALLGRASS.getDefaultState());
        private static final Cell NETHERRACK = new Cell("minecraft:netherrack", Blocks.NETHERRACK.getDefaultState());
        private static final Cell STONE = new Cell("minecraft:stone", Blocks.STONE.getDefaultState());

        final String name;
        @Nullable
        final IBlockState state;

        Cell(String name, @Nullable IBlockState state) {
            this.name = name;
            this.state = state;
        }

        static Cell solid(boolean nether) {
            return nether ? NETHERRACK : STONE;
        }

        boolean is(String n) {
            return this.name.equals(n);
        }

        boolean isAir() {
            return is("minecraft:air") || is("minecraft:cave_air") || is("minecraft:void_air");
        }

        boolean blocksMotion() {
            IBlockState s = state();
            return s != null && s.getMaterial().blocksMovement();
        }

        @Nullable
        IBlockState state() {
            if (this.state != null) {
                return this.state;
            }
            return com.exiledradio.backportcore.structure.ModernPalette.resolve(this.name).state;
        }

        /** {@code Block.isFaceFull(state.getCollisionShape(...), direction)}. */
        boolean fullFace(EnumFacing direction) {
            IBlockState s = state();
            if (s == null) {
                return false;
            }
            try {
                return s.getBlockFaceShape(EMPTY, BlockPos.ORIGIN, direction) == BlockFaceShape.SOLID;
            } catch (RuntimeException e) {
                return s.isFullCube();
            }
        }
    }

    /** A world of air, for asking a lone block about its faces. */
    private static final IBlockAccess EMPTY = new IBlockAccess() {
        @Nullable
        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getCombinedLight(BlockPos pos, int lightValue) {
            return 0;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            return true;
        }

        @Override
        public Biome getBiome(BlockPos pos) {
            return net.minecraft.init.Biomes.PLAINS;
        }

        @Override
        public int getStrongPower(BlockPos pos, EnumFacing direction) {
            return 0;
        }

        @Override
        public WorldType getWorldType() {
            return WorldType.DEFAULT;
        }

        @Override
        public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
            return _default;
        }
    };
}
