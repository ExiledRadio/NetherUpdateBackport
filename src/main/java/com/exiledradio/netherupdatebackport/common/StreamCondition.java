package com.exiledradio.netherupdatebackport.common;

import com.google.gson.JsonObject;

import net.minecraft.item.Item;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.function.BooleanSupplier;

/**
 * Recipe condition {@code netherupdatebackport:items}: true when every item named exists.
 *
 * <pre>    "conditions": [{"type": "netherupdatebackport:items", "items": ["netherupdatebackport:blackstone"]}]</pre>
 *
 * <p>Every recipe the generators write carries it, listing each of this mod's items the recipe uses or
 * makes. A stream switched off registers nothing, so its recipes, and other streams' recipes that use its
 * items, are skipped quietly instead of failing to parse. Declared in {@code recipes/_factories.json}.
 */
public final class StreamCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        final String[] names = new String[JsonUtils.getJsonArray(json, "items").size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = JsonUtils.getJsonArray(json, "items").get(i).getAsString();
        }
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                for (String name : names) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
                    if (item == null || !new ResourceLocation(name).equals(item.getRegistryName())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
