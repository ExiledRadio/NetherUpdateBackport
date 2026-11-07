package com.exiledradio.netherupdatebackport.mobs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * 1.16.5's {@code ZombifiedPiglinEntity} anger rules, for vanilla's {@code EntityPigZombie}, through events.
 *
 * <ul>
 *   <li><b>Joining a world</b>: 1.12.2's two target tasks (angry at the nearest player) give way to 1.16's:
 *       {@link HurtByTargetGoal} with the alarm, and the nearest player it is angry at (1 in 10 ticks, needs
 *       sight). Lava costs 8 to path through. Never from inside a goal: the list being changed is this new
 *       entity's, which nothing is iterating.</li>
 *   <li><b>Each tick</b> ({@code customServerAiStep}): the first angry grunt; taking a new target starts 20-39
 *       s of anger at it; the timer runs only while the target is not a player, so a pigman forgets 20-39 s
 *       after losing sight of you; anger at a mob ends when that mob is gone; while it has a target it raises
 *       the alarm every 4-6 s when it can see the target. While angry, its death counts as a player kill.</li>
 *   <li><b>Taking a target</b> ({@code setTarget}): the first grunt comes 0-1 s later.</li>
 *   <li><b>A player dying</b> calms every pigman angry at them within 32 (10 up and down):
 *       {@code forgiveDeadPlayers}, on by default in 1.16.</li>
 *   <li><b>Spawning</b>: never on a nether wart block.</li>
 * </ul>
 * Speed while angry, the sounds and the sleeping check are vanilla's own code reading {@link Anger} through
 * patch P1.
 */
public final class ZombifiedPiglins {

    private static final Field ANGER_LEVEL = ReflectionHelper.findField(EntityPigZombie.class, "angerLevel", "field_70837_d");
    private static final Field ANGER_TARGET = ReflectionHelper.findField(EntityPigZombie.class, "angerTargetUUID", "field_175459_bn");
    private static final Field ATTACKING_PLAYER = ReflectionHelper.findField(EntityLivingBase.class, "attackingPlayer", "field_70717_bb");
    private static final Field RECENTLY_HIT = ReflectionHelper.findField(EntityLivingBase.class, "recentlyHit", "field_70718_bc");

