package com.exiledradio.netherupdatebackport.mobs;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * 1.16.5's {@code WitherSkeletonEntity.registerGoals}: {@code NearestAttackableTargetGoal<>(this,
 * AbstractPiglinEntity.class, true)} at priority 3, after the player (2) and with the iron golem. Piglins and brutes
 * already treat wither skeletons as their nemesis.
 *
 * <p>Stream P's piglin class is found through the entity registry (the base class of its piglin), so this stream
 * never links against it; with piglins off there is nothing to target.
 */
public final class WitherSkeletons {

    @Nullable
    private static Class<? extends EntityLivingBase> piglins;
    private static boolean looked;

    @SuppressWarnings("unchecked")
    @Nullable
    static Class<? extends EntityLivingBase> piglinClass() {
        if (!looked) {
            looked = true;
            EntityEntry entry = ForgeRegistries.ENTITIES.getValue(Content.id("piglin"));
            Class<?> c = entry == null ? null : entry.getEntityClass().getSuperclass();
            piglins = c != null && EntityLivingBase.class.isAssignableFrom(c) ? (Class<? extends EntityLivingBase>) c : null;
        }
        return piglins;
    }

    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityWitherSkeleton)) {
            return;
        }
        Class<? extends EntityLivingBase> target = piglinClass();
        if (target == null) {
            return;
        }
        EntityWitherSkeleton skeleton = (EntityWitherSkeleton) event.getEntity();
        for (EntityAITasks.EntityAITaskEntry entry : skeleton.targetTasks.taskEntries) {
            if (entry.action instanceof TargetPiglins) {
                return;
            }
        }
        skeleton.targetTasks.addTask(3, new TargetPiglins(skeleton, target));
    }

    /** The goal; idle while the setting is off. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final class TargetPiglins extends EntityAINearestAttackableTarget {
        TargetPiglins(EntityWitherSkeleton skeleton, Class<? extends EntityLivingBase> target) {
            super(skeleton, target, true);
        }

        @Override
        public boolean shouldExecute() {
            return ModConfig.mobs.enabled && ModConfig.mobs.witherSkeletonsTargetPiglins && super.shouldExecute();
        }
    }
}
