package com.exiledradio.netherupdatebackport.netherite;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A dropped netherite item: it never burns, and it floats up through lava (1.16's {@code fireResistant}
 * items).
 *
 * <p>Fire damage of every kind is ignored (1.16's {@code canBeHurtBy}), and 1.12.2's fire-immune flag skips
 * the burning. In lava the item moves as 1.16.5's {@code ItemEntity} does: no gravity, sideways motion slowed to
 * 95%, and a slight rise, so it slows down, then floats up; 1.12.2's random bounce of items in lava is undone.
 * 1.12.2 still plays its fizz when the item enters a new lava block (1.16 is silent for these). Made by {@link Fireproof#createEntity}, so every drop of
 * these items (a player's toss, a death, a broken block) is one of these.
 */
public final class EntityFireproofItem extends EntityItem {

    public EntityFireproofItem(World world) {
        super(world);
        this.isImmuneToFire = true;
    }

    public EntityFireproofItem(World world, Entity original, ItemStack stack) {
        super(world, original.posX, original.posY, original.posZ, stack);
        this.isImmuneToFire = true;
        this.motionX = original.motionX;
        this.motionY = original.motionY;
        this.motionZ = original.motionZ;
        if (original instanceof EntityItem) {
            EntityItem item = (EntityItem) original;
            Integer delay = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(EntityItem.class, item, "field_145804_b");
            setPickupDelay(delay != null ? delay : 10);
            setThrower(item.getThrower());
            setOwner(item.getOwner());
        }
    }

    @Override
    public void onUpdate() {
        boolean lava = inLava();
        double mx = this.motionX;
        double my = this.motionY;
        double mz = this.motionZ;
        if (lava) {
            // 1.16 applies no gravity in lava: cancel the 0.04 vanilla's update is about to take away.
            this.motionY += 0.04;
        }
        super.onUpdate();
        if (lava && !this.isDead) {
            // 1.16's setUnderLavaMovement, then the usual 0.98 drag; this also undoes 1.12.2's random bounce of an
            // item entering a lava block, which 1.16 no longer does for fire-immune items.
            this.motionX = mx * 0.95 * 0.98;
            this.motionY = (my + (my < 0.06 ? 5.0E-4 : 0.0)) * 0.98;
            this.motionZ = mz * 0.95 * 0.98;
        }
    }

    private boolean inLava() {
        return this.world.isMaterialInBB(getEntityBoundingBox().shrink(0.001), Material.LAVA);
    }

    /** 1.16's {@code Item.canBeHurtBy}: a fire-resistant item ignores every fire damage. */
    @Override
    public boolean attackEntityFrom(net.minecraft.util.DamageSource source, float amount) {
        return !source.isFireDamage() && super.attackEntityFrom(source, amount);
    }

    @Override
    public boolean isBurning() {
        return false;
    }
}
