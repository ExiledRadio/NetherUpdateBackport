package com.exiledradio.netherupdatebackport.strider;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Triggers;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * The strider: 1.16.5's {@code StriderEntity}.
 *
 * <h2>Walking on lava</h2>
 * 1.16 lets a strider stand on lava as if it were ground ({@code canStandOnFluid}: lava's top half is solid to it).
 * 1.12.2 has no such thing, so after every move a strider whose feet are in a lava block with no lava above it is
 * set on the lava's half-height surface, standing ({@code onGround}); fully under lava it rises (1.16's
 * {@code floatStrider}: motion halved, +0.05 up). Standing there it is not "in lava" for movement, so it walks
 * at land speed; lava costs nothing to path through and is where it most likes to be.
 *
 * <h2>Rules</h2>
 * 20 health, speed 0.175; never burns and lava never hurts it; water and rain hurt it. Away from lava it is cold:
 * it shivers, turns purple and moves at 2/3 speed (ridden, 0.23 of its speed instead of 0.55). Panics (1.65) when
 * hurt, follows warped fungus or a warped fungus on a stick (1.4), heads for lava within 8 blocks (1.5), breeds with
 * warped fungus, babies ride or follow adults. Saddled, a player holding a warped fungus on a stick steers it, and
 * the stick boosts it as a carrot on a stick boosts a pig. One spawn in 30 comes saddled with a zombified piglin
 * riding it, one in 10 carries a baby. Drops 2-5 string, and its saddle.
 */
public class EntityStrider extends EntityAnimal {

    private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(EntityStrider.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> COLD = EntityDataManager.createKey(EntityStrider.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> BOOST_TIME = EntityDataManager.createKey(EntityStrider.class, DataSerializers.VARINT);

    public static final ResourceLocation LOOT = Content.id("entities/strider");

    private boolean boosting;
    private int boostTime;
    private int totalBoostTime;

    /** Where a ride on overworld lava began, for "Feels Like Home". */
    @Nullable
    private BlockPos rideStart;

    private EntityAIPanic panic;
    private EntityAITempt tempt;

    public EntityStrider(World world) {
        super(world);
        setSize(0.9F, 1.7F);
        this.isImmuneToFire = true;
        setPathPriority(PathNodeType.LAVA, 0.0F);
        setPathPriority(PathNodeType.DANGER_FIRE, 0.0F);
        setPathPriority(PathNodeType.DAMAGE_FIRE, 0.0F);
        setPathPriority(PathNodeType.WATER, -1.0F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SADDLED, false);
        this.dataManager.register(COLD, false);
        this.dataManager.register(BOOST_TIME, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.175);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
    }

    @Override
    protected void initEntityAI() {
        this.panic = new EntityAIPanic(this, 1.65);
        this.tasks.addTask(1, this.panic);
        this.tasks.addTask(2, new EntityAIMate(this, 1.0));
        this.tempt = new EntityAITempt(this, 1.4, false, StriderContent.temptItems());
        this.tasks.addTask(3, this.tempt);
        this.tasks.addTask(4, new MoveToLava(this, 1.5));
        this.tasks.addTask(5, new EntityAIFollowParent(this, 1.1));
        this.tasks.addTask(7, new EntityAIWander(this, 1.0, 60));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityStrider.class, 8.0F));
    }

    // ---------------------------------------------------------------------------------------------
    // Lava
    // ---------------------------------------------------------------------------------------------

    /** @return true with its feet in the top of a lava surface block: it stands there */
    public boolean standingOnLava() {
        BlockPos feet = new BlockPos(this.posX, this.posY, this.posZ);
        return this.world.getBlockState(feet).getMaterial() == Material.LAVA
                && this.world.getBlockState(feet.up()).getMaterial() != Material.LAVA;
    }

    /** Not "in lava" for movement while standing on it: 1.16 treats the surface as ground. */
    @Override
    public boolean isInLava() {
        return super.isInLava() && !standingOnLava();
    }

    /** 1.16's {@code STRIDER_WARM_BLOCKS} (lava) where it stands or under it, or lava around it. */
    boolean isWarm() {
        BlockPos at = new BlockPos(this);
        return this.world.getBlockState(at).getMaterial() == Material.LAVA
                || this.world.getBlockState(at.down()).getMaterial() == Material.LAVA
                || this.world.isMaterialInBB(getEntityBoundingBox(), Material.LAVA);
    }

    @Override
    public void onUpdate() {
        if (!this.world.isRemote) {
            if (this.tempt != null && this.tempt.isRunning() && this.rand.nextInt(140) == 0) {
                playSound(StriderContent.HAPPY, 1.0F, getSoundPitch());
            } else if (isPanicking() && this.rand.nextInt(60) == 0) {
                playSound(StriderContent.RETREAT, 1.0F, getSoundPitch());
            }
            setCold(!isWarm());
        }
        super.onUpdate();
        floatOnLava();
    }

    private boolean isPanicking() {
        // EntityAIPanic runs while the revenge target is fresh (100 ticks).
        return getRevengeTarget() != null && this.ticksExisted - getRevengeTimer() < 100;
    }

