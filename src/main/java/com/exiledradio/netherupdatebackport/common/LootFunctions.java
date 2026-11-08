package com.exiledradio.netherupdatebackport.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.EnchantRandomly;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The loot functions this mod's tables share (piglin bartering, stream P; bastion and ruined portal chests, stream
 * X). A 1.12.2 table that names an item nobody registers fails as a whole, so an entry names a vanilla stand-in and
 * these turn it into the real thing when the table is rolled. Registered once, by whichever stream asks first.
 *
 * <ul>
 *   <li>{@code netherupdatebackport:set_item}: the first registered of {@code items} (or the one {@code item}), with
 *       {@code data}; count and tags kept. None registered: the stack is empty.</li>
 *   <li>{@code netherupdatebackport:enchant_randomly}: vanilla's {@code enchant_randomly} over the registered ones of
 *       {@code enchantments}, named by registry name (a book becomes an enchanted book). None registered: unchanged.</li>
 * </ul>
 */
public final class LootFunctions {

    private static boolean registered;

    private LootFunctions() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        LootFunctionManager.registerFunction(new SetItem.Serializer());
        LootFunctionManager.registerFunction(new EnchantByName.Serializer());
    }

    /** @return the first of these items that is registered under that very name, or null */
    @Nullable
    public static Item first(String[] names) {
        for (String name : names) {
            ResourceLocation id = new ResourceLocation(name);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && id.equals(item.getRegistryName())) {
                return item;
            }
        }
        return null;
    }

    public static String[] strings(JsonObject json, String key) {
        if (!json.has(key)) {
            return new String[0];
        }
        JsonArray a = JsonUtils.getJsonArray(json, key);
        String[] out = new String[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsString();
        }
        return out;
    }

    public static JsonArray array(String[] values) {
        JsonArray a = new JsonArray();
        for (String v : values) {
            a.add(v);
        }
        return a;
    }

    /** {@code netherupdatebackport:set_item}. */
    public static final class SetItem extends LootFunction {
        final String[] items;
        final int data;

        SetItem(LootCondition[] conditions, String[] items, int data) {
            super(conditions);
            this.items = items;
            this.data = data;
        }

        @Override
        public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
            Item item = first(this.items);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            ItemStack out = new ItemStack(item, stack.getCount(), this.data);
            out.setTagCompound(stack.getTagCompound());
            return out;
        }

        static final class Serializer extends LootFunction.Serializer<SetItem> {
            Serializer() {
                super(Content.id("set_item"), SetItem.class);
            }

            @Override
            public void serialize(JsonObject json, SetItem value, JsonSerializationContext context) {
                json.add("items", array(value.items));
                if (value.data != 0) {
                    json.addProperty("data", value.data);
                }
            }

            @Override
            public SetItem deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
                String[] items = json.has("item") ? new String[] {JsonUtils.getString(json, "item")} : strings(json, "items");
                return new SetItem(conditions, items, JsonUtils.getInt(json, "data", 0));
            }
        }
    }

    /** {@code netherupdatebackport:enchant_randomly}. */
    public static final class EnchantByName extends LootFunction {
        final String[] enchantments;

        EnchantByName(LootCondition[] conditions, String[] enchantments) {
            super(conditions);
            this.enchantments = enchantments;
        }

        @Override
        public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
            List<Enchantment> list = new ArrayList<Enchantment>();
            for (String name : this.enchantments) {
                Enchantment e = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(name));
                if (e != null) {
                    list.add(e);
                }
            }
            if (list.isEmpty()) {
                return stack;
            }
            return new EnchantRandomly(new LootCondition[0], list).apply(stack, rand, context);
        }

        static final class Serializer extends LootFunction.Serializer<EnchantByName> {
            Serializer() {
                super(Content.id("enchant_randomly"), EnchantByName.class);
            }

            @Override
            public void serialize(JsonObject json, EnchantByName value, JsonSerializationContext context) {
                json.add("enchantments", array(value.enchantments));
            }

            @Override
            public EnchantByName deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
                return new EnchantByName(conditions, strings(json, "enchantments"));
            }
        }
    }
}
