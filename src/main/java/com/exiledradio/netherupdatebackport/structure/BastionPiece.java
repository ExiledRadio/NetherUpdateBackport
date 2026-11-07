package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A piece of a bastion: Core's jigsaw piece, except for the four piglin mob pieces.
 *
 * <p>1.16's bastions place their piglins and brutes from one-entity templates ({@code bastion/mobs/*}) with
 * {@code SpawnReason.STRUCTURE}, which skips the piglin's baby and random-weapon rolls. 1.12.2's {@code onInitialSpawn}
 * has no reason, so those pieces place their blocks here without the entity and ask stream P's
 * {@code piglin.BastionMobs.spawn} (by name, so this stream never links against P) for the mob, with the template's
 * weapon: a crossbow piglin keeps its crossbow (1.12.2 cannot read a {@code minecraft:crossbow} item), a sword piglin its
 * golden sword, and both brute templates (1.16.2's {@code melee_piglin}s) a brute. With piglins switched off nothing
 * spawns there. The stables' hoglins spawn as the template has them, through Backport Core's entity mapping, which reads
 * their {@code CannotBeHunted} and gives them 1.16's first-spawn roll (one in five a baby).
 */
public class BastionPiece extends JigsawPiece {

    private static final String MOBS = "bastion/mobs/";
    private static final Map<ResourceLocation, List<Mob>> MOB_ENTITIES = new HashMap<ResourceLocation, List<Mob>>();
    private static Method spawn;
    private static boolean looked;

    /** For loading. */
    public BastionPiece() {
    }

    public BastionPiece(JigsawElement element, BlockPos position, int groundLevelDelta, Rotation rotation, StructureBoundingBox box) {
        super(element, position, groundLevelDelta, rotation, box);
    }

    /** @return the piglin mob template's name ({@code sword_piglin}), or null for any other piece */
    @Nullable
    String piglinTemplate() {
        if (this.element == null || this.element.template() == null) {
            return null;
        }
        String path = this.element.template().getPath();
        if (!path.startsWith(MOBS)) {
            return null;
        }
        String name = path.substring(MOBS.length());
        return name.endsWith("piglin") || name.endsWith("piglin_always") ? name : null;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunk) {
        String mob = piglinTemplate();
        if (mob == null) {
            super.addComponentParts(world, random, chunk);
            placeLoneJigsaws(world, chunk);
            return true;
        }
        ResourceLocation id = JigsawPools.templateId(this.element.template());
        ModernTemplate template = ModernTemplate.load(id);
        // A mob piece's own blocks (air, and its jigsaw's final state), as the single element places them.
        TemplatePlacement placement = TemplatePlacement.of(this.pieceRotation, Mirror.NONE).clip(chunk).entities(false);
        template.place(world, this.position, placement);
        for (Mob m : mobs(id)) {
            BlockPos at = ModernTemplate.worldPos(this.position, m.blockPos, placement);
            if (chunk.isVecInside(at)) {
                // Entity.getRotatedYaw, as a template turns its entities.
                float yaw = m.yaw - this.pieceRotation.ordinal() * 90.0F;
                spawnPiglin(world, at, yaw, mob);
            }
        }
        return true;
    }

    /**
     * A template that is nothing but jigsaw blocks ({@code bastion/blocks/gold} and {@code air}, the treasure room's
     * {@code extensions/empty}): 1.16 places each jigsaw as its final state (a gold block, air, basalt). Backport Core
     * 1.2.0's {@code ModernTemplate.place} stops early for a template with no other block, so these are placed here.
     * None of 1.16.5's processor lists changes these final states, so none is run.
     */
    private void placeLoneJigsaws(World world, StructureBoundingBox chunk) {
        if (this.element == null || this.element.template() == null) {
            return;
        }
        ModernTemplate template = ModernTemplate.load(JigsawPools.templateId(this.element.template()));
        if (template.jigsaws().isEmpty() || hasBlocks(template)) {
            return;
        }
        TemplatePlacement placement = TemplatePlacement.of(this.pieceRotation, Mirror.NONE);
        for (ModernTemplate.Jigsaw j : template.jigsaws()) {
            BlockPos at = ModernTemplate.worldPos(this.position, j.pos, placement);
            if (!chunk.isVecInside(at)) {
                continue;
            }
            net.minecraft.block.state.IBlockState state = com.exiledradio.backportcore.structure.ModernPalette.resolve(j.finalState).state;
            if (state != null) {
                world.setBlockState(at, state.withRotation(this.pieceRotation), 2);
            }
        }
    }

    private static boolean hasBlocks(ModernTemplate template) {
        boolean[] any = {false};
        template.forEachBlock(0, (local, block) -> any[0] = true);
        return any[0];
    }

    /** Stream P's bastion mob, by name: {@code BastionMobs.spawn(world, pos, yaw, template)}. */
    static void spawnPiglin(World world, BlockPos at, float yaw, String template) {
        Method m = spawnMethod();
        if (m == null) {
            return;
        }
        try {
            m.invoke(null, world, at, yaw, template);
        } catch (ReflectiveOperationException | RuntimeException e) {
            NetherUpdateBackport.LOGGER.error("A bastion's {} could not be spawned at {}.", template, at, e);
        }
    }

    /** @return true when stream P's bastion mobs can be asked for (its class is in this build) */
    public static boolean piglinsAvailable() {
        return spawnMethod() != null;
    }

    @Nullable
    private static synchronized Method spawnMethod() {
        if (!looked) {
            looked = true;
            try {
                spawn = Class.forName(NetherUpdateBackport.class.getPackage().getName() + ".piglin.BastionMobs")
                        .getMethod("spawn", World.class, BlockPos.class, float.class, String.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                NetherUpdateBackport.LOGGER.info("No piglins in this build: bastions are built without them.");
            }
        }
        return spawn;
    }

    private static final class Mob {
        final BlockPos blockPos;
        final float yaw;

        Mob(BlockPos blockPos, float yaw) {
            this.blockPos = blockPos;
            this.yaw = yaw;
        }
    }

    /** The entities of a mob template, read once from the file. */
    private static synchronized List<Mob> mobs(ResourceLocation id) {
        List<Mob> known = MOB_ENTITIES.get(id);
        if (known != null) {
            return known;
        }
        List<Mob> out = new ArrayList<Mob>();
        String path = "/assets/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt";
        try (InputStream in = BastionPiece.class.getResourceAsStream(path)) {
            if (in != null) {
                NBTTagCompound tag = CompressedStreamTools.readCompressed(in);
                NBTTagList entities = tag.getTagList("entities", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < entities.tagCount(); i++) {
                    NBTTagCompound e = entities.getCompoundTagAt(i);
                    NBTTagList bp = e.getTagList("blockPos", Constants.NBT.TAG_INT);
                    NBTTagList rot = e.getCompoundTag("nbt").getTagList("Rotation", Constants.NBT.TAG_FLOAT);
                    out.add(new Mob(new BlockPos(bp.getIntAt(0), bp.getIntAt(1), bp.getIntAt(2)), rot.tagCount() > 0 ? rot.getFloatAt(0) : 0.0F));
                }
            }
        } catch (Exception e) {
            NetherUpdateBackport.LOGGER.error("Could not read the bastion mob template {}.", id, e);
        }
        MOB_ENTITIES.put(id, out);
        return out;
    }
}
