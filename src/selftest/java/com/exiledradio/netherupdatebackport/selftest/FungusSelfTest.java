package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.fungus.BlockNetherPlant;
import com.exiledradio.netherupdatebackport.fungus.BlockNetherVines;
import com.exiledradio.netherupdatebackport.fungus.FungusContent;
import com.exiledradio.netherupdatebackport.fungus.HugeFungus;
import com.exiledradio.netherupdatebackport.fungus.NetherSigns;
import com.exiledradio.netherupdatebackport.fungus.NetherWood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Random;

/** Stream F: crimson and warped. */
public final class FungusSelfTest implements SelfTest {

    @Override
    public String name() {
        return "fungus";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        for (String name : FungusContent.names()) {
            Block block = Owners.ourBlock(name);
            if (!c.ok(block != null, name + " is registered")) {
                continue;
            }
            boolean noItem = name.endsWith("_double_slab") || name.endsWith("_wall_sign") || name.endsWith("_plant");
            c.eq(!noItem, Owners.ours(name) != null, name + (noItem ? " has no item" : " has an item"));
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int meta = block.getMetaFromState(state);
                c.eq(meta, block.getMetaFromState(block.getStateFromMeta(meta)), name + " metadata round trip " + state);
            }
        }
        hardness(c, "crimson_stem", 2.0F);
        hardness(c, "warped_planks", 2.0F);
        hardness(c, "crimson_door", 3.0F);
        hardness(c, "warped_nylium", 0.4F);
        hardness(c, "shroomlight", 1.0F);
        c.eq(EnumFacing.Axis.Y, FungusContent.stem(NetherWood.CRIMSON).getDefaultState().getValue(BlockRotatedPillar.AXIS),
                "stems stand upright by default (huge fungi use the default)");
        c.eq(15, FungusContent.shroomlight.getDefaultState().getLightValue(world, BlockPos.ORIGIN), "shroomlight gives light 15");

        EntityPlayerMP player = Players.real(world, "fungus");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        Block crimsonNylium = FungusContent.nylium(NetherWood.CRIMSON);
        Block warpedNylium = FungusContent.nylium(NetherWood.WARPED);
        Block crimsonFungus = FungusContent.block("crimson_fungus");

        // Plants on nylium --------------------------------------------------------------------------
        BlockPos area = c.area();
        world.setBlockState(area, crimsonNylium.getDefaultState());
        world.setBlockState(area.east(), Blocks.STONE.getDefaultState());
        c.ok(crimsonFungus.canPlaceBlockAt(world, area.up()), "a fungus can go on nylium");
        c.ok(!crimsonFungus.canPlaceBlockAt(world, area.east().up()), "but not on stone");
        world.setBlockState(area.up(), crimsonFungus.getDefaultState());
        world.notifyNeighborsOfStateChange(area, crimsonNylium, false);
        c.eq(crimsonFungus, world.getBlockState(area.up()).getBlock(), "and stays there");
        c.ok(FungusContent.block("warped_roots").isReplaceable(world, area), "roots report replaceable");

        // Nylium under an opaque block turns to netherrack; it drops netherrack.
        BlockPos covered = area.add(2, 0, 0);
        world.setBlockState(covered, warpedNylium.getDefaultState());
        world.setBlockState(covered.up(), Blocks.STONE.getDefaultState());
        warpedNylium.updateTick(world, covered, world.getBlockState(covered), world.rand);
        c.eq(Blocks.NETHERRACK, world.getBlockState(covered).getBlock(), "nylium under stone becomes netherrack");
        c.eq(Item.getItemFromBlock(Blocks.NETHERRACK), warpedNylium.getItemDropped(warpedNylium.getDefaultState(), world.rand, 0), "nylium drops netherrack");

        // Bone meal ---------------------------------------------------------------------------------
        BlockPos field = c.area();
        c.fill(field.add(0, -1, 0), field.add(10, -1, 10), crimsonNylium.getDefaultState());
        bonemeal(player, world, field.add(5, -1, 5));
        int plants = 0;
        for (BlockPos p : BlockPos.getAllInBox(field, field.add(10, 1, 10))) {
            plants += world.getBlockState(p).getBlock() instanceof BlockNetherPlant ? 1 : 0;
        }
        c.ok(plants > 0, "bone meal on crimson nylium grows plants around it: " + plants);

