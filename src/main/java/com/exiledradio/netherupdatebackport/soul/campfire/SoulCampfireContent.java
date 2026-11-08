package com.exiledradio.netherupdatebackport.soul.campfire;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

/**
 * The soul campfire: the block, its item and its tile entity. Registered by stream L when
 * {@code soul.enabled} and {@code soul.campfireEnabled} are on.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class SoulCampfireContent {

    @Nullable
    public static BlockSoulCampfire campfire;

    /** 1.14's crackle, under this mod's id (the sound file comes from this mod's generator). */
    public static final SoundEvent crackle = com.exiledradio.netherupdatebackport.common.Sounds.event("block.campfire.crackle");

    private SoulCampfireContent() {
    }

    /** @return true if this mod adds the soul campfire in this game - fixed for the whole session */
    public static boolean enabled() {
        return ModConfig.soul.enabled && ModConfig.soul.campfireEnabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        campfire = com.exiledradio.netherupdatebackport.common.Content.block(event.getRegistry(), "soul_campfire", new BlockSoulCampfire(), "axe");
        com.exiledradio.netherupdatebackport.common.Content.ignore(campfire, "signal_fire");
        GameRegistry.registerTileEntity(TileEntitySoulCampfire.class, new ResourceLocation(NetherUpdateBackport.MOD_ID, "soul_campfire"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (campfire != null) {
            com.exiledradio.netherupdatebackport.common.Content.registerBlockItem(event.getRegistry(), campfire);
        }
    }
}
