package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Furnace recipes from {@code assets/netherupdatebackport/smelting.json}, which the generators write from
 * 1.16's smelting recipes (1.12.2's JSON recipes cannot smelt).
 *
 * <pre>    {"stone": [{"input": "netherupdatebackport:polished_blackstone_bricks", "output": "...", "xp": 0.1}]}</pre>
 *
 * <p>One section per stream, merged by {@code tools/assets.py merge_smelting}. An entry whose input or
 * output does not exist (its stream is off) is skipped. Blast furnaces and smokers that read furnace
 * recipes (Village &amp; Pillage's) pick these up on their own.
 */
public final class Smelting {

    private Smelting() {
    }

    /** Called from init, after every registry event. */
    public static void init() {
        InputStream in = Smelting.class.getResourceAsStream("/assets/" + NetherUpdateBackport.MOD_ID + "/smelting.json");
        if (in == null) {
            return;
        }
        int added = 0;
        try {
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> section : root.entrySet()) {
                JsonArray entries = section.getValue().getAsJsonArray();
                for (JsonElement e : entries) {
                    JsonObject o = e.getAsJsonObject();
                    Item input = item(JsonUtils.getString(o, "input"));
                    Item output = item(JsonUtils.getString(o, "output"));
                    if (input == null || output == null) {
                        continue;
                    }
                    ItemStack from = new ItemStack(input, 1, JsonUtils.getInt(o, "input_meta", 0));
                    ItemStack to = new ItemStack(output, JsonUtils.getInt(o, "count", 1), JsonUtils.getInt(o, "output_meta", 0));
                    FurnaceRecipes.instance().addSmeltingRecipe(from, to, JsonUtils.getFloat(o, "xp", 0.0F));
                    added++;
                }
            }
        } catch (RuntimeException ex) {
            NetherUpdateBackport.LOGGER.error("Could not read smelting.json; some furnace recipes are missing.", ex);
        }
        NetherUpdateBackport.LOGGER.debug("Added {} furnace recipes.", added);
    }

    private static Item item(String name) {
        ResourceLocation id = new ResourceLocation(name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item != null && id.equals(item.getRegistryName()) ? item : null;
    }
}
