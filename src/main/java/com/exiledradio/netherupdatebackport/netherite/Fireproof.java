package com.exiledradio.netherupdatebackport.netherite;

import com.google.common.collect.Multimap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Netherite's items: everything here drops as an {@link EntityFireproofItem} (1.16's {@code fireResistant}).
 *
 * <p>Tools use 1.16's {@code ItemTier.NETHERITE} (level 4, 2031 uses, speed 9, damage bonus 4, enchantability
 * 15), which with 1.12.2's own tool classes gives 1.16's numbers: sword 8, pickaxe 6, axe 10 (speed 1.0),
 * shovel 6.5. The hoe's attack speed would be 5 in 1.12.2's formula (bonus - 3 + 4) and is put back to 1.16's 4.
 * Armor is 1.16's {@code ArmorMaterial.NETHERITE} plus its knockback resistance of 0.1 per piece, which 1.12.2's
 * armor material has no field for.
 */
public final class Fireproof {

    /** 1.16's armor knockback modifier ids, one per slot (vanilla's ARMOR_MODIFIER_UUID_PER_SLOT). */
    private static final UUID[] KNOCKBACK = {
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")};

    private Fireproof() {
    }

    static Entity createEntity(World world, Entity location, ItemStack stack) {
        return new EntityFireproofItem(world, location, stack);
    }

    /** Scrap and the ingot. */
    public static class Plain extends Item {
        private final boolean beacon;

        Plain(boolean beacon) {
            this.beacon = beacon;
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }

        @Override
        public boolean isBeaconPayment(ItemStack stack) {
            return this.beacon;
        }
    }

    /** Ancient debris and the netherite block as items. */
    public static class DebrisItem extends ItemBlock {
        DebrisItem(Block block) {
            super(block);
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }

    public static class Sword extends ItemSword {
        Sword(ToolMaterial material) {
            super(material);
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }

    public static class Pickaxe extends ItemPickaxe {
        Pickaxe(ToolMaterial material) {
            super(material);
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }

    public static class Axe extends ItemAxe {
        Axe(ToolMaterial material) {
            // 1.16: 5 + the tier's 4 = 9 over the hand's 1; speed -3.0. 1.12.2's axe takes the whole bonus here.
            super(material, 9.0F, -3.0F);
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }

    public static class Shovel extends ItemSpade {
        Shovel(ToolMaterial material) {
            super(material);
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }

    public static class Hoe extends ItemHoe {
        Hoe(ToolMaterial material) {
            super(material);
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers(slot);
            if (slot == EntityEquipmentSlot.MAINHAND) {
                map.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
                map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", 0.0, 0));
            }
            return map;
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }

    public static class Armor extends ItemArmor {
        Armor(ArmorMaterial material, EntityEquipmentSlot slot) {
            super(material, 0, slot);
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers(slot);
            if (slot == this.armorType) {
                map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                        new AttributeModifier(KNOCKBACK[slot.getIndex()], "Armor knockback resistance", 0.1, 0));
            }
            return map;
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) {
            return true;
        }

        @Override
        public Entity createEntity(World world, Entity location, ItemStack stack) {
            return Fireproof.createEntity(world, location, stack);
        }
    }
}
