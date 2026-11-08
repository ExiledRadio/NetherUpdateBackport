package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.NetherStructureSlots;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.structure.VanillaPlacements;
import com.exiledradio.backportcore.terrain.NetherHeights;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.biome.NetherBiomeSource;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.owned.OwnedContent;
import com.exiledradio.netherupdatebackport.structure.BastionStructure;
import com.exiledradio.netherupdatebackport.structure.NetherFossils;
import com.exiledradio.netherupdatebackport.structure.RuinedPortalPiece;
import com.exiledradio.netherupdatebackport.structure.RuinedPortals;
import com.exiledradio.netherupdatebackport.structure.StructureEvents;
import com.exiledradio.netherupdatebackport.structure.StructureLoot;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

/**
 * Stream X against vanilla 1.16.5 itself: the oracle files ({@code tools/structure/oracle}) are what Mojang's own 1.16.5
 * classes decide for four seeds. Then the structures generated in this world, without cascading, saved and loaded.
 */
public final class StructureSelfTest implements SelfTest {

    static final long[] SEEDS = {0L, 12345L, -7346534567L, 8675309L};
    /** A marker the comparisons fill a structure's box with first: where vanilla places nothing, it must stay. */
    static final IBlockState MARKER = Blocks.SPONGE.getDefaultState();

    @Override
    public String name() {
        return "structure";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        palette(c);
        loot(c);
        slots(c);
        portals(c);
        fossils(c);
        // Generation first: the block for block builds stand at the oracle seed's own coordinates, over this world's.
        generation(c);
        blockForBlock(c);
    }

    // ------------------------------------------------------------------------------------------

    /** A check whose message is worth reading when it passes too: logged as SELFTEST INFO. */
    static boolean info(Check c, boolean condition, String what) {
        NetherUpdateBackport.LOGGER.info("SELFTEST INFO " + (condition ? "ok: " : "FAILED: ") + what);
        return c.ok(condition, what);
    }

    static boolean infoEq(Check c, Object expected, Object actual, String what) {
        NetherUpdateBackport.LOGGER.info("SELFTEST INFO " + what + ": expected " + expected + ", got " + actual);
        return c.eq(expected, actual, what);
    }

