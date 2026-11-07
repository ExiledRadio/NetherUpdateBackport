package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.ModernProcessors;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePiece;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;

/**
 * A ruined portal: 1.16.5's {@code RuinedPortalPiece}.
 *
 * <p>The template places with 1.16's processors in 1.16's order: structure blocks (and, without an air pocket, air)
 * left out; the rules (gold blocks to air three times in ten, lava to magma or netherrack by placement and coldness,
 * netherrack to magma seven times in a hundred where it is warm); {@code block_age} with the type's mossiness;
 * {@code lava_submerged_block}; and in the nether {@code blackstone_replace}. Each block draws from its own position's
 * random, as 1.16's processors do, so it is the same block for block.
 *
 * <p>After it 1.16 spreads netherrack and magma around the portal, hangs drip columns under it, and (jungle and swamp
 * types) grows vines and leaves, all from the start chunk's decoration random and reading the terrain around. 1.16 does
 * that at once from one chunk; 1.12.2 builds a chunk at a time and must not load the next, so {@link PortalExtras}
 * works it out once against the terrain as it will generate (Core's {@code NetherHeights}) and each chunk writes its
 * share.
 */
public class RuinedPortalPiece extends TemplatePiece {

    RuinedPortals.Type type = RuinedPortals.Type.STANDARD;
    RuinedPortals.Placement verticalPlacement = RuinedPortals.Placement.ON_LAND_SURFACE;
    boolean airPocket;
    boolean cold;
    float mossiness = 0.2F;
    boolean overgrown;
    boolean vines;
    boolean blackstone;

    /** The netherrack, magma, vines and leaves around this portal, worked out on first placement. */
    @Nullable
    private transient PortalExtras extras;

    /** For loading. */
    public RuinedPortalPiece() {
    }

    RuinedPortalPiece(RuinedPortals.Layout layout, Random random) {
        super(RuinedPortals.templateId(layout.template), layout.position, layout.rotation, layout.mirror, layout.pivot, random);
        this.type = layout.type;
        this.verticalPlacement = layout.placement;
        this.airPocket = layout.airPocket;
        this.cold = layout.cold;
        this.mossiness = layout.mossiness;
        this.overgrown = layout.overgrown;
        this.vines = layout.vines;
        this.blackstone = layout.blackstone;
        this.boundingBox = withExtras(templateBox());
    }

    /** The template's own box: 1.16's piece box, which the extras are measured from. */
    public StructureBoundingBox templateBox() {
        return template().boundingBox(this.origin, basePlacement());
    }

    /**
     * The box this piece is built in: the template's, widened to where the extras can reach (netherrack spread 14
     * blocks out from the middle, drip columns 12 below the floor, leaves one above), so every chunk they touch builds
     * its share. 1.12.2 only asks a piece for the chunks its box covers.
     */
    static StructureBoundingBox withExtras(StructureBoundingBox t) {
        int cx = t.minX + (t.maxX - t.minX + 1) / 2;
        int cz = t.minZ + (t.maxZ - t.minZ + 1) / 2;
        int reach = 14;
        return new StructureBoundingBox(Math.min(t.minX - 1, cx - reach), t.minY - 12, Math.min(t.minZ - 1, cz - reach),
                Math.max(t.maxX + 1, cx + reach), t.maxY + 1, Math.max(t.maxZ + 1, cz + reach));
    }

    public BlockPos position() {
        return this.origin;
    }

    public ResourceLocation templateName() {
        return this.template;
    }

    public net.minecraft.util.Rotation rotation() {
        return this.rotation;
    }

    public net.minecraft.util.Mirror mirror() {
        return this.mirror;
    }

    /** The template's settings and processors, 1.16's {@code loadTemplate}. */
    @Override
    protected TemplatePlacement placement() {
        TemplatePlacement p = super.placement().ignoreAir(!this.airPocket).processor(rules());
        p.processor(ModernProcessors.blockAge(this.mossiness)).processor(ModernProcessors.lavaSubmerged());
        if (this.blackstone) {
            p.processor(ModernProcessors.blackstoneReplace());
        }
        return p;
    }

    /** The rule processor: 1.16's three {@code ProcessorRule}s, read by Core as a 1.16 processor list. */
    private com.exiledradio.backportcore.structure.TemplateProcessor rules() {
        JsonArray rules = new JsonArray();
        rules.add(rule("minecraft:gold_block", 0.3F, "minecraft:air"));
        if (this.verticalPlacement == RuinedPortals.Placement.ON_OCEAN_FLOOR) {
            rules.add(rule("minecraft:lava", -1.0F, "minecraft:magma_block"));
        } else if (this.cold) {
            rules.add(rule("minecraft:lava", -1.0F, "minecraft:netherrack"));
        } else {
            rules.add(rule("minecraft:lava", 0.2F, "minecraft:magma_block"));
        }
        if (!this.cold) {
            rules.add(rule("minecraft:netherrack", 0.07F, "minecraft:magma_block"));
        }
        JsonObject processor = new JsonObject();
        processor.addProperty("processor_type", "minecraft:rule");
        processor.add("rules", rules);
        JsonArray list = new JsonArray();
        list.add(processor);
        return ModernProcessors.parse(list).build(BlockPos.ORIGIN).get(0);
    }

