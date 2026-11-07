package com.exiledradio.netherupdatebackport.strider;

import com.exiledradio.netherupdatebackport.common.Triggers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/**
 * The warped fungus on a stick: steers a saddled strider and, used while riding one, boosts it for one point of
 * durability (100 uses). Worn out, it leaves the fishing rod. 1.16's {@code OnAStickItem} with its strider numbers;
 * vanilla's carrot on a stick, which is the same code for pigs.
 */
public final class ItemWarpedFungusOnAStick extends Item {

    ItemWarpedFungusOnAStick() {
        setMaxStackSize(1);
        setMaxDamage(100);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        }
        if (player.getRidingEntity() instanceof EntityStrider) {
            EntityStrider strider = (EntityStrider) player.getRidingEntity();
            if (strider.canBeSteered() && stack.getMaxDamage() - stack.getMetadata() >= 1 && strider.boost()) {
                stack.damageItem(1, player);
                Triggers.fire(player, "ride_strider");
                if (stack.isEmpty()) {
                    ItemStack rod = new ItemStack(Items.FISHING_ROD);
                    rod.setTagCompound(stack.getTagCompound());
                    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, rod);
                }
                return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
            }
        }
        return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
    }
}
