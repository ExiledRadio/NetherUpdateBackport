package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stream Fm: this mod's entries in Backport Core's tables for the other mod that adds some of 1.16's blocks
 * ({@code futuremc}, 0.2.21), so a world can move over and duplicates stay out of sight (Rule 3). Quiet compat: no
 * user-facing text names that mod.
 *
 * <h2>What it registers in a game</h2>
 * Read from its jar and from a world it was installed in: {@code ancient_debris}, {@code chain}, {@code
 * netherite_block}, {@code soul_soil}, {@code soul_fire_lantern}, {@code soul_fire_torch}, the netherite items and
 * {@code record_pigstep}. Its blackstone set and nether gold ore register only in a development game. It also adds
 * Village &amp; Pillage's lantern, smithing table and stone brick walls, which this mod copies when that mod is absent
 * ({@code owned}); those copies pair with its blocks too.
 *
 * <ul>
 *   <li>Blocks and items of the same names stand in for theirs. {@code soul_fire_lantern} is our {@code
 *       soul_lantern}, {@code soul_fire_torch} our {@code soul_torch} (both 1.12.2 torches, same facings) and {@code
 *       record_pigstep} our {@code music_disc_pigstep}.</li>
 *   <li>Their metadata meanings are in {@code migration/futuremc_states.json} ({@code tools/futuremc/states.py}).</li>
 *   <li>With both installed, {@code compat.duplicates} picks which blocks and items stay visible.</li>
 *   <li>Ancient debris: only the visible one generates (theirs has its own world generator, which is taken out
 *       when ours is kept), and the hidden debris smelts into the visible scrap, so debris already in a world
 *       stays useful.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class FutureMcPairs {

    /** Their block's name to ours, where both are the same block under the same name. */
    static final List<String> SAME_BLOCKS = Collections.unmodifiableList(Arrays.asList(
            "ancient_debris", "chain", "netherite_block", "soul_soil",
            // Village & Pillage's, which this mod copies only when that mod is absent.
            "lantern", "smithing_table", "stone_brick_wall", "mossy_stone_brick_wall"));

    /** {their block, our block} where the names differ. */
    static final String[][] RENAMED_BLOCKS = {
            {"soul_fire_lantern", "soul_lantern"},
            {"soul_fire_torch", "soul_torch"},
    };

    /** Items without a block, the same name in both. */
    static final List<String> SAME_ITEMS = Collections.unmodifiableList(Arrays.asList(
            "netherite_ingot", "netherite_scrap",
            "netherite_sword", "netherite_shovel", "netherite_pickaxe", "netherite_axe", "netherite_hoe",
            "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"));

    /** {their item, our item} where the names differ. */
    static final String[][] RENAMED_ITEMS = {
            {"record_pigstep", "music_disc_pigstep"},
    };

    /** Their ancient debris generator, by class name (it is only ever looked up, never linked against). */
    static final String THEIR_DEBRIS_GENERATOR = "thedarkcolour.futuremc.world.gen.feature.AncientDebrisWorldGen";

    private FutureMcPairs() {
    }

    /** @return every {our block, their block} pair */
    public static List<String[]> blockPairs() {
        List<String[]> pairs = new ArrayList<String[]>();
        for (String name : SAME_BLOCKS) {
            pairs.add(new String[] {name, name});
        }
        for (String[] pair : RENAMED_BLOCKS) {
            pairs.add(new String[] {pair[1], pair[0]});
        }
        return pairs;
    }

    /** @return every {our item, their item} pair of items without a block */
    public static List<String[]> itemPairs() {
        List<String[]> pairs = new ArrayList<String[]>();
        for (String name : SAME_ITEMS) {
            pairs.add(new String[] {name, name});
        }
        for (String[] pair : RENAMED_ITEMS) {
            pairs.add(new String[] {pair[1], pair[0]});
        }
        return pairs;
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        FutureMcTable.Contributor table = FutureMcTable.contributor(NetherUpdateBackport.MOD_ID, () -> ModConfig.compat.convertRemoved);
        for (String[] pair : RENAMED_BLOCKS) {
            table.rename(pair[0], NetherUpdateBackport.MOD_ID + ":" + pair[1]);
        }
        // Theirs faces the player; Village & Pillage's table, which ours copies, has one state.
        table.ignoreProperty("smithing_table", "facing");
        table.items((path, damage) -> {
            for (String[] pair : RENAMED_ITEMS) {
                if (pair[0].equals(path)) {
                    Item ours = Owners.ours(pair[1]);
                    return ours == null ? null : new FutureMcTable.ItemTarget(ours, 0);
                }
            }
            return null;
        });
        table.recordedStates("/assets/netherupdatebackport/migration/futuremc_states.json");

        DuplicateBlocks.register(NetherUpdateBackport.MOD_ID, FutureMcTable.FUTURE_MC, () -> ModConfig.compat.duplicates,
                FutureMcPairs::blockPairs);
        DuplicateBlocks.registerItems(FutureMcPairs::hiddenItems);
    }

    /** @return the items without a block that {@code compat.duplicates} hides, all of them */
    public static List<Item> itemsToHide() {
        List<Item> hide = new ArrayList<Item>();
        DuplicateBlocks.Keep keep = ModConfig.compat.duplicates;
        if (keep == DuplicateBlocks.Keep.BOTH || !Loader.isModLoaded(FutureMcTable.FUTURE_MC)) {
            return hide;
        }
        for (String[] pair : itemPairs()) {
            Item ours = item(NetherUpdateBackport.MOD_ID, pair[0]);
            Item theirs = item(FutureMcTable.FUTURE_MC, pair[1]);
            if (ours != null && theirs != null) {
                hide.add(keep == DuplicateBlocks.Keep.OURS ? theirs : ours);
            }
        }
        return hide;
    }

    /**
     * Items hidden here rather than by Backport Core: theirs whose {@code setCreativeTab} refuses null (a non-null
     * parameter in the language it is written in), which would throw out of Core's {@code hideFromCreative} and stop
     * the game loading. Their tab is cleared through the field, and their recipes are removed here
     * ({@code docs/CORE_REQUESTS.md}, section 6).
     */
    static final Set<Item> HIDDEN_HERE = new LinkedHashSet<Item>();

    /** What Backport Core hides: {@link #itemsToHide}, less those that must be hidden here. */
    static Collection<Item> hiddenItems() {
        List<Item> forCore = new ArrayList<Item>();
        for (Item item : itemsToHide()) {
            if (HIDDEN_HERE.contains(item)) {
                continue;
            }
            try {
                // Core sets this too; doing it first finds the items that refuse.
                item.setCreativeTab(null);
                forCore.add(item);
            } catch (RuntimeException refused) {
                clearTab(item);
                HIDDEN_HERE.add(item);
            }
        }
        return forCore;
    }

    private static void clearTab(Item item) {
        try {
            ReflectionHelper.findField(Item.class, "tabToDisplayOn", "field_77701_a").set(item, null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            NetherUpdateBackport.LOGGER.warn("Could not take {} out of the creative menu.", item.getRegistryName(), e);
        }
    }

    /** As Backport Core's, for {@link #HIDDEN_HERE}: after every mod's recipes, and again from post-init. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removeRecipes(RegistryEvent.Register<IRecipe> event) {
        hiddenItems();
        removeRecipes(event.getRegistry());
    }

    private static void removeRecipes(IForgeRegistry<IRecipe> registry) {
        if (HIDDEN_HERE.isEmpty() || !(registry instanceof IForgeRegistryModifiable)) {
            return;
        }
        List<ResourceLocation> doomed = new ArrayList<ResourceLocation>();
        for (IRecipe recipe : registry) {
            ItemStack output = recipe.getRecipeOutput();
            if (!output.isEmpty() && HIDDEN_HERE.contains(output.getItem())) {
                doomed.add(recipe.getRegistryName());
            }
        }
        for (ResourceLocation name : doomed) {
            ((IForgeRegistryModifiable<IRecipe>) registry).remove(name);
        }
        int smelting = 0;
        for (Iterator<Map.Entry<ItemStack, ItemStack>> it = FurnaceRecipes.instance().getSmeltingList().entrySet().iterator(); it.hasNext(); ) {
            if (HIDDEN_HERE.contains(it.next().getValue().getItem())) {
                it.remove();
                smelting++;
            }
        }
        if (!doomed.isEmpty() || smelting > 0) {
            NetherUpdateBackport.LOGGER.info("Removed {} recipes and {} smelting recipes for hidden duplicates: {}", doomed.size(), smelting, doomed);
        }
    }

    /** @return the item registered under exactly this mod id and name, or null */
    @Nullable
    static Item item(String modId, String name) {
        ResourceLocation key = new ResourceLocation(modId, name);
        Item item = ForgeRegistries.ITEMS.containsKey(key) ? ForgeRegistries.ITEMS.getValue(key) : null;
        return item == null || item.getRegistryName() == null || !key.equals(item.getRegistryName()) ? null : item;
    }

    @Nullable
    static Block block(String modId, String name) {
        ResourceLocation key = new ResourceLocation(modId, name);
        Block block = ForgeRegistries.BLOCKS.containsKey(key) ? ForgeRegistries.BLOCKS.getValue(key) : null;
        return block == null || block == Blocks.AIR || !key.equals(block.getRegistryName()) ? null : block;
    }

    /**
     * From the mod's post-init, after Backport Core's (which removes the hidden items' smelting): one ancient
     * debris generator, and the hidden debris smelting into the visible scrap.
     */
    public static void postInit() {
        hiddenItems();
        removeRecipes(ForgeRegistries.RECIPES);
        DuplicateBlocks.Keep keep = ModConfig.compat.duplicates;
        if (keep == DuplicateBlocks.Keep.BOTH || !Loader.isModLoaded(FutureMcTable.FUTURE_MC)) {
            return;
        }
        Block ourDebris = block(NetherUpdateBackport.MOD_ID, "ancient_debris");
        Block theirDebris = block(FutureMcTable.FUTURE_MC, "ancient_debris");
        if (ourDebris == null || theirDebris == null) {
            return;
        }
        // Ours checks DuplicateBlocks itself (AncientDebrisGeneration), so with theirs kept it stops on its own.
        if (keep == DuplicateBlocks.Keep.OURS) {
            int removed = removeGenerators(THEIR_DEBRIS_GENERATOR);
            NetherUpdateBackport.LOGGER.info("Another mod's ancient debris is hidden; removed {} of its debris generators.", removed);
        }
        Item hidden = Item.getItemFromBlock(keep == DuplicateBlocks.Keep.OURS ? theirDebris : ourDebris);
        Item scrap = item(keep == DuplicateBlocks.Keep.OURS ? NetherUpdateBackport.MOD_ID : FutureMcTable.FUTURE_MC, "netherite_scrap");
        if (scrap != null && FurnaceRecipes.instance().getSmeltingResult(new ItemStack(hidden)).isEmpty()) {
            GameRegistry.addSmelting(hidden, new ItemStack(scrap), 2.0F);
        }
    }

    /** Takes world generators of this class out of Forge's list. */
    @SuppressWarnings("unchecked")
    static int removeGenerators(String className) {
        int removed = 0;
        try {
            Field setField = GameRegistry.class.getDeclaredField("worldGenerators");
            Field indexField = GameRegistry.class.getDeclaredField("worldGeneratorIndex");
            Field sortedField = GameRegistry.class.getDeclaredField("sortedGeneratorList");
            setField.setAccessible(true);
            indexField.setAccessible(true);
            sortedField.setAccessible(true);
            Collection<IWorldGenerator> set = (Collection<IWorldGenerator>) setField.get(null);
            Map<IWorldGenerator, Integer> index = (Map<IWorldGenerator, Integer>) indexField.get(null);
            for (Iterator<IWorldGenerator> it = set.iterator(); it.hasNext(); ) {
                IWorldGenerator generator = it.next();
                if (generator.getClass().getName().equals(className)) {
                    it.remove();
                    index.remove(generator);
                    removed++;
                }
            }
            if (removed > 0) {
                // Rebuilt from the set the next time a chunk is decorated.
                sortedField.set(null, null);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            NetherUpdateBackport.LOGGER.warn("Could not take out another mod's world generator {}.", className, e);
        }
        return removed;
    }
}