    /** @param chance below 0 for a plain {@code block_match} */
    private static JsonObject rule(String block, float chance, String output) {
        JsonObject input = new JsonObject();
        input.addProperty("predicate_type", chance < 0.0F ? "minecraft:block_match" : "minecraft:random_block_match");
        input.addProperty("block", block);
        if (chance >= 0.0F) {
            input.addProperty("probability", chance);
        }
        JsonObject location = new JsonObject();
        location.addProperty("predicate_type", "minecraft:always_true");
        JsonObject out = new JsonObject();
        out.addProperty("Name", output);
        JsonObject rule = new JsonObject();
        rule.add("input_predicate", input);
        rule.add("location_predicate", location);
        rule.add("output_state", out);
        return rule;
    }

    TemplatePlacement basePlacement() {
        return TemplatePlacement.of(this.rotation, this.mirror).pivot(this.pivot);
    }

    ModernTemplate loadTemplate() {
        return template();
    }

    int palette() {
        return this.palette;
    }

    /** The template alone, in one chunk (for the self-test's block for block comparison). */
    public void placeTemplate(World world, StructureBoundingBox chunk) {
        super.addComponentParts(world, new Random(0L), chunk);
    }

    /** @return the netherrack, magma, vines and leaves this portal adds around itself, by position */
    public Map<BlockPos, String> extras(World world) {
        if (this.extras == null) {
            this.extras = PortalExtras.plan(world, this);
        }
        return java.util.Collections.unmodifiableMap(this.extras.writes);
    }

    public RuinedPortals.Placement verticalPlacement() {
        return this.verticalPlacement;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunk) {
        super.addComponentParts(world, random, chunk);
        if (this.extras == null) {
            this.extras = PortalExtras.plan(world, this);
        }
        for (Map.Entry<BlockPos, String> e : this.extras.writes.entrySet()) {
            BlockPos pos = e.getKey();
            if (!chunk.isVecInside(pos)) {
                continue;
            }
            IBlockState state = ModernPalette.resolve(e.getValue()).state;
            if (state != null) {
                world.setBlockState(pos, state, 2);
            }
        }
        // The chest's loot seed: the start chunk's decoration random, as 1.16's template placement draws it.
        for (Map.Entry<BlockPos, Long> e : this.extras.lootSeeds.entrySet()) {
            if (chunk.isVecInside(e.getKey())) {
                TileEntity te = world.getTileEntity(e.getKey());
                if (te instanceof TileEntityLockableLoot && ((TileEntityLockableLoot) te).getLootTable() != null) {
                    ((TileEntityLockableLoot) te).setLootTable(((TileEntityLockableLoot) te).getLootTable(), e.getValue());
                }
            }
        }
        return true;
    }

    @Override
    protected void writeExtra(NBTTagCompound tag) {
        tag.setString("PortalType", this.type.id());
        tag.setString("VerticalPlacement", this.verticalPlacement.id());
        tag.setBoolean("AirPocket", this.airPocket);
        tag.setBoolean("Cold", this.cold);
        tag.setFloat("Mossiness", this.mossiness);
        tag.setBoolean("Overgrown", this.overgrown);
        tag.setBoolean("Vines", this.vines);
        tag.setBoolean("ReplaceWithBlackstone", this.blackstone);
    }

    @Override
    protected void readExtra(NBTTagCompound tag) {
        this.type = byId(RuinedPortals.Type.class, tag.getString("PortalType"), RuinedPortals.Type.STANDARD);
        this.verticalPlacement = byId(RuinedPortals.Placement.class, tag.getString("VerticalPlacement"), RuinedPortals.Placement.ON_LAND_SURFACE);
        this.airPocket = tag.getBoolean("AirPocket");
        this.cold = tag.getBoolean("Cold");
        this.mossiness = tag.getFloat("Mossiness");
        this.overgrown = tag.getBoolean("Overgrown");
        this.vines = tag.getBoolean("Vines");
        this.blackstone = tag.getBoolean("ReplaceWithBlackstone");
    }

    private static <E extends Enum<E>> E byId(Class<E> type, String id, E fallback) {
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(id)) {
                return e;
            }
        }
        return fallback;
    }
}
