package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.structure.TemplatePiece;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.VanillaPlacements;
import com.exiledradio.backportcore.terrain.NetherHeights;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;

import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.16.5's nether fossils ({@code NetherFossilStructure}): in soul sand valleys, a candidate every two chunks (salt
 * 14357921), a random column of the chunk, the first floor of 1.16's base column going down from a random height
 * between the lava sea and the roof; a random rotation and one of fourteen bone templates, air left out.
 */
public final class NetherFossils extends ScatteredStructure {

    /** The nether's sea level and {@code getGenDepth}. */
    static final int SEA_LEVEL = 32;
    static final int GEN_DEPTH = 128;

    public NetherFossils() {
        super("NUB_NetherFossil", VanillaPlacements.V1_16_5.NETHER_FOSSIL);
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return StructureContent.noiseKind(world, chunkX, chunkZ) == NetherBiomes.Kind.SOUL_SAND_VALLEY;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(this.world, chunkX, chunkZ);
    }

    public static final class Start extends StructureStart {
        /** For loading. */
        public Start() {
        }

        Start(World world, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            Layout l = layout(world.getSeed(), chunkX, chunkZ, NetherHeights.baseForWorld(world));
            if (l != null) {
                this.components.add(new Piece(l));
            }
            updateBoundingBox();
        }
    }

    /** What 1.16.5's start decides. */
    public static final class Layout {
        public BlockPos position;
        public Rotation rotation;
        public int template;
        /** The column and the height the search starts from, as the random decides them alone. */
        public int x;
        public int z;
        public int startY;
    }

    /** {@code NetherFossilStructure.Start.generatePieces}, line for line; null when the column has no floor above the lava sea. */
    @Nullable
    public static Layout layout(long seed, int chunkX, int chunkZ, @Nullable NetherHeights base) {
        Random random = StructureRandoms.start(seed, chunkX, chunkZ);
        Layout l = new Layout();
        int i = (chunkX << 4) + random.nextInt(16);
        int j = (chunkZ << 4) + random.nextInt(16);
        int k = SEA_LEVEL;
        int y = k + random.nextInt(GEN_DEPTH - 2 - k);
        l.x = i;
        l.z = j;
        l.startY = y;
        if (base == null) {
            return null;
        }
        for (; y > k; --y) {
            // The block is air and the one under it soul sand or a sturdy top face: in the bare noise, netherrack.
            if (base.isAir(i, y, j) && base.isSolid(i, y - 1, j)) {
                break;
            }
        }
        if (y <= k) {
            return null;
        }
        l.rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        l.template = random.nextInt(14) + 1;
        l.position = new BlockPos(i, y, j);
        return l;
    }

    /**
     * The same start with the floor search's answer given: the draws after it (rotation, template) do not depend on
     * the terrain, so this compares them with another game's fossil at its own height.
     */
    @Nullable
    public static Layout layoutAtFloor(long seed, int chunkX, int chunkZ, int floorY) {
        Random random = StructureRandoms.start(seed, chunkX, chunkZ);
        Layout l = new Layout();
        l.x = (chunkX << 4) + random.nextInt(16);
        l.z = (chunkZ << 4) + random.nextInt(16);
        l.startY = SEA_LEVEL + random.nextInt(GEN_DEPTH - 2 - SEA_LEVEL);
        if (floorY <= SEA_LEVEL || floorY > l.startY) {
            return null;
        }
        l.rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        l.template = random.nextInt(14) + 1;
        l.position = new BlockPos(l.x, floorY, l.z);
        return l;
    }

    /** A fossil: one template, turned about its corner, air left out ({@code STRUCTURE_AND_AIR}). */
    public static class Piece extends TemplatePiece {
        /** For loading. */
        public Piece() {
        }

        Piece(Layout l) {
            super(JigsawPools.templateId(new ResourceLocation("minecraft", "nether_fossils/fossil_" + l.template)), l.position,
                    l.rotation, Mirror.NONE, BlockPos.ORIGIN, new Random(l.position.toLong()));
        }

        public ResourceLocation templateName() {
            return this.template;
        }

        public BlockPos position() {
            return this.origin;
        }

        public Rotation rotation() {
            return this.rotation;
        }

        @Override
        protected TemplatePlacement placement() {
            return super.placement().ignoreAir(true);
        }
    }
}
