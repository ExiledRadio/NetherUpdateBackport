package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Triggers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The lodestone compass: a compass bound to a lodestone, pointing at it from anywhere in the lodestone's
 * dimension, spinning elsewhere or once the lodestone is gone.
 *
 * <p>1.16 keeps it as a compass with NBT; 1.12.2's compass item cannot change its own name or needle, so it
 * is an item of its own, made the 1.16 way: use a compass (or a lodestone compass, to rebind it) on a
 * lodestone. A single compass changes in place; from a stack, one is taken off and the new one goes to the
 * inventory. Same tag as 1.16: {@code LodestonePos} {X, Y, Z}, {@code LodestoneDimension} (1.12.2's number),
 * {@code LodestoneTracked}. Removed {@code LodestonePos} when, in the lodestone's dimension, the block is no
 * longer there. The needle is drawn by {@code client.BlocksClient}.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class ItemLodestoneCompass extends Item {

    ItemLodestoneCompass() {
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    /** @return the bound lodestone, or null if it is gone */
    @Nullable
    public static BlockPos target(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("LodestonePos")) {
            return null;
        }
        NBTTagCompound p = tag.getCompoundTag("LodestonePos");
        return new BlockPos(p.getInteger("X"), p.getInteger("Y"), p.getInteger("Z"));
    }

    public static int dimension(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger("LodestoneDimension") : 0;
    }

    static void bind(ItemStack stack, World world, BlockPos pos) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagCompound p = new NBTTagCompound();
        p.setInteger("X", pos.getX());
        p.setInteger("Y", pos.getY());
        p.setInteger("Z", pos.getZ());
        tag.setTag("LodestonePos", p);
        tag.setInteger("LodestoneDimension", world.provider.getDimension());
        tag.setBoolean("LodestoneTracked", true);
        stack.setTagCompound(tag);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        return useOn(player, world, pos, hand) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    /** A vanilla compass used on a lodestone becomes a lodestone compass. */
    @SubscribeEvent
    public static void compassOnLodestone(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() == Items.COMPASS && useOn(event.getEntityPlayer(), event.getWorld(), event.getPos(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    /** 1.16's {@code CompassItem.useOn} for a lodestone. @return true if the click was a lodestone's */
    static boolean useOn(EntityPlayer player, World world, BlockPos pos, EnumHand hand) {
        if (BlocksContent.lodestone == null || world.getBlockState(pos).getBlock() != BlocksContent.lodestone) {
            return false;
        }
        world.playSound(null, pos, BlocksContent.COMPASS_LOCK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (world.isRemote) {
            return true;
        }
        ItemStack held = player.getHeldItem(hand);
        boolean inPlace = !player.capabilities.isCreativeMode && held.getCount() == 1;
        if (inPlace) {
            ItemStack bound = held.getItem() == BlocksContent.lodestoneCompass ? held : new ItemStack(BlocksContent.lodestoneCompass);
            bind(bound, world, pos);
            if (bound != held) {
                player.setHeldItem(hand, bound);
            }
        } else {
            ItemStack bound = new ItemStack(BlocksContent.lodestoneCompass);
            bind(bound, world, pos);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            if (!player.inventory.addItemStackToInventory(bound)) {
                player.dropItem(bound, false);
            }
        }
        Triggers.fire(player, "use_lodestone");
        return true;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("LodestoneTracked") && !tag.getBoolean("LodestoneTracked")) {
            return;
        }
        BlockPos pos = target(stack);
        if (pos != null && dimension(stack) == world.provider.getDimension() && world.isBlockLoaded(pos)
                && world.getBlockState(pos).getBlock() != BlocksContent.lodestone) {
            tag.removeTag("LodestonePos");
        }
    }
}
