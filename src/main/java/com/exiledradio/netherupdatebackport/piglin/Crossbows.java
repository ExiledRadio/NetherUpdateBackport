package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.owned.ItemCrossbow;
import com.exiledradio.netherupdatebackport.owned.OwnedContent;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * The crossbow a piglin carries: the owner's (Village &amp; Pillage), else this mod's copy, else another mod's.
 *
 * <h2>Held, never used</h2>
 * As Village &amp; Pillage's pillagers do, the piglin times its own loading and shoots its own arrows (1.16's
 * {@code ShootTargetTask} timings); the crossbow is held and shown, and marked loaded so its model shows an arrow.
 * No other mod's item code runs for a mob. Loaded is 1.14's {@code Charged} tag, which every one of these crossbows
 * reads.
 */
public final class Crossbows {

    /** Where to look, in order: the owner's, the copy, then the crossbows the owner itself steps aside for. */
    private static final String[] NAMES = {Owners.VILLAGE_AND_PILLAGE + ":crossbow", "netherupdatebackport:crossbow",
            "futuremc:crossbow", "crossbows:crossbow"};

    private Crossbows() {
    }

    /** @return the crossbow item piglins carry in this game, or null when no mod adds one */
    @Nullable
    public static Item item() {
        for (String name : NAMES) {
            ResourceLocation id = new ResourceLocation(name);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && id.equals(item.getRegistryName())) {
                return item;
            }
        }
        return null;
    }

    /** @return true for anything a piglin treats as a crossbow (1.16's {@code instanceof CrossbowItem}), by name */
    public static boolean isCrossbow(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        return name != null && name.getPath().contains("crossbow");
    }

    public static void setCharged(ItemStack stack, boolean charged) {
        if (!isCrossbow(stack)) {
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setBoolean("Charged", charged);
        NBTTagList projectiles = new NBTTagList();
        if (charged) {
            projectiles.appendTag(new ItemStack(Items.ARROW).writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("ChargedProjectiles", projectiles);
        stack.setTagCompound(tag);
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean("Charged");
    }

    /** @return 1.14's charge time for a crossbow: 25 ticks, 5 fewer per level of the copy's Quick Charge */
    public static int chargeTicks(ItemStack stack) {
        return stack.getItem() instanceof ItemCrossbow ? ItemCrossbow.chargeTicks(stack) : 25;
    }

    /** @return true when the held crossbow is the copy with Multishot */
    public static boolean multishot(ItemStack stack) {
        return stack.getItem() instanceof ItemCrossbow && OwnedContent.level(OwnedContent.multishot, stack) > 0;
    }

    /** @return the arrow a crossbow shot makes: the copy's own (so its Piercing goes with it) or vanilla's */
    public static EntityArrow arrow(World world, EntityLivingBase shooter, ItemStack crossbow) {
        if (crossbow.getItem() instanceof ItemCrossbow) {
            return ItemCrossbow.arrow(world, shooter, crossbow, new ItemStack(Items.ARROW));
        }
        EntityTippedArrow arrow = new EntityTippedArrow(world, shooter);
        arrow.setPotionEffect(new ItemStack(Items.ARROW));
        return arrow;
    }
}
