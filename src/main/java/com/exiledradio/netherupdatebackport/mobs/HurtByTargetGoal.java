package com.exiledradio.netherupdatebackport.mobs;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * 1.16.5's {@code HurtByTargetGoal(mob).setAlertOthers()}: fight back at whatever hurt it, and set every mob of
 * its kind within follow range (10 up and down) that has no target on it too.
 *
 * <p>1.12.2's {@code EntityAIHurtByTarget} is the same but for one thing: it only fights back at an attacker it
 * can see. 1.16 fights back unseen ({@code HURT_BY_TARGETING.allowUnseeable()}); keeping the target afterwards
 * still needs sight now and then (300 ticks of memory), as in both.
 */
public final class HurtByTargetGoal extends EntityAITarget {

    private int timestamp;

    public HurtByTargetGoal(EntityCreature creature) {
        super(creature, true);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        int stamp = this.taskOwner.getRevengeTimer();
        EntityLivingBase attacker = this.taskOwner.getRevengeTarget();
        return stamp != this.timestamp && attacker != null
                && isSuitableTarget(this.taskOwner, attacker, false, false)
                && this.taskOwner.isWithinHomeDistanceFromPosition(new BlockPos(attacker));
    }

    @Override
    public void startExecuting() {
        this.taskOwner.setAttackTarget(this.taskOwner.getRevengeTarget());
        this.target = this.taskOwner.getAttackTarget();
        this.timestamp = this.taskOwner.getRevengeTimer();
        this.unseenMemoryTicks = 300;
        this.alertOthers();
        super.startExecuting();
    }

    private void alertOthers() {
        EntityCreature mob = this.taskOwner;
        EntityLivingBase attacker = mob.getRevengeTarget();
        double range = this.getTargetDistance();
        AxisAlignedBB box = new AxisAlignedBB(mob.posX, mob.posY, mob.posZ, mob.posX + 1.0D, mob.posY + 1.0D, mob.posZ + 1.0D)
                .grow(range, 10.0D, range);
        for (EntityCreature other : mob.world.getEntitiesWithinAABB(mob.getClass(), box)) {
            if (other != mob && other.getAttackTarget() == null && !other.isOnSameTeam(attacker)) {
                other.setAttackTarget(attacker);
            }
        }
    }
}
