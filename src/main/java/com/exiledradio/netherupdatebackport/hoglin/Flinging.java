package com.exiledradio.netherupdatebackport.hoglin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;

/**
 * Hoglins' and zoglins' attack: 1.16.5's {@code IFlinging}.
 *
 * <p>An adult hits for half its attack damage plus a random part of it (6 gives 3-8), and throws the target:
 * sideways by {@code (knockback - target's knockback resistance) * (0.2..0.7)}, turned off the line between them
 * by a whole number from -10 to 10 that the game reads as radians (so in practice any direction, as in current
 * Java), and up by up to half of it. 1.12.2 has no attack-knockback attribute; hoglins' and
 * zoglins' is 1.
 */
public final class Flinging {

    static final double KNOCKBACK = 1.0;

    private Flinging() {
    }

    /** @return true if the target was hurt */
    public static boolean hurtAndThrow(EntityLivingBase attacker, EntityLivingBase target, boolean adult) {
        float damage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float dealt = adult && (int) damage > 0 ? damage / 2.0F + attacker.getRNG().nextInt((int) damage) : damage;
        boolean hurt = target.attackEntityFrom(DamageSource.causeMobDamage(attacker), dealt);
        if (hurt) {
            EnchantmentHelper.applyThornEnchantments(target, attacker);
            EnchantmentHelper.applyArthropodEnchantments(attacker, target);
            if (adult) {
                throwTarget(attacker, target);
            }
        }
        return hurt;
    }

    /** The throw alone (also when a shield blocks an adult's hit). */
    public static void throwTarget(EntityLivingBase attacker, EntityLivingBase target) {
        double strength = KNOCKBACK - target.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
        if (strength <= 0) {
            return;
        }
        double dx = target.posX - attacker.posX;
        double dz = target.posZ - attacker.posZ;
        float turn = attacker.getRNG().nextInt(21) - 10;
        double side = strength * (attacker.getRNG().nextFloat() * 0.5F + 0.2F);
        Vec3d push = new Vec3d(dx, 0, dz).normalize().scale(side).rotateYaw(turn);
        double up = strength * attacker.getRNG().nextFloat() * 0.5;
        target.addVelocity(push.x, up, push.z);
        target.velocityChanged = true;
        if (target instanceof EntityPlayerMP) {
            ((EntityPlayerMP) target).connection.sendPacket(new SPacketEntityVelocity(target));
        }
    }
}
