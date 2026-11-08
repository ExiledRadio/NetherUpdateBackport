package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.LootFunctions;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bartering: what a piglin throws back for a gold ingot, from the loot table
 * {@code netherupdatebackport:gameplay/piglin_bartering} (current Java's {@code gameplay/piglin_bartering}, one roll,
 * the same 18 entries and weights; written by {@code tools/piglin/generate.py}). Packs can edit it like any loot table.
 *
 * <h2>Other streams' items</h2>
 * Blackstone, crying obsidian and Soul Speed belong to other streams, which can be switched off; 1.12.2 fails a
 * whole loot table over one unknown name. So those entries name a vanilla placeholder and carry this mod's
 * condition {@code netherupdatebackport:registered} (the entry is left out of the draw when the thing is missing,
 * as if the entry were not there) and function {@code netherupdatebackport:set_item} or
 * {@code netherupdatebackport:enchant_randomly} (turns the placeholder into the real thing).
 */
public final class Bartering {

    public static final ResourceLocation TABLE = Content.id("gameplay/piglin_bartering");

    private Bartering() {
    }

    /** Called once, from registration: the table and its condition and functions. */
    static void register() {
        LootTableList.register(TABLE);
        LootConditionManager.registerCondition(new Registered.Serializer());
        // set_item and enchant_randomly are shared with stream X's chests.
        LootFunctions.register();
    }

    /** @return one barter's worth of items for this piglin */
    public static List<ItemStack> roll(EntityPiglin piglin) {
        if (!(piglin.world instanceof WorldServer)) {
            return Collections.emptyList();
        }
        WorldServer world = (WorldServer) piglin.world;
        return world.getLootTableManager().getLootTableFromLocation(TABLE)
                .generateLootForPools(world.rand, new LootContext.Builder(world).withLootedEntity(piglin).build());
    }

    private static String[] names(JsonObject json, String key) {
        if (!json.has(key)) {
            return new String[0];
        }
        JsonArray array = JsonUtils.getJsonArray(json, key);
        String[] out = new String[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsString();
        }
        return out;
    }

    private static JsonArray array(String[] names) {
        JsonArray a = new JsonArray();
        for (String n : names) {
            a.add(new JsonPrimitive(n));
        }
        return a;
    }

    /** {@code netherupdatebackport:registered}: true when every named item and enchantment exists. */
    static final class Registered implements LootCondition {
        final String[] items;
        final String[] enchantments;

        Registered(String[] items, String[] enchantments) {
            this.items = items;
            this.enchantments = enchantments;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            for (String n : this.items) {
                ResourceLocation id = new ResourceLocation(n);
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item == null || !id.equals(item.getRegistryName())) {
                    return false;
                }
            }
            for (String n : this.enchantments) {
                if (ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(n)) == null) {
                    return false;
                }
            }
            return true;
        }

        static final class Serializer extends LootCondition.Serializer<Registered> {
            Serializer() {
                super(Content.id("registered"), Registered.class);
            }

            @Override
            public void serialize(JsonObject json, Registered value, JsonSerializationContext context) {
                json.add("items", array(value.items));
                json.add("enchantments", array(value.enchantments));
            }

            @Override
            public Registered deserialize(JsonObject json, JsonDeserializationContext context) {
                return new Registered(names(json, "items"), names(json, "enchantments"));
            }
        }
    }
}
