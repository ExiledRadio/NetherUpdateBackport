package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.piglin.Memories.Mem;
import com.exiledradio.netherupdatebackport.piglin.PiglinTasks.Activity;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The piglin: 1.16.5's {@code PiglinEntity}, its brain in {@link PiglinTasks}.
 *
 * <ul>
 *   <li>16 health, speed 0.35 (a baby 20% faster), attack 5; 5 experience. One in five spawns a baby; an adult holds
 *       a crossbow or a golden sword, half and half, and each piece of gold armor one time in ten.</li>
 *   <li>Picks up and admires gold for 6 s, then barters a gold ingot ({@link Bartering}); keeps other things in
 *       an inventory of 8, wears better gear, eats porkchops. Handed a gold ingot, an adult admires it at once.</li>
 *   <li>Fights players without gold armor, anyone it is angry at, and wither skeletons; angry at players who
 *       open chests near it or break gold blocks and ores; hunts hoglins; dances after some hoglin kills.</li>
 *   <li>Avoids soul fire and its relatives, zombified piglins and zoglins; babies ride baby hoglins.</li>
 *   <li>Outside the nether it becomes a zombified piglin after 15 s ({@link AbstractPiglin}).</li>
 * </ul>
 */
public class EntityPiglin extends AbstractPiglin {

    private static final DataParameter<Boolean> BABY = EntityDataManager.createKey(EntityPiglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> CHARGING_CROSSBOW = EntityDataManager.createKey(EntityPiglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DANCING = EntityDataManager.createKey(EntityPiglin.class, DataSerializers.BOOLEAN);

    private static final UUID BABY_SPEED_ID = UUID.fromString("766bfa64-11f3-11ea-8d71-362b9e155667");
    private static final AttributeModifier BABY_SPEED = new AttributeModifier(BABY_SPEED_ID, "Baby speed boost", 0.2, 1).setSaved(false);

    private final InventoryBasic inventory = new InventoryBasic("Inventory", false, 8);
    private boolean cannotHunt;

    /** The current activity (server). */
    Activity activity = Activity.IDLE;
    /** The baby's ride-a-hoglin countdown, -1 until first needed. */
    int rideCountdown = -1;
    /** Set by {@link BastionMobs} before the first spawn: 1.16's {@code SpawnReason.STRUCTURE}. */
    boolean structureSpawn;

    // 1.16's idle look and move groups, one set per activity that has them (each 1.16 activity makes its own).
    final Shuffled<EntityPiglin> idleLook = lookGroup();
    final Shuffled<EntityPiglin> idleMove = moveGroup();
    final Shuffled<EntityPiglin> avoidLook = lookGroup();
    final Shuffled<EntityPiglin> avoidMove = moveGroup();
    final Shuffled<EntityPiglin> rideLook = lookGroup();
    final Shuffled<EntityPiglin> celebrate = new Shuffled<EntityPiglin>()
            .add(1, p -> PiglinTasks.lookAt(p, e -> e instanceof EntityPiglin, 8.0))
            .add(1, p -> PiglinTasks.walkRandomly(p, 0.6F, 2, 1))
            .idle(1, 10, 20);

    private static Shuffled<EntityPiglin> lookGroup() {
        return new Shuffled<EntityPiglin>()
                .add(1, p -> PiglinTasks.lookAt(p, e -> e instanceof EntityPlayer, 8.0))
                .add(1, p -> PiglinTasks.lookAt(p, e -> e instanceof EntityPiglin, 8.0))
                .add(1, p -> PiglinTasks.lookAt(p, e -> true, 8.0))
                .idle(1, 30, 60);
    }

    private static Shuffled<EntityPiglin> moveGroup() {
        return new Shuffled<EntityPiglin>()
                .add(2, p -> PiglinTasks.walkRandomly(p, 0.6F, 10, 7))
                .add(2, p -> PiglinTasks.interactWith(p, EntityPiglin.class, 8.0, 0.6F, 2))
                .add(2, p -> !p.memories.has(Mem.NEAREST_PLAYER_HOLDING_WANTED_ITEM) && PiglinTasks.walkTowardsLookTarget(p, 0.6F, 3))
                .idle(1, 30, 60);
    }

    public EntityPiglin(World world) {
        super(world);
        setSize(0.6F, 1.95F);
        this.experienceValue = 5;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BABY, false);
        this.dataManager.register(CHARGING_CROSSBOW, false);
        this.dataManager.register(DANCING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35F);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0);
    }

