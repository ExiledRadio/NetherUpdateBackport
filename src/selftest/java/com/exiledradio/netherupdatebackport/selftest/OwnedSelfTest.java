package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.netherite.NetheriteContent;
import com.exiledradio.netherupdatebackport.owned.ContainerSmithingTable;
import com.exiledradio.netherupdatebackport.owned.OwnedContent;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;

import java.util.Collections;

/** Owner copies (Rule 1): Village & Pillage's smithing table, with 1.16's upgrade screen. */
public final class OwnedSelfTest implements SelfTest {

    @Override
    public String name() {
        return "owned";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(OwnedContent.copies(), "this test game has no Village & Pillage, so the copies are registered");
        Block table = Owners.ourBlock("smithing_table");
        if (!c.ok(table != null && Owners.ours("smithing_table") != null, "the smithing table copy and its item exist")) {
            return;
        }
        c.eq(1, table.getBlockState().getValidStates().size(), "one state, as the owner's");
        c.eq(0, table.getMetaFromState(table.getDefaultState()), "metadata 0");
        c.eq(2.5F, table.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "hardness 2.5, as the owner's");

        Recipes.checkAll(c);
        ItemStack i = new ItemStack(Items.IRON_INGOT);
        ItemStack p = new ItemStack(Blocks.PLANKS, 1, 1);
        Crafting.expect(c, Crafting.grid(2, 3, i, i, p, p, p, p), new ItemStack(table), "2 iron and 4 planks make a smithing table");

        EntityPlayerMP player = Players.real(world, "smith");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        BlockPos at = c.area();
        world.setBlockState(at, table.getDefaultState());
        player.setPosition(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
        // Opening it for real sends a packet this test's player cannot carry; the pieces are checked instead.
        c.ok(com.exiledradio.backportcore.smithing.SmithingUpgrades.isActive(), "with netherite in the game the table is active");
        c.ok(new com.exiledradio.netherupdatebackport.common.GuiHandler().getServerGuiElement(
                com.exiledradio.netherupdatebackport.common.GuiHandler.SMITHING, player, world, at.getX(), at.getY(), at.getZ())
                instanceof ContainerSmithingTable, "and its screen is the upgrade screen");

        ContainerSmithingTable smithing = new ContainerSmithingTable(player.inventory, world, at);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setItemDamage(250);
        sword.setStackDisplayName("Old Faithful");
        EnchantmentHelper.setEnchantments(Collections.singletonMap(Enchantments.SHARPNESS, 5), sword);
        smithing.getSlot(ContainerSmithingTable.SLOT_BASE).putStack(sword);
        smithing.getSlot(ContainerSmithingTable.SLOT_ADDITION).putStack(new ItemStack(NetheriteContent.ingot, 2));
        Slot result = smithing.getSlot(ContainerSmithingTable.SLOT_RESULT);
        ItemStack out = result.getStack();
        c.eq(Owners.ours("netherite_sword"), out.getItem(), "a diamond sword and a netherite ingot show a netherite sword");
        c.eq(5, EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, out), "keeping Sharpness V");
        c.eq("Old Faithful", out.getDisplayName(), "its name");
        c.eq(250, out.getItemDamage(), "and its damage");
        result.onTake(player, result.decrStackSize(1));
        c.ok(smithing.getSlot(ContainerSmithingTable.SLOT_BASE).getStack().isEmpty(), "taking it uses the sword");
        c.eq(1, smithing.getSlot(ContainerSmithingTable.SLOT_ADDITION).getStack().getCount(), "and one ingot");
        smithing.getSlot(ContainerSmithingTable.SLOT_BASE).putStack(new ItemStack(Items.IRON_SWORD));
        c.ok(smithing.getSlot(ContainerSmithingTable.SLOT_RESULT).getStack().isEmpty(), "an iron sword upgrades to nothing");
    }
}
