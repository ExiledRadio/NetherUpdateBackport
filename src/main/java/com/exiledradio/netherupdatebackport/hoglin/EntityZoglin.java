package com.exiledradio.netherupdatebackport.hoglin;

import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The zoglin: a zombified hoglin. 1.16.5's {@code ZoglinEntity}.
 *
 * <p>40 health, speed 0.3, knockback resistance 0.6, attack 6 (a baby, from a baby hoglin, is half the size and
 * bites for 0.5); undead; attacks every living thing it can see within 16 blocks but
 * creepers and other zoglins (and players in creative or spectator), every 40 ticks, throwing what it hits like
 * a hoglin. Does not burn in daylight. Drops rotten flesh.
 */
public class EntityZoglin extends EntityMob {

    private static final DataParameter<Boolean> BABY = EntityDataManager.createKey(EntityZoglin.class, DataSerializers.BOOLEAN);

    public static final ResourceLocation LOOT = Content.id("entities/zoglin");

    private int attackAnimation;

    public EntityZoglin(World world) {
        super(world);
        setSize(1.3964844F, 1.4F);
        this.experienceValue = 5;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BABY, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.0, true) {
            @Override
            protected void checkAndPerformAttack(EntityLivingBase target, double distance) {
                if (distance <= getAttackReachSqr(target) && this.attackTick <= 0) {
                    this.attackTick = 40;
                    this.attacker.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                    this.attacker.attackEntityAsMob(target);
                }
            }
        });
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.4));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityLivingBase>(this, EntityLivingBase.class, 10, true, false,
                EntityZoglin::isTargetable));
    }

    /** 1.16's {@code isTargetable}: anything living but creepers and zoglins (and players it may not hurt). */
    static boolean isTargetable(EntityLivingBase e) {
        if (e == null || e instanceof EntityZoglin || e instanceof EntityCreeper) {
            return false;
        }
        return !(e instanceof EntityPlayer) || !((EntityPlayer) e).isCreative() && !((EntityPlayer) e).isSpectator();
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (!(target instanceof EntityLivingBase)) {
            return false;
        }
        this.attackAnimation = 10;
        this.world.setEntityState(this, (byte) 4);
        playSound(HoglinContent.ZOGLIN_ATTACK, 1.0F, getSoundPitch());
        return Flinging.hurtAndThrow(this, (EntityLivingBase) target, !isChild());
    }

    @Override
    public void onLivingUpdate() {
        if (this.attackAnimation > 0) {
            this.attackAnimation--;
        }
        super.onLivingUpdate();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == 4) {
            this.attackAnimation = 10;
            playSound(HoglinContent.ZOGLIN_ATTACK, 1.0F, getSoundPitch());
        } else {
            super.handleStatusUpdate(id);
        }
    }

    @SideOnly(Side.CLIENT)
    public int attackAnimation() {
        return this.attackAnimation;
    }

    @Override
    public boolean isChild() {
        return this.dataManager.get(BABY);
    }

    public void setChild(boolean baby) {
        this.dataManager.set(BABY, baby);
        setSize(baby ? 1.3964844F * 0.5F : 1.3964844F, baby ? 0.7F : 1.4F);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(baby ? 0.5 : 6.0);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (BABY.equals(key)) {
            setSize(isChild() ? 1.3964844F * 0.5F : 1.3964844F, isChild() ? 0.7F : 1.4F);
        }
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return isChild() ? 3 : this.experienceValue;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !getLeashed();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getAttackTarget() != null ? HoglinContent.ZOGLIN_ANGRY : HoglinContent.ZOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HoglinContent.ZOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HoglinContent.ZOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(HoglinContent.ZOGLIN_STEP, 0.15F, 1.0F);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (isChild()) {
            tag.setBoolean("IsBaby", true);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.getBoolean("IsBaby")) {
            setChild(true);
        }
    }
}