    // ---------------------------------------------------------------------------------------------
    // Age
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean isChild() {
        return this.dataManager.get(BABY);
    }

    public void setChild(boolean baby) {
        this.dataManager.set(BABY, baby);
        if (this.world != null && !this.world.isRemote) {
            IAttributeInstance speed = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            speed.removeModifier(BABY_SPEED);
            if (baby) {
                speed.applyModifier(BABY_SPEED);
            }
        }
        updateSize(baby);
    }

    private void updateSize(boolean baby) {
        float scale = baby ? 0.5F : 1.0F;
        setSize(0.6F * scale, 1.95F * scale);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (BABY.equals(key)) {
            updateSize(isChild());
        } else if (CHARGING_CROSSBOW.equals(key) && isChargingCrossbow()) {
            this.chargeStartTick = this.ticksExisted;
        }
    }

    @Override
    public float getEyeHeight() {
        return isChild() ? 0.93F : 1.74F;
    }

    @Override
    public double getMountedYOffset() {
        return this.height * 0.92;
    }

    @Override
    protected boolean canHunt() {
        return !this.cannotHunt;
    }

    public void setCannotHunt(boolean cannotHunt) {
        this.cannotHunt = cannotHunt;
    }

    // ---------------------------------------------------------------------------------------------
    // Brain
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void tickBrain() {
        PiglinTasks.tick(this);
    }

    @Override
    protected void sense(List<EntityLivingBase> nearby, List<EntityLivingBase> visible) {
        PiglinTasks.sense(this, nearby, visible);
    }

    public Activity activity() {
        return this.activity;
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        if (!this.structureSpawn) {
            if (this.world.rand.nextFloat() < 0.2F) {
                setChild(true);
            } else if (isAdult()) {
                setItemStackToSlot(EntityEquipmentSlot.MAINHAND, createSpawnWeapon());
            }
        }
        PiglinTasks.initMemories(this);
        if (isAdult()) {
            // 1.16's populateDefaultEquipmentSlots: each piece of gold armor one time in ten.
            maybeWear(EntityEquipmentSlot.HEAD, Items.GOLDEN_HELMET);
            maybeWear(EntityEquipmentSlot.CHEST, Items.GOLDEN_CHESTPLATE);
            maybeWear(EntityEquipmentSlot.LEGS, Items.GOLDEN_LEGGINGS);
            maybeWear(EntityEquipmentSlot.FEET, Items.GOLDEN_BOOTS);
        }
        setEnchantmentBasedOnDifficulty(difficulty);
        return super.onInitialSpawn(difficulty, data);
    }

    private void maybeWear(EntityEquipmentSlot slot, Item item) {
        if (this.world.rand.nextFloat() < 0.1F) {
            setItemStackToSlot(slot, new ItemStack(item));
        }
    }

    /** Half crossbow (the owner's, or the copy), half golden sword. */
    ItemStack createSpawnWeapon() {
        Item crossbow = Crossbows.item();
        return this.rand.nextFloat() < 0.5F && crossbow != null ? new ItemStack(crossbow) : new ItemStack(Items.GOLDEN_SWORD);
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (super.processInteract(player, hand)) {
            return true;
        }
        if (!this.world.isRemote) {
            return PiglinTasks.mobInteract(this, player, hand);
        }
        return isAdult() && PiglinTasks.isBarterCurrency(player.getHeldItem(hand)) && getArmPose() != PiglinAction.ADMIRING_ITEM;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (!this.world.isRemote && hurt && source.getTrueSource() instanceof EntityLivingBase) {
            PiglinTasks.wasHurtBy(this, (EntityLivingBase) source.getTrueSource());
        }
        return hurt;
    }