    @SubscribeEvent
    public void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPigZombie) {
            event.addCapability(Anger.KEY, new Anger.Provider());
        }
    }

    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityPigZombie)) {
            return;
        }
        EntityPigZombie pigman = (EntityPigZombie) event.getEntity();
        Anger anger = Anger.of(pigman);
        if (anger == null || anger.installed) {
            return;
        }
        anger.installed = true;
        install(pigman, anger);
        migrate(pigman, anger);
    }

    /** Swaps vanilla's target tasks for 1.16's. Tasks other mods added stay. */
    static void install(final EntityPigZombie pigman, final Anger anger) {
        List<EntityAIBase> vanilla = new ArrayList<EntityAIBase>();
        for (EntityAITasks.EntityAITaskEntry entry : pigman.targetTasks.taskEntries) {
            if (entry.action.getClass().getEnclosingClass() == EntityPigZombie.class) {
                vanilla.add(entry.action);
            }
        }
        for (EntityAIBase task : vanilla) {
            pigman.targetTasks.removeTask(task);
        }
        pigman.targetTasks.addTask(1, new HurtByTargetGoal(pigman));
        pigman.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(pigman, EntityPlayer.class, 10, true, false,
                new com.google.common.base.Predicate<EntityPlayer>() {
                    @Override
                    public boolean apply(EntityPlayer player) {
                        return anger.isAngryAt(player);
                    }
                }));
        pigman.setPathPriority(PathNodeType.LAVA, 8.0F);
    }

    /** A pigman saved angry by 1.12.2 (or before this mod) stays angry at the same player, on 1.16's timer. */
    private static void migrate(EntityPigZombie pigman, Anger anger) {
        try {
            int level = ANGER_LEVEL.getInt(pigman);
            UUID old = (UUID) ANGER_TARGET.get(pigman);
            if (level > 0 && old != null && anger.target == null && anger.remaining == 0) {
                anger.target = old;
                anger.remaining = level;
            }
            if (level != 0) {
                ANGER_LEVEL.setInt(pigman, 0);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SubscribeEvent
    public void update(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPigZombie) || event.getEntityLiving().world.isRemote) {
            return;
        }
        EntityPigZombie pigman = (EntityPigZombie) event.getEntityLiving();
        Anger anger = Anger.of(pigman);
        if (anger == null) {
            return;
        }
        try {
            // Vanilla's counter is never set (patch P1) but its updateAITasks counts it down while we say angry.
            if (ANGER_LEVEL.getInt(pigman) != 0) {
                ANGER_LEVEL.setInt(pigman, 0);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        // 1.16 runs this in serverAiStep, which a pigman without AI or dying never reaches.
        if (pigman.isAIDisabled() || pigman.getHealth() <= 0.0F) {
            return;
        }
        tick(pigman, anger);
    }

    /** 1.16.5's {@code ZombifiedPiglinEntity.customServerAiStep}, the parts outside vanilla's own speed boost. */
    static void tick(EntityPigZombie pigman, Anger anger) {
        if (anger.isAngry() && anger.firstSoundIn > 0 && --anger.firstSoundIn == 0) {
            playAngerSound(pigman);
        }
        updatePersistentAnger(pigman, anger);
        if (pigman.getAttackTarget() != null) {
            maybeAlertOthers(pigman, anger);
        }
        if (anger.isAngry()) {
            setRecentlyHit(pigman, null, false);
        }
    }

    /** 1.16.5's {@code IAngerable.updatePersistentAnger(world, true)}. */
    static void updatePersistentAnger(EntityPigZombie pigman, Anger anger) {
        EntityLivingBase target = pigman.getAttackTarget();
        UUID id = anger.target;
        if ((target == null || target.getHealth() <= 0.0F) && id != null
                && ((WorldServer) pigman.world).getEntityFromUuid(id) instanceof EntityLiving) {
            stopBeingAngry(pigman, anger);
            return;
        }
        if (target != null && !Objects.equals(id, target.getUniqueID())) {
            anger.target = target.getUniqueID();
            startAngerTimer(pigman, anger);
        }
        if (anger.remaining > 0 && !(target instanceof EntityPlayer)) {
            if (--anger.remaining == 0) {
                stopBeingAngry(pigman, anger);
            }
        }
    }

    /** {@code PERSISTENT_ANGER_TIME}: 20 to 39 seconds. */
    static void startAngerTimer(EntityPigZombie pigman, Anger anger) {
        anger.remaining = range(pigman.getRNG(), 400, 780);
    }

    static void stopBeingAngry(EntityPigZombie pigman, Anger anger) {
        pigman.setRevengeTarget(null);
        anger.target = null;
        pigman.setAttackTarget(null);
        anger.remaining = 0;
    }

    /** Every 4-6 s ({@code ALERT_INTERVAL}), if it can see its target, the alarm. */
    private static void maybeAlertOthers(EntityPigZombie pigman, Anger anger) {
        if (anger.alertIn > 0) {
            --anger.alertIn;
            return;
        }
        if (pigman.getEntitySenses().canSee(pigman.getAttackTarget())) {
            alertOthers(pigman);
        }
        anger.alertIn = range(pigman.getRNG(), 80, 120);
    }

    /** Every pigman within follow range (10 up and down) with no target of its own takes this one's. */
    static void alertOthers(EntityPigZombie pigman) {
        EntityLivingBase target = pigman.getAttackTarget();
        double range = pigman.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        AxisAlignedBB box = new AxisAlignedBB(pigman.posX, pigman.posY, pigman.posZ, pigman.posX + 1.0D, pigman.posY + 1.0D,
                pigman.posZ + 1.0D).grow(range, 10.0D, range);
        for (EntityPigZombie other : pigman.world.getEntitiesWithinAABB(EntityPigZombie.class, box)) {
            if (other != pigman && other.getAttackTarget() == null && !other.isOnSameTeam(target)) {
                other.setAttackTarget(target);
            }
        }
    }

    private static void playAngerSound(EntityPigZombie pigman) {
        Random rand = pigman.getRNG();
        float pitch = (rand.nextFloat() - rand.nextFloat()) * 0.2F + (pigman.isChild() ? 1.5F : 1.0F);
        pigman.playSound(MobsContent.ANGRY, 2.0F, pitch * 1.8F);
    }

    /** 1.16.5's {@code ZombifiedPiglinEntity.setTarget}. */
    @SubscribeEvent
    public void setTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPigZombie) || event.getEntityLiving().world.isRemote) {
            return;
        }
        EntityPigZombie pigman = (EntityPigZombie) event.getEntityLiving();
        Anger anger = Anger.of(pigman);
        if (anger == null) {
            return;
        }
        EntityLivingBase target = event.getTarget();
        if (anger.knownTarget == null && target != null) {
            anger.firstSoundIn = range(pigman.getRNG(), 0, 20);
            anger.alertIn = range(pigman.getRNG(), 80, 120);
        }
        if (target instanceof EntityPlayer) {
            setRecentlyHit(pigman, (EntityPlayer) target, true);
        }
        anger.knownTarget = target;
    }

    /** {@code setLastHurtByPlayer} (with a player) or 1.16's {@code lastHurtByPlayerTime = tickCount} (without). */
    private static void setRecentlyHit(EntityPigZombie pigman, EntityPlayer player, boolean withPlayer) {
        try {
            if (withPlayer) {
                ATTACKING_PLAYER.set(pigman, player);
            }
            RECENTLY_HIT.setInt(pigman, pigman.ticksExisted);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 1.16.5's {@code ServerPlayerEntity.tellNeutralMobsThatIDied}, with {@code forgiveDeadPlayers} on. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerDied(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        forgive(player);
    }

    static void forgive(EntityPlayer player) {
        AxisAlignedBB box = new AxisAlignedBB(new BlockPos(player)).grow(32.0D, 10.0D, 32.0D);
        for (EntityPigZombie pigman : player.world.getEntitiesWithinAABB(EntityPigZombie.class, box)) {
            Anger anger = Anger.of(pigman);
            if (anger != null && player.getUniqueID().equals(anger.target)) {
                stopBeingAngry(pigman, anger);
            }
        }
    }

    /** 1.16's {@code checkZombifiedPiglinSpawnRules}: not on a nether wart block. */
    @SubscribeEvent
    public void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getEntityLiving() instanceof EntityPigZombie
                && event.getWorld().getBlockState(new BlockPos(event.getX(), event.getY(), event.getZ()).down()).getBlock()
                == Blocks.NETHER_WART_BLOCK) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** 1.16's {@code RangedInteger.randomValue}: min to max inclusive. */
    private static int range(Random rand, int min, int max) {
        return min + rand.nextInt(max - min + 1);
    }
}
