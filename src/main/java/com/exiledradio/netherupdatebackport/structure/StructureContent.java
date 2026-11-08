package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.NetherStructureSlots;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.biome.NetherBiomeSource;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenStructureIO;

/**
 * Stream X: 1.16's bastion remnants, ruined portals and nether fossils, in the chunks vanilla 1.16.5 puts them for the
 * world's seed.
 *
 * <ul>
 *   <li><b>Bastions</b> ({@link BastionStructure}) take nether fortress slots, as in 1.16: Backport Core's
 *       {@link NetherStructureSlots} swaps the fortress generator for one on 1.16.5's shared grid, and the biome check
 *       reads stream V's 1.16 noise biome ({@link #allows}). Laid out by Core's {@code Jigsaw} from Mojang's own pool
 *       and template files.</li>
 *   <li><b>Ruined portals</b> ({@link RuinedPortals}) in the overworld and the nether, 1.16.5's start code line for
 *       line; <b>nether fossils</b> ({@link NetherFossils}) in soul sand valleys.</li>
 *   <li>Their blocks, mobs and loot through Backport Core's {@link ModernPalette} ({@link StructurePalette},
 *       {@link StructureLoot}); the bastion advancements through {@link StructureEvents}.</li>
 * </ul>
 * Everything is placed from the populate step by Core's {@link ScatteredStructure} driver: no chunk generator is
 * touched, and nothing is built in chunks generated before.
 */
public final class StructureContent {

    private StructureContent() {
    }

    public static boolean enabled() {
        return ModConfig.structures.enabled;
    }

    public static boolean bastions() {
        return enabled() && ModConfig.structures.bastions;
    }

    public static boolean ruinedPortals() {
        return enabled() && ModConfig.structures.ruinedPortals;
    }

    public static boolean netherFossils() {
        return enabled() && ModConfig.structures.netherFossils;
    }

    /** Called from pre-init: before any world exists. */
    public static void preInit() {
        if (!enabled()) {
            return;
        }
        // minecraft:bastion/... pools, processor lists and templates are read from this mod's assets.
        JigsawPools.source(NetherUpdateBackport.MOD_ID);
        // This mod's blocks are named as 1.16 names them: netherupdatebackport:blackstone serves minecraft:blackstone.
        ModernPalette.source(NetherUpdateBackport.MOD_ID);
        if (bastions()) {
            // Bastions take fortress slots (1.16.5's shared grid); without bastions, 1.12.2's fortresses stay.
            NetherStructureSlots.install(StructureContent::allows);
        }
        StructurePalette.preInit();
        StructureLoot.preInit();
    }

    /** Called from init: entities are registered by now. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        StructurePalette.init();
        StructureLoot.init();
        MapGenStructureIO.registerStructure(BastionStructure.Start.class, "NUB_Bastion");
        MapGenStructureIO.registerStructureComponent(BastionPiece.class, "NUB_BastionPiece");
        MapGenStructureIO.registerStructure(RuinedPortals.Start.class, "NUB_RuinedPortal");
        MapGenStructureIO.registerStructureComponent(RuinedPortalPiece.class, "NUB_RuinedPortalPiece");
        MapGenStructureIO.registerStructure(NetherFossils.Start.class, "NUB_NetherFossil");
        MapGenStructureIO.registerStructureComponent(NetherFossils.Piece.class, "NUB_NetherFossilPiece");
        ScatteredStructure.register(Structures.BASTION_REMNANT,
                world -> bastions() && world.provider.getDimension() == -1 ? new BastionStructure() : null);
        ScatteredStructure.register(Structures.RUINED_PORTAL, world -> !ruinedPortals() ? null
                : world.provider.getDimension() == -1 ? new RuinedPortals(true)
                : world.provider.getDimension() == 0 ? new RuinedPortals(false) : null);
        ScatteredStructure.register(Structures.NETHER_FOSSIL,
                world -> netherFossils() && world.provider.getDimension() == -1 ? new NetherFossils() : null);
        StructureEvents.init();
        registerLocate();
    }

    /** Today's ruined portal ids, standard first. */
    static final String[] RUINED_PORTAL_IDS = {"minecraft:ruined_portal", "minecraft:ruined_portal_desert",
            "minecraft:ruined_portal_jungle", "minecraft:ruined_portal_swamp", "minecraft:ruined_portal_mountain",
            "minecraft:ruined_portal_ocean", "minecraft:ruined_portal_nether"};

    /**
     * Backport Core's {@code /locate structure}, under today's ids: bastions and nether fossils, and the seven ruined
     * portals by the type each start is built as (tag {@code #minecraft:ruined_portal}). Fortresses are Core's.
     */
    static void registerLocate() {
        StructureLocate.scattered("minecraft:nether_complexes", Structures.BASTION_REMNANT, null, "minecraft:bastion_remnant");
        StructureLocate.scattered("minecraft:nether_fossils", Structures.NETHER_FOSSIL, null, "minecraft:nether_fossil");
        StructureLocate.scattered("minecraft:ruined_portals", Structures.RUINED_PORTAL,
                (world, structure, x, z) -> ruinedPortalId(structure.variantAt(world, x, z)), RUINED_PORTAL_IDS);
        StructureLocate.tag("minecraft:ruined_portal", RUINED_PORTAL_IDS);
    }

    /** @return today's id of a ruined portal type ({@link RuinedPortals.Type#id}); a start with no type is built standard */
    static String ruinedPortalId(String type) {
        return type == null || RuinedPortals.Type.STANDARD.id().equals(type) ? RUINED_PORTAL_IDS[0] : "minecraft:ruined_portal_" + type;
    }

    /**
     * The biome check of 1.16.5's {@code ChunkGenerator.createStructures} for the shared slots: the noise biome at the
     * candidate chunk's middle quart, {@code ((x << 2) + 2, 0, (z << 2) + 2)}, must list the structure. Every nether
     * biome lists fortresses; all but the basalt deltas list bastions.
     *
     * <p>With stream V's biome source that is its 1.16 noise biome itself (the column biomes 1.12.2 stores are the
     * fuzzed ones, a block finer, so they would disagree near biome edges). Any other nether keeps Core's reading of
     * the block biome.
     */
    static boolean allows(World world, int chunkX, int chunkZ, NetherStructureSlots.Slot slot) {
        if (slot != NetherStructureSlots.Slot.BASTION) {
            return true;
        }
        NetherBiomes.Kind kind = noiseKind(world, chunkX, chunkZ);
        if (kind != null) {
            return kind != NetherBiomes.Kind.BASALT_DELTAS;
        }
        return NetherStructureSlots.VANILLA_1_16_5.allows(world, chunkX, chunkZ, slot);
    }

    /** @return stream V's 1.16 noise biome at a chunk's middle quart, or null when V's biome source is not this world's */
    static NetherBiomes.Kind noiseKind(World world, int chunkX, int chunkZ) {
        BiomeProvider provider = world.getBiomeProvider();
        if (provider instanceof NetherBiomeSource) {
            return ((NetherBiomeSource) provider).noiseKind((chunkX << 2) + 2, 0, (chunkZ << 2) + 2);
        }
        return null;
    }
}