    // ---------------------------------------------------------------------------------------------
    // Items
    // ---------------------------------------------------------------------------------------------

    /** 1.12.2's item pickup check, which 1.16 calls {@code wantsToPickUp}/{@code pickUpItem}. */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        if (wantsToPickUp(item.getItem())) {
            PiglinTasks.pickUpItem(this, item);
        }
    }

    public boolean wantsToPickUp(ItemStack stack) {
        return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this) && canPickUpLoot()
                && PiglinTasks.wantsToPickup(this, stack);
    }

    boolean canReplaceCurrentItem(ItemStack candidate) {
        return canReplaceCurrentItem(candidate, getItemStackFromSlot(PiglinTasks.slotFor(candidate)));
    }

    /** 1.16's piglin: gold and crossbows beat anything else, and an adult never trades its crossbow for a non-crossbow. */
    @Override
    protected boolean canReplaceCurrentItem(ItemStack candidate, ItemStack current) {
        if (EnchantmentHelper.hasBindingCurse(current)) {
            return false;
        }
        boolean a = PiglinTasks.isLovedItem(candidate) || Crossbows.isCrossbow(candidate);
        boolean b = PiglinTasks.isLovedItem(current) || Crossbows.isCrossbow(current);
        if (a && !b) {
            return true;
        }
        if (!a && b) {
            return false;
        }
        if (isAdult() && !Crossbows.isCrossbow(candidate) && Crossbows.isCrossbow(current)) {
            return false;
        }
        return super.canReplaceCurrentItem(candidate, current);
    }

    /** 1.16's {@code equipItemIfPossible}. @return true if it now wears or holds it */
    boolean equipItemIfPossible(ItemStack stack) {
        EntityEquipmentSlot slot = PiglinTasks.slotFor(stack);
        ItemStack current = getItemStackFromSlot(slot);
        if (!canReplaceCurrentItem(stack, current)) {
            return false;
        }
        if (!current.isEmpty() && Math.max(this.rand.nextFloat() - 0.1F, 0.0F) < dropChance(slot)) {
            entityDropItem(current, 0.0F);
        }
        setItemSlotAndDropWhenKilled(slot, stack);
        playEquipSound(stack);
        return true;
    }

    void holdInMainHand(ItemStack stack) {
        setItemSlotAndDropWhenKilled(EntityEquipmentSlot.MAINHAND, stack);
    }

    /** Gold being admired always drops if it dies, without keeping the piglin from despawning. */
    void holdInOffHand(ItemStack stack) {
        if (PiglinTasks.isBarterCurrency(stack)) {
            setItemStackToSlot(EntityEquipmentSlot.OFFHAND, stack);
            setGuaranteedDrop(EntityEquipmentSlot.OFFHAND);
        } else {
            setItemSlotAndDropWhenKilled(EntityEquipmentSlot.OFFHAND, stack);
        }
    }

    /** 1.16's {@code Inventory.addItem}: merges into stacks of the same item, then empty slots. @return what is left */
    ItemStack addToInventory(ItemStack stack) {
        ItemStack left = stack.copy();
        for (int i = 0; i < this.inventory.getSizeInventory() && !left.isEmpty(); i++) {
            ItemStack in = this.inventory.getStackInSlot(i);
            if (!in.isEmpty() && ItemStack.areItemsEqual(in, left) && ItemStack.areItemStackTagsEqual(in, left)) {
                int move = Math.min(left.getCount(), Math.min(this.inventory.getInventoryStackLimit(), in.getMaxStackSize()) - in.getCount());
                if (move > 0) {
                    in.grow(move);
                    left.shrink(move);
                }
            }
        }
        for (int i = 0; i < this.inventory.getSizeInventory() && !left.isEmpty(); i++) {
            if (this.inventory.getStackInSlot(i).isEmpty()) {
                this.inventory.setInventorySlotContents(i, left);
                left = ItemStack.EMPTY;
            }
        }
        this.inventory.markDirty();
        return left;
    }

    boolean canAddToInventory(ItemStack stack) {
        for (int i = 0; i < this.inventory.getSizeInventory(); i++) {
            ItemStack in = this.inventory.getStackInSlot(i);
            if (in.isEmpty() || ItemStack.areItemsEqual(in, stack) && ItemStack.areItemStackTagsEqual(in, stack)
                    && in.getCount() < in.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public InventoryBasic inventory() {
        return this.inventory;
    }

    private void dropInventory() {
        for (int i = 0; i < this.inventory.getSizeInventory(); i++) {
            ItemStack s = this.inventory.removeStackFromSlot(i);
            if (!s.isEmpty()) {
                entityDropItem(s, 0.0F);
            }
        }
    }

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        dropInventory();
    }

    @Override
    protected net.minecraft.entity.monster.EntityPigZombie finishConversion() {
        PiglinTasks.cancelAdmiring(this);
        dropInventory();
        return super.finishConversion();
    }

    /** A baby boarding a hoglin climbs to the top of up to three riders. */
    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (isChild() && PiglinTasks.isHoglin(vehicle)) {
            vehicle = topPassenger(vehicle, 3);
        }
        return super.startRiding(vehicle, force);
    }

    private Entity topPassenger(Entity e, int depth) {
        List<Entity> riders = e.getPassengers();
        return depth != 1 && !riders.isEmpty() ? topPassenger(riders.get(0), depth - 1) : e;
    }

    // ---------------------------------------------------------------------------------------------
    // The crossbow (1.16's ShootTargetTask and performCrossbowAttack)
    // ---------------------------------------------------------------------------------------------

    private enum Shot {
        UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK
    }

    private Shot shot = Shot.UNCHARGED;
    private int chargeTicks;
    private int attackDelay;
    /** Client: when the current charge started, for the arm pose. */
    private int chargeStartTick;

    ItemStack heldCrossbow() {
        return Crossbows.isCrossbow(getHeldItemMainhand()) ? getHeldItemMainhand() : getHeldItemOffhand();
    }

    void tickShooting(EntityLivingBase target) {
        ItemStack crossbow = heldCrossbow();
        switch (this.shot) {
            case UNCHARGED:
                this.shot = Shot.CHARGING;
                this.chargeTicks = 0;
                setChargingCrossbow(true);
                break;
            case CHARGING:
                int total = Crossbows.chargeTicks(crossbow);
                this.chargeTicks++;
                if (this.chargeTicks == Math.max(1, Math.round(total * 0.2F))) {
                    playSound(PiglinContent.CROSSBOW_LOADING_START, 0.5F, 1.0F);
                } else if (this.chargeTicks == Math.round(total * 0.5F)) {
                    playSound(PiglinContent.CROSSBOW_LOADING_MIDDLE, 0.5F, 1.0F);
                }
                if (this.chargeTicks >= total) {
                    playSound(PiglinContent.CROSSBOW_LOADING_END, 1.0F, 1.0F / (this.rand.nextFloat() * 0.5F + 1.0F) + 0.2F);
                    Crossbows.setCharged(crossbow, true);
                    setChargingCrossbow(false);
                    this.shot = Shot.CHARGED;
                    this.attackDelay = 20 + this.rand.nextInt(20);
                }
                break;
            case CHARGED:
                if (--this.attackDelay <= 0) {
                    this.shot = Shot.READY_TO_ATTACK;
                }
                break;
            case READY_TO_ATTACK:
                shootAt(target);
                Crossbows.setCharged(crossbow, false);
                this.shot = Shot.UNCHARGED;
                break;
        }
    }

    /** 1.16's {@code ShootTargetTask.stop}: a half-drawn crossbow is let down; a loaded one stays loaded. */
    void stopShooting() {
        if (this.shot == Shot.CHARGING) {
            this.shot = Shot.UNCHARGED;
        }
        if (isChargingCrossbow()) {
            setChargingCrossbow(false);
        }
    }

    /** {@code ICrossbowUser.performCrossbowAttack(1.6)}: 14 - 4 x difficulty inaccuracy, Multishot's side bolts. */
    void shootAt(EntityLivingBase target) {
        ItemStack crossbow = heldCrossbow();
        float inaccuracy = 14 - this.world.getDifficulty().getId() * 4;
        float[] angles = Crossbows.multishot(crossbow) ? new float[]{0.0F, -10.0F, 10.0F} : new float[]{0.0F};
        for (float angle : angles) {
            EntityArrow arrow = Crossbows.arrow(this.world, this, crossbow);
            double dx = target.posX - this.posX;
            double dz = target.posZ - this.posZ;
            double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
            double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - arrow.posY + horizontal * 0.2;
            double radians = Math.toRadians(angle);
            double rx = dx * Math.cos(radians) - dz * Math.sin(radians);
            double rz = dx * Math.sin(radians) + dz * Math.cos(radians);
            arrow.shoot(rx, dy, rz, 1.6F, inaccuracy);
            if (angle != 0.0F) {
                arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
            }
            this.world.spawnEntity(arrow);
        }
        crossbow.damageItem(1, this);
        playSound(PiglinContent.CROSSBOW_SHOOT, 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
        this.idleTime = 0;
    }

    public boolean isChargingCrossbow() {
        return this.dataManager.get(CHARGING_CROSSBOW);
    }

    void setChargingCrossbow(boolean charging) {
        this.dataManager.set(CHARGING_CROSSBOW, charging);
    }

    /** Client: how far into its 25-tick charge it is, 0 to 1. */
    public float chargeProgress(float partialTicks) {
        int total = Crossbows.chargeTicks(heldCrossbow());
        return total <= 0 ? 1.0F : MathHelper.clamp((this.ticksExisted - this.chargeStartTick + partialTicks) / total, 0.0F, 1.0F);
    }

    public boolean isDancing() {
        return this.dataManager.get(DANCING);
    }

    void setDancing(boolean dancing) {
        if (dancing != isDancing()) {
            this.dataManager.set(DANCING, dancing);
        }
    }

    /** 1.16's {@code getArmPose}, from what the client knows. */
    public PiglinAction getArmPose() {
        if (isDancing()) {
            return PiglinAction.DANCING;
        }
        if (PiglinTasks.isLovedItem(getHeldItemOffhand())) {
            return PiglinAction.ADMIRING_ITEM;
        }
        if (isAggressive() && isHoldingMeleeWeapon()) {
            return PiglinAction.ATTACKING_WITH_MELEE_WEAPON;
        }
        if (isChargingCrossbow()) {
            return PiglinAction.CROSSBOW_CHARGE;
        }
        return isAggressive() && PiglinTasks.hasCrossbow(this) ? PiglinAction.CROSSBOW_HOLD : PiglinAction.DEFAULT;
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds, experience, saving
    // ---------------------------------------------------------------------------------------------

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return this.world.isRemote ? null : PiglinTasks.soundForActivity(this, this.activity);
    }

    /** 1.16's {@code AbstractPiglinEntity.playAmbientSound}: only while idle. */
    @Override
    public void playLivingSound() {
        if (this.activity == Activity.IDLE) {
            super.playLivingSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PiglinContent.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PiglinContent.PIGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(PiglinContent.PIGLIN_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent convertedSound() {
        return PiglinContent.PIGLIN_CONVERTED;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.experienceValue;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (isChild()) {
            tag.setBoolean("IsBaby", true);
        }
        if (this.cannotHunt) {
            tag.setBoolean("CannotHunt", true);
        }
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < this.inventory.getSizeInventory(); i++) {
            ItemStack s = this.inventory.getStackInSlot(i);
            if (!s.isEmpty()) {
                items.appendTag(s.writeToNBT(new NBTTagCompound()));
            }
        }
        tag.setTag("Inventory", items);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setChild(tag.getBoolean("IsBaby"));
        setCannotHunt(tag.getBoolean("CannotHunt"));
        this.inventory.clear();
        NBTTagList items = tag.getTagList("Inventory", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            addToInventory(new ItemStack(items.getCompoundTagAt(i)));
        }
    }
}