    /** 1.16's {@code floatStrider}, with the surface 1.12.2 cannot give it as ground. */
    void floatOnLava() {
        BlockPos feet = new BlockPos(this.posX, this.posY, this.posZ);
        if (this.world.getBlockState(feet).getMaterial() != Material.LAVA) {
            return;
        }
        if (standingOnLava()) {
            double surface = feet.getY() + 0.5;
            if (this.posY > surface + 1.0E-3) {
                // Still above the surface: let it fall onto it.
                return;
            }
            if (this.posY < surface) {
                setPosition(this.posX, surface, this.posZ);
            }
            if (this.motionY < 0) {
                this.motionY = 0;
            }
            this.onGround = true;
            this.fallDistance = 0;
        } else {
            this.motionX *= 0.5;
            this.motionY = this.motionY * 0.5 + 0.05;
            this.motionZ *= 0.5;
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && isWet()) {
            // 1.16's isSensitiveToWater: water and rain hurt it.
            attackEntityFrom(DamageSource.DROWN, 1.0F);
        }
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        if (this.world.getBlockState(pos).getMaterial() == Material.LAVA) {
            return 10.0F;
        }
        return isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
    }

    /** 1.16's {@code checkStriderSpawnRules}: up through the lava, there must be air. */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos p = new BlockPos(this);
        while (this.world.getBlockState(p).getMaterial() == Material.LAVA) {
            p = p.up();
        }
        return this.world.isAirBlock(p);
    }

    /** Spawned in lava: only blocks get in the way, not the lava (vanilla's check refuses any liquid). */
    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(getEntityBoundingBox(), this)
                && this.world.getCollisionBoxes(this, getEntityBoundingBox()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // Cold, saddle, riding
    // ---------------------------------------------------------------------------------------------

    /** @return true away from lava (1.16's "suffocating"); a rider shares its mount's */
    public boolean isCold() {
        return getRidingEntity() instanceof EntityStrider ? ((EntityStrider) getRidingEntity()).isCold() : this.dataManager.get(COLD);
    }

    void setCold(boolean cold) {
        if (this.dataManager.get(COLD) != cold) {
            this.dataManager.set(COLD, cold);
            ColdSpeed.cold(this, cold);
        }
    }

    public boolean isSaddled() {
        return this.dataManager.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.dataManager.set(SADDLED, saddled);
    }

    public boolean canBeSaddled() {
        return isEntityAlive() && !isChild();
    }

    public void equipSaddle(boolean sound) {
        setSaddled(true);
        if (sound) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, StriderContent.SADDLE, SoundCategory.NEUTRAL, 0.5F, 1.0F);
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        boolean food = isBreedingItem(held);
        if (!food && isSaddled() && !isBeingRidden() && !player.isSneaking()) {
            if (!this.world.isRemote) {
                player.startRiding(this);
            }
            return true;
        }
        if (held.getItem() == Items.SADDLE && canBeSaddled() && !isSaddled()) {
            if (!this.world.isRemote) {
                equipSaddle(true);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return true;
        }
        boolean used = super.processInteract(player, hand);
        if (used && food && !isSilent()) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, StriderContent.EAT, getSoundCategory(), 1.0F,
                    1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
        }
        return used;
    }

    @Override
    public boolean canBeSteered() {
        Entity rider = getControllingPassenger();
        if (!(rider instanceof EntityPlayer)) {
            return false;
        }
        Item stick = StriderContent.stick;
        EntityPlayer player = (EntityPlayer) rider;
        return stick != null && (player.getHeldItemMainhand().getItem() == stick || player.getHeldItemOffhand().getItem() == stick);
    }

    @Override
    @Nullable
    public Entity getControllingPassenger() {
        return getPassengers().isEmpty() ? null : getPassengers().get(0);
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().isEmpty() && !isInsideOfMaterial(Material.LAVA);
    }

    @Override
    public double getMountedYOffset() {
        float swing = Math.min(0.25F, this.limbSwingAmount);
        return this.height - 0.19 + 0.12F * MathHelper.cos(this.limbSwing * 1.5F) * 2.0F * swing;
    }

    /** As a carrot on a stick boosts a pig. @return true if it started a boost */
    public boolean boost() {
        if (this.boosting) {
            return false;
        }
        this.boosting = true;
        this.boostTime = 0;
        this.totalBoostTime = this.rand.nextInt(841) + 140;
        this.dataManager.set(BOOST_TIME, this.totalBoostTime);
        return true;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (BOOST_TIME.equals(key) && this.world.isRemote) {
            this.boosting = true;
            this.boostTime = 0;
            this.totalBoostTime = this.dataManager.get(BOOST_TIME);
        }
        super.notifyDataManagerChange(key);
    }

    /** EntityPig's riding travel, with the strider's speeds: ridden at 0.55 of its speed (0.23 when cold). */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        Entity rider = getControllingPassenger();
        if (isBeingRidden() && canBeSteered()) {
            this.rotationYaw = rider.rotationYaw;
            this.prevRotationYaw = this.rotationYaw;
            this.rotationPitch = rider.rotationPitch * 0.5F;
            setRotation(this.rotationYaw, this.rotationPitch);
            this.renderYawOffset = this.rotationYaw;
            this.rotationYawHead = this.rotationYaw;
            this.stepHeight = 1.0F;
            this.jumpMovementFactor = getAIMoveSpeed() * 0.1F;
            if (this.boosting && this.boostTime++ > this.totalBoostTime) {
                this.boosting = false;
            }
            if (canPassengerSteer()) {
                float speed = (float) getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() * (isCold() ? 0.23F : 0.55F);
                if (this.boosting) {
                    speed += speed * 1.15F * MathHelper.sin((float) this.boostTime / (float) this.totalBoostTime * (float) Math.PI);
                }
                setAIMoveSpeed(speed);
                super.travel(0.0F, 0.0F, 1.0F);
            } else {
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
            }
            this.prevLimbSwingAmount = this.limbSwingAmount;
            double dx = this.posX - this.prevPosX;
            double dz = this.posZ - this.prevPosZ;
            float f = Math.min(1.0F, MathHelper.sqrt(dx * dx + dz * dz) * 4.0F);
            this.limbSwingAmount += (f - this.limbSwingAmount) * 0.4F;
            this.limbSwing += this.limbSwingAmount;
            trackOverworldRide(rider);
        } else {
            this.stepHeight = 0.5F;
            this.jumpMovementFactor = 0.02F;
            super.travel(strafe, vertical, forward);
            this.rideStart = null;
        }
    }

    /** "Feels Like Home": a ride of 50 blocks on lava in the overworld. */
    private void trackOverworldRide(Entity rider) {
        if (this.world.isRemote || !(rider instanceof EntityPlayerMP)) {
            return;
        }
        if (this.world.provider.getDimensionType() != DimensionType.OVERWORLD || !standingOnLava()) {
            this.rideStart = null;
            return;
        }
        BlockPos here = new BlockPos(this);
        if (this.rideStart == null) {
            this.rideStart = here;
        } else if (this.rideStart.distanceSq(here) >= 50 * 50) {
            Triggers.fire((EntityPlayerMP) rider, "ride_strider_in_overworld_lava");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Spawning, breeding, drops, sounds
    // ---------------------------------------------------------------------------------------------

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (isChild()) {
            return data;
        }
        if (this.rand.nextInt(30) == 0) {
            EntityPigZombie rider = new EntityPigZombie(this.world);
            rider.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, 0.0F);
            rider.onInitialSpawn(difficulty, null);
            if (StriderContent.stick != null) {
                rider.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(StriderContent.stick));
            }
            this.world.spawnEntity(rider);
            rider.startRiding(this, true);
            equipSaddle(false);
        } else if (this.rand.nextInt(10) == 0) {
            EntityStrider baby = new EntityStrider(this.world);
            baby.setGrowingAge(-24000);
            baby.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, 0.0F);
            this.world.spawnEntity(baby);
            baby.startRiding(this, true);
        }
        return data;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item fungus = Owners.ours("warped_fungus");
        return fungus != null && stack.getItem() == fungus;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityStrider(this.world);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int looting) {
        super.dropEquipment(wasRecentlyHit, looting);
        if (isSaddled()) {
            dropItem(Items.SADDLE, 1);
        }
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return isPanicking() || this.tempt != null && this.tempt.isRunning() ? null : StriderContent.AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return StriderContent.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return StriderContent.DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(standingOnLava() ? StriderContent.STEP_LAVA : StriderContent.STEP, 1.0F, 1.0F);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("Saddle", isSaddled());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setSaddled(tag.getBoolean("Saddle"));
    }

    /** 1.16's {@code MoveToLavaGoal}: out of lava, walk to lava within 8 blocks (2 up or down) with air above. */
    static final class MoveToLava extends EntityAIMoveToBlock {
        private final EntityStrider strider;

        MoveToLava(EntityStrider strider, double speed) {
            super(strider, speed, 8);
            this.strider = strider;
        }

        @Override
        public boolean shouldExecute() {
            return !this.strider.standingOnLava() && !this.strider.isInLava() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.strider.standingOnLava() && super.shouldContinueExecuting();
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.getBlockState(pos).getMaterial() == Material.LAVA && world.isAirBlock(pos.up());
        }
    }

    /** The cold slowdown as a speed modifier (1.16 multiplies its speed by 0.66 while cold). */
    static final class ColdSpeed {
        private static final java.util.UUID COLD = java.util.UUID.fromString("2b6f0a4c-5f6a-4b21-9a0e-6d2f1a7e3c11");

        private ColdSpeed() {
        }

        static void cold(EntityLivingBase mob, boolean cold) {
            net.minecraft.entity.ai.attributes.IAttributeInstance speed = mob.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            if (speed.getModifier(COLD) != null) {
                speed.removeModifier(COLD);
            }
            if (cold) {
                speed.applyModifier(new net.minecraft.entity.ai.attributes.AttributeModifier(COLD, "Strider cold", -0.34, 2).setSaved(false));
            }
        }
    }
}
