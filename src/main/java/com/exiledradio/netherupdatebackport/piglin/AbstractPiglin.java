package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.piglin.Memories.Mem;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 1.16.5's {@code AbstractPiglinEntity}, the base of piglins and piglin brutes, with the parts of 1.16's brain
 * both use.
 *
 * <h2>The brain</h2>
 * 1.12.2 has no brains, so this is the brain's machinery, run from the entity's own server tick
 * ({@link #updateAITasks}) rather than from AI goals: {@link Memories} with their expiry; the shared sensors (living
 * entities and players within 16 blocks, seen or not, every 20 ticks); and 1.16's {@code WalkToTargetTask} and
 * {@code LookTask}, which carry out the walk and look targets the behaviors choose. The behaviors themselves are the
 * subclasses' ({@code PiglinTasks}, {@code PiglinBruteBrain}). The 1.12.2 goal list holds only what every 1.16 mob
 * does outside its brain: floating in water, and 1.16's {@code InteractWithDoorTask} as the door goal.
 *
 * <h2>Zombification</h2>
 * Outside the nether (not when immune or AI-less) it shakes, and after 300 ticks becomes vanilla's zombie pigman
 * (1.16's zombified piglin, which stream Z restyles), keeping its equipment, age, name and persistence, with
 * 10 s of Nausea. 1.12.2 has no conversion event for other mods to cancel.
 */
public abstract class AbstractPiglin extends EntityCreature implements IMob {

    private static final DataParameter<Boolean> IMMUNE_TO_ZOMBIFICATION = EntityDataManager.createKey(AbstractPiglin.class, DataSerializers.BOOLEAN);
    /** 1.16's {@code MobEntity.isAggressive}, which 1.12.2 lacks: set while it has an attack target. */
    private static final DataParameter<Boolean> AGGRESSIVE = EntityDataManager.createKey(AbstractPiglin.class, DataSerializers.BOOLEAN);

    public final Memories memories = new Memories();

    protected int timeInOverworld;

    /** 1.16's brain sensors scan every 20 ticks, each starting at a random point. */
    private int livingScan;
    private int playerScan;

    @Nullable
    private List<EntityLivingBase> visibleLiving;
    @Nullable
    private List<EntityLivingBase> nearbyLiving;

    public AbstractPiglin(World world) {
        super(world);
        setCanPickUpLoot(true);
        if (getNavigator() instanceof PathNavigateGround) {
            ((PathNavigateGround) getNavigator()).setBreakDoors(true);
        }
        setPathPriority(PathNodeType.DANGER_FIRE, 16.0F);
        setPathPriority(PathNodeType.DAMAGE_FIRE, -1.0F);
        this.livingScan = this.rand.nextInt(20);
        this.playerScan = this.rand.nextInt(20);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(IMMUNE_TO_ZOMBIFICATION, false);
        this.dataManager.register(AGGRESSIVE, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        // 1.16's MonsterEntity follow range.
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIOpenDoor(this, true));
    }

    protected abstract boolean canHunt();

    /** @return the zombified piglin's conversion sound for this kind */
    protected abstract SoundEvent convertedSound();

    /** The brain's tick: 1.16's {@code Brain.tick}, then the subclass's {@code updateActivity}. */
    protected abstract void tickBrain();

    public boolean isAdult() {
        return !isChild();
    }

    public void setImmuneToZombification(boolean immune) {
        this.dataManager.set(IMMUNE_TO_ZOMBIFICATION, immune);
    }

    public boolean isImmuneToZombification() {
        return this.dataManager.get(IMMUNE_TO_ZOMBIFICATION);
    }

    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE);
    }

    protected void setAggressive(boolean aggressive) {
        this.dataManager.set(AGGRESSIVE, aggressive);
    }

    // ---------------------------------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        this.memories.tick();
        if (--this.livingScan <= 0) {
            this.livingScan = 20;
            scanLiving();
        }
        if (--this.playerScan <= 0) {
            this.playerScan = 20;
            scanPlayers();
        }
        tickBrain();
        tickWalk();
        tickLook();

        if (isConverting()) {
            this.timeInOverworld++;
        } else {
            this.timeInOverworld = 0;
        }
        if (this.timeInOverworld > 300) {
            playSound(convertedSound(), getSoundVolume(), getSoundPitch());
            finishConversion();
        }
    }

    /** @return true outside the nether (1.16's dimensions that are not piglin-safe), unless immune or AI-less */
    public boolean isConverting() {
        return this.world.provider.getDimensionType() != DimensionType.NETHER && !isImmuneToZombification() && !isAIDisabled();
    }

    public int timeInOverworld() {
        return this.timeInOverworld;
    }

    /** 1.16's {@code convertTo(ZOMBIFIED_PIGLIN, true)}: same place, equipment, age, name; 10 s of Nausea. */
    protected EntityPigZombie finishConversion() {
        EntityPigZombie zombie = new EntityPigZombie(this.world);
        zombie.copyLocationAndAnglesFrom(this);
        zombie.setChild(isChild());
        zombie.setNoAI(isAIDisabled());
        if (hasCustomName()) {
            zombie.setCustomNameTag(getCustomNameTag());
            zombie.setAlwaysRenderNameTag(getAlwaysRenderNameTag());
        }
        if (isNoDespawnRequired()) {
            zombie.enablePersistence();
        }
        zombie.setInvisible(isInvisible());
        zombie.setEntityInvulnerable(getIsInvulnerable());
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = getItemStackFromSlot(slot);
            if (!stack.isEmpty()) {
                zombie.setItemStackToSlot(slot, stack.copy());
                zombie.setDropChance(slot, dropChance(slot));
                setItemStackToSlot(slot, ItemStack.EMPTY);
            }
        }
        zombie.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
        this.world.spawnEntity(zombie);
        setDead();
        return zombie;
    }

    /** @return the drop chance 1.12.2 keeps for a slot (2 is a guaranteed drop) */
    public float dropChance(EntityEquipmentSlot slot) {
        return slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? this.inventoryHandsDropChances[slot.getIndex()]
                : this.inventoryArmorDropChances[slot.getIndex()];
    }

    /** 1.16's {@code setGuaranteedDrop}. */
    public void setGuaranteedDrop(EntityEquipmentSlot slot) {
        setDropChance(slot, 2.0F);
    }

    /** 1.16's {@code setItemSlotAndDropWhenKilled}. */
    protected void setItemSlotAndDropWhenKilled(EntityEquipmentSlot slot, ItemStack stack) {
        setItemStackToSlot(slot, stack);
        setGuaranteedDrop(slot);
        enablePersistence();
    }

    // ---------------------------------------------------------------------------------------------
    // Sensors shared by both kinds (1.16's NEAREST_LIVING_ENTITIES, NEAREST_PLAYERS, HURT_BY)
    // ---------------------------------------------------------------------------------------------

    private void scanLiving() {
        List<EntityLivingBase> all = this.world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(16.0),
                e -> e != this && e.isEntityAlive());
        Collections.sort(all, (a, b) -> Double.compare(getDistanceSq(a), getDistanceSq(b)));
        List<EntityLivingBase> visible = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase e : all) {
            if (isTargetable(e)) {
                visible.add(e);
            }
        }
        this.nearbyLiving = all;
        this.visibleLiving = visible;
        this.memories.set(Mem.VISIBLE_LIVING_ENTITIES, visible);
        sense(all, visible);
    }

    /** The subclass's own sensor, run right after the living-entities scan with its results. */
    protected abstract void sense(List<EntityLivingBase> nearby, List<EntityLivingBase> visible);

    private void scanPlayers() {
        EntityPlayer nearestVisible = null;
        EntityPlayer nearestTargetable = null;
        List<EntityPlayer> players = new ArrayList<EntityPlayer>(this.world.playerEntities);
        Collections.sort(players, (a, b) -> Double.compare(getDistanceSq(a), getDistanceSq(b)));
        for (EntityPlayer p : players) {
            if (p.isSpectator() || getDistanceSq(p) >= 256.0 || !isTargetable(p)) {
                continue;
            }
            if (nearestVisible == null) {
                nearestVisible = p;
            }
            if (nearestTargetable == null && isAttackAllowed(p)) {
                nearestTargetable = p;
            }
        }
        this.memories.set(Mem.NEAREST_VISIBLE_PLAYER, nearestVisible);
        this.memories.set(Mem.NEAREST_VISIBLE_TARGETABLE_PLAYER, nearestTargetable);
    }

    /** @return the living entities seen at the last scan (1.16's VISIBLE_LIVING_ENTITIES) */
    public List<EntityLivingBase> visibleLiving() {
        return this.visibleLiving == null ? Collections.<EntityLivingBase>emptyList() : this.visibleLiving;
    }

    /**
     * 1.16's {@code Sensor.isEntityTargetable}: within 16 blocks (less for a sneaking or invisible one, unless it is
     * the attack target) and in line of sight.
     */
    public boolean isTargetable(EntityLivingBase e) {
        if (e == this || !e.isEntityAlive() || e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()) {
            return false;
        }
        double range = 16.0;
        if (e != getAttackTarget()) {
            range *= visibility(e);
        }
        return getDistanceSq(e) <= range * range && canEntityBeSeen(e);
    }

    /** 1.16's {@code LivingEntity.getVisibilityPercent}, without mob heads. */
    static double visibility(EntityLivingBase e) {
        double d = 1.0;
        if (e.isSneaking()) {
            d *= 0.8;
        }
        if (e.isInvisible()) {
            float cover = 0.0F;
            int pieces = 0;
            for (ItemStack armor : e.getArmorInventoryList()) {
                pieces++;
                if (!armor.isEmpty()) {
                    cover++;
                }
            }
            float f = pieces == 0 ? 0.0F : cover / pieces;
            d *= 0.7 * Math.max(0.1F, f);
        }
        return d;
    }

    /** 1.16's {@code EntityPredicates.ATTACK_ALLOWED}: anything but creative or spectator players, or any player in peaceful. */
    public static boolean isAttackAllowed(@Nullable EntityLivingBase e) {
        if (!(e instanceof EntityPlayer)) {
            return e != null;
        }
        EntityPlayer p = (EntityPlayer) e;
        return !p.isSpectator() && !p.isCreative() && p.world.getDifficulty() != EnumDifficulty.PEACEFUL;
    }

    @Nullable
    public EntityLivingBase entityByUuid(@Nullable UUID id) {
        if (id == null || !(this.world instanceof net.minecraft.world.WorldServer)) {
            return null;
        }
        Entity e = ((net.minecraft.world.WorldServer) this.world).getEntityFromUuid(id);
        return e instanceof EntityLivingBase ? (EntityLivingBase) e : null;
    }

    // ---------------------------------------------------------------------------------------------
    // Walk and look targets (1.16's WALK_TARGET / LOOK_TARGET and the core tasks that carry them out)
    // ---------------------------------------------------------------------------------------------

    @Nullable
    private Object walkTarget;
    private float walkSpeed;
    private int walkClose;
    @Nullable
    private BlockPos walkLastPos;
    private int walkCooldown;

    @Nullable
    private Object lookTarget;
    private int lookTicks;

    /** 1.16's {@code BrainUtil.setWalkAndLookTargetMemories} for an entity. */
    public void setWalkAndLook(Entity target, float speed, int closeEnough) {
        setWalkTarget(target, speed, closeEnough);
        setLookTarget(target);
    }

    /** As {@link #setWalkAndLook(Entity, float, int)} for a block position. */
    public void setWalkAndLook(BlockPos target, float speed, int closeEnough) {
        setWalkTarget(target, speed, closeEnough);
        setLookTarget(new Vec3d(target).add(0.5, 0.5, 0.5));
    }

    /** @param target an entity or a {@link BlockPos} */
    public void setWalkTarget(Object target, float speed, int closeEnough) {
        if (!target.equals(this.walkTarget)) {
            this.walkLastPos = null;
        }
        this.walkTarget = target;
        this.walkSpeed = speed;
        this.walkClose = closeEnough;
    }

    public boolean hasWalkTarget() {
        return this.walkTarget != null;
    }

    public void eraseWalkTarget() {
        this.walkTarget = null;
        this.walkLastPos = null;
        getNavigator().clearPath();
    }

    /** @param target an entity or a {@link Vec3d} */
    public void setLookTarget(Object target) {
        if (!target.equals(this.lookTarget)) {
            // 1.16's LookTask(45, 90): one look lasts 45 to 90 ticks.
            this.lookTicks = 45 + this.rand.nextInt(46);
        }
        this.lookTarget = target;
    }

    public boolean hasLookTarget() {
        return this.lookTarget != null;
    }

    @Nullable
    public Object lookTarget() {
        return this.lookTarget;
    }

    /** 1.16's {@code RunAwayTask.alreadyWalkingAwayFromPosWithSameSpeed}. */
    public boolean isWalkingAwayFrom(Vec3d danger, float speed) {
        BlockPos t = walkPos();
        if (t == null || this.walkSpeed != speed) {
            return false;
        }
        Vec3d toTarget = new Vec3d(t).add(0.5, 0.0, 0.5).subtract(getPositionVector());
        return toTarget.dotProduct(danger.subtract(getPositionVector())) < 0.0;
    }

    public float soundVolume() {
        return getSoundVolume();
    }

    public float soundPitch() {
        return getSoundPitch();
    }

    @Nullable
    private BlockPos walkPos() {
        if (this.walkTarget instanceof Entity) {
            Entity e = (Entity) this.walkTarget;
            return e.isEntityAlive() ? new BlockPos(e) : null;
        }
        return (BlockPos) this.walkTarget;
    }

    private boolean reached(BlockPos target) {
        BlockPos here = new BlockPos(this);
        int manhattan = Math.abs(target.getX() - here.getX()) + Math.abs(target.getY() - here.getY()) + Math.abs(target.getZ() - here.getZ());
        return manhattan <= this.walkClose;
    }

    /** 1.16's {@code WalkToTargetTask}: paths to the walk target, re-paths when it moves 2 blocks, gives up when stuck. */
    private void tickWalk() {
        if (this.walkTarget == null) {
            return;
        }
        BlockPos target = walkPos();
        if (target == null) {
            eraseWalkTarget();
            return;
        }
        if (reached(target)) {
            eraseWalkTarget();
            this.memories.erase(Mem.CANT_REACH_WALK_TARGET_SINCE);
            return;
        }
        if (this.walkLastPos == null || target.distanceSq(this.walkLastPos) > 4.0 || getNavigator().noPath()) {
            if (this.walkLastPos != null && getNavigator().noPath() && this.walkCooldown-- > 0) {
                return;
            }
            if (!computePath(target)) {
                this.walkTarget = null;
                this.walkLastPos = null;
                this.walkCooldown = this.rand.nextInt(40);
                return;
            }
            this.walkLastPos = target;
        }
    }

    private boolean computePath(BlockPos target) {
        Path path = getNavigator().getPathToPos(target);
        boolean canReach = false;
        if (path != null) {
            PathPoint end = path.getFinalPathPoint();
            canReach = end != null && end.x == target.getX() && end.z == target.getZ() && Math.abs(end.y - target.getY()) <= 1;
        }
        if (canReach) {
            this.memories.erase(Mem.CANT_REACH_WALK_TARGET_SINCE);
        } else if (!this.memories.has(Mem.CANT_REACH_WALK_TARGET_SINCE)) {
            this.memories.set(Mem.CANT_REACH_WALK_TARGET_SINCE, this.world.getTotalWorldTime());
        }
        if (path == null) {
            Vec3d toward = RandomPositionGenerator.findRandomTargetBlockTowards(this, 10, 7, new Vec3d(target).add(0.5, 0, 0.5));
            if (toward != null) {
                path = getNavigator().getPathToXYZ(toward.x, toward.y, toward.z);
            }
        }
        return path != null && getNavigator().setPath(path, this.walkSpeed);
    }

    /** 1.16's {@code LookTask}: looks at the look target until it is out of sight, gone, or 45-90 ticks have passed. */
    private void tickLook() {
        if (this.lookTarget == null) {
            return;
        }
        if (this.lookTarget instanceof Entity) {
            Entity e = (Entity) this.lookTarget;
            if (!e.isEntityAlive() || e instanceof EntityLivingBase && !canEntityBeSeen(e) || getDistanceSq(e) > 256.0) {
                this.lookTarget = null;
                return;
            }
            getLookHelper().setLookPositionWithEntity(e, 30.0F, 30.0F);
        } else {
            Vec3d v = (Vec3d) this.lookTarget;
            getLookHelper().setLookPosition(v.x, v.y, v.z, 30.0F, 30.0F);
        }
        if (--this.lookTicks <= 0) {
            this.lookTarget = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Fighting (EntityMob's melee hit, 1.16's ranges)
    // ---------------------------------------------------------------------------------------------

    /** 1.16's {@code BrainUtil.isWithinMeleeAttackRange}. */
    public boolean isWithinMeleeAttackRange(EntityLivingBase target) {
        double reach = this.width * 2.0F * this.width * 2.0F + target.width;
        return getDistanceSq(target.posX, target.posY, target.posZ) <= reach;
    }

    /** {@code EntityMob.attackEntityAsMob}: the held weapon's damage and knockback, fire aspect, shield disabling by axes. */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        float damage = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        int knockback = 0;
        if (target instanceof EntityLivingBase) {
            damage += EnchantmentHelper.getModifierForCreature(getHeldItemMainhand(), ((EntityLivingBase) target).getCreatureAttribute());
            knockback += EnchantmentHelper.getKnockbackModifier(this);
        }
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) {
            if (knockback > 0 && target instanceof EntityLivingBase) {
                ((EntityLivingBase) target).knockBack(this, knockback * 0.5F, MathHelper.sin(this.rotationYaw * 0.017453292F),
                        -MathHelper.cos(this.rotationYaw * 0.017453292F));
                this.motionX *= 0.6;
                this.motionZ *= 0.6;
            }
            int fire = EnchantmentHelper.getFireAspectModifier(this);
            if (fire > 0) {
                target.setFire(fire * 4);
            }
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;
                ItemStack weapon = getHeldItemMainhand();
                ItemStack shield = player.isHandActive() ? player.getActiveItemStack() : ItemStack.EMPTY;
                if (!weapon.isEmpty() && !shield.isEmpty() && weapon.getItem().canDisableShield(weapon, shield, player, this)
                        && shield.getItem().isShield(shield, player)) {
                    float chance = 0.25F + EnchantmentHelper.getEfficiencyModifier(this) * 0.05F;
                    if (this.rand.nextFloat() < chance) {
                        player.getCooldownTracker().setCooldown(shield.getItem(), 100);
                        this.world.setEntityState(player, (byte) 30);
                    }
                }
            }
            applyEnchantments(this, target);
        }
        return hit;
    }

    /** @return true when the main hand holds a tool or weapon (1.16's {@code isHoldingMeleeWeapon}: a tiered item) */
    public boolean isHoldingMeleeWeapon() {
        net.minecraft.item.Item item = getHeldItemMainhand().getItem();
        return item instanceof ItemSword || item instanceof ItemTool || item instanceof net.minecraft.item.ItemHoe;
    }

    // ---------------------------------------------------------------------------------------------
    // Equipment comparison (1.16's MobEntity.canReplaceCurrentItem)
    // ---------------------------------------------------------------------------------------------

    protected boolean canReplaceCurrentItem(ItemStack candidate, ItemStack current) {
        if (current.isEmpty()) {
            return true;
        }
        net.minecraft.item.Item a = candidate.getItem();
        net.minecraft.item.Item b = current.getItem();
        if (a instanceof ItemSword) {
            if (!(b instanceof ItemSword)) {
                return true;
            }
            float da = ((ItemSword) a).getAttackDamage();
            float db = ((ItemSword) b).getAttackDamage();
            return da != db ? da > db : canReplaceEqualItem(candidate, current);
        } else if (a instanceof ItemBow && b instanceof ItemBow) {
            return canReplaceEqualItem(candidate, current);
        } else if (Crossbows.isCrossbow(candidate) && Crossbows.isCrossbow(current)) {
            return canReplaceEqualItem(candidate, current);
        } else if (a instanceof ItemArmor) {
            if (EnchantmentHelper.hasBindingCurse(current)) {
                return false;
            }
            if (!(b instanceof ItemArmor)) {
                return true;
            }
            ItemArmor x = (ItemArmor) a;
            ItemArmor y = (ItemArmor) b;
            if (x.damageReduceAmount != y.damageReduceAmount) {
                return x.damageReduceAmount > y.damageReduceAmount;
            }
            if (x.toughness != y.toughness) {
                return x.toughness > y.toughness;
            }
            return canReplaceEqualItem(candidate, current);
        } else if (a instanceof ItemTool || a instanceof ItemAxe) {
            if (b instanceof ItemBlock) {
                return true;
            }
            if (b instanceof ItemTool) {
                float da = toolDamage(candidate);
                float db = toolDamage(current);
                return da != db ? da > db : canReplaceEqualItem(candidate, current);
            }
        }
        return false;
    }

    private static float toolDamage(ItemStack stack) {
        double d = 0;
        for (net.minecraft.entity.ai.attributes.AttributeModifier m : stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            d += m.getAmount();
        }
        return (float) d;
    }

    /** 1.16's {@code canReplaceEqualItem}: the less damaged one, or the one with tags beyond damage. */
    protected boolean canReplaceEqualItem(ItemStack candidate, ItemStack current) {
        if (candidate.getItemDamage() < current.getItemDamage() || candidate.hasTagCompound() && !current.hasTagCompound()) {
            return true;
        }
        if (candidate.hasTagCompound() && current.hasTagCompound()) {
            return otherKeys(candidate.getTagCompound()) && !otherKeys(current.getTagCompound());
        }
        return false;
    }

    private static boolean otherKeys(NBTTagCompound tag) {
        for (String key : tag.getKeySet()) {
            if (!"Damage".equals(key)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // Spawning, saving, sounds
    // ---------------------------------------------------------------------------------------------

    /** 1.16's {@code checkPiglinSpawnRules}: anywhere but on nether wart blocks; no light rule. */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos below = new BlockPos(this).down();
        return this.world.getBlockState(below).getBlock() != Blocks.NETHER_WART_BLOCK
                && this.world.getBlockState(below).canEntitySpawn(this);
    }

    @Override
    protected boolean canDespawn() {
        return true;
    }

    @Override
    public double getYOffset() {
        return isChild() ? -0.05 : -0.45;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_HOSTILE_SWIM;
    }

    @Override
    protected SoundEvent getSplashSound() {
        return SoundEvents.ENTITY_HOSTILE_SPLASH;
    }

    @Override
    protected SoundEvent getFallSound(int height) {
        return height > 4 ? SoundEvents.ENTITY_HOSTILE_BIG_FALL : SoundEvents.ENTITY_HOSTILE_SMALL_FALL;
    }

    @Override
    protected boolean canDropLoot() {
        return true;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (isImmuneToZombification()) {
            tag.setBoolean("IsImmuneToZombification", true);
        }
        tag.setInteger("TimeInOverworld", this.timeInOverworld);
        tag.setTag("Brain", this.memories.write());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setImmuneToZombification(tag.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = tag.getInteger("TimeInOverworld");
        this.memories.read(tag.getCompoundTag("Brain"));
    }
}
