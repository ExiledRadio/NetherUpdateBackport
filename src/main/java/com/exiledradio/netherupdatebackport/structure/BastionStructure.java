package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.Jigsaw;
import com.exiledradio.backportcore.structure.NetherStructureSlots;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.structure.VanillaPlacements;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;

/**
 * 1.16's bastion remnant ({@code BastionFeature}): a jigsaw structure from Mojang's {@code bastion/starts} pool, depth
 * 6, its start at y 33 on the chunk's corner, in the chunks 1.16.5's shared bastion and fortress grid gives bastions.
 */
public final class BastionStructure extends ScatteredStructure {

    public static final ResourceLocation START_POOL = new ResourceLocation("minecraft", "bastion/starts");
    /** {@code BastionFeature}'s start height and depth. */
    public static final int START_Y = 33;
    public static final int DEPTH = 6;

    public BastionStructure() {
        super("NUB_Bastion", VanillaPlacements.V1_16_5.BASTION_REMNANT);
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return NetherStructureSlots.startAt(world, chunkX, chunkZ) == NetherStructureSlots.Slot.BASTION;
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
            this.components.addAll(layout(world, world.getSeed(), chunkX, chunkZ));
            updateBoundingBox();
        }
    }

    /** The pieces 1.16.5 lays out for a bastion starting in this chunk of a world with this seed. Loads nothing. */
    public static java.util.List<com.exiledradio.backportcore.structure.JigsawPiece> layout(World world, long seed, int chunkX, int chunkZ) {
        return Jigsaw.of(START_POOL).maxDepth(DEPTH).pieces(BastionPiece::new)
                .assemble(world, new BlockPos(chunkX << 4, START_Y, chunkZ << 4), StructureRandoms.start(seed, chunkX, chunkZ));
    }
}
