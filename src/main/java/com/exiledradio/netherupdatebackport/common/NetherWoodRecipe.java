package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.google.gson.JsonObject;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Recipe type {@code netherupdatebackport:nether_wood_shaped}: a shaped recipe that only matches a grid with at
 * least one of this mod's items in it.
 *
 * <p>The "nether_wood_" copies of vanilla's wooden recipes take any planks, so 1.16's mixed planks work (three
 * crimson and three oak make a bookshelf). Vanilla's own recipe already covers a grid of only 1.12.2 planks, and
 * a mod that gives each wood its own bookshelf or chest narrows vanilla's recipe to oak and adds one per wood.
 * Matching those grids too, a copy listed before that mod's recipes took them over: spruce planks and books made
 * a vanilla bookshelf instead of that mod's spruce one. Declared in {@code recipes/_factories.json}.
 */
public final class NetherWoodRecipe extends ShapedOreRecipe {

    public NetherWoodRecipe(ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
        super(group, result, primer);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        if (!super.matches(inv, world)) {
            return false;
        }
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem().getRegistryName() != null
                    && NetherUpdateBackport.MOD_ID.equals(stack.getItem().getRegistryName().getNamespace())) {
                return true;
            }
        }
        return false;
    }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            ShapedOreRecipe parsed = ShapedOreRecipe.factory(context, json);
            CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
            primer.width = parsed.getRecipeWidth();
            primer.height = parsed.getRecipeHeight();
            primer.mirrored = true;
            primer.input = parsed.getIngredients();
            String group = parsed.getGroup();
            return new NetherWoodRecipe(group.isEmpty() ? null : new ResourceLocation(group), parsed.getRecipeOutput(), primer);
        }
    }
}
