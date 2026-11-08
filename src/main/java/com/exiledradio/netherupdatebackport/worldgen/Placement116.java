package com.exiledradio.netherupdatebackport.worldgen;

import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.StructurePlacements;
import com.exiledradio.backportcore.worldgen.StructurePlacements.BiomePoint;

/**
 * 1.16's overworld structure placement, for Backport Core's placement table (the newest installed version's
 * numbers win), so a world with this mod matches 1.16.5's seed maps. Every number is 1.16.5's own, read from
 * its decompiled code ({@code DimensionStructuresSettings.DEFAULTS}, {@code ChunkGenerator}, {@code BiomeMaker},
 * {@code OverworldBiomeProvider}) and checked against Backport Core's vanilla oracle ({@code Oracle1165OW}).
 *
 * <ul>
 *   <li>Villages 32/8 (salt 10387312), outposts 32/8 (165745296), the four temples 32/8 (14357617 desert pyramid,
 *       14357618 igloo, 14357619 jungle temple, 14357620 witch hut), monument 32/5 (10387313) and mansion 80/20
 *       (10387319), both triangular; and 1.16's wider ocean grids: shipwrecks 24/4 (165745295), ocean ruins 20/8
 *       (14357621). Ruined portals (40/15, salt 34222645) are this mod's own ({@code structure/RuinedPortals}).</li>
 *   <li>{@code ChunkGenerator.createStructures} reads one biome per chunk, the 1:4 cell at (4x + 2, 0, 4z + 2),
 *       and tries the structures that biome lists; so every single-biome check reads that cell. The monument's
 *       and the mansion's squares around block (9, 9) are unchanged.</li>
 *   <li>Strongholds: {@code generateStrongholds} takes the biomes of the overworld biome source's
 *       {@code possibleBiomes()} that have strongholds, and 1.16.5's list leaves out the bamboo jungles (both
 *       still list strongholds, but the source never names them); the mushroom field shore is in.</li>
 *   <li>Outposts ask only the village grid for a village within ten chunks
 *       ({@code PillagerOutpostStructure.isNearVillage}); Village &amp; Pillage reads that from the version.</li>
 * </ul>
 * The spawn biomes (1.16's {@code playerSpawnFriendly}: plains, forest, taiga, wooded hills, taiga hills, jungle,
 * jungle hills) are 1.12.2's seven, and the biome search is 1.13's (Backport Core's P8); the block-scale biome is
 * 1.15's zoom (Core, from 1.15 on).
 */
public final class Placement116 {

    private Placement116() {
    }

    /** Pre-init. */
    public static void register() {
        StructurePlacements.version("1.16")
                .biomePoint(BiomePoint.QUART_CENTER)
                .grid(Structures.VILLAGE, 32, 8, 10387312)
                .grid(Structures.PILLAGER_OUTPOST, 32, 8, 165745296)
                .grid(StructurePlacements.DESERT_PYRAMID, 32, 8, 14357617)
                .grid(StructurePlacements.IGLOO, 32, 8, 14357618)
                .grid(StructurePlacements.JUNGLE_PYRAMID, 32, 8, 14357619)
                .grid(StructurePlacements.SWAMP_HUT, 32, 8, 14357620)
                .grid(StructurePlacements.OCEAN_MONUMENT, 32, 5, 10387313, true)
                .grid(StructurePlacements.MANSION, 80, 20, 10387319, true)
                .grid(Structures.SHIPWRECK, 24, 4, 165745295)
                .grid(Structures.OCEAN_RUIN, 20, 8, 14357621)
                .grid(Structures.RUINED_PORTAL, 40, 15, 34222645)
                .structureBiome(StructurePlacements.JUNGLE_PYRAMID, "bamboo_jungle")
                .structureBiome(StructurePlacements.JUNGLE_PYRAMID, "bamboo_jungle_hills")
                .strongholdBiome("minecraft:mushroom_island_shore", true)
                .strongholdBiome("bamboo_jungle", false)
                .strongholdBiome("bamboo_jungle_hills", false);
    }
}
