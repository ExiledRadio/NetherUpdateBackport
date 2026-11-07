package com.exiledradio.netherupdatebackport.strider;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraft.entity.EntitySpawnPlacementRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Stream R: striders and the warped fungus on a stick. Entity id 20; spawn egg in 1.16's colors.
 *
 * <p>Striders are the one 1.16 mob that never needed a new biome: they spawn on lava across the nether (1.16: weight
 * 60, groups of 1-2, as creatures). 1.12.2 has no "in lava" spawn placement, so one is added through Forge
 * ({@code EnumHelper.addSpawnPlacementType}): a lava block, the strider then floats up to its surface.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class StriderContent {

    static final SoundEvent AMBIENT = Sounds.event("entity.strider.ambient");
    static final SoundEvent DEATH = Sounds.event("entity.strider.death");
    static final SoundEvent EAT = Sounds.event("entity.strider.eat");
    static final SoundEvent HAPPY = Sounds.event("entity.strider.happy");
    static final SoundEvent HURT = Sounds.event("entity.strider.hurt");
    static final SoundEvent RETREAT = Sounds.event("entity.strider.retreat");
    static final SoundEvent SADDLE = Sounds.event("entity.strider.saddle");
    static final SoundEvent STEP = Sounds.event("entity.strider.step");
    static final SoundEvent STEP_LAVA = Sounds.event("entity.strider.step_lava");

    public static Item stick;

    private StriderContent() {
    }

    public static boolean enabled() {
        return ModConfig.striders.enabled;
    }

    /** Warped fungus (stream F's, by name) and the warped fungus on a stick tempt striders. */
    static Set<Item> temptItems() {
        Set<Item> items = new HashSet<Item>();
        if (stick != null) {
            items.add(stick);
        }
        Item fungus = Owners.ours("warped_fungus");
        if (fungus != null) {
            items.add(fungus);
        }
        return items;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            stick = Content.item(event.getRegistry(), "warped_fungus_on_a_stick", new ItemWarpedFungusOnAStick());
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityStrider.class).id(Content.id("strider"), 20)
                .name(NetherUpdateBackport.MOD_ID + ".strider").tracker(80, 3, true).egg(0x9C3436, 0x4D494D).build());
        LootTableList.register(EntityStrider.LOOT);
    }

    /** Called from post-init: striders on the nether's lava. */
    public static void postInit() {
        if (!enabled() || !ModConfig.striders.spawn) {
            return;
        }
        EntityLiving.SpawnPlacementType inLava = EnumHelper.addSpawnPlacementType("NETHERUPDATEBACKPORT_IN_LAVA",
                (world, pos) -> world.getBlockState(pos).getMaterial() == Material.LAVA);
        EntitySpawnPlacementRegistry.setPlacementType(EntityStrider.class, inLava);
        EntityRegistry.addSpawn(EntityStrider.class, 60, 1, 2, EnumCreatureType.CREATURE, net.minecraft.init.Biomes.HELL);
    }
}
