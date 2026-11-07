package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.asm.PigZombieTransformer;
import com.exiledradio.netherupdatebackport.mobs.Anger;
import com.exiledradio.netherupdatebackport.mobs.HurtByTargetGoal;
import com.exiledradio.netherupdatebackport.mobs.PigmanHooks;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/** Stream Z: zombie pigmen as 1.16's zombified piglins. */
public final class MobsSelfTest implements SelfTest {

    private static final UUID SPEED_BOOST = UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718");
    private static final Field ANGER_LEVEL = ReflectionHelper.findField(EntityPigZombie.class, "angerLevel", "field_70837_d");
    private static final Field ANGER_TARGET = ReflectionHelper.findField(EntityPigZombie.class, "angerTargetUUID", "field_175459_bn");

    @Override
    public String name() {
        return "mobs";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(PigZombieTransformer.applied, "patch P1 is applied to EntityPigZombie");
        c.ok(PigmanHooks.active(), "and switched on");
        c.ok(langHas("entity.PigZombie.name=Zombified Piglin"), "the zombie pigman is named Zombified Piglin");

        // Goals at spawn ---------------------------------------------------------------------------------
        BlockPos a = c.area();
        EntityPigZombie pigman = spawn(world, a.add(16, 0, 16));
        c.ok(Anger.of(pigman) != null, "a pigman carries 1.16's anger state");
        boolean hurtBy = false;
        boolean nearest = false;
        boolean vanilla = false;
        for (EntityAITasks.EntityAITaskEntry e : pigman.targetTasks.taskEntries) {
            hurtBy |= e.action instanceof HurtByTargetGoal && e.priority == 1;
            nearest |= e.action instanceof EntityAINearestAttackableTarget && e.priority == 2;
            vanilla |= e.action.getClass().getEnclosingClass() == EntityPigZombie.class;
        }
        c.ok(hurtBy && nearest, "1.16's two target goals are installed");
        c.ok(!vanilla, "and 1.12.2's angry-at-every-player goals are gone");
        c.eq(8.0F, pigman.getPathPriority(PathNodeType.LAVA), "lava costs 8 to path through");
        c.eq(0, ANGER_LEVEL.getInt(pigman), "vanilla's anger counter is not used");

        // A player's hit: angry at that player, the group joins ------------------------------------------
        FakePlayer p1 = c.player("zp1", a.add(16, 0, 20));
        FakePlayer p2 = c.player("zp2", a.add(16, 0, 12));
        EntityPigZombie near = spawn(world, a.add(20, 0, 16));
        EntityPigZombie out = spawn(world, a.add(30, 0, 16));
        EntityPigZombie high = spawn(world, a.add(16, 12, 16));
        high.setNoGravity(true);
        pigman.attackEntityFrom(DamageSource.causePlayerDamage(p1), 1.0F);
        c.eq(0, ANGER_LEVEL.getInt(pigman), "1.12.2's 20-40 s of anger at everyone did not start");
        tick(pigman, 6);
        Anger anger = Anger.of(pigman);
        c.eq(p1, pigman.getAttackTarget(), "it fights back at the player who hit it");
        c.eq(p1.getUniqueID(), anger.target(), "and is angry at that player");
        c.ok(anger.remaining() >= 390 && anger.remaining() <= 780, "for 20-39 s: " + anger.remaining());
        c.ok(pigman.isAngry(), "isAngry() reads 1.16's state");
        c.ok(anger.isAngryAt(p1) && !anger.isAngryAt(p2), "angry at the hitter only, not at another player");
        c.ok(pigman.isPreventingPlayerRest(p1) && !pigman.isPreventingPlayerRest(p2), "it keeps only that player from sleeping");
        c.eq(p1, near.getAttackTarget(), "a pigman 4 blocks off joins in");
        c.ok(out.getAttackTarget() == null, "one beyond follow range does not");
        c.ok(high.getAttackTarget() == null, "nor one 12 blocks above");
        IAttributeInstance speed = pigman.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        c.ok(speed.getModifier(SPEED_BOOST) != null, "an angry adult runs faster (vanilla's boost)");
        c.eq("netherupdatebackport:entity.zombified_piglin.angry", sound(pigman, "getAmbientSound", "func_184639_G"),
                "an angry one grunts angrily");
        c.eq("netherupdatebackport:entity.zombified_piglin.hurt", soundHurt(pigman), "the zombified piglin's hurt sound");
        c.eq("netherupdatebackport:entity.zombified_piglin.death", sound(pigman, "getDeathSound", "func_184615_bR"),
                "and death sound");
        c.eq("netherupdatebackport:entity.zombified_piglin.ambient", sound(out, "getAmbientSound", "func_184639_G"),
                "a calm one grunts as ever");

        int held = anger.remaining();
        tick(pigman, 20);
        c.eq(held, anger.remaining(), "the timer holds while it has the player in its sights");

        // Out of reach, it forgets after the timer ----------------------------------------------------------
        // (Clearing the target is not enough: as in 1.16, the target goal keeps its own copy and sets it back.)
        p1.setPosition(a.getX() + 16.5, a.getY(), a.getZ() + 80.5);
        tick(pigman, 1);
        c.ok(pigman.getAttackTarget() == null, "a player beyond follow range is dropped as target");
        int left = anger.remaining();
        int ticks = 0;
        while (anger.isAngry() && ticks < 900) {
            tick(pigman, 1);
            ticks++;
        }
        c.ok(!anger.isAngry() && Math.abs(ticks - left) <= 3, "with no player target it calms after its timer: " + ticks + " of " + left);
        c.ok(anger.target() == null && pigman.getAttackTarget() == null, "and forgets the player");
        c.ok(!pigman.isPreventingPlayerRest(p1), "who may sleep again");
        tick(pigman, 1);
        c.ok(speed.getModifier(SPEED_BOOST) == null, "and loses its speed boost");

        p1.setPosition(a.getX() + 16.5, a.getY(), a.getZ() + 20.5);

        // A dead player is forgiven ------------------------------------------------------------------------
        EntityPigZombie grudge = spawn(world, a.add(10, 0, 10));
        grudge.attackEntityFrom(DamageSource.causePlayerDamage(p1), 1.0F);
        tick(grudge, 6);
        c.ok(Anger.of(grudge).isAngryAt(p1), "angry at a player");
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(p1, DamageSource.GENERIC));
        c.ok(!Anger.of(grudge).isAngry() && grudge.getAttackTarget() == null, "until that player dies");