    static List<String[]> oracle(String name) throws Exception {
        List<String[]> out = new ArrayList<String[]>();
        InputStream in = StructureSelfTest.class.getResourceAsStream("/assets/" + NetherUpdateBackport.MOD_ID + "/oracle/" + name + ".gz");
        if (in == null) {
            return out;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    out.add(line.trim().split(" "));
                }
            }
        }
        return out;
    }

    private static WorldServer nether(Check c) {
        DimensionManager.keepDimensionLoaded(-1, true);
        return c.world.getMinecraftServer().getWorld(-1);
    }

    private static void registration(Check c) {
        c.ok(NetherStructureSlots.installed(), "bastions share fortress slots (Core's rule installed)");
        WorldServer nether = nether(c);
        c.ok(nether.getBiomeProvider() instanceof NetherBiomeSource, "the nether has stream V's 1.16 biome source");
        c.ok(ScatteredStructure.of(nether, Structures.BASTION_REMNANT) != null, "bastions are laid out in the nether");
        c.ok(ScatteredStructure.of(c.world, Structures.BASTION_REMNANT) == null, "no bastions in the overworld");
        c.ok(ScatteredStructure.of(nether, Structures.RUINED_PORTAL) != null && ScatteredStructure.of(c.world, Structures.RUINED_PORTAL) != null,
                "ruined portals in the overworld and the nether");
        c.eq(25, ScatteredStructure.of(nether, Structures.RUINED_PORTAL).placement().spacing(), "nether ruined portal spacing (1.16.5's nether settings)");
        c.eq(10, ScatteredStructure.of(nether, Structures.RUINED_PORTAL).placement().separation(), "nether ruined portal separation");
        c.eq(40, ScatteredStructure.of(c.world, Structures.RUINED_PORTAL).placement().spacing(), "overworld ruined portal spacing");
        c.ok(ScatteredStructure.of(nether, Structures.NETHER_FOSSIL) != null && ScatteredStructure.of(c.world, Structures.NETHER_FOSSIL) == null,
                "nether fossils in the nether only");
        c.ok(Structures.has(Structures.BASTION_REMNANT) && Structures.has(Structures.RUINED_PORTAL) && Structures.has(Structures.NETHER_FOSSIL),
                "all three can be located through Core");
        // Without Village & Pillage, this mod's copies of its lantern, smooth quartz slab and stone brick wall.
        c.ok(OwnedContent.lantern != null && OwnedContent.smoothQuartzSlab != null && OwnedContent.stoneBrickWall != null,
                "owner copies: lantern, smooth quartz slab, stone brick wall");
        for (String name : new String[] {"lantern", "smooth_quartz_slab", "smooth_quartz_double_slab", "stone_brick_wall"}) {
            c.ok(ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(NetherUpdateBackport.MOD_ID, name)), "registered " + name);
        }
        c.eq(1.0F, OwnedContent.lantern.getDefaultState().getLightValue() / 15.0F, "the lantern copy gives light 15");
    }

    // ---- blocks and mobs of the files

    private static void palette(Check c) throws Exception {
        Set<ResourceLocation> templates = new java.util.LinkedHashSet<ResourceLocation>();
        for (String path : poolNames()) {
            JigsawPools.Pool pool = JigsawPools.pool(new ResourceLocation("minecraft", path));
            if (!c.ok(pool != null, "pool " + path + " loads")) {
                continue;
            }
            for (JigsawElement e : pool.elements()) {
                for (JigsawElement part : e.parts()) {
                    if (part.template() != null) {
                        templates.add(JigsawPools.templateId(part.template()));
                    }
                }
            }
        }
        for (int i = 1; i <= 10; i++) {
            templates.add(new ResourceLocation(NetherUpdateBackport.MOD_ID, "ruined_portal/portal_" + i));
        }
        for (int i = 1; i <= 3; i++) {
            templates.add(new ResourceLocation(NetherUpdateBackport.MOD_ID, "ruined_portal/giant_portal_" + i));
        }
        for (int i = 1; i <= 14; i++) {
            templates.add(new ResourceLocation(NetherUpdateBackport.MOD_ID, "nether_fossils/fossil_" + i));
        }
        Set<String> unresolved = new TreeSet<String>();
        int empty = 0;
        for (ResourceLocation t : templates) {
            ModernTemplate template = ModernTemplate.load(t);
            if (template.isEmpty() && !t.getPath().endsWith("empty")) {
                empty++;
            }
            unresolved.addAll(template.unresolvedBlocks());
        }
        c.ok(templates.size() > 180 && empty == 0, "every bastion, ruined portal and fossil template loads (" + templates.size() + ", "
                + empty + " empty)");
        c.ok(unresolved.isEmpty(), "every block of every template has a block here: unresolved " + unresolved);
        // What the processors turn blocks into must exist too.
        String[] outputs = {"minecraft:crying_obsidian", "minecraft:magma_block", "minecraft:polished_blackstone", "minecraft:polished_blackstone_bricks",
                "minecraft:polished_blackstone_stairs", "minecraft:polished_blackstone_slab", "minecraft:polished_blackstone_brick_slab",
                "minecraft:polished_blackstone_brick_wall", "minecraft:blackstone_wall", "minecraft:chiseled_polished_blackstone",
                "minecraft:cracked_polished_blackstone_bricks", "minecraft:chain", "minecraft:iron_chain", "minecraft:mossy_stone_bricks",
                "minecraft:mossy_stone_brick_stairs", "minecraft:mossy_stone_brick_slab", "minecraft:mossy_stone_brick_wall",
                "minecraft:stone_slab", "minecraft:gilded_blackstone", "minecraft:blackstone", "minecraft:jungle_leaves[persistent=true]",
                "minecraft:vine[east=true]"};
        List<String> missing = new ArrayList<String>();
        for (String o : outputs) {
            if (ModernPalette.resolve(o).state == null) {
                missing.add(o);
            }
        }
        c.ok(missing.isEmpty(), "every block the processors make resolves: missing " + missing);
        // Slabs: type picks the half or the double.
        IBlockState top = ModernPalette.resolve("minecraft:blackstone_slab[type=top]").state;
        IBlockState dbl = ModernPalette.resolve("minecraft:blackstone_slab[type=double]").state;
        c.ok(top != null && top.getBlock().getRegistryName().getPath().equals("blackstone_slab") && top.toString().contains("half=top"),
                "blackstone_slab[type=top] is the top half: " + top);
        c.ok(dbl != null && dbl.getBlock().getRegistryName().getPath().equals("blackstone_double_slab"), "blackstone_slab[type=double] is the double: " + dbl);
        Map<String, String> slabProps = new LinkedHashMap<String, String>();
        slabProps.put("type", "bottom");
        slabProps.put("waterlogged", "false");
        IBlockState fileSlab = ModernPalette.resolve("minecraft:stone_brick_slab", slabProps, 2586).state;
        c.ok(fileSlab != null && fileSlab.getBlock() == Blocks.STONE_SLAB, "a 1.16 file's stone_brick_slab[type=bottom] is 1.12.2's half slab: " + fileSlab);
        IBlockState chain = ModernPalette.resolve("minecraft:iron_chain[axis=x]").state;
        c.ok(chain != null && chain.getBlock().getRegistryName().toString().equals(NetherUpdateBackport.MOD_ID + ":chain") && chain.toString().contains("axis=x"),
                "chain is stream U's, axis kept: " + chain);
        IBlockState lantern = ModernPalette.resolve("minecraft:lantern[hanging=true]").state;
        c.ok(lantern != null && lantern.getBlock() == OwnedContent.lantern && lantern.toString().contains("hanging=true"), "lantern is the copy, hanging: " + lantern);
        IBlockState quartzSlab = ModernPalette.resolve("minecraft:smooth_quartz_slab[type=bottom]").state;
        c.ok(quartzSlab != null && quartzSlab.getBlock() == OwnedContent.smoothQuartzSlab, "smooth quartz slab is the copy: " + quartzSlab);
        c.eq(NetherUpdateBackport.MOD_ID + ":hoglin", ModernPalette.entity("minecraft:hoglin"), "a file's hoglin is stream H's");
        c.eq(NetherUpdateBackport.MOD_ID + ":chests/bastion_treasure", ModernPalette.lootTable("minecraft:chests/bastion_treasure"),
                "bastion treasure chests use this mod's table");
        c.eq(NetherUpdateBackport.MOD_ID + ":chests/ruined_portal", ModernPalette.lootTable("minecraft:chests/ruined_portal"),
                "ruined portal chests use this mod's table");
    }

    private static List<String> poolNames() {
        List<String> out = new ArrayList<String>();
        java.io.File dir = null;
        // The pools shipped under worldgen/template_pool/bastion; listed from the jar's index of them.
        String[] known = {"bastion/starts", "bastion/mobs/piglin", "bastion/mobs/hoglin", "bastion/mobs/piglin_melee", "bastion/blocks/gold",
                "bastion/units/center_pieces", "bastion/units/pathways", "bastion/units/walls/wall_bases", "bastion/units/stages/stage_0",
                "bastion/units/stages/stage_1", "bastion/units/stages/stage_2", "bastion/units/stages/stage_3", "bastion/units/fillers/stage_0",
                "bastion/units/edges", "bastion/units/wall_units", "bastion/units/edge_wall_units", "bastion/units/ramparts",
                "bastion/units/large_ramparts", "bastion/units/rampart_plates", "bastion/units/stages/rot/stage_1",
                "bastion/hoglin_stable/starting_pieces", "bastion/hoglin_stable/mirrored_starting_pieces", "bastion/hoglin_stable/wall_bases",
                "bastion/hoglin_stable/walls", "bastion/hoglin_stable/stairs", "bastion/hoglin_stable/small_stables/inner",
                "bastion/hoglin_stable/small_stables/outer", "bastion/hoglin_stable/large_stables/inner", "bastion/hoglin_stable/large_stables/outer",
                "bastion/hoglin_stable/posts", "bastion/hoglin_stable/ramparts", "bastion/hoglin_stable/rampart_plates",
                "bastion/hoglin_stable/connectors", "bastion/treasure/bases", "bastion/treasure/stairs", "bastion/treasure/bases/centers",
                "bastion/treasure/brains", "bastion/treasure/walls", "bastion/treasure/walls/outer", "bastion/treasure/walls/bottom",
                "bastion/treasure/walls/mid", "bastion/treasure/walls/top", "bastion/treasure/connectors", "bastion/treasure/entrances",
                "bastion/treasure/ramparts", "bastion/treasure/corners/bottom", "bastion/treasure/corners/edges", "bastion/treasure/corners/middle",
                "bastion/treasure/corners/top", "bastion/treasure/extensions/large_pool", "bastion/treasure/extensions/small_pool",
                "bastion/treasure/extensions/houses", "bastion/treasure/roofs", "bastion/bridge/starting_pieces", "bastion/bridge/bridge_pieces",
                "bastion/bridge/legs", "bastion/bridge/walls", "bastion/bridge/ramparts", "bastion/bridge/rampart_plates", "bastion/bridge/connectors"};
        Collections.addAll(out, known);
        return out;
    }

    // ---- loot

    private static void loot(Check c) throws Exception {
        WorldServer nether = nether(c);
        EntityPlayerMP player = Players.real(nether, "loot");
        Map<String, Set<String>> seen = new LinkedHashMap<String, Set<String>>();
        int empty = 0;
        for (String t : StructureLoot.TABLES) {
            LootTable table = nether.getLootTableManager().getLootTableFromLocation(new ResourceLocation(NetherUpdateBackport.MOD_ID, "chests/" + t));
            if (!c.ok(table != LootTable.EMPTY_LOOT_TABLE, "loot table " + t + " loads")) {
                continue;
            }
            Set<String> items = new TreeSet<String>();
            Random random = new Random(t.hashCode());
            for (int i = 0; i < 400; i++) {
                LootContext ctx = new LootContext.Builder(nether).withLuck(0).withPlayer(player).build();
                for (ItemStack s : table.generateLootForPools(random, ctx)) {
                    if (s.isEmpty()) {
                        empty++;
                    } else {
                        items.add(s.getItem().getRegistryName().getPath() + (s.isItemEnchanted() || s.getItem() == net.minecraft.init.Items.ENCHANTED_BOOK ? "*" : ""));
                    }
                }
            }
            seen.put(t, items);
        }
        c.eq(0, empty, "no empty stacks from the chest tables");
        Set<String> other = seen.getOrDefault("bastion_other", Collections.<String>emptySet());
        Set<String> treasure = seen.getOrDefault("bastion_treasure", Collections.<String>emptySet());
        Set<String> portal = seen.getOrDefault("ruined_portal", Collections.<String>emptySet());
        Set<String> stable = seen.getOrDefault("bastion_hoglin_stable", Collections.<String>emptySet());
        c.ok(treasure.contains("netherite_ingot") && treasure.contains("ancient_debris") && treasure.contains("golden_apple"),
                "bastion treasure: netherite ingots, debris, enchanted golden apples " + treasure);
        c.ok(other.contains("enchanted_book*") && other.contains("netherite_scrap") && other.contains("chain") && other.contains("piglin_banner_pattern") == has("piglin_banner_pattern")
                && other.contains("music_disc_pigstep") == has("music_disc_pigstep"), "bastion other: Soul Speed books, scrap, chains, Snout, Pigstep by name " + other);
        c.ok(stable.contains("crimson_fungus") && stable.contains("crimson_nylium") && stable.contains("saddle"), "hoglin stable: crimson fungi, nylium, saddles " + stable);
        c.ok(portal.contains("lodestone") && portal.contains("obsidian") && portal.contains("speckled_melon") && portal.contains("golden_apple"),
                "ruined portal: today's lodestones, obsidian, glistering melon " + portal);
        c.ok(!other.contains("stick") && !portal.contains("stick") && !treasure.contains("stick") && !stable.contains("stick"), "no stand-in sticks left");
        Set<String> fired = new HashSet<String>(StructureEvents.firedForTest());
        c.ok(fired.contains(player.getName() + " loot_bastion_other") && fired.contains(player.getName() + " loot_bastion_treasure")
                && fired.contains(player.getName() + " loot_bastion_hoglin_stable") && fired.contains(player.getName() + " loot_bastion_bridge")
                && !fired.contains(player.getName() + " loot_ruined_portal"), "War Pigs' four events fire for the player the chest is rolled for " + fired);
    }

    private static boolean has(String item) {
        return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(NetherUpdateBackport.MOD_ID, item));
    }

    // ---- seed parity: which chunks start what

    private static void slots(Check c) throws Exception {
        List<String[]> lines = oracle("slots_1165.txt");
        c.ok(lines.size() == 4 * 1600, "oracle: " + lines.size() + " slot lines");
        Map<Long, NetherBiomeSource> sources = new HashMap<Long, NetherBiomeSource>();
        int bastions = 0;
        int fortresses = 0;
        int none = 0;
        int wrong = 0;
        int biomeWrong = 0;
        String first = "";
        for (String[] l : lines) {
            long seed = Long.parseLong(l[0]);
            int x = Integer.parseInt(l[1]);
            int z = Integer.parseInt(l[2]);
            NetherBiomeSource src = sources.computeIfAbsent(seed, s -> new NetherBiomeSource(s, Collections.<NetherBiomes.Kind, Float>emptyMap(), false));
            NetherBiomes.Kind kind = src.noiseKind((x << 2) + 2, 0, (z << 2) + 2);
            if (!("minecraft:" + kind.id).equals(l[3])) {
                biomeWrong++;
            }
            NetherStructureSlots.Slot roll = NetherStructureSlots.roll(seed, x, z);
            String ours = roll == NetherStructureSlots.Slot.FORTRESS ? "fortress"
                    : roll == NetherStructureSlots.Slot.BASTION ? (kind == NetherBiomes.Kind.BASALT_DELTAS ? "none" : "bastion") : "not a candidate";
            if (l[4].equals("bastion")) {
                bastions++;
            } else if (l[4].equals("fortress")) {
                fortresses++;
            } else {
                none++;
            }
            if (!ours.equals(l[4])) {
                if (wrong++ == 0) {
                    first = String.join(" ", l) + " ours " + ours;
                }
            }
        }
        info(c, wrong == 0 && bastions > 1000 && fortresses > 1000 && none > 100, "seed parity: bastions, fortresses and empty slots in exactly 1.16.5's chunks: "
                + bastions + " bastions, " + fortresses + " fortresses, " + none + " basalt delta slots over 4 seeds; " + wrong + " differ " + first);
        c.eq(0, biomeWrong, "the noise biome at every slot is 1.16.5's");
        // In this world, through the installed rule.
        WorldServer nether = nether(c);
        int agree = 0;
        int checked = 0;
        for (String[] l : lines) {
            if (Long.parseLong(l[0]) != nether.getSeed()) {
                continue;
            }
            checked++;
            NetherStructureSlots.Slot s = NetherStructureSlots.startAt(nether, Integer.parseInt(l[1]), Integer.parseInt(l[2]));
            String ours = s == NetherStructureSlots.Slot.FORTRESS ? "fortress" : s == NetherStructureSlots.Slot.BASTION ? "bastion" : "none";
            if (ours.equals(l[4])) {
                agree++;
            }
        }
        if (checked > 0) {
            c.eq(checked, agree, "this world (seed " + nether.getSeed() + "): Core's startAt with this stream's biome rule is 1.16.5's in every slot");
        } else {
            NetherUpdateBackport.LOGGER.info("SELFTEST INFO world seed {} is none of the oracle's; set level-seed=12345 to check this world's slots too", nether.getSeed());
        }
    }

    private static void portals(Check c) throws Exception {
        List<String[]> lines = oracle("portals_1165.txt");
        c.ok(lines.size() == 4 * (400 + 1024), "oracle: " + lines.size() + " ruined portal lines");
        WorldServer nether = nether(c);
        int candidates = 0;
        int candidateWrong = 0;
        int decided = 0;
        int decisionWrong = 0;
        int firstYWrong = 0;
        int boxWrong = 0;
        int below = 0;
        String first = "";
        Map<String, Integer> types = new LinkedHashMap<String, Integer>();
        for (String[] l : lines) {
            long seed = Long.parseLong(l[0]);
            boolean inNether = l[1].equals("nether");
            int x = Integer.parseInt(l[2]);
            int z = Integer.parseInt(l[3]);
            candidates++;
            ChunkPos want = (inNether ? RuinedPortals.NETHER_PLACEMENT : VanillaPlacements.V1_16_5.RUINED_PORTAL).candidate(seed, x, z);
            if (want.x != x || want.z != z) {
                candidateWrong++;
            }
            if (!l[6].equals("valid")) {
                continue;
            }
            types.merge(l[5], 1, Integer::sum);
            RuinedPortals.Type type = RuinedPortals.Type.valueOf(l[5].toUpperCase(java.util.Locale.ROOT));
            RuinedPortals.Layout ours = RuinedPortals.layout(inNether ? nether : c.world, seed, x, z, type, null);
            decided++;
            String mine = "minecraft:" + ours.template + " " + ours.rotation.name() + " " + ours.mirror.name() + " " + ours.placement.id() + " "
                    + ours.airPocket + " " + (inNether ? l[12] : l[12]) + " " + ours.mossiness + " " + ours.overgrown + " " + ours.vines + " " + ours.blackstone;
            String theirs = l[7] + " " + l[8] + " " + l[9] + " " + l[10] + " " + l[11] + " " + l[12] + " " + l[13] + " " + l[14] + " " + l[15] + " " + l[16];
            if (!mine.equals(theirs) || ours.position.getX() != Integer.parseInt(l[17]) || ours.position.getZ() != Integer.parseInt(l[19])) {
                if (decisionWrong++ == 0 && first.isEmpty()) {
                    first = String.join(" ", l) + " / ours " + mine + " " + ours.position;
                }
            }
            int vy = Integer.parseInt(l[18]);
            if (inNether && vy > ours.firstY) {
                firstYWrong++;
            }
            if (inNether && vy == ours.firstY) {
                below++;
            }
            StructureBoundingBox b = ours.box;
            if (b.minX != Integer.parseInt(l[20]) || b.minZ != Integer.parseInt(l[22]) || b.maxX != Integer.parseInt(l[23]) || b.maxZ != Integer.parseInt(l[25])
                    || b.maxY - b.minY != Integer.parseInt(l[24]) - Integer.parseInt(l[21])) {
                if (boxWrong++ == 0 && first.isEmpty()) {
                    first = String.join(" ", l) + " / box " + b;
                }
            }
        }
        c.eq(0, candidateWrong, "seed parity: ruined portal candidate chunks are 1.16.5's (" + candidates + " regions over 4 seeds, both dimensions)");
        info(c, decided > 4000 && decisionWrong == 0, "seed parity: every ruined portal start decides as 1.16.5's (placement, template, rotation, mirror, air pocket, "
                + "mossiness, column): " + decided + " starts " + types + ", " + decisionWrong + " differ " + first);
        c.eq(0, boxWrong, "every ruined portal's box is 1.16.5's");
        c.eq(0, firstYWrong, "nether portals: 1.16.5's height is never above the random's (the corner test only lowers it)");
        NetherUpdateBackport.LOGGER.info("SELFTEST INFO nether portals left where the random put them by 1.16.5's corner test: {}", below);
        // The variant table against 1.16.5's biomes, through 1.12.2's ids.
        int typesChecked = 0;
        List<String> typeWrong = new ArrayList<String>();
        for (String[] l : oracle("portal_types_1165.txt")) {
            Biome b = Biome.getBiomeForId(Integer.parseInt(l[0]));
            // Ids 1.12.2 leaves free are given to other mods' biomes (stream V's nether biomes among them).
            if (b == null || l[2].equals("nether") || !"minecraft".equals(b.getRegistryName().getNamespace())) {
                continue;
            }
            typesChecked++;
            RuinedPortals.Type t = RuinedPortals.typeOf(b);
            String ours = t == null ? "none" : t.id();
            if (!ours.equals(l[2])) {
                typeWrong.add(l[0] + " " + l[1] + " " + l[2] + " ours " + ours);
            }
        }
        info(c, typesChecked > 55 && typeWrong.isEmpty(), "every 1.12.2 biome lists 1.16.5's ruined portal (" + typesChecked + " biomes) " + typeWrong);
    }

    private static void fossils(Check c) throws Exception {
        List<String[]> lines = oracle("fossils_1165.txt");
        Map<Long, NetherBiomeSource> sources = new HashMap<Long, NetherBiomeSource>();
        Set<String> theirs = new HashSet<String>();
        int valid = 0;
        int wrong = 0;
        String first = "";
        for (String[] l : lines) {
            long seed = Long.parseLong(l[0]);
            int x = Integer.parseInt(l[1]);
            int z = Integer.parseInt(l[2]);
            theirs.add(seed + " " + x + " " + z);
            if (!l[4].equals("valid")) {
                continue;
            }
            valid++;
            // The column and the draws after the floor search do not depend on the terrain: compare them with the floor
            // where 1.16.5 found it.
            int y = Integer.parseInt(l[8]);
            NetherFossils.Layout at = NetherFossils.layoutAtFloor(seed, x, z, y);
            NetherFossils.Layout ours = at == null ? new NetherFossils.Layout() : at;
            String mine = at == null ? "none" : "minecraft:nether_fossils/fossil_" + at.template + " " + at.rotation.name() + " " + at.position.getX() + " "
                    + at.position.getY() + " " + at.position.getZ();
            String want = l[5] + " " + l[6] + " " + l[7] + " " + l[8] + " " + l[9];
            if (!mine.equals(want) || ours.x != Integer.parseInt(l[7]) || ours.z != Integer.parseInt(l[9]) || ours.startY < y) {
                if (wrong++ == 0) {
                    first = String.join(" ", l) + " / ours " + mine;
                }
            }
        }
        // Every candidate chunk in a soul sand valley, and no other, is one the oracle lists.
        int candidates = 0;
        int listWrong = 0;
        for (long seed : SEEDS) {
            NetherBiomeSource src = sources.computeIfAbsent(seed, s -> new NetherBiomeSource(s, Collections.<NetherBiomes.Kind, Float>emptyMap(), false));
            for (int rx = -60; rx < 60; rx++) {
                for (int rz = -60; rz < 60; rz++) {
                    ChunkPos cp = VanillaPlacements.V1_16_5.NETHER_FOSSIL.candidate(seed, rx * 2, rz * 2);
                    boolean valley = src.noiseKind((cp.x << 2) + 2, 0, (cp.z << 2) + 2) == NetherBiomes.Kind.SOUL_SAND_VALLEY;
                    if (valley) {
                        candidates++;
                    }
                    if (valley != theirs.contains(seed + " " + cp.x + " " + cp.z)) {
                        listWrong++;
                    }
                }
            }
        }
        info(c, candidates > 4000 && listWrong == 0, "seed parity: nether fossils start in exactly 1.16.5's soul sand valley chunks: " + candidates
                + " over 4 seeds, " + listWrong + " differ");
        info(c, valid > 3000 && wrong == 0, "seed parity: every fossil's column, template and rotation are 1.16.5's: " + valid + " fossils, " + wrong
                + " differ " + first);
    }

    // ---- block for block

    private static void blockForBlock(Check c) throws Exception {
        List<String[]> all = oracle("blocks_1165.txt");
        Map<String, Map<BlockPos, String>> structures = new LinkedHashMap<String, Map<BlockPos, String>>();
        Map<BlockPos, String> current = null;
        for (String[] l : all) {
            if (l[0].equals("=")) {
                current = new HashMap<BlockPos, String>();
                structures.put(String.join(" ", java.util.Arrays.copyOfRange(l, 1, l.length - 1)), current);
                continue;
            }
            current.put(new BlockPos(Integer.parseInt(l[0]), Integer.parseInt(l[1]), Integer.parseInt(l[2])), l[3]);
        }
        Map<String, String[]> portals = new HashMap<String, String[]>();
        for (String[] l : oracle("portals_1165.txt")) {
            portals.put(l[0] + " " + l[1] + " " + l[2] + " " + l[3], l);
        }
        int bastions = 0;
        int portalCount = 0;
        for (Map.Entry<String, Map<BlockPos, String>> e : structures.entrySet()) {
            String[] k = e.getKey().split(" ");
            if (k[0].equals("bastion")) {
                long seed = Long.parseLong(k[1]);
                int cx = Integer.parseInt(k[2]);
                int cz = Integer.parseInt(k[3]);
                List<JigsawPiece> pieces = BastionStructure.layout(c.world, seed, cx, cz);
                c.eq(Integer.parseInt(k[4]), pieces.size(), "bastion " + seed + " " + cx + "," + cz + " (" + k[5] + "): pieces as 1.16.5's");
                StructureBoundingBox box = new StructureBoundingBox(pieces.get(0).getBoundingBox());
                for (JigsawPiece p : pieces) {
                    box.expandTo(p.getBoundingBox());
                }
                BlockPos bad = compare(c, "bastion " + seed + " " + cx + "," + cz + " (" + k[5] + ")", e.getValue(), box, chunk -> {
                    for (JigsawPiece p : pieces) {
                        if (p.getBoundingBox().intersectsWith(chunk)) {
                            p.addComponentParts(c.world, new Random(0L), chunk);
                        }
                    }
                });
                if (bad != null) {
                    explain(c, pieces, bad, e.getValue().get(bad));
                }
                bastions++;
            } else {
                String[] l = portals.get(k[1] + " " + k[2] + " " + k[3] + " " + k[4]);
                if (!c.ok(l != null && l[6].equals("valid"), "portal " + e.getKey() + " is in the oracle's list")) {
                    continue;
                }
                RuinedPortals.Layout layout = RuinedPortals.given(RuinedPortals.Type.valueOf(l[5].toUpperCase(java.util.Locale.ROOT)),
                        RuinedPortals.Placement.valueOf(l[10].toUpperCase(java.util.Locale.ROOT)), l[7].substring("minecraft:".length()),
                        Rotation.valueOf(l[8]), Mirror.valueOf(l[9]), new BlockPos(Integer.parseInt(l[17]), Integer.parseInt(l[18]), Integer.parseInt(l[19])),
                        Boolean.parseBoolean(l[11]), Boolean.parseBoolean(l[12]), Float.parseFloat(l[13]), Boolean.parseBoolean(l[14]),
                        Boolean.parseBoolean(l[15]), Boolean.parseBoolean(l[16]));
                RuinedPortalPiece piece = layout.piece();
                compare(c, "portal " + e.getKey() + " (" + l[7] + " " + l[5] + ")", e.getValue(), piece.templateBox(), chunk -> piece.placeTemplate(c.world, chunk));
                portalCount++;
            }
        }
        info(c, bastions == 4 && portalCount == 18, "compared " + bastions + " whole bastions and " + portalCount + " ruined portals block for block");
    }

    /**
     * Fills the structure's box with a marker, builds it chunk by chunk (at its own coordinates in the overworld: the
     * processors' randoms are per position), then compares every position with what 1.16.5 placed: the same block
     * there, and the marker wherever 1.16.5 placed nothing.
     */
    /** Which pieces cover a position that came out differently, and what each leaves there, piece by piece. */
    private static void explain(Check c, List<JigsawPiece> pieces, BlockPos pos, String want) {
        StructureBoundingBox chunk = new StructureBoundingBox(pos.getX() >> 4 << 4, 0, pos.getZ() >> 4 << 4, (pos.getX() >> 4 << 4) + 15, 255,
                (pos.getZ() >> 4 << 4) + 15);
        StringBuilder out = new StringBuilder("SELFTEST INFO at " + pos + " 1.16.5 has " + want + ":");
        c.world.setBlockState(pos, MARKER, 2);
        for (int i = 0; i < pieces.size(); i++) {
            JigsawPiece p = pieces.get(i);
            if (!p.getBoundingBox().isVecInside(pos)) {
                continue;
            }
            p.addComponentParts(c.world, new Random(0L), chunk);
            out.append(" | piece ").append(i).append(' ').append(p.element().template()).append(' ').append(p.rotation()).append(" at ")
                    .append(p.position()).append(" pivot ").append(p.pivot()).append(" -> ").append(c.world.getBlockState(pos))
                    .append(" element ").append(p.element());
        }
        NetherUpdateBackport.LOGGER.info(out.toString());
    }

    private static BlockPos compare(Check c, String what, Map<BlockPos, String> want, StructureBoundingBox box, java.util.function.Consumer<StructureBoundingBox> build) {
        WorldServer w = c.world;
        for (int x = (box.minX >> 4) - 1; x <= (box.maxX >> 4) + 1; x++) {
            for (int z = (box.minZ >> 4) - 1; z <= (box.maxZ >> 4) + 1; z++) {
                w.getChunk(x, z);
            }
        }
        fill(w, box, MARKER);
        for (int x = box.minX >> 4; x <= box.maxX >> 4; x++) {
            for (int z = box.minZ >> 4; z <= box.maxZ >> 4; z++) {
                build.accept(new StructureBoundingBox(x << 4, 0, z << 4, (x << 4) + 15, 255, (z << 4) + 15));
            }
        }
        Map<String, IBlockState> resolved = new HashMap<String, IBlockState>();
        int same = 0;
        int differ = 0;
        int unresolved = 0;
        int extra = 0;
        Map<String, Integer> kinds = new LinkedHashMap<String, Integer>();
        String firstDiff = "";
        BlockPos firstBad = null;
        Set<String> unresolvedNames = new TreeSet<String>();
        int jigsaws = 0;
        for (Map.Entry<BlockPos, String> e : want.entrySet()) {
            if (e.getValue().startsWith("minecraft:jigsaw")) {
                // 1.16.5 leaves a ruined portal's one jigsaw block in the floor; 1.12.2 has none, and Core places its
                // final state (netherrack) instead.
                jigsaws++;
                continue;
            }
            IBlockState expected = resolved.computeIfAbsent(e.getValue(), s -> ModernPalette.resolve(s).state);
            IBlockState got = w.getBlockState(e.getKey());
            if (expected == null) {
                unresolved++;
                unresolvedNames.add(e.getValue());
                continue;
            }
            if (got.getBlock() == expected.getBlock() && got.getBlock().getMetaFromState(got) == expected.getBlock().getMetaFromState(expected)) {
                same++;
            } else {
                differ++;
                String key = e.getValue() + " -> " + got;
                kinds.merge(key, 1, Integer::sum);
                if (firstDiff.isEmpty()) {
                    firstDiff = e.getKey() + " " + key + " (want " + expected + ")";
                    firstBad = e.getKey();
                }
            }
        }
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            if (!want.containsKey(p) && w.getBlockState(p) != MARKER) {
                extra++;
            }
        }
        StringBuilder top = new StringBuilder();
        kinds.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(5).forEach(k -> top.append(k.getKey()).append(" x").append(k.getValue()).append("; "));
        info(c, differ == 0 && unresolved == 0 && extra == 0 && same + jigsaws == want.size(), what + " block for block as 1.16.5: " + same + "/" + want.size()
                + " same (" + jigsaws + " jigsaw left by 1.16.5), " + differ + " differ, " + unresolved + " unresolved " + unresolvedNames + ", " + extra + " placed where 1.16.5 places nothing. "
                + firstDiff + " " + top);
        // Leave nothing behind for later tests: mobs and items of the build.
        for (Entity en : w.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1))) {
            if (!(en instanceof EntityPlayer)) {
                en.setDead();
            }
        }
        return firstBad;
    }

    /** Writes a state straight into the chunks' storage over a box (no light, no updates: a blank to build on). */
    static void fill(World w, StructureBoundingBox box, IBlockState state) {
        for (int cx = box.minX >> 4; cx <= box.maxX >> 4; cx++) {
            for (int cz = box.minZ >> 4; cz <= box.maxZ >> 4; cz++) {
                Chunk chunk = w.getChunk(cx, cz);
                for (TileEntity te : new ArrayList<TileEntity>(chunk.getTileEntityMap().values())) {
                    if (box.isVecInside(te.getPos())) {
                        w.removeTileEntity(te.getPos());
                    }
                }
                ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
                for (int x = Math.max(box.minX, cx << 4); x <= Math.min(box.maxX, (cx << 4) + 15); x++) {
                    for (int z = Math.max(box.minZ, cz << 4); z <= Math.min(box.maxZ, (cz << 4) + 15); z++) {
                        for (int y = Math.max(0, box.minY); y <= Math.min(255, box.maxY); y++) {
                            int s = y >> 4;
                            if (storage[s] == Chunk.NULL_BLOCK_STORAGE) {
                                storage[s] = new ExtendedBlockStorage(s << 4, w.provider.hasSkyLight());
                            }
                            storage[s].set(x & 15, y & 15, z & 15, state);
                        }
                    }
                }
                chunk.markDirty();
            }
        }
    }

    // ---- generation in this world

    /** Counts chunks loaded while another is populated: each would be cascading worldgen. */
    public static final class CascadeWatch {
        int loads;
        final List<String> where = new ArrayList<String>();
        private static Field populating;

        @SubscribeEvent
        public void load(ChunkEvent.Load e) {
            if (e.getWorld().isRemote) {
                return;
            }
            Object during = null;
            try {
                if (populating == null) {
                    populating = Chunk.class.getDeclaredField("populating");
                    populating.setAccessible(true);
                }
                during = populating.get(null);
            } catch (ReflectiveOperationException ignored) {
            }
            if (during != null) {
                this.loads++;
                String source = "vanilla";
                for (StackTraceElement f : new Throwable().getStackTrace()) {
                    if (f.getClassName().startsWith("com.exiledradio") && !f.getClassName().contains(".selftest.")) {
                        source = f.getClassName() + "." + f.getMethodName();
                        break;
                    }
                }
                this.where.add(e.getWorld().provider.getDimension() + ":" + e.getChunk().x + "," + e.getChunk().z + " by " + source);
                if (this.loads <= 3) {
                    NetherUpdateBackport.LOGGER.error("SELFTEST cascade: chunk " + e.getChunk().x + "," + e.getChunk().z + " loaded while populating "
                            + during, new Throwable("here"));
                }
            }
        }
    }

    private static void generation(Check c) throws Exception {
        WorldServer nether = nether(c);
        CascadeWatch watch = new CascadeWatch();
        MinecraftForge.EVENT_BUS.register(watch);
        try {
            bastionInWorld(c, nether);
            portalsInWorld(c, nether);
            portalsInWorld(c, c.world);
            fossilsInWorld(c, nether);
            fortressInWorld(c, nether);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(watch);
        }
        infoEq(c, 0, watch.loads, "no cascading worldgen while bastions, portals, fossils and fortresses generate " + watch.where);
    }

    private static void generate(WorldServer w, StructureBoundingBox box) {
        for (int x = (box.minX >> 4) - 1; x <= (box.maxX >> 4) + 1; x++) {
            for (int z = (box.minZ >> 4) - 1; z <= (box.maxZ >> 4) + 1; z++) {
                w.getChunk(x, z);
            }
        }
    }

    /** A candidate chunk near the middle that starts this kind, nearest first. */
    private static ChunkPos nearest(WorldServer w, String kind, int maxRegions) {
        ScatteredStructure s = ScatteredStructure.of(w, kind);
        int spacing = s.placement().spacing();
        for (int r = 0; r <= maxRegions; r++) {
            for (int rx = -r; rx <= r; rx++) {
                for (int rz = -r; rz <= r; rz++) {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) != r) {
                        continue;
                    }
                    ChunkPos cp = s.placement().candidate(w.getSeed(), rx * spacing, rz * spacing);
                    if (s.startsAt(w, cp.x, cp.z)) {
                        return cp;
                    }
                }
            }
        }
        return null;
    }

    private static StructureStart saved(ScatteredStructure s, World w, ChunkPos cp) {
        // MapGenStructure keeps laid-out starts in structureMap; isInsideStructure loads the saved data first.
        s.isInsideStructure(new BlockPos(cp.x << 4, 64, cp.z << 4));
        try {
            Field f = net.minecraft.world.gen.structure.MapGenStructure.class.getDeclaredField("structureMap");
            f.setAccessible(true);
            Object map = f.get(s);
            return (StructureStart) ((it.unimi.dsi.fastutil.longs.Long2ObjectMap<?>) map).get(ChunkPos.asLong(cp.x, cp.z));
        } catch (ReflectiveOperationException e) {
            try {
                Field f = net.minecraft.world.gen.structure.MapGenStructure.class.getDeclaredField("field_75053_d");
                f.setAccessible(true);
                return (StructureStart) ((it.unimi.dsi.fastutil.longs.Long2ObjectMap<?>) f.get(s)).get(ChunkPos.asLong(cp.x, cp.z));
            } catch (ReflectiveOperationException e2) {
                return null;
            }
        }
    }

    /** Saving and loading: the start through MapGenStructureIO, each piece's data the same. */
    private static void roundTrip(Check c, String what, StructureStart start, World w) {
        NBTTagCompound tag = start.writeStructureComponentsToNBT(start.getChunkPosX(), start.getChunkPosZ());
        StructureStart back = MapGenStructureIO.getStructureStart(tag, w);
        boolean same = back != null && back.getComponents().size() == start.getComponents().size()
                && back.writeStructureComponentsToNBT(start.getChunkPosX(), start.getChunkPosZ()).equals(tag);
        c.ok(same, what + ": saves and loads the same (" + start.getComponents().size() + " pieces, " + (back == null ? "none" : back.getClass().getSimpleName()) + ")");
    }

    private static Map<Block, Integer> count(World w, StructureBoundingBox box) {
        Map<Block, Integer> out = new HashMap<Block, Integer>();
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            out.merge(w.getBlockState(p).getBlock(), 1, Integer::sum);
        }
        return out;
    }

    private static int n(Map<Block, Integer> count, String name) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
        return b == null ? 0 : count.getOrDefault(b, 0);
    }

    private static void bastionInWorld(Check c, WorldServer nether) {
        ChunkPos cp = nearest(nether, Structures.BASTION_REMNANT, 4);
        if (!c.ok(cp != null, "a bastion slot near the middle of this world")) {
            return;
        }
        List<JigsawPiece> planned = BastionStructure.layout(nether, nether.getSeed(), cp.x, cp.z);
        StructureBoundingBox box = new StructureBoundingBox(planned.get(0).getBoundingBox());
        for (JigsawPiece p : planned) {
            box.expandTo(p.getBoundingBox());
        }
        generate(nether, box);
        ScatteredStructure s = ScatteredStructure.of(nether, Structures.BASTION_REMNANT);
        StructureStart start = saved(s, nether, cp);
        c.ok(start != null && start.getComponents().size() == planned.size(), "the bastion at " + cp + " is laid out from the populate step: "
                + (start == null ? "none" : start.getComponents().size()) + " pieces, planned " + planned.size());
        Map<Block, Integer> blocks = count(nether, box);
        int chests = blocks.getOrDefault(Blocks.CHEST, 0);
        info(c, n(blocks, NetherUpdateBackport.MOD_ID + ":polished_blackstone_bricks") > 3000 && n(blocks, NetherUpdateBackport.MOD_ID + ":blackstone") > 3000
                && chests > 0, "the bastion is built: " + n(blocks, NetherUpdateBackport.MOD_ID + ":polished_blackstone_bricks") + " polished blackstone bricks, "
                + chests + " chests");
        int loot = 0;
        String table = "";
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            TileEntity te = nether.getTileEntity(p);
            if (te instanceof TileEntityChest && ((TileEntityChest) te).getLootTable() != null) {
                loot++;
                table = ((TileEntityChest) te).getLootTable().toString();
            }
        }
        c.ok(loot > 0 && table.startsWith(NetherUpdateBackport.MOD_ID + ":chests/bastion_"), "its chests hold this mod's bastion tables (" + loot + ", " + table + ")");
        List<EntityHoglin> hoglins = nether.getEntitiesWithinAABB(EntityHoglin.class, new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1));
        boolean stableKind = planned.get(0).element().template() != null && planned.get(0).element().template().getPath().contains("hoglin_stable");
        boolean marked = true;
        for (EntityHoglin h : hoglins) {
            NBTTagCompound tag = new NBTTagCompound();
            h.writeEntityToNBT(tag);
            marked &= tag.getBoolean("CannotBeHunted") && tag.getBoolean("PersistenceRequired");
        }
        c.ok(!stableKind || (!hoglins.isEmpty() && marked), "a hoglin stable's hoglins spawn, persistent and never hunted (" + hoglins.size() + ", stable "
                + stableKind + ")");
        c.ok(nether.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1)).isEmpty(),
                "no item dropped while the bastion was built");
        // Piglins and brutes come from stream P's BastionMobs, by name; without it the mob pieces stay empty.
        int piglins = 0;
        int brutes = 0;
        int armed = 0;
        for (net.minecraft.entity.EntityLiving e : nether.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class,
                new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1))) {
            net.minecraft.util.ResourceLocation id = net.minecraft.entity.EntityList.getKey(e);
            if (id == null) {
                continue;
            }
            if (id.getPath().equals("piglin")) {
                piglins++;
                if (!e.getHeldItemMainhand().isEmpty()) {
                    armed++;
                }
            } else if (id.getPath().equals("piglin_brute")) {
                brutes++;
            }
        }
        if (com.exiledradio.netherupdatebackport.structure.BastionPiece.piglinsAvailable()) {
            info(c, piglins + brutes > 0 && armed == piglins, "the bastion's piglins and brutes spawn from stream P's BastionMobs, piglins armed as their"
                    + " template: " + piglins + " piglins (" + armed + " armed), " + brutes + " brutes");
        } else {
            c.eq(0, piglins + brutes, "without stream P no piglin spawns in a bastion");
        }
        c.ok(!NetherStructureSlots.fortressStartsAt(nether, cp.x, cp.z), "no fortress in the bastion's slot");
        if (start != null) {
            roundTrip(c, "bastion", start, nether);
            // Those Were the Days: inside a piece, not merely its box.
            BlockPos inside = null;
            for (StructureComponent p : start.getComponents()) {
                StructureBoundingBox b = p.getBoundingBox();
                inside = new BlockPos((b.minX + b.maxX) / 2, (b.minY + b.maxY) / 2, (b.minZ + b.maxZ) / 2);
                break;
            }
            c.ok(StructureEvents.inBastion(nether, inside), "a player inside a piece is in the bastion");
            c.ok(!StructureEvents.inBastion(nether, new BlockPos(box.maxX + 40, 60, box.maxZ + 40)), "one outside is not");
            EntityPlayerMP p = Players.real(nether, "explorer");
            p.setPosition(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
            p.ticksExisted = 40;
            new StructureEvents().onPlayerTick(new net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                    net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, p));
            c.ok(StructureEvents.firedForTest().contains(p.getName() + " enter_bastion"), "Those Were the Days' event fires inside");
        }
    }

    private static void portalsInWorld(Check c, WorldServer w) throws Exception {
        String dim = w.provider.getDimension() == -1 ? "nether" : "overworld";
        Map<String, String[]> oracle = new HashMap<String, String[]>();
        for (String[] l : oracle("portals_1165.txt")) {
            if (Long.parseLong(l[0]) == w.getSeed() && l[1].equals(dim)) {
                oracle.put(l[2] + " " + l[3], l);
            }
        }
        ScatteredStructure s = ScatteredStructure.of(w, Structures.RUINED_PORTAL);
        int built = 0;
        int sameY = 0;
        int compared = 0;
        int typeSame = 0;
        String yDiff = "";
        boolean saved = false;
        for (int rx = -1; rx <= 1; rx++) {
            for (int rz = -1; rz <= 1; rz++) {
                ChunkPos cp = s.placement().candidate(w.getSeed(), rx * s.placement().spacing(), rz * s.placement().spacing());
                if (!s.startsAt(w, cp.x, cp.z)) {
                    continue;
                }
                RuinedPortalPiece planned = RuinedPortals.layout(w, w.getSeed(), cp.x, cp.z,
                        dim.equals("nether") ? RuinedPortals.Type.NETHER : RuinedPortals.typeOf(quart(w, cp)), NetherHeights.baseForWorld(w)).piece();
                generate(w, planned.getBoundingBox());
                StructureStart start = saved(s, w, cp);
                if (!c.ok(start != null && start.getComponents().size() == 1 && start.getComponents().get(0) instanceof RuinedPortalPiece,
                        dim + " ruined portal at " + cp + " laid out")) {
                    continue;
                }
                RuinedPortalPiece piece = (RuinedPortalPiece) start.getComponents().get(0);
                Map<Block, Integer> blocks = count(w, piece.templateBox());
                int obsidian = blocks.getOrDefault(Blocks.OBSIDIAN, 0) + n(blocks, NetherUpdateBackport.MOD_ID + ":crying_obsidian");
                if (c.ok(obsidian > 0, dim + " ruined portal at " + cp + " is built (" + obsidian + " obsidian, " + piece.templateName() + ")")) {
                    built++;
                }
                Map<BlockPos, String> extras = piece.extras(w);
                int extrasHere = 0;
                int decorated = 0;
                Map<String, Integer> other = new HashMap<String, Integer>();
                for (Map.Entry<BlockPos, String> e : extras.entrySet()) {
                    IBlockState want = ModernPalette.resolve(e.getValue()).state;
                    IBlockState got = w.getBlockState(e.getKey());
                    if (want != null && got.getBlock() == want.getBlock()) {
                        extrasHere++;
                    } else if (decoration(got)) {
                        // Decoration after the structure, as 1.16's features come after its structures.
                        decorated++;
                    } else {
                        other.merge(e.getValue() + " -> " + got.getBlock().getRegistryName(), 1, Integer::sum);
                    }
                }
                info(c, !extras.isEmpty() && other.isEmpty(), dim + " ruined portal at " + cp + ": its netherrack, magma, vines and leaves are placed: "
                        + extrasHere + "/" + extras.size() + ", " + decorated + " decorated over since; otherwise " + other);
                if (!saved) {
                    roundTrip(c, dim + " ruined portal", start, w);
                    saved = true;
                }
                String[] l = oracle.get(cp.x + " " + cp.z);
                if (l != null && l[6].equals("valid")) {
                    compared++;
                    if (l[5].equals(piece.verticalPlacement() == RuinedPortals.Placement.IN_NETHER ? "nether" : l[5])) {
                        typeSame++;
                    }
                    if (Integer.parseInt(l[18]) == piece.position().getY()) {
                        sameY++;
                    } else if (yDiff.length() < 200) {
                        yDiff += cp + " 1.16.5 y " + l[18] + " ours " + piece.position().getY() + "; ";
                    }
                }
            }
        }
        c.ok(built > 0, dim + ": ruined portals generate (" + built + ")");
        if (compared > 0) {
            NetherUpdateBackport.LOGGER.info("SELFTEST INFO {} ruined portals at 1.16.5's height: {}/{} (the height follows each game's own terrain) {}",
                    dim, sameY, compared, yDiff);
        }
    }

    /** Blocks the decoration after a structure puts down (1.16's features come after its structures too). */
    private static boolean decoration(IBlockState s) {
        Block b = s.getBlock();
        String id = b.getRegistryName().getPath();
        return b == Blocks.QUARTZ_ORE || b == Blocks.MAGMA || b == Blocks.GLOWSTONE || b == Blocks.FIRE || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA
                || b == Blocks.GRAVEL || b == Blocks.SOUL_SAND || b == Blocks.BROWN_MUSHROOM || b == Blocks.RED_MUSHROOM || b == Blocks.WATER
                || b == Blocks.FLOWING_WATER || b == Blocks.TALLGRASS || b == Blocks.DOUBLE_PLANT || b == Blocks.YELLOW_FLOWER || b == Blocks.RED_FLOWER
                || b == Blocks.LEAVES || b == Blocks.LEAVES2 || b == Blocks.LOG || b == Blocks.LOG2 || b == Blocks.SNOW_LAYER || b == Blocks.VINE
                || b == Blocks.DIRT || b == Blocks.STONE || b == Blocks.COAL_ORE || b == Blocks.IRON_ORE || b == Blocks.GOLD_ORE || b == Blocks.REDSTONE_ORE
                || b == Blocks.LAPIS_ORE || b == Blocks.DIAMOND_ORE || b == Blocks.EMERALD_ORE || b == Blocks.CLAY || b == Blocks.SAND || b == Blocks.REEDS
                || b == Blocks.DEADBUSH || b == Blocks.CACTUS || b == Blocks.PUMPKIN || b == Blocks.WATERLILY || b == Blocks.MOSSY_COBBLESTONE
                || b == Blocks.MOB_SPAWNER || b == Blocks.CHEST || b == Blocks.AIR
                || id.contains("ore") || id.contains("blackstone") || id.contains("basalt") || id.contains("debris") || id.contains("fungus")
                || id.contains("roots") || id.contains("nylium") || id.contains("vines") || id.contains("sprouts") || id.contains("soul");
    }

    private static Biome quart(World w, ChunkPos cp) {
        Biome[] b = w.getBiomeProvider().getBiomesForGeneration(null, (cp.x << 2) + 2, (cp.z << 2) + 2, 1, 1);
        return b[0];
    }

    private static void fossilsInWorld(Check c, WorldServer nether) throws Exception {
        ScatteredStructure s = ScatteredStructure.of(nether, Structures.NETHER_FOSSIL);
        int built = 0;
        int tried = 0;
        int sameY = 0;
        int compared = 0;
        Map<String, String[]> oracle = new HashMap<String, String[]>();
        for (String[] l : oracle("fossils_1165.txt")) {
            if (Long.parseLong(l[0]) == nether.getSeed() && l[4].equals("valid")) {
                oracle.put(l[1] + " " + l[2], l);
            }
        }
        boolean saved = false;
        for (int rx = -60; rx < 60 && tried < 6; rx++) {
            for (int rz = -60; rz < 60 && tried < 6; rz++) {
                ChunkPos cp = s.placement().candidate(nether.getSeed(), rx * 2, rz * 2);
                if (!s.startsAt(nether, cp.x, cp.z)) {
                    continue;
                }
                NetherFossils.Layout l = NetherFossils.layout(nether.getSeed(), cp.x, cp.z, NetherHeights.baseForWorld(nether));
                if (l == null) {
                    continue;
                }
                tried++;
                generate(nether, new StructureBoundingBox(l.position.getX() - 8, l.position.getY() - 8, l.position.getZ() - 8,
                        l.position.getX() + 8, l.position.getY() + 8, l.position.getZ() + 8));
                StructureStart start = saved(s, nether, cp);
                if (!c.ok(start != null && start.getComponents().size() == 1, "fossil at " + cp + " laid out")) {
                    continue;
                }
                StructureBoundingBox b = start.getComponents().get(0).getBoundingBox();
                if (count(nether, b).getOrDefault(Blocks.BONE_BLOCK, 0) > 0) {
                    built++;
                }
                if (!saved) {
                    roundTrip(c, "nether fossil", start, nether);
                    saved = true;
                }
                String[] o = oracle.get(cp.x + " " + cp.z);
                if (o != null) {
                    compared++;
                    if (Integer.parseInt(o[8]) == l.position.getY()) {
                        sameY++;
                    }
                }
            }
        }
        info(c, tried > 0 && built == tried, "nether fossils are built in soul sand valleys: " + built + "/" + tried);
        if (compared > 0) {
            NetherUpdateBackport.LOGGER.info("SELFTEST INFO nether fossils at 1.16.5's height: {}/{} (the height follows each game's own terrain)", sameY, compared);
        }
    }

    private static void fortressInWorld(Check c, WorldServer nether) {
        c.ok(NetherStructureSlots.fortressRuleActive(), "the nether's fortress generator follows 1.16.5's shared slots");
        ChunkPos fortress = null;
        for (int r = 0; r < 6 && fortress == null; r++) {
            for (int rx = -r; rx <= r && fortress == null; rx++) {
                for (int rz = -r; rz <= r && fortress == null; rz++) {
                    ChunkPos cp = VanillaPlacements.V1_16_5.NETHER_BRIDGE.candidate(nether.getSeed(), rx * 27, rz * 27);
                    if (NetherStructureSlots.startAt(nether, cp.x, cp.z) == NetherStructureSlots.Slot.FORTRESS) {
                        fortress = cp;
                    }
                }
            }
        }
        if (!c.ok(fortress != null, "a fortress slot near the middle")) {
            return;
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                nether.getChunk(fortress.x + x, fortress.z + z);
            }
        }
        StructureStart start = NetherStructureSlots.savedFortress(nether, fortress.x, fortress.z);
        c.ok(start != null && start.getComponents().size() > 1, "a fortress starts in its 1.16.5 slot " + fortress);
    }
}
