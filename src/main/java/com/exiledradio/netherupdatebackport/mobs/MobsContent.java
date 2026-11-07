package com.exiledradio.netherupdatebackport.mobs;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.asm.PigZombieTransformer;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Stream Z: zombie pigmen become 1.16's zombified piglins and stay the same entity.
 *
 * <p>Vanilla's {@code minecraft:zombie_pigman} keeps its id, class, loot table (already 1.16's) and spawn egg, so
 * every world, spawner and other mod that names it keeps working. What changes:
 * <ul>
 *   <li>the name, through 1.12.2's own key {@code entity.PigZombie.name} in this mod's {@code en_us.lang};</li>
 *   <li>the model and texture (client, {@code mobs.client});</li>
 *   <li>the sounds (patch P1 answers the sound getters with the events below);</li>
 *   <li>1.16's anger rules ({@link Anger}, {@link ZombifiedPiglins}, {@link HurtByTargetGoal}), set up when a
 *       pigman joins a world, with patch P1 switching 1.12.2's own anger code off.</li>
 * </ul>
 */
public final class MobsContent {

    static final SoundEvent AMBIENT = Sounds.event("entity.zombified_piglin.ambient");
    static final SoundEvent ANGRY = Sounds.event("entity.zombified_piglin.angry");
    static final SoundEvent HURT = Sounds.event("entity.zombified_piglin.hurt");
    static final SoundEvent DEATH = Sounds.event("entity.zombified_piglin.death");

    private MobsContent() {
    }

    public static boolean enabled() {
        return ModConfig.mobs.enabled;
    }

    public static void preInit() {
        if (!enabled()) {
            return;
        }
        if (!PigZombieTransformer.applied) {
            NetherUpdateBackport.LOGGER.warn("Patch P1 is not applied: zombified piglins keep 1.16's targeting, but 1.12.2's "
                    + "sounds, speed boost and sleeping check.");
        }
        Anger.register();
        PigmanHooks.activate();
        MinecraftForge.EVENT_BUS.register(new ZombifiedPiglins());
    }
}
