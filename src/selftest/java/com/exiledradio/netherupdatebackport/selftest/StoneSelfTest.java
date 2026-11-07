package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.stone.BasaltGenerator;
import com.exiledradio.netherupdatebackport.stone.StoneContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;

/** Stream S: blackstone and basalt. */
public final class StoneSelfTest implements SelfTest {

    @Override
    public String name() {
        return "stone";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;

        // Registration --------------------------------------------------------------------------------
        for (String name : StoneContent.NAMES) {
            Block block = Owners.ourBlock(name);
            if (!c.ok(block != null, name + " is registered")) {
                continue;
            }
            boolean isDouble = name.endsWith("_double_slab");
            c.eq(!isDouble, Owners.ours(name) != null, name + (isDouble ? " has no item" : " has an item"));
            for (IBlockState state : block.getBlockState().getValidStates()) {
                // Only what metadata stores survives (a stair's shape and a wall's links are computed).
                int meta = block.getMetaFromState(state);
                c.eq(meta, block.getMetaFromState(block.getStateFromMeta(meta)), name + " metadata round trip " + state);
            }
            c.eq(block.getDefaultState(), block.getStateFromMeta(block.getMetaFromState(block.getDefaultState())),
                    name + " default state survives metadata");
        }

        // Strength: 1.16.5's numbers --------------------------------------------------------------------
        strength(c, "basalt", 1.25F, 4.2F);
        strength(c, "polished_basalt", 1.25F, 4.2F);
        strength(c, "blackstone", 1.5F, 6.0F);
        strength(c, "polished_blackstone", 2.0F, 6.0F);
        strength(c, "polished_blackstone_bricks", 1.5F, 6.0F);
        strength(c, "chiseled_polished_blackstone", 1.5F, 6.0F);
        strength(c, "gilded_blackstone", 1.5F, 6.0F);
        strength(c, "blackstone_slab", 2.0F, 6.0F);
        strength(c, "polished_blackstone_brick_slab", 2.0F, 6.0F);
        strength(c, "polished_blackstone_pressure_plate", 0.5F, 0.5F);
        c.eq("pickaxe", block("blackstone").getHarvestTool(block("blackstone").getDefaultState()), "blackstone needs a pickaxe");
        c.ok(block("basalt").getSoundType() != block("blackstone").getSoundType(), "basalt has its own sounds");
        c.eq(new net.minecraft.util.ResourceLocation("netherupdatebackport", "block.basalt.break"),
                block("basalt").getSoundType().getBreakSound().getRegistryName(), "basalt's break sound is registered");

        // Placement by a real player -------------------------------------------------------------------
        EntityPlayerMP player = Players.real(world, "stone");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        BlockPos area = c.area();
        BlockPos floor = area.down();

        world.setBlockState(area, Blocks.STONE.getDefaultState());
        place(player, item("basalt"), area, EnumFacing.EAST, 0.5F);
        c.eq(block("basalt"), world.getBlockState(area.east()).getBlock(), "basalt is placed on a block's side");
        c.eq(EnumFacing.Axis.X, world.getBlockState(area.east()).getValue(BlockRotatedPillar.AXIS), "and lies along that axis");

        BlockPos slabAt = floor.add(3, 0, 0);
        place(player, item("blackstone_slab"), slabAt, EnumFacing.UP, 1.0F);
        IBlockState slab = world.getBlockState(slabAt.up());
        c.eq(block("blackstone_slab"), slab.getBlock(), "a blackstone slab is placed");
        c.eq(BlockSlab.EnumBlockHalf.BOTTOM, slab.getValue(BlockSlab.HALF), "on the bottom half");
        place(player, item("blackstone_slab"), slabAt.up(), EnumFacing.UP, 0.5F);
        c.eq(block("blackstone_double_slab"), world.getBlockState(slabAt.up()).getBlock(), "a second slab makes the double slab");
        NonNullList<ItemStack> drops = NonNullList.create();
        block("blackstone_double_slab").getDrops(drops, world, slabAt.up(), world.getBlockState(slabAt.up()), 0);
        int slabs = 0;
        for (ItemStack d : drops) {
            slabs += d.getItem() == item("blackstone_slab") ? d.getCount() : 100;
        }
        c.eq(2, slabs, "the double slab drops two slabs " + drops);

        BlockPos wallAt = floor.add(6, 1, 0);
        world.setBlockState(wallAt, block("polished_blackstone_wall").getDefaultState());
        world.setBlockState(wallAt.east(), Blocks.COBBLESTONE_WALL.getDefaultState());
        c.ok(world.getBlockState(wallAt).getActualState(world, wallAt).getValue(BlockWall.EAST), "a blackstone wall joins a cobblestone wall");
        c.ok(world.getBlockState(wallAt.east()).getActualState(world, wallAt.east()).getValue(BlockWall.WEST), "and the cobblestone wall joins it");

        // Gilded blackstone ---------------------------------------------------------------------------
        Block gilded = block("gilded_blackstone");
        int nuggetDrops = 0;
        int nuggets = 0;
        int trials = 4000;
        for (int i = 0; i < trials; i++) {
            NonNullList<ItemStack> d = NonNullList.create();
            gilded.getDrops(d, world, area, gilded.getDefaultState(), 0);
            if (d.get(0).getItem() == Items.GOLD_NUGGET) {
                nuggetDrops++;
                nuggets += d.get(0).getCount();
                c.ok(d.get(0).getCount() >= 2 && d.get(0).getCount() <= 5, "2-5 nuggets");
            } else {
                c.eq(item("gilded_blackstone"), d.get(0).getItem(), "or the block itself");
            }
        }
        c.ok(nuggetDrops > trials * 0.08 && nuggetDrops < trials * 0.12, "nuggets about 10% of the time without Fortune: " + nuggetDrops);
        NonNullList<ItemStack> f3 = NonNullList.create();
        gilded.getDrops(f3, world, area, gilded.getDefaultState(), 3);
        c.eq(Items.GOLD_NUGGET, f3.get(0).getItem(), "Fortune III always gives nuggets");
        c.ok(gilded.canSilkHarvest(world, area, gilded.getDefaultState(), player), "Silk Touch keeps gilded blackstone");

        // Recipes ----------------------------------------------------------------------------------
        Recipes.checkAll(c);
        ItemStack bs = new ItemStack(item("blackstone"));
        ItemStack cobble = new ItemStack(Blocks.COBBLESTONE);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack none = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 3, bs, bs, bs, bs, none, bs, bs, bs, bs), new ItemStack(Blocks.FURNACE), "8 blackstone make a furnace");
        Crafting.expect(c, Crafting.grid(3, 3, bs, cobble, bs, none, stick, none, none, stick, none), new ItemStack(Items.STONE_PICKAXE),
                "blackstone and cobblestone mixed make a stone pickaxe");
        Crafting.expect(c, Crafting.grid(2, 2, bs, bs, bs, bs), new ItemStack(item("polished_blackstone"), 4), "4 blackstone make 4 polished blackstone");
        ItemStack b = new ItemStack(item("basalt"));
        Crafting.expect(c, Crafting.grid(2, 2, b, b, b, b), new ItemStack(item("polished_basalt"), 4), "4 basalt make 4 polished basalt");
        ItemStack pb = new ItemStack(item("polished_blackstone"));
        Crafting.expect(c, Crafting.grid(3, 3, pb, none, none, pb, pb, none, pb, pb, pb), new ItemStack(item("polished_blackstone_stairs"), 4), "stairs");
        Crafting.expect(c, Crafting.grid(3, 1, pb, pb, pb), new ItemStack(item("polished_blackstone_slab"), 6), "slabs");
        Crafting.expect(c, Crafting.grid(1, 1, pb), new ItemStack(item("polished_blackstone_button")), "button");
        ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(new ItemStack(item("polished_blackstone_bricks")));
        c.eq(item("cracked_polished_blackstone_bricks"), smelted.getItem(), "polished blackstone bricks smelt to cracked ones");

        // Basalt generator, with stand-ins for soul soil and blue ice (neither is in this test's game) -------------
        basaltGenerator(c, world, floor.add(10, 0, 0));

        // Generation in the nether ---------------------------------------------------------------------
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        int cx = 1500;
        int cz = 1500;
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 2; z++) {
                nether.getChunkProvider().provideChunk(cx + x, cz + z);
            }
        }
        int blackstone = 0;
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                Chunk chunk = nether.getChunk(cx + x, cz + z);
                c.ok(chunk.isTerrainPopulated(), "nether chunk " + (cx + x) + "," + (cz + z) + " is populated");
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int y = 0; y < 48; y++) {
                            if (chunk.getBlockState(bx, y, bz).getBlock() == StoneContent.blackstone) {
                                blackstone++;
                                c.ok(y >= 2 && y <= 40, "blackstone near its range: y=" + y);
                            }
                        }
                    }
                }
            }
        }
        c.ok(blackstone > 0, "freshly generated nether chunks have blackstone: " + blackstone);
    }

    private static void basaltGenerator(Check c, WorldServer world, BlockPos floor) throws Exception {
        Field soil = BasaltGenerator.class.getDeclaredField("soulSoil");
        Field ice = BasaltGenerator.class.getDeclaredField("blueIce");
        Field looked = BasaltGenerator.class.getDeclaredField("looked");
        soil.setAccessible(true);
        ice.setAccessible(true);
        looked.setAccessible(true);
        Object oldSoil = soil.get(null);
        Object oldIce = ice.get(null);
        Object oldLooked = looked.get(null);
        soil.set(null, Blocks.SOUL_SAND);
        ice.set(null, Blocks.PACKED_ICE);
        looked.set(null, true);
        try {
            BlockPos lava = floor.up(2);
            c.fill(lava.add(-2, -1, -2), lava.add(2, 1, 2), Blocks.AIR.getDefaultState());
            c.fill(lava.add(-2, -1, -2), lava.add(2, -1, 2), Blocks.SOUL_SAND.getDefaultState());
            world.setBlockState(lava.north(), Blocks.PACKED_ICE.getDefaultState());
            world.setBlockState(lava, Blocks.LAVA.getDefaultState(), 3);
            BasaltGenerator.worldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));
            c.eq(StoneContent.basalt, world.getBlockState(lava).getBlock(), "lava over soul soil beside blue ice becomes basalt");

            BlockPos away = lava.east(2);
            world.setBlockState(away, Blocks.LAVA.getDefaultState(), 3);
            BasaltGenerator.worldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));
            c.eq(Blocks.LAVA, world.getBlockState(away).getBlock(), "lava without blue ice stays lava");

            BlockPos over = lava.west(2);
            world.setBlockState(over.up(), Blocks.PACKED_ICE.getDefaultState());
            world.setBlockState(over, Blocks.LAVA.getDefaultState(), 3);
            BasaltGenerator.worldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));
            c.eq(StoneContent.basalt, world.getBlockState(over).getBlock(), "blue ice above counts too");

            BlockPos stoneFloor = lava.south(2);
            world.setBlockState(stoneFloor.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(stoneFloor.east(), Blocks.PACKED_ICE.getDefaultState());
            world.setBlockState(stoneFloor, Blocks.LAVA.getDefaultState(), 3);
            BasaltGenerator.worldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));
            c.eq(Blocks.LAVA, world.getBlockState(stoneFloor).getBlock(), "not over soul soil: stays lava");
        } finally {
            soil.set(null, oldSoil);
            ice.set(null, oldIce);
            looked.set(null, oldLooked);
        }
    }

    private static void strength(Check c, String name, float hardness, float blast) {
        Block block = block(name);
        c.eq(hardness, block.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), name + " hardness");
        @SuppressWarnings("deprecation")
        float resistance = block.getExplosionResistance(null);
        c.ok(Math.abs(resistance - blast) < 0.01F, name + " blast resistance: expected " + blast + ", got " + resistance);
    }

    private static void place(EntityPlayerMP player, Item item, BlockPos on, EnumFacing face, float hitY) {
        ItemStack stack = new ItemStack(item);
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, player.world, on, EnumHand.MAIN_HAND, face, 0.5F, hitY, 0.5F);
    }

    private static Block block(String name) {
        return Owners.ourBlock(name);
    }

    private static Item item(String name) {
        return Owners.ours(name);
    }
}