        BlockPos rack = field.add(0, -1, 12);
        world.setBlockState(rack, Blocks.NETHERRACK.getDefaultState());
        world.setBlockState(rack.east(), warpedNylium.getDefaultState());
        c.ok(bonemeal(player, world, rack), "bone meal on netherrack beside nylium is used");
        c.eq(warpedNylium, world.getBlockState(rack).getBlock(), "and spreads that nylium");
        BlockPos lone = field.add(5, -1, 14);
        world.setBlockState(lone, Blocks.NETHERRACK.getDefaultState());
        c.ok(!bonemeal(player, world, lone), "netherrack with no nylium nearby takes no bone meal");

        // Huge fungi -------------------------------------------------------------------------------
        BlockPos tree = c.area();
        world.setBlockState(tree.down(), crimsonNylium.getDefaultState());
        world.setBlockState(tree, crimsonFungus.getDefaultState());
        c.ok(HugeFungus.place(world, new Random(42), tree, NetherWood.CRIMSON, true), "a huge crimson fungus grows on crimson nylium");
        c.eq(FungusContent.stem(NetherWood.CRIMSON), world.getBlockState(tree).getBlock(), "its stem starts where the fungus was");
        int stem = 0;
        int wart = 0;
        for (BlockPos p : BlockPos.getAllInBox(tree.add(-4, 0, -4), tree.add(4, 30, 4))) {
            Block b = world.getBlockState(p).getBlock();
            stem += b == FungusContent.stem(NetherWood.CRIMSON) ? 1 : 0;
            wart += b == Blocks.NETHER_WART_BLOCK ? 1 : 0;
        }
        c.ok(stem >= 4 && stem <= 26, "a stem 4-26 tall: " + stem);
        c.ok(wart > 5, "and a hat of nether wart blocks: " + wart);
        BlockPos wrong = tree.add(10, 0, 0);
        world.setBlockState(wrong.down(), warpedNylium.getDefaultState());
        c.ok(!HugeFungus.place(world, new Random(1), wrong, NetherWood.CRIMSON, true), "not on the other nylium");
        BlockNetherPlant.Fungus fungusBlock = (BlockNetherPlant.Fungus) crimsonFungus;
        world.setBlockState(wrong, crimsonFungus.getDefaultState());
        c.ok(!fungusBlock.canGrow(world, wrong, world.getBlockState(wrong), false), "a crimson fungus on warped nylium takes no bone meal");
        int hits = 0;
        for (int i = 0; i < 1000; i++) {
            hits += fungusBlock.canUseBonemeal(world, world.rand, tree, crimsonFungus.getDefaultState()) ? 1 : 0;
        }
        c.ok(hits > 340 && hits < 460, "bone meal works 40% of the time: " + hits + "/1000");

        // Vines --------------------------------------------------------------------------------------
        vines(c, world, player);

        // Stripping ----------------------------------------------------------------------------------
        BlockPos log = c.area();
        world.setBlockState(log, FungusContent.stem(NetherWood.WARPED).getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        PlayerInteractEvent.RightClickBlock click = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, log, EnumFacing.UP, new Vec3d(0.5, 1, 0.5));
        MinecraftForge.EVENT_BUS.post(click);
        c.eq(Owners.ourBlock("stripped_warped_stem"), world.getBlockState(log).getBlock(), "an axe strips a warped stem");
        c.eq(EnumFacing.Axis.X, world.getBlockState(log).getValue(BlockRotatedPillar.AXIS), "keeping its axis");
        c.eq(1, player.getHeldItem(EnumHand.MAIN_HAND).getItemDamage(), "and wears the axe");

        // Wood set ------------------------------------------------------------------------------------
        c.eq(0, TileEntityFurnace.getItemBurnTime(new ItemStack(Owners.ours("crimson_planks"))), "crimson planks are not fuel");
        c.eq(0, TileEntityFurnace.getItemBurnTime(new ItemStack(Owners.ours("warped_slab"))), "nor warped slabs");
        c.eq(300, TileEntityFurnace.getItemBurnTime(new ItemStack(Blocks.PLANKS)), "oak planks still are");
        c.eq(0, Blocks.FIRE.getFlammability(Owners.ourBlock("crimson_planks")), "nether wood never catches fire");
        boolean ore = false;
        for (ItemStack s : OreDictionary.getOres("plankWood")) {
            ore |= s.getItem() == Owners.ours("warped_planks");
        }
        c.ok(ore, "warped planks are plankWood");