        // Hit by a mob: anger at the mob, never at a player, over when the mob is gone -----------------------
        BlockPos m = c.area();
        FakePlayer p3 = c.player("zp3", m.add(8, 0, 4));
        EntityPigZombie hit = spawn(world, m.add(4, 0, 4));
        EntitySkeleton skeleton = new EntitySkeleton(world);
        skeleton.setPosition(m.getX() + 4.5, m.getY(), m.getZ() + 7.5);
        world.spawnEntity(skeleton);
        hit.attackEntityFrom(DamageSource.causeMobDamage(skeleton), 1.0F);
        tick(hit, 6);
        Anger hitAnger = Anger.of(hit);
        c.eq(skeleton, hit.getAttackTarget(), "a skeleton's hit: it fights the skeleton");
        c.eq(skeleton.getUniqueID(), hitAnger.target(), "angry at the skeleton");
        c.ok(!hitAnger.isAngryAt(p3), "and not at the player beside it");
        int before = hitAnger.remaining();
        tick(hit, 5);
        c.ok(hitAnger.remaining() < before, "the timer runs while the target is not a player");
        skeleton.setHealth(0.0F);
        tick(hit, 2);
        c.ok(!hitAnger.isAngry() && hitAnger.target() == null, "once the skeleton is dead it calms at once");
        skeleton.setDead();

        // The alarm every 4-6 s, only with sight -----------------------------------------------------------
        BlockPos g = c.area();
        FakePlayer p4 = c.player("zp4", g.add(4, 0, 8));
        EntityPigZombie leader = spawn(world, g.add(4, 0, 4));
        leader.attackEntityFrom(DamageSource.causePlayerDamage(p4), 1.0F);
        tick(leader, 3);
        EntityPigZombie late = spawn(world, g.add(7, 0, 4));
        int waited = 0;
        while (late.getAttackTarget() == null && waited < 130) {
            tick(leader, 1);
            waited++;
        }
        c.eq(p4, late.getAttackTarget(), "a pigman that came later hears the alarm");
        c.ok(waited <= 121, "within 4-6 s: " + waited + " ticks");

        // Shut in a stone box (3x3, 3 high inside), so it can neither see the player nor walk round to them.
        BlockPos w = g.add(20, 0, 0);
        FakePlayer p5 = c.player("zp5", w.add(4, 0, 9));
        c.fill(w.add(2, 0, 2), w.add(6, 3, 6), Blocks.STONE.getDefaultState());
        c.fill(w.add(3, 0, 3), w.add(5, 2, 5), Blocks.AIR.getDefaultState());
        EntityPigZombie blind = spawn(world, w.add(4, 0, 4));
        blind.attackEntityFrom(DamageSource.causePlayerDamage(p5), 1.0F);
        tick(blind, 3);
        c.eq(p5, blind.getAttackTarget(), "an unseen attacker is still fought (1.16, unlike 1.12.2)");
        EntityPigZombie unwarned = spawn(world, w.add(9, 0, 4));
        tick(blind, 125);
        c.ok(unwarned.getAttackTarget() == null, "but with no sight of it no alarm is raised");

