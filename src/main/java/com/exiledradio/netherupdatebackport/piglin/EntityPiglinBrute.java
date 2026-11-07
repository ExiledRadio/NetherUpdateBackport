package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.piglin.Memories.Mem;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The piglin brute (1.16.2): 1.16.5's {@code PiglinBruteEntity} and {@code PiglinBruteBrain}.
 *
 * <ul>
 *   <li>50 health, speed 0.35, attack 7, a golden axe; 20 experience.</li>
 *   <li>Always hostile: attacks any player within 12 blocks it can see, gold armor or not, anyone it is angry at, and
 *       wither skeletons; never barters, picks up nothing but golden axes; does not zombify any slower.</li>
 *   <li>Stays near where it spawned (its home): walks back from up to 100 blocks off, potters about within 5.</li>
 *   <li>Spawns only in bastion remnants (stream X, through {@link BastionMobs}); despawns in peaceful.</li>
 * </ul>
 */
public class EntityPiglinBrute extends AbstractPiglin {

    private boolean fighting;
    private int homeDimension;
    private long nextWalkHome;
    private long nextWork;

    final Shuffled<EntityPiglinBrute> idleLook = new Shuffled<EntityPiglinBrute>()
            .add(1, b -> PiglinTasks.lookAt(b, e -> e instanceof EntityPlayer, 8.0))
            .add(1, b -> PiglinTasks.lookAt(b, e -> e instanceof EntityPiglin, 8.0))
            .add(1, b -> PiglinTasks.lookAt(b, e -> e instanceof EntityPiglinBrute, 8.0))
            .add(1, b -> PiglinTasks.lookAt(b, e -> true, 8.0))
            .idle(1, 30, 60);
    final Shuffled<EntityPiglinBrute> idleMove = new Shuffled<EntityPiglinBrute>()
            .add(2, b -> PiglinTasks.walkRandomly(b, 0.6F, 10, 7))
            .add(2, b -> PiglinTasks.interactWith(b, EntityPiglin.class, 8.0, 0.6F, 2))
            .add(2, b -> PiglinTasks.interactWith(b, EntityPiglinBrute.class, 8.0, 0.6F, 2))
            .add(2, EntityPiglinBrute::walkTowardsHome)
            .add(2, EntityPiglinBrute::work)
            .idle(1, 30, 60);

