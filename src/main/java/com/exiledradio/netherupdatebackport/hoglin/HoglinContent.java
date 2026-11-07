package com.exiledradio.netherupdatebackport.hoglin;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Stream H: hoglins and zoglins. Entity ids 10 (hoglin) and 11 (zoglin); spawn eggs in 1.16's colors.
 *
 * <p>1.16 spawns hoglins only in crimson forests (stream V registers that spawn with the biome), as vanilla does.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class HoglinContent {

    static final SoundEvent HOGLIN_AMBIENT = Sounds.event("entity.hoglin.ambient");
    static final SoundEvent HOGLIN_ANGRY = Sounds.event("entity.hoglin.angry");
    static final SoundEvent HOGLIN_ATTACK = Sounds.event("entity.hoglin.attack");
    static final SoundEvent HOGLIN_CONVERTED = Sounds.event("entity.hoglin.converted_to_zombified");
    static final SoundEvent HOGLIN_DEATH = Sounds.event("entity.hoglin.death");
    static final SoundEvent HOGLIN_HURT = Sounds.event("entity.hoglin.hurt");
    static final SoundEvent HOGLIN_RETREAT = Sounds.event("entity.hoglin.retreat");
    static final SoundEvent HOGLIN_STEP = Sounds.event("entity.hoglin.step");
    static final SoundEvent ZOGLIN_AMBIENT = Sounds.event("entity.zoglin.ambient");
    static final SoundEvent ZOGLIN_ANGRY = Sounds.event("entity.zoglin.angry");
    static final SoundEvent ZOGLIN_ATTACK = Sounds.event("entity.zoglin.attack");
    static final SoundEvent ZOGLIN_DEATH = Sounds.event("entity.zoglin.death");
    static final SoundEvent ZOGLIN_HURT = Sounds.event("entity.zoglin.hurt");
    static final SoundEvent ZOGLIN_STEP = Sounds.event("entity.zoglin.step");

    private HoglinContent() {
    }

    public static boolean enabled() {
        return ModConfig.hoglins.enabled;
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityHoglin.class).id(Content.id("hoglin"), 10)
                .name(NetherUpdateBackport.MOD_ID + ".hoglin").tracker(80, 3, true).egg(0xC66E55, 0x5F6464).build());
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityZoglin.class).id(Content.id("zoglin"), 11)
                .name(NetherUpdateBackport.MOD_ID + ".zoglin").tracker(80, 3, true).egg(0xC66E55, 0xE6E6E6).build());
        LootTableList.register(EntityHoglin.LOOT);
        LootTableList.register(EntityZoglin.LOOT);
    }
}
