package com.exiledradio.netherupdatebackport.mobs;

import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundEvent;

/**
 * What patch P1 ({@code asm.PigZombieTransformer}) calls from vanilla's {@code EntityPigZombie}. Every patched
 * method first asks {@link #active()}; false (stream Z off, or before pre-init) runs vanilla's own code.
 *
 * <p>Called from Minecraft's class by name: keep the names and descriptors as the transformer writes them.
 */
public final class PigmanHooks {

    private static volatile boolean active;

    private PigmanHooks() {
    }

    static void activate() {
        active = true;
    }

    public static boolean active() {
        return active;
    }

    /** {@code isAngry()}: 1.16's {@code remainingPersistentAngerTime > 0}. */
    public static boolean isAngry(EntityPigZombie pigman) {
        Anger anger = Anger.of(pigman);
        return anger != null && anger.isAngry();
    }

    /** {@code isPreventingPlayerRest(player)}: 1.16's {@code isAngryAt(player)}. */
    public static boolean isAngryAt(EntityPigZombie pigman, EntityPlayer player) {
        Anger anger = Anger.of(pigman);
        return anger != null && anger.isAngryAt(player);
    }

    /** {@code getAmbientSound()}: the angry grunt while angry, as 1.16. */
    public static SoundEvent ambientSound(EntityPigZombie pigman) {
        return isAngry(pigman) ? MobsContent.ANGRY : MobsContent.AMBIENT;
    }

    public static SoundEvent hurtSound() {
        return MobsContent.HURT;
    }

    public static SoundEvent deathSound() {
        return MobsContent.DEATH;
    }
}