    public EntityPiglinBrute(World world) {
        super(world);
        setSize(0.6F, 1.95F);
        this.experienceValue = 20;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35F);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0);
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        setHome(new BlockPos(this));
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        return super.onInitialSpawn(difficulty, data);
    }

    /** 1.16's {@code PiglinBruteBrain.initMemories}: home is where it spawned. */
    public void setHome(BlockPos home) {
        this.memories.set(Mem.HOME, home);
        this.homeDimension = this.world.provider.getDimension();
    }

    @Nullable
    public BlockPos home() {
        return this.memories.get(Mem.HOME);
    }

    @Override
    public boolean canHunt() {
        return false;
    }

    /** Despawns in peaceful, as a 1.16 monster that does not say otherwise. */
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            setDead();
        }
    }

    /** Picks up golden axes only. */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        if (item.getItem().getItem() == Items.GOLDEN_AXE) {
            super.updateEquipmentIfNeeded(item);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Brain (PiglinBruteBrain)
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void sense(List<EntityLivingBase> nearby, List<EntityLivingBase> visible) {
        EntityLivingBase nemesis = null;
        for (EntityLivingBase e : visible) {
            if (e instanceof net.minecraft.entity.monster.EntityWitherSkeleton || e instanceof net.minecraft.entity.boss.EntityWither) {
                nemesis = e;
                break;
            }
        }
        List<AbstractPiglin> adults = new ArrayList<AbstractPiglin>();
        for (EntityLivingBase e : nearby) {
            if (e instanceof AbstractPiglin && ((AbstractPiglin) e).isAdult()) {
                adults.add((AbstractPiglin) e);
            }
        }
        this.memories.set(Mem.NEAREST_VISIBLE_NEMESIS, nemesis);
        this.memories.set(Mem.NEARBY_ADULT_PIGLINS, adults);
    }

    @Override
    protected void tickBrain() {
        Memories m = this.memories;
        // GetAngryTask.
        EntityLivingBase angry = entityByUuid(m.<java.util.UUID>get(Mem.ANGRY_AT));
        if (angry != null && PiglinTasks.isDeadOrDying(angry)) {
            m.erase(Mem.ANGRY_AT);
        }
        boolean fight = getAttackTarget() != null;
        if (fight != this.fighting) {
            this.fighting = fight;
            if (fight) {
                playSound(PiglinContent.BRUTE_ANGRY, 1.0F, getSoundPitch());
            }
        }
        if (fight) {
            fight();
        } else {
            // ForgetAttackTargetTask(findNearestValidAttackTarget).
            EntityLivingBase target = findNearestValidAttackTarget();
            if (target != null && target.isEntityAlive()) {
                setAttackTarget(target);
                m.erase(Mem.CANT_REACH_WALK_TARGET_SINCE);
            }
            this.idleLook.run(this);
            this.idleMove.run(this);
            PiglinTasks.findInteraction(this, 4.0);
        }
        setAggressive(getAttackTarget() != null);
        // maybePlayActivitySound: 1.25% a tick while fighting.
        if (this.fighting && this.world.rand.nextFloat() < 0.0125F) {
            playSound(PiglinContent.BRUTE_ANGRY, 1.0F, getSoundPitch());
        }
    }

    private void fight() {
        EntityLivingBase target = getAttackTarget();
        Long since = this.memories.get(Mem.CANT_REACH_WALK_TARGET_SINCE);
        if (since != null && this.world.getTotalWorldTime() - since > 200L || PiglinTasks.isDeadOrDying(target)
                || target.world != this.world || !isAttackAllowed(target) || findNearestValidAttackTarget() != target) {
            setAttackTarget(null);
            return;
        }
        boolean sees = visibleLiving().contains(target);
        if (isWithinMeleeAttackRange(target) && sees) {
            if (hasWalkTarget()) {
                eraseWalkTarget();
            }
        } else {
            setWalkAndLook(target, 1.0F, 0);
        }
        if (!this.memories.has(Mem.ATTACK_COOLING_DOWN) && sees && isWithinMeleeAttackRange(target)) {
            getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            swingArm(EnumHand.MAIN_HAND);
            attackEntityAsMob(target);
            this.memories.setFor(Mem.ATTACK_COOLING_DOWN, true, 20);
        }
    }

    /** {@code PiglinBruteBrain.findNearestValidAttackTarget}: anger, then a player within 12, then a nemesis. */
    @Nullable
    public EntityLivingBase findNearestValidAttackTarget() {
        EntityLivingBase angry = entityByUuid(this.memories.<java.util.UUID>get(Mem.ANGRY_AT));
        if (angry != null && isAttackAllowed(angry)) {
            return angry;
        }
        EntityPlayer player = this.memories.get(Mem.NEAREST_VISIBLE_TARGETABLE_PLAYER);
        if (player != null && player.getDistance(this) < 12.0) {
            return player;
        }
        return this.memories.get(Mem.NEAREST_VISIBLE_NEMESIS);
    }

    private boolean homeWithin(double range) {
        BlockPos home = home();
        return home != null && this.world.provider.getDimension() == this.homeDimension
                && new Vec3d(home).distanceTo(getPositionVector()) < range;
    }

    /** {@code WalkTowardsPosTask(HOME, 0.6, 2, 100)}: head home from up to 100 blocks, at most every 80 ticks. */
    private boolean walkTowardsHome() {
        if (!homeWithin(100.0)) {
            return false;
        }
        long now = this.world.getTotalWorldTime();
        if (now > this.nextWalkHome) {
            setWalkTarget(home(), 0.6F, 2);
            this.nextWalkHome = now + 80L;
        }
        return true;
    }

    /** {@code WorkTask(HOME, 0.6, 5)}: within 5 of home, stroll somewhere near, at most every 180 ticks. */
    private boolean work() {
        if (!homeWithin(5.0)) {
            return false;
        }
        long now = this.world.getTotalWorldTime();
        if (now > this.nextWork) {
            Vec3d to = RandomPositionGenerator.getLandPos(this, 8, 6);
            if (to != null) {
                setWalkTarget(new BlockPos(to), 0.6F, 1);
            }
            this.nextWork = now + 180L;
        }
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (!this.world.isRemote && hurt && source.getTrueSource() instanceof EntityLivingBase
                && !(source.getTrueSource() instanceof AbstractPiglin)) {
            PiglinTasks.maybeRetaliate(this, (EntityLivingBase) source.getTrueSource());
        }
        return hurt;
    }

    public PiglinAction getArmPose() {
        return isAggressive() && isHoldingMeleeWeapon() ? PiglinAction.ATTACKING_WITH_MELEE_WEAPON : PiglinAction.DEFAULT;
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds, saving
    // ---------------------------------------------------------------------------------------------

    @Override
    public void playLivingSound() {
        if (!this.fighting) {
            super.playLivingSound();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PiglinContent.BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PiglinContent.BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PiglinContent.BRUTE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(PiglinContent.BRUTE_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent convertedSound() {
        return PiglinContent.BRUTE_CONVERTED;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("HomeDimension", this.homeDimension);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.homeDimension = tag.getInteger("HomeDimension");
    }
}
