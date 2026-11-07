package com.exiledradio.netherupdatebackport.piglin;

import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Spawning bastion remnants' piglins and brutes, for stream X.
 *
 * <p>1.16's bastions place their mobs from four one-entity templates ({@code bastion/mobs/*}), each a jigsaw that
 * resolves to one of these; X calls {@link #spawn} with the template's name where the jigsaw would place it. Every
 * one matches the template's entity: persistent, never hunting (bastion piglins leave the stables' hoglins be), and
 * placed as 1.16's {@code SpawnReason.STRUCTURE} spawn, which skips the baby and random-weapon rolls but keeps the
 * armor and enchantment rolls (a brute's golden axe and home always come from its own spawn code).
 *
 * <pre>
 *   crossbow_piglin       a piglin with a crossbow
 *   sword_piglin          a piglin with a golden sword
 *   melee_piglin          a piglin brute (1.16.2 replaced this template's piglin with a brute)
 *   melee_piglin_always   a piglin brute
 * </pre>
 *
 * <p>Stream X marks a stable's hoglins with {@code EntityHoglin.setCannotBeHunted(true)}.
 */
public final class BastionMobs {

    private BastionMobs() {
    }

    /**
     * Spawns one bastion mob at a block, facing {@code yaw}.
     *
     * @param template one of the four names above
     * @return the entity spawned, or null when piglins are switched off or the name is unknown
     */
    @Nullable
    public static EntityLiving spawn(World world, BlockPos at, float yaw, String template) {
        if (!PiglinContent.enabled() || world.isRemote) {
            return null;
        }
        EntityLiving mob;
        switch (template) {
            case "crossbow_piglin":
            case "sword_piglin": {
                EntityPiglin piglin = new EntityPiglin(world);
                piglin.structureSpawn = true;
                piglin.setCannotHunt(true);
                Item crossbow = Crossbows.item();
                Item weapon = "crossbow_piglin".equals(template) && crossbow != null ? crossbow : Items.GOLDEN_SWORD;
                piglin.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(weapon));
                mob = piglin;
                break;
            }
            case "melee_piglin":
            case "melee_piglin_always":
                mob = new EntityPiglinBrute(world);
                break;
            default:
                return null;
        }
        mob.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
        mob.enablePersistence();
        mob.onInitialSpawn(world.getDifficultyForLocation(at), null);
        world.spawnEntity(mob);
        return mob;
    }
}
