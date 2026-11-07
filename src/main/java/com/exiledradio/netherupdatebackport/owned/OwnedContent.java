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
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Copies of things another family mod owns, registered only when the owner is absent (Rule 1), so this mod
 * works fully on its own: Village &amp; Pillage's smithing table (for netherite), its lantern, smooth quartz slab,
 * stone brick wall and mossy stone brick stairs, slab and wall (for 1.16's bastions and ruined portals), and later its
 * crossbow (for piglins). Same registry paths, states and metadata as the owner's, under this mod's id, so the owner can
 * rename them in place when it is added to a world.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class OwnedContent {

    static final SoundEvent SMITHING_USE = Sounds.event("block.smithing_table.use");

    public static Block smithingTable;
    /** Village &amp; Pillage's blocks that 1.16's bastions use (stream X): lantern, smooth quartz slab, stone brick wall. */
    public static Block lantern;
    public static Block smoothQuartzSlab;
    public static Block stoneBrickWall;
    /** And what 1.16's {@code block_age} turns ruined portals' stone bricks into: the mossy stairs, slab and wall. */
    public static Block mossyStoneBrickStairs;
    public static Block mossyStoneBrickSlab;
    public static Block mossyStoneBrickWall;

    private OwnedContent() {
    }

    /** @return true when this game has no Village &amp; Pillage, so this mod registers its copies */
    public static boolean copies() {
        return !Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE);
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
