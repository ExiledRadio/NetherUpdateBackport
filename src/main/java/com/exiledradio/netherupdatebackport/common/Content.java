package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one way every stream names and registers its blocks and items.
 *
 * <p>Going through here means a block always gets its translation key, creative tab and harvest tool
 * the same way, and the client always gets a model request for every item: {@code client.ClientSetup}
 * registers every entry in {@link #itemModels()} and nothing else, so an item can never end up
 * without a model because a stream forgot its client class. It holds names only, never client types,
 * so the server loads it safely.
 *
 * <p>A stream keeps the blocks it registers in its own fields; this class only records what the client
 * and the self-test need.
 */
public final class Content {

    /** item -> (metadata -> model name under this mod's {@code models/item/}), in registration order */
    private static final Map<Item, Map<Integer, String>> ITEM_MODELS = new LinkedHashMap<Item, Map<Integer, String>>();

    /** Every block registered through here, for the blockstate check. */
    private static final List<Block> BLOCKS = new ArrayList<Block>();

    private Content() {
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(NetherUpdateBackport.MOD_ID, name);
    }

    /**
     * Names and registers a block. Its item, if it has one, is registered separately with
     * {@link #registerBlockItem} in the item event.
     *
     * @param tool the harvest tool ("axe", "pickaxe", "shovel"), or null for none
     */
    public static <B extends Block> B block(IForgeRegistry<Block> registry, String name, B block, @Nullable String tool) {
        block.setRegistryName(id(name));
        block.setTranslationKey(NetherUpdateBackport.MOD_ID + "." + name);
        block.setCreativeTab(ModCreativeTab.TAB);
        if (tool != null) {
            block.setHarvestLevel(tool, 0);
        }
        registry.register(block);
        BLOCKS.add(block);
        return block;
    }

    /** Registers a plain {@link ItemBlock} for a block, with its model at metadata 0. */
    public static ItemBlock registerBlockItem(IForgeRegistry<Item> registry, Block block) {
        ItemBlock item = new ItemBlock(block);
        return blockItem(registry, block, item);
    }

    /** Registers a given item for a block, under the block's name, with its model at metadata 0. */
    public static <I extends Item> I blockItem(IForgeRegistry<Item> registry, Block block, I item) {
        item.setRegistryName(block.getRegistryName());
        registry.register(item);
        model(item, 0, block.getRegistryName().getPath());
        return item;
    }

    /** Names and registers an item, with its model at metadata 0. */
    public static <I extends Item> I item(IForgeRegistry<Item> registry, String name, I item) {
        item.setRegistryName(id(name));
        item.setTranslationKey(NetherUpdateBackport.MOD_ID + "." + name);
        item.setCreativeTab(ModCreativeTab.TAB);
        registry.register(item);
        model(item, 0, name);
        return item;
    }

    /** Asks the client for one more model: {@code models/item/<model>.json} for this metadata. */
    public static void model(Item item, int meta, String model) {
        Map<Integer, String> metas = ITEM_MODELS.get(item);
        if (metas == null) {
            metas = new LinkedHashMap<Integer, String>();
            ITEM_MODELS.put(item, metas);
        }
        metas.put(meta, model);
    }

    /** @return every item model request, for the client and the self-test */
    public static Map<Item, Map<Integer, String>> itemModels() {
        return Collections.unmodifiableMap(ITEM_MODELS);
    }

    /** @return every block registered through here */
    public static List<Block> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    /** Blocks drawn from a multipart-only blockstate: every state maps to the one model name "multipart". */
    private static final List<Block> MULTIPART = new ArrayList<Block>();

    /**
     * Declares that a block's blockstate file is multipart-only.
     *
     * <p>The client then maps every state to one model name instead of one per state - the parts are
     * picked from the state when drawn, so nothing is lost, and a block whose states multiply never asks
     * the model loader for thousands of identical models.
     */
    public static void multipart(Block block) {
        MULTIPART.add(block);
    }

    public static List<Block> multipartBlocks() {
        return Collections.unmodifiableList(MULTIPART);
    }

    /** block -> properties its blockstate file does not list (the client's state mapper ignores them). */
    private static final Map<Block, String[]> IGNORED = new LinkedHashMap<Block, String[]>();

    /**
     * Declares properties a block's variants file leaves out, as vanilla's fence gates leave out
     * {@code powered}: the client maps states without them and the asset check strips them.
     */
    public static void ignore(Block block, String... properties) {
        IGNORED.put(block, properties);
    }

    public static Map<Block, String[]> ignoredProperties() {
        return Collections.unmodifiableMap(IGNORED);
    }
}