        BlockPos fenceAt = c.area();
        world.setBlockState(fenceAt, Owners.ourBlock("crimson_fence").getDefaultState());
        world.setBlockState(fenceAt.east(), Blocks.OAK_FENCE.getDefaultState());
        world.setBlockState(fenceAt.west(), Owners.ourBlock("warped_fence").getDefaultState());
        IBlockState fence = world.getBlockState(fenceAt).getActualState(world, fenceAt);
        c.ok(fence.getValue(BlockFence.EAST) && fence.getValue(BlockFence.WEST), "a crimson fence joins oak and warped fences");

        BlockPos doorAt = fenceAt.add(4, -1, 0);
        place(player, Owners.ours("crimson_door"), doorAt, EnumFacing.UP);
        c.eq(Owners.ourBlock("crimson_door"), world.getBlockState(doorAt.up()).getBlock(), "a crimson door is placed");
        c.eq(BlockDoor.EnumDoorHalf.UPPER, world.getBlockState(doorAt.up(2)).getValue(BlockDoor.HALF), "with its upper half");
        c.eq(Owners.ours("crimson_door"), Owners.ourBlock("crimson_door").getItemDropped(world.getBlockState(doorAt.up()), world.rand, 0),
                "and drops itself");

        BlockPos signAt = fenceAt.add(8, -1, 0);
        place(player, Owners.ours("warped_sign"), signAt, EnumFacing.UP);
        c.eq(Owners.ourBlock("warped_sign"), world.getBlockState(signAt.up()).getBlock(), "a warped sign stands on a block");
        c.ok(world.getTileEntity(signAt.up()) instanceof NetherSigns.Tile, "with the nether sign tile");
        c.ok(world.getTileEntity(signAt.up()) instanceof TileEntitySign, "which is a vanilla sign tile");
        world.setBlockState(signAt.add(0, 1, 2), Blocks.STONE.getDefaultState());
        place(player, Owners.ours("warped_sign"), signAt.add(0, 1, 2), EnumFacing.NORTH);
        c.eq(Owners.ourBlock("warped_wall_sign"), world.getBlockState(signAt.add(0, 1, 1)).getBlock(), "and hangs on a side");

