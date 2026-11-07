package com.exiledradio.netherupdatebackport.music;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Stream M: the Pigstep disc, 1.16's nether music and the nether biomes' ambience. No entity ids, no packets.
 *
 * <p>This class is common: it registers the sound events and the disc, and puts Pigstep in nether fortress chests
 * until bastions (stream X) exist. Which music plays is {@link MusicChoice}; the playing, and the ambience, are the
 * client's ({@code music.client}).
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class MusicContent {

    /** 1.16's {@code music_disc_pigstep}: comparator signal 13. */
    public static final int PIGSTEP_SIGNAL = 13;

    /**
     * Pigstep's chance in a fortress chest: one pool rolled once, Pigstep 5 against nothing 84, which is its chance in
     * a 1.16 bastion's {@code bastion_other} chests (5 of 89).
     */
    public static final int PIGSTEP_WEIGHT = 5;
    public static final int PIGSTEP_EMPTY_WEIGHT = 84;
    public static final String PIGSTEP_POOL = NetherUpdateBackport.MOD_ID + ":pigstep";

    public static ItemMusicDisc pigstep;

    private MusicContent() {
    }

    public static boolean enabled() {
        return ModConfig.music.enabled;
    }

    /** Items register before sound events, so the events are made here (see {@link Sounds}). */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            MusicChoice.setNether(kind, Sounds.event("music.nether." + kind.id));
        }
        NetherAmbience.makeEvents();
        SoundEvent disc = Sounds.event("music_disc.pigstep");
        pigstep = Content.item(event.getRegistry(), "music_disc_pigstep",
                new ItemMusicDisc(NetherUpdateBackport.MOD_ID + ".pigstep", disc, PIGSTEP_SIGNAL));
    }

    /** Pigstep in nether fortress chests, until stream X puts it in bastions. */
    @SubscribeEvent
    public static void lootLoad(LootTableLoadEvent event) {
        if (pigstep == null || !ModConfig.music.pigstepInFortressChests
                || !LootTableList.CHESTS_NETHER_BRIDGE.equals(event.getName())) {
            return;
        }
        LootEntry[] entries = {
                new LootEntryItem(pigstep, PIGSTEP_WEIGHT, 0, new LootFunction[0], new LootCondition[0],
                        new ResourceLocation(NetherUpdateBackport.MOD_ID, "music_disc_pigstep").toString()),
                new LootEntryEmpty(PIGSTEP_EMPTY_WEIGHT, 0, new LootCondition[0], NetherUpdateBackport.MOD_ID + ":empty"),
        };
        event.getTable().addPool(new LootPool(entries, new LootCondition[0], new RandomValueRange(1.0F),
                new RandomValueRange(0.0F), PIGSTEP_POOL));
    }
}
