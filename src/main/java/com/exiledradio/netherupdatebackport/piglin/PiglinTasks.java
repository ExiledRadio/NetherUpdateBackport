package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Triggers;
import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.piglin.Memories.Mem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 1.16.5's {@code PiglinTasks}: the piglin's brain, behavior for behavior, run once a server tick by
 * {@link EntityPiglin}.
 *
 * <h2>Activities</h2>
 * Every tick the core behaviors run (avoid zombified piglins and, as a baby, wither skeletons; barter when done
 * admiring; start admiring gold it sees; celebrate a kill; forget the dead), then the first activity whose memory is
 * set wins: admiring an item, fighting, avoiding, celebrating, riding, else idle. Leaving an activity forgets its
 * memory, as 1.16's brain does ({@code addActivityAndRemoveMemoryWhenStopped}). Then the activity's own behaviors
 * run; they set walk and look targets, which {@link AbstractPiglin} carries out.
 *
 * <h2>Item lists</h2>
 * 1.16's item and block tags as current Java fills them, from what 1.12.2 and the installed mods have:
 * <ul>
 *   <li><b>loved</b> ({@code piglin_loved}): gold ores (vanilla's, nether gold ore, and the {@code oreGold} ore
 *       name), gold block, gilded blackstone, the light weighted pressure plate, gold ingot, the bell (Village &amp;
 *       Pillage's), clock, golden carrot, glistering melon, golden apples, golden armor and horse armor, golden tools,
 *       raw gold and its block from any mod;</li>
 *   <li><b>repellent</b> items: soul torch, soul lantern, soul campfire; blocks: soul fire, soul torch, soul lantern,
 *       a lit soul campfire;</li>
 *   <li><b>guarded</b> ({@code guarded_by_piglins}): gold block, chests, trapped chests, ender chests, barrels,
 *       shulker boxes, gilded blackstone, raw gold blocks and gold ores;</li>
 *   <li><b>food</b>: raw and cooked porkchops.</li>
 * </ul>
 */
public final class PiglinTasks {

    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;

    public enum Activity {
        IDLE, ADMIRE_ITEM, FIGHT, AVOID, CELEBRATE, RIDE
    }

    private PiglinTasks() {
    }

    /** 1.16's {@code TickRangeConverter.rangeOfSeconds(min, max).randomValue}. */
    static int seconds(Random random, int min, int max) {
        return min * 20 + random.nextInt((max - min) * 20 + 1);
    }

    static int timeBetweenHunts(Random r) {
        return seconds(r, 30, 120);
    }

    // =============================================================================================
    // The tick
    // =============================================================================================

    static void initMemories(EntityPiglin p) {
        p.memories.setFor(Mem.HUNTED_RECENTLY, true, timeBetweenHunts(p.getRNG()));
    }

    static void tick(EntityPiglin p) {
        core(p);
        updateActivity(p);
        switch (p.activity) {
            case ADMIRE_ITEM:
                admire(p);
                break;
            case FIGHT:
                fight(p);
                break;
            case AVOID:
                avoid(p);
                break;
            case CELEBRATE:
                celebrate(p);
                break;
            case RIDE:
                ride(p);
                break;
            default:
                idle(p);
                break;
        }
        afterActivity(p);
    }

    /** 1.16's CORE activity (the look, walk and door tasks are {@link AbstractPiglin}'s). */
    private static void core(EntityPiglin p) {
        Memories m = p.memories;
        // babyAvoidNemesis, avoidZombified: copy the nemesis or zombified into AVOID_TARGET for 5-7 s.
        if (p.isChild() && m.has(Mem.NEAREST_VISIBLE_NEMESIS) && !m.has(Mem.AVOID_TARGET)) {
            m.setFor(Mem.AVOID_TARGET, m.get(Mem.NEAREST_VISIBLE_NEMESIS), seconds(p.getRNG(), 5, 7));
        }
        if (isNearZombified(p) && !m.has(Mem.AVOID_TARGET)) {
            m.setFor(Mem.AVOID_TARGET, m.get(Mem.NEAREST_VISIBLE_ZOMBIFIED), seconds(p.getRNG(), 5, 7));
        }
        // StartAdmiringItemTask: the admiring is over, so let go of what is in the off hand (bartering).
        if (!m.has(Mem.ADMIRING_ITEM) && !p.getHeldItemOffhand().isEmpty()
                && !p.getHeldItemOffhand().getItem().isShield(p.getHeldItemOffhand(), p)) {
            stopHoldingOffHandItem(p, true);
        }
        // AdmireItemTask(120): a loved item in sight starts six seconds of admiring.
        EntityItem wanted = m.get(Mem.NEAREST_VISIBLE_WANTED_ITEM);
        if (wanted != null && !m.has(Mem.ADMIRING_ITEM) && !m.has(Mem.ADMIRING_DISABLED)
                && !m.has(Mem.DISABLE_WALK_TO_ADMIRE_ITEM) && isLovedItem(wanted.getItem())) {
            m.setFor(Mem.ADMIRING_ITEM, true, 120);
        }
        // EndAttackTask(300, wantsToDance): the target died; celebrate where it fell.
        EntityLivingBase target = p.getAttackTarget();
        if (target != null && !m.has(Mem.CELEBRATE_LOCATION) && isDeadOrDying(target)) {
            if (wantsToDance(p, target)) {
                m.setFor(Mem.DANCING, true, 300);
            }
            m.setFor(Mem.CELEBRATE_LOCATION, new BlockPos(target), 300);
            // 1.16's forgiveDeadPlayers game rule, which defaults to true and 1.12.2 lacks.
            p.setAttackTarget(null);
            m.erase(Mem.ANGRY_AT);
        }
        // GetAngryTask: forget anger at the dead.
        EntityLivingBase angry = p.entityByUuid(m.<java.util.UUID>get(Mem.ANGRY_AT));
        if (m.has(Mem.ANGRY_AT) && angry != null && isDeadOrDying(angry)) {
            m.erase(Mem.ANGRY_AT);
        }
    }

    /** Picks the activity: 1.16's {@code updateActivity}, first half. */
    private static void updateActivity(EntityPiglin p) {
        Memories m = p.memories;
        Activity now = m.has(Mem.ADMIRING_ITEM) ? Activity.ADMIRE_ITEM
                : p.getAttackTarget() != null ? Activity.FIGHT
                : m.has(Mem.AVOID_TARGET) ? Activity.AVOID
                : m.has(Mem.CELEBRATE_LOCATION) ? Activity.CELEBRATE
                : m.has(Mem.RIDE_TARGET) ? Activity.RIDE
                : Activity.IDLE;
        Activity was = p.activity;
        if (now != was) {
            // Leaving an activity forgets its memory.
            switch (was) {
                case ADMIRE_ITEM:
                    m.erase(Mem.ADMIRING_ITEM);
                    break;
                case FIGHT:
                    p.setAttackTarget(null);
                    p.stopShooting();
                    break;
                case AVOID:
                    m.erase(Mem.AVOID_TARGET);
                    break;
                case CELEBRATE:
                    m.erase(Mem.CELEBRATE_LOCATION);
                    break;
                case RIDE:
                    m.erase(Mem.RIDE_TARGET);
                    break;
                default:
                    break;
            }
            p.activity = now;
            SoundEvent sound = soundForActivity(p, now);
            if (sound != null) {
                p.playSound(sound, p.soundVolume(), p.soundPitch());
            }
        }
    }

    /** 1.16's {@code updateActivity}, second half. */
    private static void afterActivity(EntityPiglin p) {
        p.setAggressive(p.getAttackTarget() != null);
        if (!p.memories.has(Mem.RIDE_TARGET) && isBabyRidingBaby(p)) {
            p.dismountRidingEntity();
        }
        if (!p.memories.has(Mem.CELEBRATE_LOCATION)) {
            p.memories.erase(Mem.DANCING);
        }
        p.setDancing(p.memories.has(Mem.DANCING));
    }

    // =============================================================================================
    // Activities
    // =============================================================================================

    private static void idle(EntityPiglin p) {
        lookAt(p, PiglinTasks::isPlayerHoldingLovedItem, 14.0);
        forgetAttackTarget(p);
        if (p.canHunt()) {
            startHunt(p);
        }
        avoidRepellent(p);
        babySometimesRideBabyHoglin(p);
        p.idleLook.run(p);
        p.idleMove.run(p);
        findInteraction(p, 4.0);
    }

    private static void fight(EntityPiglin p) {
        EntityLivingBase target = p.getAttackTarget();
        // FindNewAttackTargetTask: stop when stuck 10 s, the target is gone or not allowed, or not the nearest valid one.
        Long since = p.memories.get(Mem.CANT_REACH_WALK_TARGET_SINCE);
        if (since != null && p.world.getTotalWorldTime() - since > 200L || isDeadOrDying(target) || target.world != p.world
                || !AbstractPiglin.isAttackAllowed(target) || findNearestValidAttackTarget(p) != target) {
            p.setAttackTarget(null);
            return;
        }
        boolean sees = p.visibleLiving().contains(target);
        // AttackStrafingTask(5, 0.75) with a crossbow: back off from a target within 5 blocks.
        if (hasCrossbow(p) && !p.hasWalkTarget() && sees && p.getDistance(target) < 5.0) {
            p.setLookTarget(target);
            p.getMoveHelper().strafe(-0.75F, 0.0F);
            p.rotationYaw = p.rotationYawHead;
        }
        // MoveToTargetTask(1.0): stops within BrainUtil.isWithinAttackRange(mob, target, 1) - a crossbow's 8 less 1.
        if (isWithinAttackRange(p, target, 1) && sees) {
            if (p.hasWalkTarget()) {
                p.eraseWalkTarget();
            }
        } else {
            p.setWalkAndLook(target, 1.0F, 0);
        }
        // AttackTargetTask(20).
        if (!p.memories.has(Mem.ATTACK_COOLING_DOWN) && !hasCrossbow(p) && sees && p.isWithinMeleeAttackRange(target)) {
            p.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            p.swingArm(EnumHand.MAIN_HAND);
            p.attackEntityAsMob(target);
            p.memories.setFor(Mem.ATTACK_COOLING_DOWN, true, 20);
        }
        // ShootTargetTask.
        if (hasCrossbow(p) && sees && isWithinAttackRange(p, target, 0)) {
            p.setLookTarget(target);
            p.tickShooting(target);
        } else {
            p.stopShooting();
        }
        // FinishedHuntTask.
        if (isHoglin(target) && isDeadOrDying(target)) {
            dontKillAnyMoreHoglinsForAWhile(p);
        }
        // PredicateTask(isNearZombified): drop the fight near a zombified piglin.
        if (isNearZombified(p)) {
            p.setAttackTarget(null);
        }
    }

    private static void avoid(EntityPiglin p) {
        EntityLivingBase avoid = p.memories.get(Mem.AVOID_TARGET);
        if (avoid != null) {
            runAway(p, avoid.getPositionVector(), 1.0F, 12);
        }
        p.avoidLook.run(p);
        p.avoidMove.run(p);
        if (wantsToStopFleeing(p)) {
            p.memories.erase(Mem.AVOID_TARGET);
        }
    }

    private static void celebrate(EntityPiglin p) {
        avoidRepellent(p);
        lookAt(p, PiglinTasks::isPlayerHoldingLovedItem, 14.0);
        forgetAttackTarget(p);
        BlockPos at = p.memories.get(Mem.CELEBRATE_LOCATION);
        if (at != null && p.getAttackTarget() == null && !p.hasWalkTarget()) {
            // HuntCelebrationTask: (2, 1.0) walking, (4, 0.6) dancing.
            int close = p.isDancing() ? 4 : 2;
            float speed = p.isDancing() ? 0.6F : 1.0F;
            if (at.distanceSq(new BlockPos(p)) >= close * close) {
                Random r = p.getRNG();
                p.setWalkAndLook(at.add(r.nextInt(3) - 1, 0, r.nextInt(3) - 1), speed, close);
            }
        }
        p.celebrate.run(p);
    }

    private static void admire(EntityPiglin p) {
        Memories m = p.memories;
        EntityItem wanted = m.get(Mem.NEAREST_VISIBLE_WANTED_ITEM);
        // PickupWantedItemTask(notHoldingLovedItemInOffHand, 1.0, true, 9).
        if (wanted != null && isNotHoldingLovedItemInOffHand(p) && wanted.getDistance(p) < 9.0) {
            p.setWalkAndLook(wanted, 1.0F, 0);
        }
        // ForgetAdmiredItemTask(9).
        if (p.getHeldItemOffhand().isEmpty() && (wanted == null || wanted.getDistance(p) >= 9.0)) {
            m.erase(Mem.ADMIRING_ITEM);
        }
        // StopReachingItemTask(200, 200).
        if (m.has(Mem.ADMIRING_ITEM) && wanted != null && p.getHeldItemOffhand().isEmpty()) {
            Integer tried = m.get(Mem.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            if (tried == null) {
                m.set(Mem.TIME_TRYING_TO_REACH_ADMIRE_ITEM, 0);
            } else if (tried > 200) {
                m.erase(Mem.ADMIRING_ITEM);
                m.erase(Mem.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
                m.setFor(Mem.DISABLE_WALK_TO_ADMIRE_ITEM, true, 200);
            } else {
                m.set(Mem.TIME_TRYING_TO_REACH_ADMIRE_ITEM, tried + 1);
            }
        }
    }

    private static void ride(EntityPiglin p) {
        Entity target = p.memories.get(Mem.RIDE_TARGET);
        // RideEntityTask(0.8).
        if (target != null && !p.isRiding() && !p.hasWalkTarget()) {
            if (target.getDistance(p) < 1.0) {
                p.startRiding(target);
            } else {
                p.setWalkAndLook(target, 0.8F, 1);
            }
        }
        lookAt(p, PiglinTasks::isPlayerHoldingLovedItem, 8.0);
        if (p.isRiding()) {
            p.rideLook.run(p);
        }
        // StopRidingEntityTask(8, wantsToStopRiding).
        Entity vehicle = p.getRidingEntity() != null ? p.getRidingEntity() : target;
        if (vehicle != null && (!vehicle.isEntityAlive() || vehicle.getDistance(p) >= 8.0 || vehicle.world != p.world
                || wantsToStopRiding(p, vehicle))) {
            p.dismountRidingEntity();
            p.memories.erase(Mem.RIDE_TARGET);
        }
    }

    // =============================================================================================
    // Behaviors
    // =============================================================================================

    /** 1.16's {@code LookAtEntityTask}: when it looks at nothing, look at the nearest visible match within range. */
    static boolean lookAt(AbstractPiglin p, java.util.function.Predicate<EntityLivingBase> what, double range) {
        if (p.hasLookTarget()) {
            return false;
        }
        for (EntityLivingBase e : p.visibleLiving()) {
            if (what.test(e) && e.getDistanceSq(p) <= range * range) {
                p.setLookTarget(e);
                return true;
            }
        }
        return false;
    }

    /** 1.16's {@code FindInteractionAndLookTargetTask(PLAYER, range)}. */
    static void findInteraction(AbstractPiglin p, double range) {
        if (p.memories.has(Mem.INTERACTION_TARGET)) {
            return;
        }
        for (EntityLivingBase e : p.visibleLiving()) {
            if (e instanceof EntityPlayer && e.getDistanceSq(p) <= range * range) {
                p.memories.set(Mem.INTERACTION_TARGET, e);
                p.setLookTarget(e);
                return;
            }
        }
    }

    /** 1.16's {@code InteractWithEntityTask}: walk up to a visible one of these within range and look at it. */
    static boolean interactWith(AbstractPiglin p, Class<? extends EntityLivingBase> kind, double range, float speed, int close) {
        if (p.hasWalkTarget()) {
            return false;
        }
        for (EntityLivingBase e : p.visibleLiving()) {
            if (kind.isInstance(e) && e.getDistanceSq(p) <= range * range) {
                p.memories.set(Mem.INTERACTION_TARGET, e);
                p.setLookTarget(e);
                p.setWalkTarget(e, speed, close);
                return true;
            }
        }
        return false;
    }

    /** 1.16's {@code WalkRandomlyTask}. */
    static boolean walkRandomly(AbstractPiglin p, float speed, int horizontal, int vertical) {
        if (p.hasWalkTarget()) {
            return false;
        }
        Vec3d to = RandomPositionGenerator.getLandPos(p, horizontal, vertical);
        if (to != null) {
            p.setWalkTarget(new BlockPos(to), speed, 0);
        }
        return true;
    }

    /** 1.16's {@code WalkTowardsLookTargetTask}. */
    static boolean walkTowardsLookTarget(AbstractPiglin p, float speed, int close) {
        Object look = p.lookTarget();
        if (p.hasWalkTarget() || look == null) {
            return false;
        }
        p.setWalkTarget(look instanceof Entity ? look : new BlockPos((Vec3d) look), speed, close);
        return true;
    }

    /** 1.16's {@code RunAwayTask.pos(NEAREST_REPELLENT, 1.0, 8, false)}. */
    private static void avoidRepellent(EntityPiglin p) {
        BlockPos rep = p.memories.get(Mem.NEAREST_REPELLENT);
        if (rep != null && !p.hasWalkTarget()) {
            runAway(p, new Vec3d(rep.getX() + 0.5, rep.getY(), rep.getZ() + 0.5), 1.0F, 8);
        }
    }

    /** 1.16's {@code RunAwayTask}: walk to a spot up to 16 blocks off, away from the danger, if it is within range. */
    static void runAway(AbstractPiglin p, Vec3d danger, float speed, int range) {
        if (p.getPositionVector().distanceTo(danger) >= range || p.isWalkingAwayFrom(danger, speed)) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            Vec3d to = RandomPositionGenerator.findRandomTargetBlockAwayFrom(p, 16, 7, danger);
            if (to != null) {
                p.setWalkTarget(new BlockPos(to), speed, 0);
                return;
            }
        }
    }

    /** {@code ForgetAttackTargetTask(isAdult, findNearestValidAttackTarget)}: pick a fight with the nearest valid target. */
    private static void forgetAttackTarget(EntityPiglin p) {
        if (p.isAdult() && p.getAttackTarget() == null) {
            EntityLivingBase target = findNearestValidAttackTarget(p);
            if (target != null && target.isEntityAlive()) {
                p.setAttackTarget(target);
                p.memories.erase(Mem.CANT_REACH_WALK_TARGET_SINCE);
            }
        }
    }

    /** 1.16's {@code StartHuntTask}. */
    private static void startHunt(EntityPiglin p) {
        Memories m = p.memories;
        EntityLivingBase hoglin = m.get(Mem.NEAREST_VISIBLE_HUNTABLE_HOGLIN);
        if (hoglin == null || m.has(Mem.ANGRY_AT) || m.has(Mem.HUNTED_RECENTLY) || p.isChild() || hasAnyoneNearbyHuntedRecently(p)) {
            return;
        }
        setAngerTarget(p, hoglin);
        dontKillAnyMoreHoglinsForAWhile(p);
        broadcastAngerTarget(p, hoglin);
        for (AbstractPiglin q : visibleAdultPiglins(p)) {
            dontKillAnyMoreHoglinsForAWhile(q);
        }
    }

    /** {@code RunSometimesTask(PiglinIdleActivityTask(isBaby, NEAREST_VISIBLE_BABY_HOGLIN, RIDE_TARGET, 10-30 s), 10-40 s)}. */
    private static void babySometimesRideBabyHoglin(EntityPiglin p) {
        Memories m = p.memories;
        if (!p.isChild() || !m.has(Mem.NEAREST_VISIBLE_BABY_HOGLIN) || m.has(Mem.RIDE_TARGET)) {
            return;
        }
        if (p.rideCountdown < 0) {
            p.rideCountdown = seconds(p.getRNG(), 10, 40);
        }
        if (p.rideCountdown > 0) {
            p.rideCountdown--;
        }
        if (p.rideCountdown == 0) {
            m.setFor(Mem.RIDE_TARGET, m.get(Mem.NEAREST_VISIBLE_BABY_HOGLIN), seconds(p.getRNG(), 10, 30));
            p.rideCountdown = seconds(p.getRNG(), 10, 40);
        }
    }

    // =============================================================================================
    // Items
    // =============================================================================================

    /** 1.16's {@code pickUpItem}: nuggets whole, anything else one at a time; admire, eat, wear or keep it. */
    static void pickUpItem(EntityPiglin p, EntityItem entity) {
        stopWalking(p);
        ItemStack stack;
        if (entity.getItem().getItem() == Items.GOLD_NUGGET) {
            p.onItemPickup(entity, entity.getItem().getCount());
            stack = entity.getItem();
            entity.setDead();
        } else {
            p.onItemPickup(entity, 1);
            stack = removeOneItemFromItemEntity(entity);
        }
        triggerDistracted(p, entity, stack);
        if (isLovedItem(stack)) {
            p.memories.erase(Mem.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            holdInOffhand(p, stack);
            admireGoldItem(p);
        } else if (isFood(stack) && !p.memories.has(Mem.ATE_RECENTLY)) {
            p.memories.setFor(Mem.ATE_RECENTLY, true, 200);
        } else if (!p.equipItemIfPossible(stack)) {
            putInInventory(p, stack);
        }
    }

    /** <i>Oh Shiny</i>: a player in gold armor threw a loved item an adult piglin picked up. */
    private static void triggerDistracted(EntityPiglin p, EntityItem entity, ItemStack stack) {
        String thrower = entity.getThrower();
        if (thrower == null || !p.isAdult() || !isLovedItem(stack)) {
            return;
        }
        EntityPlayer player = p.world.getPlayerEntityByName(thrower);
        if (player != null && isWearingGold(player)) {
            Triggers.fire(player, "distract_piglin");
        }
    }

    private static void holdInOffhand(EntityPiglin p, ItemStack stack) {
        if (!p.getHeldItemOffhand().isEmpty()) {
            p.entityDropItem(p.getHeldItemOffhand(), 0.0F);
        }
        p.holdInOffHand(stack);
    }

    private static ItemStack removeOneItemFromItemEntity(EntityItem entity) {
        ItemStack stack = entity.getItem();
        ItemStack one = stack.splitStack(1);
        if (stack.isEmpty()) {
            entity.setDead();
        } else {
            entity.setItem(stack);
        }
        return one;
    }

    /** 1.16's {@code stopHoldingOffHandItem}: after admiring gold, barter it (an adult), or keep or wear the item. */
    static void stopHoldingOffHandItem(EntityPiglin p, boolean barter) {
        ItemStack stack = p.getHeldItemOffhand();
        p.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        if (p.isAdult()) {
            boolean currency = isBarterCurrency(stack);
            if (barter && currency) {
                throwItems(p, Bartering.roll(p));
            } else if (!currency && !p.equipItemIfPossible(stack)) {
                putInInventory(p, stack);
            }
        } else if (!p.equipItemIfPossible(stack)) {
            ItemStack main = p.getHeldItemMainhand();
            if (isLovedItem(main)) {
                putInInventory(p, main);
            } else {
                throwItems(p, Collections.singletonList(main));
            }
            p.holdInMainHand(stack);
        }
    }

    /** 1.16's {@code cancelAdmiring}, when it converts: drop what it was admiring. */
    static void cancelAdmiring(EntityPiglin p) {
        if (p.memories.has(Mem.ADMIRING_ITEM) && !p.getHeldItemOffhand().isEmpty()) {
            p.entityDropItem(p.getHeldItemOffhand(), 0.0F);
            p.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static void putInInventory(EntityPiglin p, ItemStack stack) {
        ItemStack left = p.addToInventory(stack);
        throwItemsTowardPos(p, Collections.singletonList(left), randomNearbyPos(p));
    }

    private static void throwItems(EntityPiglin p, List<ItemStack> stacks) {
        EntityPlayer player = p.memories.get(Mem.NEAREST_VISIBLE_PLAYER);
        throwItemsTowardPos(p, stacks, player != null ? player.getPositionVector() : randomNearbyPos(p));
    }

    private static void throwItemsTowardPos(EntityPiglin p, List<ItemStack> stacks, Vec3d at) {
        boolean any = false;
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) {
                any = true;
                throwItem(p, s, at.add(0.0, 1.0, 0.0));
            }
        }
        if (any) {
            p.swingArm(EnumHand.OFF_HAND);
        }
    }

    /** 1.16's {@code BrainUtil.throwItem}: from just below the eyes, at 0.3 blocks a tick toward the spot. */
    static void throwItem(EntityLivingBase thrower, ItemStack stack, Vec3d at) {
        EntityItem item = new EntityItem(thrower.world, thrower.posX, thrower.posY + thrower.getEyeHeight() - 0.3, thrower.posZ, stack);
        Vec3d v = at.subtract(thrower.getPositionVector()).normalize().scale(0.3);
        item.motionX = v.x;
        item.motionY = v.y;
        item.motionZ = v.z;
        item.setDefaultPickupDelay();
        thrower.world.spawnEntity(item);
    }

    private static Vec3d randomNearbyPos(EntityPiglin p) {
        Vec3d v = RandomPositionGenerator.getLandPos(p, 4, 2);
        return v == null ? p.getPositionVector() : v;
    }

    /** 1.16's {@code wantsToPickup}. */
    static boolean wantsToPickup(EntityPiglin p, ItemStack stack) {
        if (isRepellentItem(stack)) {
            return false;
        }
        if (p.memories.has(Mem.ADMIRING_DISABLED) && p.getAttackTarget() != null) {
            return false;
        }
        if (isBarterCurrency(stack)) {
            return isNotHoldingLovedItemInOffHand(p);
        }
        boolean room = p.canAddToInventory(stack);
        if (stack.getItem() == Items.GOLD_NUGGET) {
            return room;
        }
        if (isFood(stack)) {
            return !p.memories.has(Mem.ATE_RECENTLY) && room;
        }
        if (!isLovedItem(stack)) {
            return p.canReplaceCurrentItem(stack);
        }
        return isNotHoldingLovedItemInOffHand(p) && room;
    }

    /** 1.16's {@code mobInteract}: handed gold, an adult admires it. @return true if it took the item */
    static boolean mobInteract(EntityPiglin p, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!canAdmire(p, held)) {
            return false;
        }
        ItemStack one = held.splitStack(1);
        holdInOffhand(p, one);
        admireGoldItem(p);
        stopWalking(p);
        if (isWearingGold(player)) {
            Triggers.fire(player, "distract_piglin_directly");
        }
        return true;
    }

    static boolean canAdmire(EntityPiglin p, ItemStack stack) {
        return !p.memories.has(Mem.ADMIRING_DISABLED) && !p.memories.has(Mem.ADMIRING_ITEM) && p.isAdult() && isBarterCurrency(stack);
    }

    private static void admireGoldItem(EntityPiglin p) {
        p.memories.setFor(Mem.ADMIRING_ITEM, true, 120);
    }

    private static void stopWalking(EntityPiglin p) {
        p.eraseWalkTarget();
    }

    // =============================================================================================
    // Anger
    // =============================================================================================

    /**
     * 1.16's {@code angerNearbyPiglins}: idle piglins within 16 blocks of the player (that can see them, when asked)
     * get angry at them. Called when a player opens a chest or breaks a guarded block.
     */
    public static void angerNearbyPiglins(EntityPlayer player, boolean requireLineOfSight) {
        for (EntityPiglin p : player.world.getEntitiesWithinAABB(EntityPiglin.class, player.getEntityBoundingBox().grow(16.0))) {
            if (p.activity == Activity.IDLE && (!requireLineOfSight || p.visibleLiving().contains(player) || p.isTargetable(player))) {
                setAngerTarget(p, player);
            }
        }
    }

    /** 1.16's {@code wasHurtBy}. */
    static void wasHurtBy(EntityPiglin p, EntityLivingBase by) {
        if (by instanceof EntityPiglin) {
            return;
        }
        Memories m = p.memories;
        if (!p.getHeldItemOffhand().isEmpty()) {
            stopHoldingOffHandItem(p, false);
        }
        m.erase(Mem.CELEBRATE_LOCATION);
        m.erase(Mem.DANCING);
        m.erase(Mem.ADMIRING_ITEM);
        if (by instanceof EntityPlayer) {
            m.setFor(Mem.ADMIRING_DISABLED, true, 400);
        }
        EntityLivingBase avoid = m.get(Mem.AVOID_TARGET);
        if (avoid != null && avoid.getClass() != by.getClass()) {
            m.erase(Mem.AVOID_TARGET);
        }
        if (p.isChild()) {
            m.setFor(Mem.AVOID_TARGET, by, 100);
            if (AbstractPiglin.isAttackAllowed(by)) {
                broadcastAngerTarget(p, by);
            }
        } else if (isHoglin(by) && hoglinsOutnumberPiglins(p)) {
            setAvoidTargetAndDontHuntForAWhile(p, by);
            broadcastRetreat(p, by);
        } else {
            maybeRetaliate(p, by);
        }
    }

    /** 1.16's {@code maybeRetaliate}, for piglins and brutes. */
    static void maybeRetaliate(AbstractPiglin p, EntityLivingBase by) {
        if (p instanceof EntityPiglin && ((EntityPiglin) p).activity == Activity.AVOID) {
            return;
        }
        if (!AbstractPiglin.isAttackAllowed(by) || isOtherTargetMuchFurtherAway(p, by, 4.0)) {
            return;
        }
        setAngerTarget(p, by);
        broadcastAngerTarget(p, by);
    }

    private static boolean isOtherTargetMuchFurtherAway(AbstractPiglin p, EntityLivingBase other, double range) {
        EntityLivingBase current = p.getAttackTarget();
        return current != null && p.getDistanceSq(other) > p.getDistanceSq(current) + range * range;
    }

    static void broadcastAngerTarget(AbstractPiglin p, EntityLivingBase target) {
        for (AbstractPiglin q : nearbyAdultPiglins(p)) {
            if (!isHoglin(target) || q.canHunt() && canBeHunted(target)) {
                setAngerTargetIfCloserThanCurrent(q, target);
            }
        }
    }

    /** 1.16's {@code setAngerTarget}: angry at it for 30 s; a hunted hoglin means no more hunting for a while. */
    public static void setAngerTarget(AbstractPiglin p, EntityLivingBase target) {
        if (!AbstractPiglin.isAttackAllowed(target)) {
            return;
        }
        p.memories.erase(Mem.CANT_REACH_WALK_TARGET_SINCE);
        p.memories.setFor(Mem.ANGRY_AT, target.getUniqueID(), 600);
        if (isHoglin(target) && p.canHunt()) {
            dontKillAnyMoreHoglinsForAWhile(p);
        }
    }

    private static void setAngerTargetIfCloserThanCurrent(AbstractPiglin p, EntityLivingBase target) {
        EntityLivingBase current = p.entityByUuid(p.memories.<java.util.UUID>get(Mem.ANGRY_AT));
        EntityLivingBase nearest = current == null ? target : p.getDistanceSq(current) < p.getDistanceSq(target) ? current : target;
        if (current == null || current != nearest) {
            setAngerTarget(p, nearest);
        }
    }

    private static void broadcastRetreat(EntityPiglin p, EntityLivingBase from) {
        for (AbstractPiglin q : visibleAdultPiglins(p)) {
            if (q instanceof EntityPiglin) {
                EntityPiglin r = (EntityPiglin) q;
                EntityLivingBase nearest = nearestOf(r, r.memories.get(Mem.AVOID_TARGET), from);
                nearest = nearestOf(r, r.getAttackTarget(), nearest);
                setAvoidTargetAndDontHuntForAWhile(r, nearest);
            }
        }
    }

    private static EntityLivingBase nearestOf(EntityLivingBase from, @Nullable EntityLivingBase a, EntityLivingBase b) {
        return a == null ? b : from.getDistanceSq(a) < from.getDistanceSq(b) ? a : b;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(EntityPiglin p, EntityLivingBase target) {
        p.memories.erase(Mem.ANGRY_AT);
        p.setAttackTarget(null);
        p.eraseWalkTarget();
        p.memories.setFor(Mem.AVOID_TARGET, target, seconds(p.getRNG(), 5, 20));
        dontKillAnyMoreHoglinsForAWhile(p);
    }

    static void dontKillAnyMoreHoglinsForAWhile(AbstractPiglin p) {
        p.memories.setFor(Mem.HUNTED_RECENTLY, true, timeBetweenHunts(p.getRNG()));
    }

    /** 1.16's {@code findNearestValidAttackTarget}. */
    @Nullable
    public static EntityLivingBase findNearestValidAttackTarget(EntityPiglin p) {
        if (isNearZombified(p)) {
            return null;
        }
        Memories m = p.memories;
        EntityLivingBase angry = p.entityByUuid(m.<java.util.UUID>get(Mem.ANGRY_AT));
        if (angry != null && AbstractPiglin.isAttackAllowed(angry)) {
            return angry;
        }
        EntityLivingBase nemesis = m.get(Mem.NEAREST_VISIBLE_NEMESIS);
        if (nemesis != null) {
            return nemesis;
        }
        EntityPlayer player = m.get(Mem.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
        return player != null && AbstractPiglin.isAttackAllowed(player) ? player : null;
    }

    private static boolean wantsToStopFleeing(EntityPiglin p) {
        EntityLivingBase avoid = p.memories.get(Mem.AVOID_TARGET);
        if (avoid == null) {
            return true;
        }
        if (isHoglin(avoid)) {
            return !hoglinsOutnumberPiglins(p);
        }
        if (isZombified(avoid)) {
            return !p.memories.is(Mem.NEAREST_VISIBLE_ZOMBIFIED, avoid);
        }
        return false;
    }

    public static boolean hoglinsOutnumberPiglins(EntityPiglin p) {
        Integer piglins = p.memories.get(Mem.VISIBLE_ADULT_PIGLIN_COUNT);
        Integer hoglins = p.memories.get(Mem.VISIBLE_ADULT_HOGLIN_COUNT);
        return (hoglins == null ? 0 : hoglins) > (piglins == null ? 0 : piglins) + 1;
    }

    static boolean hasAnyoneNearbyHuntedRecently(EntityPiglin p) {
        if (p.memories.has(Mem.HUNTED_RECENTLY)) {
            return true;
        }
        for (AbstractPiglin q : visibleAdultPiglins(p)) {
            if (q.memories.has(Mem.HUNTED_RECENTLY)) {
                return true;
            }
        }
        return false;
    }

    static boolean isNearZombified(EntityPiglin p) {
        EntityLivingBase z = p.memories.get(Mem.NEAREST_VISIBLE_ZOMBIFIED);
        return z != null && z.getDistance(p) < 6.0;
    }

    private static boolean wantsToDance(EntityPiglin p, EntityLivingBase killed) {
        return isHoglin(killed) && new Random(p.world.getTotalWorldTime()).nextFloat() < 0.1F;
    }

    private static boolean wantsToStopRiding(EntityPiglin p, Entity vehicle) {
        if (!(vehicle instanceof EntityLiving)) {
            return false;
        }
        EntityLiving mob = (EntityLiving) vehicle;
        return !mob.isChild() || !mob.isEntityAlive() || p.getLastDamageSource() != null || mob.getLastDamageSource() != null
                || mob instanceof EntityPiglin && mob.getRidingEntity() == null;
    }

    private static boolean isBabyRidingBaby(EntityPiglin p) {
        if (!p.isChild()) {
            return false;
        }
        Entity v = p.getRidingEntity();
        return v instanceof EntityPiglin && ((EntityPiglin) v).isChild() || isHoglin(v) && ((EntityAgeable) v).isChild();
    }

    @SuppressWarnings("unchecked")
    static List<AbstractPiglin> visibleAdultPiglins(AbstractPiglin p) {
        List<AbstractPiglin> l = p.memories.get(Mem.NEAREST_VISIBLE_ADULT_PIGLINS);
        return l == null ? Collections.<AbstractPiglin>emptyList() : l;
    }

    @SuppressWarnings("unchecked")
    static List<AbstractPiglin> nearbyAdultPiglins(AbstractPiglin p) {
        List<AbstractPiglin> l = p.memories.get(Mem.NEARBY_ADULT_PIGLINS);
        return l == null ? Collections.<AbstractPiglin>emptyList() : l;
    }

    // =============================================================================================
    // The piglin's sensor (1.16's PiglinMobsSensor) and its item sensor
    // =============================================================================================

    static void sense(EntityPiglin p, List<EntityLivingBase> nearby, List<EntityLivingBase> visible) {
        Memories m = p.memories;
        m.set(Mem.NEAREST_REPELLENT, findNearestRepellent(p.world, new BlockPos(p)));
        EntityLivingBase nemesis = null;
        EntityLivingBase huntable = null;
        EntityLivingBase babyHoglin = null;
        EntityLivingBase zombified = null;
        EntityPlayer notWearingGold = null;
        EntityPlayer holdingLoved = null;
        int hoglins = 0;
        List<AbstractPiglin> adults = new ArrayList<AbstractPiglin>();
        List<AbstractPiglin> nearbyAdults = new ArrayList<AbstractPiglin>();
        for (EntityLivingBase e : visible) {
            if (isHoglin(e)) {
                if (((EntityAgeable) e).isChild()) {
                    if (babyHoglin == null) {
                        babyHoglin = e;
                    }
                } else {
                    hoglins++;
                    if (huntable == null && canBeHunted(e)) {
                        huntable = e;
                    }
                }
            } else if (e instanceof EntityPiglinBrute) {
                adults.add((AbstractPiglin) e);
            } else if (e instanceof EntityPiglin) {
                if (((EntityPiglin) e).isAdult()) {
                    adults.add((AbstractPiglin) e);
                }
            } else if (e instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) e;
                if (notWearingGold == null && AbstractPiglin.isAttackAllowed(player) && !isWearingGold(player)) {
                    notWearingGold = player;
                }
                if (holdingLoved == null && !player.isSpectator() && isPlayerHoldingLovedItem(player)) {
                    holdingLoved = player;
                }
            } else if (nemesis != null || !isNemesis(e)) {
                if (zombified == null && isZombified(e)) {
                    zombified = e;
                }
            } else {
                nemesis = e;
            }
        }
        for (EntityLivingBase e : nearby) {
            if (e instanceof AbstractPiglin && ((AbstractPiglin) e).isAdult()) {
                nearbyAdults.add((AbstractPiglin) e);
            }
        }
        m.set(Mem.NEAREST_VISIBLE_NEMESIS, nemesis);
        m.set(Mem.NEAREST_VISIBLE_HUNTABLE_HOGLIN, huntable);
        m.set(Mem.NEAREST_VISIBLE_BABY_HOGLIN, babyHoglin);
        m.set(Mem.NEAREST_VISIBLE_ZOMBIFIED, zombified);
        m.set(Mem.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, notWearingGold);
        m.set(Mem.NEAREST_PLAYER_HOLDING_WANTED_ITEM, holdingLoved);
        m.set(Mem.NEARBY_ADULT_PIGLINS, nearbyAdults);
        m.set(Mem.NEAREST_VISIBLE_ADULT_PIGLINS, adults);
        m.set(Mem.VISIBLE_ADULT_PIGLIN_COUNT, adults.size());
        m.set(Mem.VISIBLE_ADULT_HOGLIN_COUNT, hoglins);

        // 1.16's WantedItemsSensor: the nearest item within 8 (4 up or down) it wants, within 9 and in sight.
        List<EntityItem> items = p.world.getEntitiesWithinAABB(EntityItem.class, p.getEntityBoundingBox().grow(8.0, 4.0, 8.0));
        Collections.sort(items, (a, b) -> Double.compare(p.getDistanceSq(a), p.getDistanceSq(b)));
        EntityItem wanted = null;
        for (EntityItem item : items) {
            if (p.wantsToPickUp(item.getItem()) && item.getDistance(p) < 9.0 && p.canEntityBeSeen(item)) {
                wanted = item;
                break;
            }
        }
        m.set(Mem.NEAREST_VISIBLE_WANTED_ITEM, wanted);
    }

    /** 1.16's {@code findNearestRepellent}: 8 blocks across, 4 up or down, nearest first. */
    @Nullable
    public static BlockPos findNearestRepellent(World world, BlockPos at) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(at.add(-8, -4, -8), at.add(8, 4, 8))) {
            if (isRepellentBlock(world.getBlockState(p))) {
                double d = p.distanceSq(at);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.toImmutable();
                }
            }
        }
        return best;
    }

    // =============================================================================================
    // Sounds
    // =============================================================================================

    @Nullable
    static SoundEvent soundForActivity(EntityPiglin p, Activity activity) {
        if (activity == Activity.FIGHT) {
            return PiglinContent.PIGLIN_ANGRY;
        }
        if (p.isConverting()) {
            return PiglinContent.PIGLIN_RETREAT;
        }
        if (activity == Activity.AVOID && isNearAvoidTarget(p)) {
            return PiglinContent.PIGLIN_RETREAT;
        }
        if (activity == Activity.ADMIRE_ITEM) {
            return PiglinContent.PIGLIN_ADMIRING_ITEM;
        }
        if (activity == Activity.CELEBRATE) {
            return PiglinContent.PIGLIN_CELEBRATE;
        }
        if (p.memories.has(Mem.NEAREST_PLAYER_HOLDING_WANTED_ITEM)) {
            return PiglinContent.PIGLIN_JEALOUS;
        }
        return p.memories.has(Mem.NEAREST_REPELLENT) ? PiglinContent.PIGLIN_RETREAT : PiglinContent.PIGLIN_AMBIENT;
    }

    private static boolean isNearAvoidTarget(EntityPiglin p) {
        EntityLivingBase avoid = p.memories.get(Mem.AVOID_TARGET);
        return avoid != null && avoid.getDistance(p) < 12.0;
    }

    // =============================================================================================
    // Kinds of things
    // =============================================================================================

    static boolean isDeadOrDying(@Nullable EntityLivingBase e) {
        return e == null || e.getHealth() <= 0.0F || e.isDead;
    }

    /** @return true for this mod's hoglin, by registry name (stream H's) */
    public static boolean isHoglin(@Nullable Entity e) {
        return e instanceof EntityAgeable && Content.id("hoglin").equals(EntityList.getKey(e));
    }

    /** 1.16's {@code HoglinEntity.canBeHunted}: an adult not marked as a bastion stable's own. */
    static boolean canBeHunted(EntityLivingBase hoglin) {
        return hoglin instanceof EntityHoglin && ((EntityHoglin) hoglin).canBeHunted();
    }

    /** 1.16's zombified piglins and zoglins: vanilla's zombie pigman (stream Z's zombified piglin) and our zoglin. */
    public static boolean isZombified(@Nullable Entity e) {
        return e instanceof EntityPigZombie || e != null && Content.id("zoglin").equals(EntityList.getKey(e));
    }

    private static boolean isNemesis(EntityLivingBase e) {
        return e instanceof net.minecraft.entity.monster.EntityWitherSkeleton || e instanceof net.minecraft.entity.boss.EntityWither;
    }

    static boolean hasCrossbow(EntityLivingBase e) {
        return Crossbows.isCrossbow(e.getHeldItemMainhand()) || Crossbows.isCrossbow(e.getHeldItemOffhand());
    }

    /** 1.16's {@code BrainUtil.isWithinAttackRange(mob, target, cooldown)}: a crossbow's 8 blocks less cooldown, else melee reach. */
    static boolean isWithinAttackRange(AbstractPiglin p, EntityLivingBase target, int cooldown) {
        if (Crossbows.isCrossbow(p.getHeldItemMainhand())) {
            return p.getDistance(target) < 8.0 - cooldown;
        }
        return p.isWithinMeleeAttackRange(target);
    }

    /** 1.16's {@code isWearingGold}: any piece of gold armor (Forge's {@code makesPiglinsNeutral}). */
    public static boolean isWearingGold(EntityLivingBase e) {
        for (ItemStack armor : e.getArmorInventoryList()) {
            if (armor.getItem() instanceof ItemArmor && ((ItemArmor) armor.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerHoldingLovedItem(EntityLivingBase e) {
        return e instanceof EntityPlayer && (isLovedItem(e.getHeldItemMainhand()) || isLovedItem(e.getHeldItemOffhand()));
    }

    static boolean isNotHoldingLovedItemInOffHand(EntityPiglin p) {
        return p.getHeldItemOffhand().isEmpty() || !isLovedItem(p.getHeldItemOffhand());
    }

    public static boolean isBarterCurrency(ItemStack stack) {
        return stack.getItem() == BARTERING_ITEM;
    }

    static boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.PORKCHOP || stack.getItem() == Items.COOKED_PORKCHOP;
    }

    private static Set<Item> loved;
    private static Set<Item> repellentItems;

    /** @return true for current Java's {@code piglin_loved} items that exist in this game */
    public static boolean isLovedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (loved == null) {
            loved = buildLoved();
        }
        return loved.contains(stack.getItem()) || isGoldOreItem(stack);
    }

    private static Set<Item> buildLoved() {
        Set<Item> s = new HashSet<Item>();
        Collections.addAll(s, Item.getItemFromBlock(Blocks.GOLD_ORE), Item.getItemFromBlock(Blocks.GOLD_BLOCK),
                Item.getItemFromBlock(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE), Items.GOLD_INGOT, Items.CLOCK, Items.GOLDEN_CARROT,
                Items.SPECKLED_MELON, Items.GOLDEN_APPLE, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS,
                Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR, Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL,
                Items.GOLDEN_AXE, Items.GOLDEN_HOE);
        addOurs(s, "nether_gold_ore", "gilded_blackstone");
        addNamed(s, Owners.VILLAGE_AND_PILLAGE + ":bell", "futuremc:bell");
        for (Item item : ForgeRegistries.ITEMS) {
            String path = item.getRegistryName().getPath();
            if ("raw_gold".equals(path) || "raw_gold_block".equals(path) || "deepslate_gold_ore".equals(path)) {
                s.add(item);
            }
        }
        s.remove(Items.AIR);
        return s;
    }

    private static boolean isGoldOreItem(ItemStack stack) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("oreGold".equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    /** @return true for current Java's {@code piglin_repellents} items: soul torch, soul lantern, soul campfire */
    public static boolean isRepellentItem(ItemStack stack) {
        if (repellentItems == null) {
            Set<Item> s = new HashSet<Item>();
            addOurs(s, "soul_torch", "soul_lantern", "soul_campfire");
            // The other mod's soul torch and lantern stand in for ours when both are installed (FutureMcPairs).
            addNamed(s, "futuremc:soul_fire_torch", "futuremc:soul_fire_lantern");
            s.remove(Items.AIR);
            repellentItems = s;
        }
        return repellentItems.contains(stack.getItem());
    }

    /** @return true for {@code piglin_repellents} blocks: soul fire, soul torch, soul lantern, and a lit soul campfire */
    static boolean isRepellentBlock(IBlockState state) {
        Block b = state.getBlock();
        ResourceLocation id = b.getRegistryName();
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        if ("futuremc".equals(id.getNamespace())) {
            // Its soul torch and lantern stand in for ours when both are installed (FutureMcPairs).
            return "soul_fire_torch".equals(path) || "soul_fire_lantern".equals(path);
        }
        if (!Content.id("x").getNamespace().equals(id.getNamespace())) {
            return false;
        }
        if ("soul_campfire".equals(path)) {
            for (net.minecraft.block.properties.IProperty<?> prop : state.getPropertyKeys()) {
                if ("lit".equals(prop.getName())) {
                    return Boolean.TRUE.equals(state.getValue(prop));
                }
            }
            return true;
        }
        return "soul_fire".equals(path) || "soul_torch".equals(path) || "soul_wall_torch".equals(path) || "soul_lantern".equals(path);
    }

    /** @return true for current Java's {@code guarded_by_piglins} blocks */
    public static boolean isGuardedBlock(IBlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.GOLD_BLOCK || b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.ENDER_CHEST
                || b == Blocks.GOLD_ORE || b instanceof BlockShulkerBox) {
            return true;
        }
        ResourceLocation id = b.getRegistryName();
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        if (id.getNamespace().equals(Content.id("x").getNamespace()) && ("gilded_blackstone".equals(path) || "nether_gold_ore".equals(path))) {
            return true;
        }
        if ("barrel".equals(path) || "raw_gold_block".equals(path) || "deepslate_gold_ore".equals(path)) {
            return true;
        }
        Item item = Item.getItemFromBlock(b);
        return item != Items.AIR && isGoldOreItem(new ItemStack(item, 1, b.damageDropped(state)));
    }

    private static void addOurs(Set<Item> s, String... names) {
        for (String n : names) {
            Item i = Owners.ours(n);
            if (i != null) {
                s.add(i);
            }
        }
    }

    private static void addNamed(Set<Item> s, String... names) {
        for (String n : names) {
            ResourceLocation id = new ResourceLocation(n);
            Item i = ForgeRegistries.ITEMS.getValue(id);
            if (i != null && id.equals(i.getRegistryName())) {
                s.add(i);
            }
        }
    }

    /** @return the slot 1.12.2 would put this in */
    static EntityEquipmentSlot slotFor(ItemStack stack) {
        return EntityLiving.getSlotForItemStack(stack);
    }
}