        // Recipes -----------------------------------------------------------------------------------
        Recipes.checkAll(c);
        ItemStack none = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(Owners.ours("stripped_crimson_hyphae"))), new ItemStack(Owners.ours("crimson_planks"), 4),
                "any crimson stem or hyphae makes 4 planks");
        ItemStack cp = new ItemStack(Owners.ours("crimson_planks"));
        ItemStack wp = new ItemStack(Owners.ours("warped_planks"));
        ItemStack oak = new ItemStack(Blocks.PLANKS, 1, 2);
        Crafting.expect(c, Crafting.grid(1, 2, cp, wp), new ItemStack(Items.STICK, 4), "crimson and warped planks make sticks");
        Crafting.expect(c, Crafting.grid(2, 2, cp, oak, wp, cp), new ItemStack(Blocks.CRAFTING_TABLE), "mixed with birch they make a crafting table");
        Crafting.expect(c, Crafting.grid(2, 3, cp, cp, cp, cp, cp, cp), new ItemStack(Owners.ours("crimson_door"), 3), "6 planks make 3 doors");
        ItemStack stick = new ItemStack(Items.STICK);
        Crafting.expect(c, Crafting.grid(3, 3, wp, wp, wp, wp, wp, wp, none, stick, none), new ItemStack(Owners.ours("warped_sign"), 3), "3 signs");
        Crafting.expect(c, Crafting.grid(3, 2, cp, cp, cp, cp, cp, cp), new ItemStack(Owners.ours("crimson_trapdoor"), 2), "2 trapdoors, not oak ones");
        Crafting.expect(c, Crafting.grid(1, 1, wp), new ItemStack(Owners.ours("warped_button")), "a warped button, not an oak one");
        Crafting.expect(c, Crafting.grid(2, 1, cp, cp), new ItemStack(Owners.ours("crimson_pressure_plate")), "a crimson pressure plate");
        Crafting.expect(c, Crafting.grid(3, 1, wp, wp, wp), new ItemStack(Owners.ours("warped_slab"), 6), "6 warped slabs");
        ItemStack oakPlanks = new ItemStack(Blocks.PLANKS);
        Crafting.expect(c, Crafting.grid(3, 3, oakPlanks, oakPlanks, oakPlanks, oakPlanks, oakPlanks, oakPlanks, none, stick, none),
                new ItemStack(Items.SIGN, 3), "oak planks still make oak signs");
    }

    private static void vines(Check c, WorldServer world, EntityPlayerMP player) {
        BlockNetherVines.Head weeping = FungusContent.weepingVines;
        Block weepingPlant = Owners.ourBlock("weeping_vines_plant");
        BlockPos ceiling = c.area().up(20);
        world.setBlockState(ceiling, Blocks.NETHERRACK.getDefaultState());
        BlockPos tip = ceiling.down();
        c.ok(weeping.canPlaceBlockAt(world, tip), "weeping vines hang from a ceiling");
        c.ok(!weeping.canPlaceBlockAt(world, tip.down(3)), "not from nothing");
        world.setBlockState(tip, weeping.getDefaultState().withProperty(BlockNetherVines.AGE, 0));
        int grown = 0;
        Random rand = new Random(7);
        for (int i = 0; i < 400; i++) {
            BlockPos head = tip;
            while (world.getBlockState(head.down()).getBlock() == weeping || world.getBlockState(head.down()).getBlock() == weepingPlant) {
                head = head.down();
            }
            if (world.getBlockState(head).getBlock() != weeping) {
                break;
            }
            weeping.updateTick(world, head, world.getBlockState(head), rand);
        }
        BlockPos p = tip;
        while (world.getBlockState(p).getBlock() == weeping || world.getBlockState(p).getBlock() == weepingPlant) {
            grown++;
            p = p.down();
        }
        c.ok(grown > 3, "random ticks grow the vine down: " + grown + " blocks");
        c.eq(weeping, world.getBlockState(p.up()).getBlock(), "with a tip at the bottom");
        c.eq(weepingPlant, world.getBlockState(tip).getBlock(), "and body above it");
        c.ok(weeping.isLadder(world.getBlockState(tip), world, tip, player), "vines are climbable");
        world.setBlockToAir(ceiling);
        for (int i = 0; i < 40; i++) {
            world.tickUpdates(true);
        }
        c.ok(world.isAirBlock(tip) && world.isAirBlock(tip.down()), "without its ceiling the vine falls");

        // Bone meal and shears on twisting vines.
        BlockNetherVines.Head twisting = FungusContent.twistingVines;
        BlockPos floor = c.area();
        world.setBlockState(floor, Owners.ourBlock("warped_nylium").getDefaultState());
        world.setBlockState(floor.up(), twisting.getDefaultState().withProperty(BlockNetherVines.AGE, 0));
        bonemeal(player, world, floor.up());
        c.ok(world.getBlockState(floor.up()).getBlock() == Owners.ourBlock("twisting_vines_plant")
                || world.getBlockState(floor.up(2)).getBlock() == twisting || world.getBlockState(floor.up()).getBlock() == twisting,
                "bone meal grows twisting vines upward");
        BlockPos top = floor.up();
        while (world.getBlockState(top.up()).getBlock() == twisting || world.getBlockState(top.up()).getBlock() == Owners.ourBlock("twisting_vines_plant")) {
            top = top.up();
        }
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        twisting.onBlockActivated(world, top, world.getBlockState(top), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.eq(BlockNetherVines.MAX_AGE, world.getBlockState(top).getValue(BlockNetherVines.AGE).intValue(), "shears on the tip stop its growth");
        IBlockState stopped = world.getBlockState(top);
        twisting.updateTick(world, top, stopped, new Random(0));
        c.ok(world.isAirBlock(top.up()), "a stopped tip does not grow");
    }

    private static boolean bonemeal(EntityPlayerMP player, WorldServer world, BlockPos pos) {
        ItemStack meal = new ItemStack(Items.DYE, 1, 15);
        return ItemDye.applyBonemeal(meal, world, pos, player, EnumHand.MAIN_HAND);
    }

    private static void hardness(Check c, String name, float hardness) {
        c.eq(hardness, Owners.ourBlock(name).getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), name + " hardness");
    }

    private static void place(EntityPlayerMP player, Item item, BlockPos on, EnumFacing face) {
        ItemStack stack = new ItemStack(item);
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, player.world, on, EnumHand.MAIN_HAND, face, 0.5F, 0.5F, 0.5F);
    }
}
