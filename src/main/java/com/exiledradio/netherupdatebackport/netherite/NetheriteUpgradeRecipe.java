package com.exiledradio.netherupdatebackport.netherite;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

/**
 * The crafting-grid way to netherite gear, for games without a smithing table (Village &amp; Pillage's is
 * the 1.16 way; with it installed these are never registered).
 *
 * <p>A diamond tool or armor piece and a netherite ingot, anywhere in the grid, make the netherite piece
 * with everything the diamond one had: its enchantments, name and repair cost (its whole tag), and the same
 * damage taken, as the smithing table carries them over. 1.16's snapshots before the smithing table did this
 * in the crafting grid too.
 */
public final class NetheriteUpgradeRecipe extends ShapelessRecipes {

    private final Item base;

    NetheriteUpgradeRecipe(Item base, Item ingot, ItemStack result) {
        super("netherite_upgrade", result, NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(base), Ingredient.fromItem(ingot)));
        this.base = base;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack out = getRecipeOutput().copy();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack in = inv.getStackInSlot(i);
            if (in.getItem() == this.base) {
                NBTTagCompound tag = in.getTagCompound();
                if (tag != null) {
                    out.setTagCompound(tag.copy());
                }
                out.setItemDamage(Math.min(in.getItemDamage(), out.getMaxDamage() - 1));
                break;
            }
        }
        return out;
    }
}
