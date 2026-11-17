package com.exiledradio.netherupdatebackport.mobs;

import com.exiledradio.netherupdatebackport.common.AngerGameRules;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 1.16.5's {@code ResetAngerGoal}: with {@code universalAnger} on, a mob a player hits forgets whoever it was angry
 * at and is angry at every player for 20-39 s ({@code forgetCurrentTargetAndRefreshUniversalAnger}); with the alarm,
 * so is every mob of its kind within follow range (10 up and down). The nearest player it can see is then taken
 * by its angry-at-player task, which makes that player its one target again.
 *
 * <p>A target task without mutex bits, as 1.16's has no flags. Does nothing while the game rule is off.
 */
final class ResetAngerGoal extends EntityAIBase {

    private final EntityCreature mob;
    private final boolean alertOthers;
    private int lastHurtByPlayer;

    ResetAngerGoal(EntityCreature mob, boolean alertOthers) {
        this.mob = mob;
        this.alertOthers = alertOthers;
    }

    @Override
    public boolean shouldExecute() {
        return (this.mob instanceof EntityPigZombie || NeutralMobs.active()) && AngerGameRules.universalAnger(this.mob.world)
                && wasHurtByPlayer();
    }

    private boolean wasHurtByPlayer() {
        EntityLivingBase by = this.mob.getRevengeTarget();
        return by instanceof EntityPlayer && this.mob.getRevengeTimer() > this.lastHurtByPlayer;
    }

    @Override
    public void startExecuting() {
        this.lastHurtByPlayer = this.mob.getRevengeTimer();
        refresh(this.mob);
        if (this.alertOthers) {
            double range = this.mob.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            AxisAlignedBB box = new AxisAlignedBB(this.mob.posX, this.mob.posY, this.mob.posZ, this.mob.posX + 1.0D,
                    this.mob.posY + 1.0D, this.mob.posZ + 1.0D).grow(range, 10.0D, range);
            for (EntityCreature other : this.mob.world.getEntitiesWithinAABB(this.mob.getClass(), box)) {
                if (other != this.mob) {
                    refresh(other);
                }
            }
        }
    }

    /** {@code IAngerable.forgetCurrentTargetAndRefreshUniversalAnger}: stop being angry, then start the timer. */
    static void refresh(EntityCreature mob) {
        Anger anger = Anger.of(mob);
        if (anger == null) {
            return;
        }
        if (mob instanceof EntityPigZombie) {
            ZombifiedPiglins.stopBeingAngry((EntityPigZombie) mob, anger);
            ZombifiedPiglins.startAngerTimer((EntityPigZombie) mob, anger);
        } else {
            NeutralMobs.stopBeingAngry(mob, anger);
            anger.remaining = 400 + mob.getRNG().nextInt(380 + 1);
        }
    }
}
