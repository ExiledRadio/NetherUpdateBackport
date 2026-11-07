package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.changes.ChangesContent;
import com.exiledradio.netherupdatebackport.changes.HoeTool;
import com.exiledradio.netherupdatebackport.changes.LavaVision;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

/** Stream C: hoes as a tool, and the lava fog numbers (the fog itself is client side). */
public final class ChangesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "changes";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        if (!c.ok(ChangesContent.hoes(), "hoes are made a tool at startup")) {
            return;
        }

        // Harvest tool ----------------------------------------------------------------------------------------
        Block[] vanilla = {Blocks.LEAVES, Blocks.LEAVES2, Blocks.HAY_BLOCK, Blocks.NETHER_WART_BLOCK, Blocks.SPONGE};
        for (Block block : vanilla) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                c.eq("hoe", block.getHarvestTool(state), "harvest tool of " + state);
            }
        }
        for (String name : new String[]{"target", "warped_wart_block", "shroomlight"}) {
            Block ours = Owners.ourBlock(name);
            if (c.ok(ours != null, "our " + name + " exists")) {
                c.eq("hoe", ours.getHarvestTool(ours.getDefaultState()), "harvest tool of our " + name);
            }
        }
        Block kelp = Owners.first(Owners.UPDATE_AQUATIC + ":dried_kelp_block");
        if (kelp != null) {
            c.eq("hoe", kelp.getHarvestTool(kelp.getDefaultState()), "harvest tool of the dried kelp block, found by name");
        } else {
            c.ok(!Owners.present(Owners.UPDATE_AQUATIC), "no dried kelp block without the mod that owns it");
        }
        c.eq("pickaxe", Blocks.STONE.getHarvestTool(Blocks.STONE.getDefaultState()), "stone keeps the pickaxe");

        Item[] hoes = {Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, Items.GOLDEN_HOE};
        int[] levels = {0, 1, 2, 3, 0};
        float[] speeds = {2.0F, 4.0F, 6.0F, 8.0F, 12.0F};
        for (int i = 0; i < hoes.length; i++) {
            ItemStack stack = new ItemStack(hoes[i]);
            c.ok(hoes[i].getToolClasses(stack).contains("hoe"), hoes[i].getRegistryName() + " is a hoe-class tool");
            c.eq(levels[i], hoes[i].getHarvestLevel(stack, "hoe", null, null), hoes[i].getRegistryName() + " harvest level");
            c.eq(speeds[i], HoeTool.efficiency(hoes[i]), hoes[i].getRegistryName() + " efficiency");
        }
        Item netherite = Owners.ours("netherite_hoe");
        if (c.ok(netherite != null, "the netherite hoe exists")) {
            c.ok(netherite.getToolClasses(new ItemStack(netherite)).contains("hoe"), "the netherite hoe is a hoe-class tool too");
            c.ok(HoeTool.efficiency(netherite) > 8.0F, "and digs faster than diamond: " + HoeTool.efficiency(netherite));
        }

        // Speed -------------------------------------------------------------------------------------------------
        BlockPos at = c.area();
        EntityPlayerMP player = Players.real(world, "farmer");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.onGround = true;
        BlockPos pos = at.add(1, 0, 0);
        IBlockState hay = Blocks.HAY_BLOCK.getDefaultState();
        world.setBlockState(pos, hay, 2);
        float hand = speed(player, ItemStack.EMPTY, hay, pos);
        c.ok(hand > 0.0F, "bare hand digs hay at " + hand);
        for (int i = 0; i < hoes.length; i++) {
            c.eq(speeds[i], round(speed(player, new ItemStack(hoes[i]), hay, pos) / hand), hoes[i].getRegistryName() + " on hay, times hand");
        }
        ItemStack enchanted = new ItemStack(Items.IRON_HOE);
        enchanted.addEnchantment(Enchantments.EFFICIENCY, 3);
        c.eq(16.0F, round(speed(player, enchanted, hay, pos) / hand), "Efficiency III iron hoe on hay: 6 + 3*3 + 1");
        c.eq(2.0F, round(speed(player, new ItemStack(Items.WOODEN_HOE), Blocks.LEAVES.getDefaultState(), pos) / hand), "wooden hoe on oak leaves");
        IBlockState wet = Blocks.SPONGE.getDefaultState().withProperty(BlockSponge.WET, true);
        c.eq(6.0F, round(speed(player, new ItemStack(Items.IRON_HOE), wet, pos) / hand), "iron hoe on a wet sponge");
        c.eq(4.0F, round(speed(player, new ItemStack(Items.STONE_HOE), Blocks.NETHER_WART_BLOCK.getDefaultState(), pos) / hand), "stone hoe on a nether wart block");
        Block target = Owners.ourBlock("target");
        if (target != null) {
            c.eq(8.0F, round(speed(player, new ItemStack(Items.DIAMOND_HOE), target.getDefaultState(), pos) / hand), "diamond hoe on a target");
        }
        float stoneHand = speed(player, ItemStack.EMPTY, Blocks.STONE.getDefaultState(), pos);
        c.eq(1.0F, round(speed(player, new ItemStack(Items.DIAMOND_HOE), Blocks.STONE.getDefaultState(), pos) / stoneHand), "a hoe is no faster on stone");
        c.eq(1.0F, round(speed(player, new ItemStack(Items.STICK), hay, pos) / hand), "a stick is no faster on hay");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        world.setBlockState(pos, Blocks.LEAVES.getDefaultState(), 2);
        c.ok(ForgeHooks.canHarvestBlock(Blocks.LEAVES, player, world, pos), "leaves are still harvestable by hand (no tool needed)");
        world.setBlockState(pos, hay, 2);
        c.ok(ForgeHooks.canHarvestBlock(Blocks.HAY_BLOCK, player, world, pos), "so are hay bales");

        // Durability --------------------------------------------------------------------------------------------
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        player.setHeldItem(EnumHand.MAIN_HAND, hoe);
        world.setBlockState(pos, hay, 2);
        c.ok(player.interactionManager.tryHarvestBlock(pos), "the player breaks a hay bale");
        c.eq(Blocks.AIR, world.getBlockState(pos).getBlock(), "it is gone");
        c.eq(1, hoe.getItemDamage(), "breaking hay wears the hoe by 1");
        world.setBlockState(pos, Blocks.STONE.getDefaultState(), 2);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(2, hoe.getItemDamage(), "any block that is not instant wears it by 1 (stone)");
        world.setBlockState(pos, Blocks.RED_FLOWER.getDefaultState(), 2);
        c.ok(player.interactionManager.tryHarvestBlock(pos), "the player breaks a flower");
        c.eq(2, hoe.getItemDamage(), "an instantly broken block costs nothing");
        player.interactionManager.setGameType(GameType.CREATIVE);
        world.setBlockState(pos, hay, 2);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(2, hoe.getItemDamage(), "a creative player's hoe does not wear");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        ItemStack stick = new ItemStack(Items.STICK);
        player.setHeldItem(EnumHand.MAIN_HAND, stick);
        world.setBlockState(pos, hay, 2);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(1, stick.getCount(), "a stick is untouched");

        ItemStack weapon = new ItemStack(Items.IRON_HOE);
        player.setHeldItem(EnumHand.MAIN_HAND, weapon);
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        world.spawnEntity(pig);
        player.attackTargetEntityWithCurrentItem(pig);
        c.ok(pig.getHealth() < pig.getMaxHealth(), "the hit lands");
        c.eq(2, weapon.getItemDamage(), "a hit wears a hoe by 2, as any 1.16 tool");
        pig.setDead();

        // Lava fog ----------------------------------------------------------------------------------------------
        c.eq(0.0F, LavaVision.start(false, true, 160.0F), "Fire Resistance: lava fog from 0");
        c.eq(5.0F, LavaVision.end(false, true, 160.0F), "to 5 blocks");
        c.eq(0.25F, LavaVision.start(false, false, 160.0F), "without it: from 0.25");
        c.eq(1.0F, LavaVision.end(false, false, 160.0F), "to 1 block");
        c.eq(-8.0F, LavaVision.start(true, false, 160.0F), "a spectator: from -8");
        c.eq(80.0F, LavaVision.end(true, false, 160.0F), "to half the render distance");
    }

    private static float speed(EntityPlayerMP player, ItemStack stack, IBlockState state, BlockPos pos) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        return player.getDigSpeed(state, pos);
    }

    private static float round(float f) {
        return Math.round(f * 100.0F) / 100.0F;
    }
}