        // Saving -------------------------------------------------------------------------------------------
        BlockPos s = c.area();
        FakePlayer p6 = c.player("zp6", s.add(4, 0, 8));
        EntityPigZombie saved = spawn(world, s.add(4, 0, 4));
        saved.attackEntityFrom(DamageSource.causePlayerDamage(p6), 1.0F);
        tick(saved, 6);
        NBTTagCompound tag = new NBTTagCompound();
        saved.writeToNBT(tag);
        EntityPigZombie copy = new EntityPigZombie(world);
        copy.readFromNBT(tag);
        c.eq(Anger.of(saved).remaining(), Anger.of(copy).remaining(), "the anger timer is saved");
        c.eq(p6.getUniqueID(), Anger.of(copy).target(), "and whom it is angry at");
        c.ok(tag.getShort("Anger") <= 0, "vanilla's own counter stays unused in the save: " + tag.getShort("Anger"));

        // A pigman saved angry by 1.12.2 ---------------------------------------------------------------------
        EntityPigZombie old = new EntityPigZombie(world);
        old.setPosition(s.getX() + 10.5, s.getY(), s.getZ() + 10.5);
        ANGER_LEVEL.setInt(old, 500);
        ANGER_TARGET.set(old, p6.getUniqueID());
        world.spawnEntity(old);
        c.eq(p6.getUniqueID(), Anger.of(old).target(), "1.12.2 anger in an old world carries over: the player");
        c.eq(500, Anger.of(old).remaining(), "and the time left");
        c.eq(0, ANGER_LEVEL.getInt(old), "vanilla's counter is cleared");

        // A baby: no speed boost -------------------------------------------------------------------------------
        EntityPigZombie baby = spawn(world, s.add(20, 0, 4));
        baby.setChild(true);
        baby.attackEntityFrom(DamageSource.causePlayerDamage(p6), 1.0F);
        tick(baby, 6);
        c.ok(baby.isAngry(), "an angry baby");
        c.ok(baby.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getModifier(SPEED_BOOST) == null,
                "gets no speed boost, as an adult would");

        // Spawning: not on a nether wart block --------------------------------------------------------------
        BlockPos sp = c.area();
        world.setBlockState(sp.add(2, -1, 2), Blocks.NETHER_WART_BLOCK.getDefaultState(), 2);
        EntityPigZombie spawning = new EntityPigZombie(world);
        c.eq(Event.Result.DENY, ForgeEventFactory.canEntitySpawn(spawning, world, sp.getX() + 2.5F, sp.getY(), sp.getZ() + 2.5F,
                (net.minecraft.tileentity.MobSpawnerBaseLogic) null), "no spawning on a nether wart block");
        c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(spawning, world, sp.getX() + 6.5F, sp.getY(), sp.getZ() + 6.5F,
                (net.minecraft.tileentity.MobSpawnerBaseLogic) null), "stone is up to vanilla as before");
    }

    private static EntityPigZombie spawn(WorldServer world, BlockPos at) {
        EntityPigZombie pigman = new EntityPigZombie(world);
        pigman.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        // A short follow range keeps each test's pigmen from alarming the next test's.
        pigman.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(8.0D);
        // A hit at tick 0 stamps revenge time 0, which a fresh hurt-by goal takes for "already seen" (vanilla).
        pigman.ticksExisted = 20;
        world.spawnEntity(pigman);
        return pigman;
    }

    private static void tick(EntityLivingBase entity, int n) {
        for (int i = 0; i < n; i++) {
            // The world counts ticksExisted, not onUpdate; revenge timestamps read it.
            entity.ticksExisted++;
            entity.onUpdate();
        }
    }

    private static String sound(EntityPigZombie pigman, String mcp, String srg) throws Exception {
        Method m = ReflectionHelper.findMethod(EntityPigZombie.class, mcp, srg);
        return String.valueOf(((SoundEvent) m.invoke(pigman)).getRegistryName());
    }

    private static String soundHurt(EntityPigZombie pigman) throws Exception {
        Method m = ReflectionHelper.findMethod(EntityPigZombie.class, "getHurtSound", "func_184601_bQ", DamageSource.class);
        return String.valueOf(((SoundEvent) m.invoke(pigman, DamageSource.GENERIC)).getRegistryName());
    }

    private static boolean langHas(String line) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                MobsSelfTest.class.getResourceAsStream("/assets/netherupdatebackport/lang/en_us.lang"), "UTF-8"));
        try {
            for (String l = in.readLine(); l != null; l = in.readLine()) {
                if (l.equals(line)) {
                    return true;
                }
            }
            return false;
        } finally {
            in.close();
        }
    }
}
