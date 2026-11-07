package com.exiledradio.netherupdatebackport.owned;

import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Copies of things another family mod owns, registered only when the owner is absent (Rule 1), so this mod
 * works fully on its own: Village &amp; Pillage's smithing table (for netherite), and later its crossbow (for
 * piglins). Same registry paths, states and metadata as the owner's, under this mod's id, so the owner can
 * rename them in place when it is added to a world.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class OwnedContent {

    static final SoundEvent SMITHING_USE = Sounds.event("block.smithing_table.use");

    public static Block smithingTable;

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
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (smithingTable != null) {
            Content.registerBlockItem(event.getRegistry(), smithingTable);
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
