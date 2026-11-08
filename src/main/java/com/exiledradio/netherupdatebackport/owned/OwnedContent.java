package com.exiledradio.netherupdatebackport.owned;

import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Sounds;

import com.exiledradio.netherupdatebackport.common.shape.BlockNetherSlab;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Copies of things another family mod owns, registered only when the owner is absent (Rule 1), so this mod
 * works fully on its own: Village &amp; Pillage's smithing table (for netherite), its crossbow (for piglins), and its
 * lantern, smooth quartz slab, stone brick wall and mossy stone brick stairs, slab and wall (for 1.16's bastions and
 * ruined portals). Same registry paths, states and metadata as the owner's, under this mod's id, so the owner can
 * rename them in place when it is added to a world.
 *
 * <h2>The crossbow</h2>
 * Village &amp; Pillage's {@code ItemCrossbow}, its three enchantments (Quick Charge, Multishot, Piercing) and its
 * three projectiles, with the owner's numbers, models, textures and sounds. Entity ids 40-42. Like the owner's, it
 * steps aside when another mod's crossbow is installed (the owner's {@code compat.crossbow = AUTO}); piglins then
 * carry that one, found by name.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class OwnedContent {

    static final SoundEvent SMITHING_USE = Sounds.event("block.smithing_table.use");

    static final SoundEvent CROSSBOW_SHOOT = Sounds.event("item.crossbow.shoot");
    static final SoundEvent CROSSBOW_LOADING_START = Sounds.event("item.crossbow.loading_start");
    static final SoundEvent CROSSBOW_LOADING_MIDDLE = Sounds.event("item.crossbow.loading_middle");
    static final SoundEvent CROSSBOW_LOADING_END = Sounds.event("item.crossbow.loading_end");
    /** The start of a load with Quick Charge I, II and III, in place of {@link #CROSSBOW_LOADING_START}. */
    static final SoundEvent[] CROSSBOW_QUICK_CHARGE = {Sounds.event("item.crossbow.quick_charge_1"),
            Sounds.event("item.crossbow.quick_charge_2"), Sounds.event("item.crossbow.quick_charge_3")};

    public static Block smithingTable;
    /** Village &amp; Pillage's blocks that 1.16's bastions use (stream X): lantern, smooth quartz slab, stone brick wall. */
    public static Block lantern;
    public static Block smoothQuartzSlab;
    public static Block stoneBrickWall;
    /** And what 1.16's {@code block_age} turns ruined portals' stone bricks into: the mossy stairs, slab and wall. */
    public static Block mossyStoneBrickStairs;
    public static Block mossyStoneBrickSlab;
    public static Block mossyStoneBrickWall;

    @Nullable
    public static ItemCrossbow crossbow;
    @Nullable
    public static Enchantment quickCharge;
    @Nullable
    public static Enchantment multishot;
    @Nullable
    public static Enchantment piercing;

    private OwnedContent() {
    }

    /** @return true when this game has no Village &amp; Pillage, so this mod registers its copies */
    public static boolean copies() {
        return !Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE);
    }

    /**
     * @return true when this mod registers the crossbow copy: no Village &amp; Pillage, and none of the other
     * crossbows the owner itself steps aside for
     */
    public static boolean crossbowCopy() {
        return copies() && !Loader.isModLoaded("futuremc") && !Loader.isModLoaded("crossbows");
    }

    /** @return the level of one of the copy's crossbow enchantments on a stack, 0 when it has none */
    public static int level(@Nullable Enchantment enchantment, ItemStack stack) {
        return enchantment == null ? 0 : EnchantmentHelper.getEnchantmentLevel(enchantment, stack);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (copies()) {
            smithingTable = Content.block(event.getRegistry(), "smithing_table", new BlockSmithingTable(), "axe");
            lantern = Content.block(event.getRegistry(), "lantern", new BlockLantern(), "pickaxe");
            // Village & Pillage's smooth quartz slab: strength from its smooth quartz, a copy of 1.12.2's quartz block.
            IBlockState quartz = Blocks.QUARTZ_BLOCK.getDefaultState();
            BlockNetherSlab.Half half = new BlockNetherSlab.Half(Blocks.QUARTZ_BLOCK, quartz, 0.8F, 0.8F);
            BlockNetherSlab.Double full = new BlockNetherSlab.Double(Blocks.QUARTZ_BLOCK, quartz, 0.8F, 0.8F, half);
            smoothQuartzSlab = Content.slab(event.getRegistry(), "smooth_quartz_slab", half, full, "pickaxe");
            stoneBrickWall = Content.block(event.getRegistry(), "stone_brick_wall", new Shapes.Wall(Blocks.STONEBRICK), "pickaxe");
            Content.multipart(stoneBrickWall);
            IBlockState mossy = Blocks.STONEBRICK.getStateFromMeta(1);
            mossyStoneBrickStairs = Content.block(event.getRegistry(), "mossy_stone_brick_stairs", new Shapes.Stairs(mossy), "pickaxe");
            BlockNetherSlab.Half mossyHalf = new BlockNetherSlab.Half(Blocks.STONEBRICK, mossy, 1.5F, 6.0F);
            BlockNetherSlab.Double mossyFull = new BlockNetherSlab.Double(Blocks.STONEBRICK, mossy, 1.5F, 6.0F, mossyHalf);
            mossyStoneBrickSlab = Content.slab(event.getRegistry(), "mossy_stone_brick_slab", mossyHalf, mossyFull, "pickaxe");
            mossyStoneBrickWall = Content.block(event.getRegistry(), "mossy_stone_brick_wall", new Shapes.Wall(Blocks.STONEBRICK), "pickaxe");
            Content.multipart(mossyStoneBrickWall);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (smithingTable != null) {
            Content.registerBlockItem(event.getRegistry(), smithingTable);
            Content.registerBlockItem(event.getRegistry(), lantern);
            Content.registerBlockItem(event.getRegistry(), smoothQuartzSlab);
            Content.registerBlockItem(event.getRegistry(), stoneBrickWall);
            Content.registerBlockItem(event.getRegistry(), mossyStoneBrickStairs);
            Content.registerBlockItem(event.getRegistry(), mossyStoneBrickSlab);
            Content.registerBlockItem(event.getRegistry(), mossyStoneBrickWall);
        }
        if (crossbowCopy()) {
            crossbow = Content.item(event.getRegistry(), "crossbow", new ItemCrossbow());
        }
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (!crossbowCopy()) {
            return;
        }
        EnumEnchantmentType type = EnumHelper.addEnchantmentType("NETHERUPDATEBACKPORT_CROSSBOW",
                item -> item instanceof ItemCrossbow);
        // The books of the three show in Combat with the bow's, as in vanilla.
        com.exiledradio.backportcore.creative.CreativePlacement.addEnchantmentType(
                net.minecraft.creativetab.CreativeTabs.COMBAT, type);

        // Village & Pillage's (current Java's) numbers.
        quickCharge = new CrossbowEnchantment("quick_charge", Enchantment.Rarity.UNCOMMON, type, 3, 12, 20);
        multishot = new CrossbowEnchantment("multishot", Enchantment.Rarity.RARE, type, 1, 20, 0);
        piercing = new CrossbowEnchantment("piercing", Enchantment.Rarity.COMMON, type, 4, 1, 10);
        event.getRegistry().registerAll(quickCharge, multishot, piercing);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!crossbowCopy()) {
            return;
        }
        event.getRegistry().registerAll(
                EntityEntryBuilder.create().entity(EntityCrossbowArrow.class).id(Content.id("crossbow_arrow"), 40)
                        .name(NetherUpdateBackport.MOD_ID + ".crossbow_arrow").tracker(64, 20, true).build(),
                EntityEntryBuilder.create().entity(EntityCrossbowSpectralArrow.class).id(Content.id("crossbow_spectral_arrow"), 41)
                        .name(NetherUpdateBackport.MOD_ID + ".crossbow_spectral_arrow").tracker(64, 20, true).build(),
                EntityEntryBuilder.create().entity(EntityCrossbowFirework.class).id(Content.id("crossbow_firework"), 42)
                        .name(NetherUpdateBackport.MOD_ID + ".crossbow_firework").tracker(64, 10, true).build());
    }

    /**
     * Called from pre-init. Backport Core asks the smithing table's owner how its upgrades are set up; without
     * Village &amp; Pillage this mod is that owner, with Village &amp; Pillage's defaults: upgrades on, other
     * mods' netherite found by name, crafting-grid upgrades of other mods left as they are.
     */
    public static void preInit() {
        if (copies()) {
            SmithingUpgrades.configure(() -> true, () -> true, () -> false);
        }
    }
}
