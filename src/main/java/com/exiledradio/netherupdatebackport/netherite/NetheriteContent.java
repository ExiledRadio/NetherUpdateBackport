package com.exiledradio.netherupdatebackport.netherite;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Sounds;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stream N: ancient debris, netherite scrap and ingots, the netherite block, netherite tools and armor.
 *
 * <p>Numbers from 1.16.5: ancient debris 30/1200 and the netherite block 50/1200 (both diamond pickaxe, so
 * explosions never break them); the netherite block is a beacon base and the ingot a beacon payment. Upgrades
 * from diamond go through the smithing table (Backport Core's {@link SmithingUpgrades}, read by Village &amp;
 * Pillage's table), or, with no smithing table installed, the crafting grid ({@link NetheriteUpgradeRecipe}).
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class NetheriteContent {

    public static final String[] NAMES = {"ancient_debris", "netherite_block"};

    /** 1.16's NETHERITE tier: level 4, 2031 uses, speed 9, damage bonus 4, enchantability 15. */
    public static final Item.ToolMaterial TOOLS = EnumHelper.addToolMaterial("NETHERUPDATEBACKPORT_NETHERITE", 4, 2031, 9.0F, 4.0F, 15);

    /** 1.16's NETHERITE armor: durability factor 37, 3/6/8/3, enchantability 15, toughness 3. */
    public static final ItemArmor.ArmorMaterial ARMOR = EnumHelper.addArmorMaterial("NETHERUPDATEBACKPORT_NETHERITE",
            NetherUpdateBackport.MOD_ID + ":netherite", 37, new int[] {3, 6, 8, 3}, 15,
            Sounds.event("item.armor.equip_netherite"), 3.0F);

    public static Block ancientDebris;
    public static Block netheriteBlock;
    public static Item scrap;
    public static Item ingot;

    /** diamond piece -> netherite piece, in 1.16's order */
    private static final Map<Item, Item> UPGRADES = new LinkedHashMap<Item, Item>();

    private NetheriteContent() {
    }

    public static boolean enabled() {
        return ModConfig.netherite.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockOwnership.declare(NetherUpdateBackport.MOD_ID, NAMES);
        if (!enabled()) {
            return;
        }
        ancientDebris = Content.block(event.getRegistry(), "ancient_debris",
                new Shapes.Plain(Material.IRON, MapColor.BLACK, 30.0F, 1200.0F, Sounds.type("ancient_debris")), "pickaxe");
        ancientDebris.setHarvestLevel("pickaxe", 3);
        netheriteBlock = Content.block(event.getRegistry(), "netherite_block",
                new Shapes.Plain(Material.IRON, MapColor.BLACK, 50.0F, 1200.0F, Sounds.type("netherite_block")) {
                    @Override
                    public boolean isBeaconBase(IBlockAccess world, BlockPos pos, BlockPos beacon) {
                        return true;
                    }
                }, "pickaxe");
        netheriteBlock.setHarvestLevel("pickaxe", 3);
        GameRegistry.registerWorldGenerator(new AncientDebrisGeneration(), 10);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        Content.blockItem(r, ancientDebris, new Fireproof.DebrisItem(ancientDebris));
        Content.blockItem(r, netheriteBlock, new Fireproof.DebrisItem(netheriteBlock));
        scrap = Content.item(r, "netherite_scrap", new Fireproof.Plain(false));
        ingot = Content.item(r, "netherite_ingot", new Fireproof.Plain(true));
        TOOLS.setRepairItem(new ItemStack(ingot));
        ARMOR.setRepairItem(new ItemStack(ingot));

        upgrade(Items.DIAMOND_SWORD, Content.item(r, "netherite_sword", new Fireproof.Sword(TOOLS)));
        upgrade(Items.DIAMOND_SHOVEL, Content.item(r, "netherite_shovel", new Fireproof.Shovel(TOOLS)));
        upgrade(Items.DIAMOND_PICKAXE, Content.item(r, "netherite_pickaxe", new Fireproof.Pickaxe(TOOLS)));
        upgrade(Items.DIAMOND_AXE, Content.item(r, "netherite_axe", new Fireproof.Axe(TOOLS)));
        upgrade(Items.DIAMOND_HOE, Content.item(r, "netherite_hoe", new Fireproof.Hoe(TOOLS)));
        upgrade(Items.DIAMOND_HELMET, Content.item(r, "netherite_helmet", new Fireproof.Armor(ARMOR, EntityEquipmentSlot.HEAD)));
        upgrade(Items.DIAMOND_CHESTPLATE, Content.item(r, "netherite_chestplate", new Fireproof.Armor(ARMOR, EntityEquipmentSlot.CHEST)));
        upgrade(Items.DIAMOND_LEGGINGS, Content.item(r, "netherite_leggings", new Fireproof.Armor(ARMOR, EntityEquipmentSlot.LEGS)));
        upgrade(Items.DIAMOND_BOOTS, Content.item(r, "netherite_boots", new Fireproof.Armor(ARMOR, EntityEquipmentSlot.FEET)));

        OreDictionary.registerOre("ingotNetherite", ingot);
        OreDictionary.registerOre("scrapNetherite", scrap);
        OreDictionary.registerOre("blockNetherite", netheriteBlock);
        OreDictionary.registerOre("oreAncientDebris", ancientDebris);
    }

    private static void upgrade(Item diamond, Item netherite) {
        UPGRADES.put(diamond, netherite);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityFireproofItem.class)
                .id(Content.id("fireproof_item"), 1).name(NetherUpdateBackport.MOD_ID + ".fireproof_item")
                .tracker(64, 20, true).build());
    }

    /** The crafting fallback, registered only when no smithing table is installed (and the switch is on). */
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled() || !ModConfig.netherite.craftingFallback || hasSmithingTable()) {
            return;
        }
        for (Map.Entry<Item, Item> e : UPGRADES.entrySet()) {
            NetheriteUpgradeRecipe recipe = new NetheriteUpgradeRecipe(e.getKey(), ingot, new ItemStack(e.getValue()));
            recipe.setRegistryName(Content.id(e.getValue().getRegistryName().getPath() + "_from_crafting"));
            event.getRegistry().register(recipe);
        }
    }

    /** @return true when a smithing table is in the game: Village &amp; Pillage's (the family's owner of it) */
    public static boolean hasSmithingTable() {
        return Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE);
    }

    /** Called from init: the smithing upgrades, for whichever smithing table reads Backport Core's registry. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        for (Map.Entry<Item, Item> e : UPGRADES.entrySet()) {
            SmithingUpgrades.add(e.getKey(), Ingredient.fromItem(ingot), new ItemStack(e.getValue()), NetherUpdateBackport.MOD_ID);
        }
    }

    /** @return the netherite piece a diamond piece upgrades to, or null */
    public static Item upgradeOf(Item diamond) {
        return UPGRADES.get(diamond);
    }
}
