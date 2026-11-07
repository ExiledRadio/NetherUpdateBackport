package com.exiledradio.netherupdatebackport.selftest;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

import java.util.List;

/** Crafting-grid helpers for the self-tests. */
final class Crafting {

    private Crafting() {
    }

    static InventoryCrafting grid(int w, int h, ItemStack... stacks) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        }, 3, 3);
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                inv.setInventorySlotContents(row * 3 + col, stacks[row * w + col].copy());
            }
        }
        return inv;
    }

    static void expect(Check c, InventoryCrafting inv, ItemStack want, String what) {
        ItemStack got = CraftingManager.findMatchingResult(inv, c.world);
        c.ok(same(got, want) && got.getCount() == want.getCount(), what + " got " + got);
    }

    static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty() && a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata();
    }

    static boolean containsItem(List<ItemStack> list, ItemStack want) {
        for (ItemStack s : list) {
            if (s.getItem() == want.getItem()) {
                return true;
            }
        }
        return false;
    }
}
