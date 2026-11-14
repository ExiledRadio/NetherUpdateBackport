package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Superflat worlds have no ruined portals (no superflat preset lists a structure newer than 1.12), and none of this
 * mod's nether generation reaches their Overworld. The Nether and End are never superflat and keep everything.
 *
 * <p>In a world that is not superflat (the suite's, {@code -Pselftest=all}) it checks the ruined portal's factory with
 * the world info saying superflat for a moment. In a superflat world ({@code tools/flat/selftest.sh}) it generates the
 * chunks around spawn and looks at every block of them.
 */
public final class FlatSelfTest implements SelfTest {

    /** Chunks each way from spawn's chunk: 13 x 13 generated, the inner 11 x 11 populated. */
    private static final int RADIUS = 6;

    @Override
    public String name() {
        return "flat";
    }

    @Override
    public void run(Check c) throws Exception {
        if (FlatWorlds.isFlat(c.world)) {
            realFlat(c);
        } else {
            rules(c);
        }
    }

    // ---- the factory, in the suite's world

    @SuppressWarnings("unchecked")
    private static Function<World, ? extends ScatteredStructure> factory(String kind) throws Exception {
        Field field = ScatteredStructure.class.getDeclaredField("FACTORIES");
        field.setAccessible(true);
        return ((Map<String, Function<World, ? extends ScatteredStructure>>) field.get(null)).get(kind);
    }

    private void rules(Check c) throws Exception {
        WorldServer world = c.world;
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        Function<World, ? extends ScatteredStructure> portals = factory(Structures.RUINED_PORTAL);
        if (!c.ok(portals != null, "ruined portals are registered")) {
            return;
        }
        c.ok(portals.apply(world) != null, "the factory makes Overworld ruined portals in a world that is not superflat");
        WorldInfo info = world.getWorldInfo();
        Field type = ReflectionHelper.findField(WorldInfo.class, "terrainType", "field_76098_b");
        Field options = ReflectionHelper.findField(WorldInfo.class, "generatorOptions", "field_82576_c");
        Object oldType = type.get(info);
        Object oldOptions = options.get(info);
        try {
            type.set(info, WorldType.FLAT);
            for (String preset : new String[] {"3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village",
                    "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village,decoration,stronghold,biome_1,dungeon,mineshaft"}) {
                options.set(info, preset);
                FlatWorlds.forget(world);
                c.ok(FlatWorlds.isFlat(world), "superflat for the check");
                c.ok(portals.apply(world) == null, "the factory makes no ruined portals in superflat (" + preset + ")");
                c.ok(portals.apply(nether) != null, "the Nether keeps its ruined portals beside a superflat Overworld");
            }
        } finally {
            type.set(info, oldType);
            options.set(info, oldOptions);
            FlatWorlds.forget(world);
        }
        c.ok(!FlatWorlds.isFlat(world) && portals.apply(world) != null, "the world is itself again");
    }

    // ---- a real superflat world

    private void realFlat(Check c) {
        WorldServer world = c.world;
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        String options = world.getWorldInfo().getGeneratorOptions();
        c.eq(options.contains("decoration"), FlatWorlds.decoration(world), "decoration as the preset says (" + options + ")");

        BlockPos spawn = world.getSpawnPoint();
        int cx = spawn.getX() >> 4;
        int cz = spawn.getZ() >> 4;
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                world.getChunkProvider().provideChunk(x, z);
            }
        }
        int populated = 0;
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                if (world.getChunk(x, z).isTerrainPopulated()) {
                    populated++;
                }
            }
        }
        c.ok(populated >= (2 * RADIUS - 1) * (2 * RADIUS - 1), "the chunks around spawn are populated: " + populated);

        c.ok(ScatteredStructure.of(world, Structures.RUINED_PORTAL) == null, "no ruined portal instance in superflat");
        c.ok(Structures.nearest(world, Structures.RUINED_PORTAL, spawn, 64) == null, "the ruined portal locator finds none");
        c.ok(locate(world, spawn, "#minecraft:ruined_portal") == null, "/locate structure #ruined_portal finds none");
        c.ok(locate(world, spawn, "minecraft:ruined_portal") == null, "/locate structure ruined_portal finds none");
        c.ok(ScatteredStructure.of(nether, Structures.RUINED_PORTAL) != null, "the Nether keeps its ruined portals");

        // What a ruined portal or this mod's nether generation would leave: none of it in the generated chunks.
        Map<String, Integer> found = new TreeMap<String, Integer>();
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                Chunk chunk = world.getChunk(x, z);
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int top = chunk.getHeightValue(dx, dz) + 1;
                        for (int y = 0; y <= top; y++) {
                            Block block = chunk.getBlockState(dx, y, dz).getBlock();
                            if (portalBlock(block)) {
                                String id = String.valueOf(block.getRegistryName());
                                Integer n = found.get(id);
                                found.put(id, n == null ? 1 : n + 1);
                            }
                        }
                    }
                }
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST flat: portal and nether blocks {} ({})", found, options);
        c.ok(found.isEmpty(), "no obsidian, crying obsidian, netherrack, magma, gold blocks or this mod's blocks: " + found);
    }

    private static StructureLocate.Found locate(WorldServer world, BlockPos from, String id) {
        try {
            return StructureLocate.nearest(world, from, id);
        } catch (StructureLocate.Unavailable u) {
            return null;
        }
    }

    /** A ruined portal's blocks (lava aside: a 1.12 village's blacksmith has some), and every block of this mod. */
    private static boolean portalBlock(Block block) {
        return block == Blocks.OBSIDIAN || block == Blocks.NETHERRACK || block == Blocks.MAGMA || block == Blocks.GOLD_BLOCK
                || block.getRegistryName() != null && NetherUpdateBackport.MOD_ID.equals(block.getRegistryName().getNamespace());
    }
}
