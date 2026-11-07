package com.exiledradio.netherupdatebackport.hoglin;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The hoglin: 1.16.5's {@code HoglinEntity} and {@code HoglinTasks}, with the brain's activities as 1.12.2 AI
 * tasks.
 *
 * <ul>
 *   <li>40 health, speed 0.3, knockback resistance 0.6, attack 6 (a baby's 0.5); 5 experience (3 for a baby);
 *       one in five spawns is a baby.</li>
 *   <li><b>Fights</b> the nearest player it can see (16 blocks), hits every 40 ticks and throws them
 *       ({@link Flinging}); hurt, it fights back and so do the adult hoglins that can see it.</li>
 *   <li><b>Repellents</b>: within 8 blocks (4 up or down) of a warped fungus, a respawn anchor or a nether
 *       portal it will not pick a fight and walks away from it.</li>
 *   <li><b>Retreats</b> for 5-20 s from piglins when they outnumber the adult hoglins about, and a baby retreats
 *       from whatever hurt it.</li>
 *   <li>Breeds with crimson fungus; babies follow adults; can be leashed; despawns like a monster unless kept.</li>
 *   <li>Outside the nether it shakes and after 15 s becomes a zoglin, with 10 s of Nausea.</li>
 *   <li>Drops porkchops (cooked if burning) and leather.</li>
 * </ul>
 */
public class EntityHoglin extends EntityAnimal implements IMob {

    private static final DataParameter<Boolean> IMMUNE_TO_ZOMBIFICATION = EntityDataManager.createKey(EntityHoglin.class, DataSerializers.BOOLEAN);

    public static final ResourceLocation LOOT = Content.id("entities/hoglin");

    private int attackAnimation;
    private int timeInOverworld;
    private boolean cannotBeHunted;

    /** What it flees, and until when (1.16's AVOID_TARGET memory). */
    @Nullable
    EntityLivingBase avoid;
    long avoidUntil;

    public EntityHoglin(World world) {
        super(world);
        setSize(1.3964844F, 1.4F);
        this.experienceValue = 5;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(IMMUNE_TO_ZOMBIFICATION, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AvoidRepellent(this));
        this.tasks.addTask(2, new Retreat(this));
        this.tasks.addTask(3, new Attack(this));
        this.tasks.addTask(4, new EntityAIMate(this, 1.0));
        this.tasks.addTask(5, new EntityAIFollowParent(this, 0.6));
        this.tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.4));
        this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, 10, true, false,
                p -> p != null && !p.isSpectator() && !p.isCreative()) {
            @Override
            public boolean shouldExecute() {
                return !isPacified() && !isInLove() && avoid == null && super.shouldExecute();
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 1.16's rules
    // ---------------------------------------------------------------------------------------------

    @Nullable
    private BlockPos repellent;
    private int repellentCheck;

    /** @return the nearest repellent within 8 blocks across and 4 up or down, refreshed once a second */
    @Nullable
    public BlockPos nearestRepellent() {
        if (this.repellentCheck-- <= 0) {
            this.repellentCheck = 20;
            this.repellent = findRepellent(this.world, new BlockPos(this));
        }
        return this.repellent;
    }

    @Nullable
    public static BlockPos findRepellent(World world, BlockPos at) {
        Block fungus = Owners.ourBlock("warped_fungus");
        Block anchor = Owners.ourBlock("respawn_anchor");
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-8, -4, -8), at.add(8, 4, 8))) {
            Block b = world.getBlockState(p).getBlock();
            if (b == Blocks.PORTAL || b == fungus && fungus != null || b == anchor && anchor != null) {
                double d = p.distanceSq(at);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.toImmutable();
                }
            }
        }
        return best;
    }

    /** 1.16's PACIFIED memory: set while a repellent is near. */
    public boolean isPacified() {
        return nearestRepellent() != null;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (!(target instanceof EntityLivingBase)) {
            return false;
        }
        this.attackAnimation = 10;
        this.world.setEntityState(this, (byte) 4);
        playSound(HoglinContent.HOGLIN_ATTACK, 1.0F, getSoundPitch());
        onHitTarget((EntityLivingBase) target);
        return Flinging.hurtAndThrow(this, (EntityLivingBase) target, !isChild());
    }

    /** Hit a piglin while piglins outnumber the hoglins: back off, and the other hoglins with it. */
    void onHitTarget(EntityLivingBase target) {
        if (isChild()) {
            return;
        }
        if (isPiglin(target) && piglinsOutnumberHoglins()) {
            setAvoid(target);
            for (EntityHoglin other : visibleAdultHoglins()) {
                other.setAvoid(target);
            }
        } else {
            broadcastAttack(target);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (!this.world.isRemote && hurt && source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase by = (EntityLivingBase) source.getTrueSource();
            this.resetInLove();
            if (isChild()) {
                setAvoid(by);
            } else if (!(this.avoid != null && isPiglin(by)) && !(by instanceof EntityHoglin)
                    && !(by instanceof EntityPlayer && (((EntityPlayer) by).isCreative() || ((EntityPlayer) by).isSpectator()))) {
                setAttackTarget(by);
                broadcastAttack(by);
            }
        }
        return hurt;
    }

    void broadcastAttack(EntityLivingBase target) {
        for (EntityHoglin other : visibleAdultHoglins()) {
            if (!other.isPacified() && (other.getAttackTarget() == null
                    || other.getDistanceSq(target) < other.getDistanceSq(other.getAttackTarget()))) {
                other.setAttackTarget(target);
            }
        }
    }

    void setAvoid(EntityLivingBase target) {
        setAttackTarget(null);
        getNavigator().clearPath();
        this.avoid = target;
        // 1.16's RETREAT_DURATION: 5 to 20 seconds.
        this.avoidUntil = this.world.getTotalWorldTime() + 100 + this.rand.nextInt(301);
    }

    List<EntityHoglin> visibleAdultHoglins() {
        List<EntityHoglin> list = this.world.getEntitiesWithinAABB(EntityHoglin.class, getEntityBoundingBox().grow(16.0),
                h -> h != this && !h.isChild() && canEntityBeSeen(h));
        return list;
    }

    /** @return true when the adult piglins it can see outnumber the adult hoglins it can see plus itself */
    boolean piglinsOutnumberHoglins() {
        if (isChild()) {
            return false;
        }
        int piglins = this.world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(16.0),
                e -> isPiglin(e) && !e.isChild() && canEntityBeSeen(e)).size();
        return piglins > visibleAdultHoglins().size() + 1;
    }

    /** @return true for this mod's piglin, by registry name (stream P's) */
    static boolean isPiglin(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity);
        return key != null && key.equals(Content.id("piglin"));
    }

    // ---------------------------------------------------------------------------------------------
    // Ticking, zombification
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onLivingUpdate() {
        if (this.attackAnimation > 0) {
            this.attackAnimation--;
        }
        super.onLivingUpdate();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (this.avoid != null && (this.world.getTotalWorldTime() > this.avoidUntil || this.avoid.isDead
                || !isChild() && !piglinsOutnumberHoglins() && isPiglin(this.avoid))) {
            this.avoid = null;
        }
        if (isConverting()) {
            if (++this.timeInOverworld > 300) {
                playSound(HoglinContent.HOGLIN_CONVERTED, 1.0F, 1.0F);
                convert();
            }
        } else {
            this.timeInOverworld = 0;
        }
    }

    /** @return true outside the nether (1.16's dimension types that are not piglin-safe), unless immune or AI-less */
    public boolean isConverting() {
        return this.world.provider.getDimensionType() != DimensionType.NETHER && !isImmuneToZombification() && !isAIDisabled();
    }

    public boolean isImmuneToZombification() {
        return this.dataManager.get(IMMUNE_TO_ZOMBIFICATION);
    }

    public void setImmuneToZombification(boolean immune) {
        this.dataManager.set(IMMUNE_TO_ZOMBIFICATION, immune);
    }

    /** Becomes a zoglin with this one's age, name and position; 10 s of Nausea. */
    public EntityZoglin convert() {
        EntityZoglin zoglin = new EntityZoglin(this.world);
        zoglin.copyLocationAndAnglesFrom(this);
        zoglin.setChild(isChild());
        zoglin.setNoAI(isAIDisabled());
        if (hasCustomName()) {
            zoglin.setCustomNameTag(getCustomNameTag());
            zoglin.setAlwaysRenderNameTag(getAlwaysRenderNameTag());
        }
        if (isNoDespawnRequired()) {
            zoglin.enablePersistence();
        }
        zoglin.setInvisible(isInvisible());
        zoglin.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
        this.world.spawnEntity(zoglin);
        setDead();
        return zoglin;
    }

    public int timeInOverworld() {
        return this.timeInOverworld;
    }

    /** @return false once marked as never to be hunted (1.16: hoglins of a hunt's own group) */
    public boolean canBeHunted() {
        return !isChild() && !this.cannotBeHunted;
    }

    public void setCannotBeHunted(boolean value) {
        this.cannotBeHunted = value;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == 4) {
            this.attackAnimation = 10;
            playSound(HoglinContent.HOGLIN_ATTACK, 1.0F, getSoundPitch());
        } else {
            super.handleStatusUpdate(id);
        }
    }

    @SideOnly(Side.CLIENT)
    public int attackAnimation() {
        return this.attackAnimation;
    }

    // ---------------------------------------------------------------------------------------------
    // Breeding, age, drops
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item fungus = Owners.ours("crimson_fungus");
        return fungus != null && stack.getItem() == fungus;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityHoglin child = new EntityHoglin(this.world);
        child.enablePersistence();
        return child;
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        return other != this && other instanceof EntityHoglin && isInLove() && other.isInLove();
    }

    @Override
    public void setGrowingAge(int age) {
        super.setGrowingAge(age);
        // 1.16's ageBoundaryReached: a baby's bite is 0.5.
        if (getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
            getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(isChild() ? 0.5 : 6.0);
        }
        this.experienceValue = isChild() ? 3 : 5;
    }

    @Override
    public net.minecraft.entity.IEntityLivingData onInitialSpawn(net.minecraft.world.DifficultyInstance difficulty,
                                                                 @Nullable net.minecraft.entity.IEntityLivingData data) {
        if (this.world.rand.nextFloat() < 0.2F) {
            setGrowingAge(-24000);
        }
        return super.onInitialSpawn(difficulty, data);
    }

    /** 1.16's {@code checkHoglinSpawnRules}: anywhere but on nether wart blocks. */
    @Override
    public boolean getCanSpawnHere() {
        return this.world.getBlockState(new BlockPos(this).down()).getBlock() != Blocks.NETHER_WART_BLOCK
                && this.world.getCollisionBoxes(this, getEntityBoundingBox()).isEmpty() && !this.world.containsAnyLiquid(getEntityBoundingBox());
    }

    @Override
    protected boolean canDespawn() {
        return true;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !getLeashed();
    }

    @Override
    public double getMountedYOffset() {
        return this.height - (isChild() ? 0.2 : 0.15);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.experienceValue;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        BlockPos rep = this.repellent;
        if (rep != null && rep.distanceSq(pos) < 64.0) {
            return -1.0F;
        }
        Block nylium = Owners.ourBlock("crimson_nylium");
        return nylium != null && this.world.getBlockState(pos.down()).getBlock() == nylium ? 10.0F : 0.0F;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (isImmuneToZombification()) {
            tag.setBoolean("IsImmuneToZombification", true);
        }
        tag.setInteger("TimeInOverworld", this.timeInOverworld);
        if (this.cannotBeHunted) {
            tag.setBoolean("CannotBeHunted", true);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setImmuneToZombification(tag.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = tag.getInteger("TimeInOverworld");
        this.cannotBeHunted = tag.getBoolean("CannotBeHunted");
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds (1.16: retreat sound while avoiding or converting, angry while fighting)
    // ---------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.avoid != null || isConverting()) {
            return HoglinContent.HOGLIN_RETREAT;
        }
        if (getAttackTarget() != null) {
            return HoglinContent.HOGLIN_ANGRY;
        }
        return this.repellent != null ? HoglinContent.HOGLIN_RETREAT : HoglinContent.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HoglinContent.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HoglinContent.HOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(HoglinContent.HOGLIN_STEP, 0.15F, 1.0F);
    }

    // ---------------------------------------------------------------------------------------------
    // AI
    // ---------------------------------------------------------------------------------------------

    /** Hits every 40 ticks (1.16's {@code MeleeAttackTask(40)}), never while pacified or retreating. */
    static final class Attack extends EntityAIAttackMelee {
        private final EntityHoglin hoglin;

        Attack(EntityHoglin hoglin) {
            super(hoglin, 1.0, true);
            this.hoglin = hoglin;
        }

        @Override
        public boolean shouldExecute() {
            return this.hoglin.avoid == null && super.shouldExecute();
        }

        @Override
        protected void checkAndPerformAttack(EntityLivingBase target, double distance) {
            if (distance <= getAttackReachSqr(target) && this.attackTick <= 0) {
                this.attackTick = 40;
                this.attacker.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                this.attacker.attackEntityAsMob(target);
            }
        }
    }

    /** Walk away from a repellent within 8 blocks (1.16's {@code RunAwayTask.pos(NEAREST_REPELLENT)}). */
    static final class AvoidRepellent extends EntityAIBase {
        private final EntityHoglin hoglin;

        AvoidRepellent(EntityHoglin hoglin) {
            this.hoglin = hoglin;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            BlockPos rep = this.hoglin.nearestRepellent();
            if (rep == null) {
                return false;
            }
            if (this.hoglin.getAttackTarget() != null && this.hoglin.rand.nextInt(200) == 0) {
                // 1.16's RandomlyStopAttackingTask(NEAREST_REPELLENT, 200).
                this.hoglin.setAttackTarget(null);
            }
            return this.hoglin.getNavigator().noPath() && runFrom(this.hoglin, new Vec3d(rep), 1.0);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.hoglin.getNavigator().noPath();
        }
    }

    /** Flee from the avoid target at 1.3x for the retreat's duration. */
    static final class Retreat extends EntityAIBase {
        private final EntityHoglin hoglin;

        Retreat(EntityHoglin hoglin) {
            this.hoglin = hoglin;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return this.hoglin.avoid != null && runFrom(this.hoglin, this.hoglin.avoid.getPositionVector(), 1.3);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.hoglin.avoid != null && !this.hoglin.getNavigator().noPath();
        }
    }

    /** Paths to a spot up to 15 blocks away, further from the danger. @return true if a path was set */
    static boolean runFrom(EntityCreature mob, Vec3d danger, double speed) {
        Vec3d to = RandomPositionGenerator.findRandomTargetBlockAwayFrom(mob, 15, 7, danger);
        return to != null && mob.getDistanceSq(to.x, to.y, to.z) > 1.0 && mob.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, speed);
    }
}
