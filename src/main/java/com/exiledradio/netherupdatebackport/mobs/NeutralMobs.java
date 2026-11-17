package com.exiledradio.netherupdatebackport.mobs;

import com.exiledradio.netherupdatebackport.ModConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

/**
 * 1.16's neutral mob anger ({@code IAngerable}, 20w12a and 20w13a) for the four 1.12.2 mobs that have it:
 * wolves, endermen, polar bears and iron golems. Zombified piglins have their own ({@link ZombifiedPiglins});
 * Buzzy Bees Backport's bee keeps its own anger and is only told to forgive.
 *
 * <ul>
 *   <li>Each keeps 1.16's {@code AngerTime} and {@code AngryAt} ({@link Anger}, saved with it).</li>
 *   <li>Each tick ({@code updatePersistentAnger(world, true)}): a new target starts 20-39 s of anger at it; the
 *       timer runs only while the target is not a player; anger at a mob ends once that mob is gone.</li>
 *   <li>Wolves, polar bears and iron golems take back a player they are angry at (1 in 10 ticks, needs sight),
 *       as 1.16's {@code NearestAttackableTargetGoal(..., this::isAngryAt)}. An untamed wolf looks angry exactly
 *       while it is.</li>
 *   <li>A player dying calms every one of them angry at that player within 32 blocks (10 up and down):
 *       {@code forgiveDeadPlayers}, on in 1.16 by default. Bees too: 1.16's bee is an {@code IAngerable}.</li>
 * </ul>
 * 1.12.2 has neither {@code universalAnger} nor {@code forgiveDeadPlayers} as game rules; both act as their 1.16
 * defaults (off and on).
 */
public final class NeutralMobs {

    private static final String BB_BEE = "com.exiledradio.buzzybeesbackport.bee.EntityBee";

    public static boolean active() {
        return ModConfig.mobs.enabled && ModConfig.mobs.neutralAnger;
    }

    static boolean neutral(Entity entity) {
        return entity instanceof EntityWolf || entity instanceof EntityEnderman || entity instanceof EntityPolarBear
                || entity instanceof EntityIronGolem;
    }

    @SubscribeEvent
    public void attach(AttachCapabilitiesEvent<Entity> event) {
        if (neutral(event.getObject())) {
            event.addCapability(Anger.KEY, new Anger.Provider());
        }
    }

    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld().isRemote || !neutral(entity)) {
            return;
        }
        Anger anger = Anger.of(entity);
        if (anger == null || anger.installed) {
            return;
        }
        anger.installed = true;
        EntityCreature mob = (EntityCreature) entity;
        int priority = entity instanceof EntityWolf ? 4 : entity instanceof EntityEnderman ? -1 : 3;
        if (priority >= 0) {
            mob.targetTasks.addTask(priority, new AngryAtPlayer(mob, anger));
        }
    }

    /** 1.16's {@code NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt)}. */
    static final class AngryAtPlayer extends EntityAINearestAttackableTarget<EntityPlayer> {
        AngryAtPlayer(EntityCreature mob, final Anger anger) {
            super(mob, EntityPlayer.class, 10, true, false, new com.google.common.base.Predicate<EntityPlayer>() {
                @Override
                public boolean apply(EntityPlayer player) {
                    return anger.isAngryAt(player);
                }
            });
        }

        @Override
        public boolean shouldExecute() {
            return active() && super.shouldExecute();
        }
    }

    @SubscribeEvent
    public void update(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || !neutral(entity) || !active()) {
            return;
        }
        EntityLiving mob = (EntityLiving) entity;
        Anger anger = Anger.of(mob);
        // 1.16 runs this in aiStep's server half, which a mob without AI or dying never reaches.
        if (anger == null || mob.isAIDisabled() || mob.getHealth() <= 0.0F) {
            return;
        }
        updatePersistentAnger(mob, anger);
        if (mob instanceof EntityWolf) {
            EntityWolf wolf = (EntityWolf) mob;
            if (!wolf.isTamed() && wolf.isAngry() != anger.isAngry()) {
                wolf.setAngry(anger.isAngry());
            }
        }
    }

    /** 1.16.5's {@code IAngerable.updatePersistentAnger(world, true)}. */
    public static void updatePersistentAnger(EntityLiving mob, Anger anger) {
        EntityLivingBase target = mob.getAttackTarget();
        UUID id = anger.target;
        if ((target == null || target.getHealth() <= 0.0F) && id != null
                && ((WorldServer) mob.world).getEntityFromUuid(id) instanceof EntityLiving) {
            stopBeingAngry(mob, anger);
            return;
        }
        if (target != null && !Objects.equals(id, target.getUniqueID())) {
            anger.target = target.getUniqueID();
            anger.remaining = 400 + mob.getRNG().nextInt(380 + 1);
        }
        if (anger.remaining > 0 && !(target instanceof EntityPlayer)) {
            if (--anger.remaining == 0) {
                stopBeingAngry(mob, anger);
            }
        }
    }

    /** 1.16.5's {@code IAngerable.stopBeingAngry}. */
    static void stopBeingAngry(EntityLiving mob, Anger anger) {
        mob.setRevengeTarget(null);
        anger.target = null;
        mob.setAttackTarget(null);
        anger.remaining = 0;
    }

    /** 1.16.5's {@code ServerPlayerEntity.tellNeutralMobsThatIDied}, with {@code forgiveDeadPlayers} on. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerDied(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntityLiving() instanceof EntityPlayerMP) || !active()) {
            return;
        }
        forgive((EntityPlayer) event.getEntityLiving());
    }

    public static void forgive(EntityPlayer player) {
        AxisAlignedBB box = new AxisAlignedBB(new BlockPos(player)).grow(32.0D, 10.0D, 32.0D);
        for (EntityLiving mob : player.world.getEntitiesWithinAABB(EntityLiving.class, box)) {
            if (neutral(mob)) {
                Anger anger = Anger.of(mob);
                if (anger != null && player.getUniqueID().equals(anger.target)) {
                    stopBeingAngry(mob, anger);
                }
            } else if (BB_BEE.equals(mob.getClass().getName()) || isBee(mob.getClass())) {
                forgiveBee(mob, player);
            }
        }
    }

    private static boolean isBee(Class<?> c) {
        for (Class<?> k = c.getSuperclass(); k != null && k != EntityLiving.class; k = k.getSuperclass()) {
            if (BB_BEE.equals(k.getName())) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> beeClass;
    private static Method beeTarget;
    private static Method beeStop;

    /** Buzzy Bees Backport's {@code EntityBee}: its public {@code getAngerTarget} and {@code stopBeingAngry}. */
    private static void forgiveBee(EntityLiving bee, EntityPlayer player) {
        try {
            if (beeClass == null) {
                Class<?> c = bee.getClass();
                while (!BB_BEE.equals(c.getName())) {
                    c = c.getSuperclass();
                }
                beeTarget = c.getMethod("getAngerTarget");
                beeStop = c.getMethod("stopBeingAngry");
                beeClass = c;
            }
            if (player.getUniqueID().equals(beeTarget.invoke(bee))) {
                beeStop.invoke(bee);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Buzzy Bees Backport's bee has no getAngerTarget/stopBeingAngry", e);
        }
    }
}
