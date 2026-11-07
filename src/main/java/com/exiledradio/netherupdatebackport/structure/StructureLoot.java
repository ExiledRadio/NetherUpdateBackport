package com.exiledradio.netherupdatebackport.structure;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.netherupdatebackport.common.Content;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.EnchantRandomly;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The chests: 1.16.5's five tables (four bastion ones and the ruined portal's), written as 1.12.2 tables by
 * {@code tools/structure/generate.py}, plus today's lodestones in ruined portal chests (current Java; no weight 1.16 had
 * changed since). The files' {@code LootTable} names point at them through {@link ModernPalette#lootTable}.
 *
 * <p>A 1.12.2 table that names an item nobody registers fails as a whole, and these name other streams' items and
 * other mods' (Village &amp; Pillage's crossbow and bell). So such an entry names a vanilla stand-in and three small
 * extensions do the rest, by name, when the table is rolled:
 * <ul>
 *   <li>{@code netherupdatebackport:item_exists} (condition): true if one of {@code items} is registered, so an item
 *       nobody adds drops out of the pool, as if 1.16's table did not have it;</li>
 *   <li>{@code netherupdatebackport:set_item} (function): the first of {@code items} that is registered;</li>
 *   <li>{@code netherupdatebackport:enchant_randomly} (function): 1.16's {@code enchant_randomly} from a list by name
 *       (Soul Speed is stream L's).</li>
 * </ul>
 * And {@code netherupdatebackport:advancement_event} (a pool condition that is always false): fires the
 * {@code common.Triggers} event for the player the chest is opened by, which <i>War Pigs</i> waits for, as 1.16's
 * {@code player_generates_container_loot} does. It draws nothing from the random, so the loot is the same.
 */
public final class StructureLoot {

    public static final String[] TABLES = {"bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure", "ruined_portal"};

    private StructureLoot() {
    }

    static void preInit() {
        LootConditionManager.registerCondition(new ItemExists.Serializer());
        LootConditionManager.registerCondition(new AdvancementEvent.Serializer());
        LootFunctionManager.registerFunction(new SetItem.Serializer());
        LootFunctionManager.registerFunction(new EnchantByName.Serializer());
    }

    static void init() {
        for (String t : TABLES) {
            ResourceLocation ours = LootTableList.register(Content.id("chests/" + t));
            ModernPalette.lootTable("minecraft:chests/" + t, ours);
        }
    }

    @Nullable
    static Item first(String[] names) {
        for (String name : names) {
            ResourceLocation id = new ResourceLocation(name);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && id.equals(item.getRegistryName())) {
                return item;
            }
        }
        return null;
    }

    static String[] strings(JsonObject json, String key) {
        JsonArray a = JsonUtils.getJsonArray(json, key);
        String[] out = new String[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsString();
        }
        return out;
    }

    static JsonArray array(String[] values) {
        JsonArray a = new JsonArray();
        for (String v : values) {
            a.add(v);
        }
        return a;
    }

    /** Condition: one of these items is registered. */
    public static final class ItemExists implements LootCondition {
        final String[] items;

        ItemExists(String[] items) {
            this.items = items;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            return first(this.items) != null;
        }

        public static final class Serializer extends LootCondition.Serializer<ItemExists> {
            public Serializer() {
                super(Content.id("item_exists"), ItemExists.class);
            }

            @Override
            public void serialize(JsonObject json, ItemExists value, JsonSerializationContext context) {
                json.add("items", array(value.items));
            }

            @Override
            public ItemExists deserialize(JsonObject json, JsonDeserializationContext context) {
                return new ItemExists(strings(json, "items"));
            }
        }
    }

    /** Function: the stack becomes the first of these items that is registered (count kept). */
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
            return item == null ? ItemStack.EMPTY : new ItemStack(item, stack.getCount(), this.data);
        }

        public static final class Serializer extends LootFunction.Serializer<SetItem> {
            public Serializer() {
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
                return new SetItem(conditions, strings(json, "items"), JsonUtils.getInt(json, "data", 0));
            }
        }
    }

    /** Function: 1.16's {@code enchant_randomly} from a list of enchantments named by registry name, those registered. */
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

        public static final class Serializer extends LootFunction.Serializer<EnchantByName> {
            public Serializer() {
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

    /** Pool condition, always false: fires an advancement event for the player the loot is rolled for. */
    public static final class AdvancementEvent implements LootCondition {
        final String event;

        AdvancementEvent(String event) {
            this.event = event;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            Entity player = context.getKillerPlayer();
            if (player instanceof EntityPlayer) {
                StructureEvents.fire((EntityPlayer) player, this.event);
            }
            return false;
        }

        public static final class Serializer extends LootCondition.Serializer<AdvancementEvent> {
            public Serializer() {
                super(Content.id("advancement_event"), AdvancementEvent.class);
            }

            @Override
            public void serialize(JsonObject json, AdvancementEvent value, JsonSerializationContext context) {
                json.addProperty("event", value.event);
            }

            @Override
            public AdvancementEvent deserialize(JsonObject json, JsonDeserializationContext context) {
                return new AdvancementEvent(JsonUtils.getString(json, "event"));
            }
        }
    }
}
