package com.exiledradio.netherupdatebackport.common;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Lookups by registry name for things other mods of the family own, and for other streams' items.
 *
 * <h2>The rule</h2>
 * The mod that shipped a block first owns it (Rule 1). Nothing 1.16 added belongs to another family
 * mod, so this mod never registers copies; it only <i>uses</i> older mods' things (Village &amp;
 * Pillage's smithing table, campfire recipes, crossbow, composter, loom, blue ice) when they are
 * installed. The ownership table itself is Backport Core's ({@code ownership.BlockOwnership}); this
 * class is the small by-name layer that never links against another mod's classes.
 */
public final class Owners {

    /** Owns the smithing table, campfire, lantern, crossbow, composter, loom and 1.13's stripped logs and blue ice. */
    public static final String VILLAGE_AND_PILLAGE = "villagerbackport";

    /** Owns 1.13's blocks, and a copy of Village &amp; Pillage's 1.13 blocks when that is absent. */
    public static final String UPDATE_AQUATIC = "updateaquaticbackport";

    private Owners() {
    }

    /** @return true if the owning mod is installed, so its block is used and ours is never registered */
    public static boolean present(String owner) {
        return Loader.isModLoaded(owner);
    }

    /**
     * @return the block registered under {@code owner:name} if the owner is installed, else ours under
     * this mod's id; null if neither exists (the stream that would register ours is switched off)
     */
    @Nullable
    public static Block block(String owner, String name) {
        ResourceLocation id = present(owner) ? new ResourceLocation(owner, name) : Content.id(name);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null || !id.equals(block.getRegistryName()) ? null : block;
    }

    /** @return the item for {@code name} the same way as {@link #block}, or null */
    @Nullable
    public static Item item(String owner, String name) {
        ResourceLocation id = present(owner) ? new ResourceLocation(owner, name) : Content.id(name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || !id.equals(item.getRegistryName()) ? null : item;
    }

    /**
     * Looks up another stream's item by name, so streams never link against each other's classes.
     *
     * @return this mod's item {@code name}, or null when its stream is switched off
     */
    @Nullable
    public static Item ours(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(Content.id(name));
        return item == null || !Content.id(name).equals(item.getRegistryName()) ? null : item;
    }

    /** As {@link #ours(String)}, for blocks. */
    @Nullable
    public static Block ourBlock(String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(Content.id(name));
        return block == null || !Content.id(name).equals(block.getRegistryName()) ? null : block;
    }
}
