package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.soul.campfire.BlockSoulCampfire;
import com.exiledradio.netherupdatebackport.soul.campfire.TileEntitySoulCampfire;

import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Waterlogging (1.16.5's rules over Fluidlogged API): which of our blocks and states hold water, placing in water,
 * buckets, flowing water, and the soul campfire and soul fire with water.
 */
public final class WaterlogSelfTest implements SelfTest {

    @Override
    public String name() {
        return "waterlog";
    }

    /** @return whether 1.16.5 makes this block of ours waterloggable (any state but a double slab) */
    static boolean waterloggable(String path) {
        if (path.endsWith("_double_slab")) {
            return false;
        }
        return path.equals("chain") || path.equals("soul_lantern") || path.equals("soul_campfire")
                || path.endsWith("_slab") || path.endsWith("_stairs") || path.endsWith("_wall")
                || path.equals("crimson_fence") || path.equals("warped_fence")
                || path.endsWith("_trapdoor") || path.endsWith("_sign");
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(Loader.isModLoaded("fluidlogged_api"), "Fluidlogged API is loaded");

        // 1. Which states hold water: every state of every block of ours ------------------------------------------
        BlockPos probe = c.area().add(16, 1, 16);
        FluidState source = FluidState.of(FluidRegistry.WATER);
        FluidState flowing = source.withLevel(3);
        FluidState lava = FluidState.of(FluidRegistry.LAVA);
        int states = 0;
        int holding = 0;
        List<String> wrong = new ArrayList<String>();
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation name = block.getRegistryName();
            if (name == null || !NetherUpdateBackport.MOD_ID.equals(name.getNamespace())) {
                continue;
            }
            boolean expected = waterloggable(name.getPath());
            if (expected) {
                c.ok(block instanceof Waterloggable, name + " is Waterloggable, as 1.16 waterlogs it");
            }
            for (IBlockState state : block.getBlockState().getValidStates()) {
                states++;
                boolean can = Waterlogging.canHoldWater(state, world, probe);
                if (can) {
                    holding++;
                }
                if (can != expected) {
                    wrong.add(state + " holds water: " + can);
                }
                if (FluidloggedUtils.isStateFluidloggable(state, world, probe, flowing)) {
                    wrong.add(state + " takes flowing water");
                }
                if (FluidloggedUtils.isStateFluidloggable(state, world, probe, lava)) {
                    wrong.add(state + " takes lava");
                }
            }
        }
        c.ok(wrong.isEmpty(), states + " states of ours checked against 1.16.5 (" + holding + " hold water), wrong: " + wrong);
        c.ok(holding > 100, "a fair number of states hold water: " + holding);
        NetherUpdateBackport.LOGGER.info("SELFTEST waterlog: {} states of ours, {} hold a water source, none flowing water or lava", states, holding);
        // Fluidlogged API on its own lets flowing water into vanilla stairs: what this mod changes for its own.
        NetherUpdateBackport.LOGGER.info("SELFTEST waterlog: vanilla oak stairs take flowing water under Fluidlogged API's own rules: {}",
                FluidloggedUtils.isStateFluidloggable(Blocks.OAK_STAIRS.getDefaultState(), world, probe, flowing));

        Block stairs = Owners.ourBlock("blackstone_stairs");
        Block slab = Owners.ourBlock("polished_blackstone_slab");
        Block doubleSlab = Owners.ourBlock("polished_blackstone_double_slab");
        Block wall = Owners.ourBlock("blackstone_wall");
        Block fence = Owners.ourBlock("crimson_fence");
        Block trapdoor = Owners.ourBlock("warped_trapdoor");
        Block sign = Owners.ourBlock("crimson_sign");
        Block wallSign = Owners.ourBlock("warped_wall_sign");
        Block chain = Owners.ourBlock("chain");
        Block lantern = Owners.ourBlock("soul_lantern");
        Block campfire = Owners.ourBlock("soul_campfire");
        Block gate = Owners.ourBlock("crimson_fence_gate");
        Block soulFire = Owners.ourBlock("soul_fire");
        Block soulSoil = Owners.ourBlock("soul_soil");
        if (!c.ok(stairs != null && slab != null && doubleSlab != null && wall != null && fence != null && trapdoor != null && sign != null
                && wallSign != null && chain != null && lantern != null && campfire != null && gate != null && soulFire != null && soulSoil != null,
                "every block this test uses exists")) {
            return;
        }

        // 2. Placed into a water source: kept. Into flowing water: replaced. ---------------------------------------
        BlockPos pool = pool(c);
        Block[] wet = {stairs, slab, wall, fence, trapdoor, sign, wallSign, chain, lantern, campfire};
        for (int i = 0; i < wet.length; i++) {
            BlockPos p = pool.add(1 + i, 1, 1);
            IBlockState state = wet[i] == slab ? slab.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP) : wet[i].getDefaultState();
            world.setBlockState(p, state, 2);
            c.ok(world.getBlockState(p).getBlock() == wet[i] && Waterlogging.hasWaterSource(world, p), wet[i].getRegistryName() + " placed into a water source keeps it");
        }
        world.setBlockState(pool.add(1, 1, 3), doubleSlab.getDefaultState(), 2);
        c.ok(!Waterlogging.hasWater(world, pool.add(1, 1, 3)), "a double slab placed into water replaces it");
        world.setBlockState(pool.add(2, 1, 3), gate.getDefaultState(), 2);
        c.ok(!Waterlogging.hasWater(world, pool.add(2, 1, 3)), "a fence gate placed into water replaces it (1.16 fence gates hold none)");
        IBlockState placedWet = campfire.getStateForPlacement(world, pool.add(3, 1, 3), EnumFacing.UP, 0.5F, 0.5F, 0.5F, 0,
                Players.real(world, "campfireplacer"));
        c.eq(false, placedWet.getValue(BlockSoulCampfire.LIT), "a soul campfire placed into a water source is unlit");
        BlockPos dry = c.area().add(4, 0, 4);
        IBlockState placedDry = campfire.getStateForPlacement(world, dry, EnumFacing.UP, 0.5F, 0.5F, 0.5F, 0,
                Players.real(world, "campfireplacer2"));
        c.eq(true, placedDry.getValue(BlockSoulCampfire.LIT), "placed on dry land it is lit");

        BlockPos runs = c.area();
        for (int i = 0; i < 4; i++) {
            BlockPos p = runs.add(2 + 2 * i, 0, 2);
            world.setBlockState(p, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 3), 2);
            Block b = i == 0 ? stairs : i == 1 ? fence : i == 2 ? lantern : chain;
            world.setBlockState(p, b.getDefaultState(), 2);
            c.ok(world.getBlockState(p).getBlock() == b && !Waterlogging.hasWater(world, p), b.getRegistryName() + " placed into flowing water replaces it");
        }

        // 3. Flowing water does not run into a dry block; water flows out of a waterlogged one --------------------
        BlockPos flat = c.area();
        BlockPos spring = flat.add(10, 0, 10);
        world.setBlockState(spring.east(), fence.getDefaultState(), 3);
        world.setBlockState(spring.west(), slab.getDefaultState(), 3);
        world.setBlockState(spring.north(), lantern.getDefaultState(), 3);
        world.setBlockState(spring, Blocks.FLOWING_WATER.getDefaultState(), 3);
        settle(world);
        c.ok(Waterlogging.hasWater(world, spring.south()), "the spring's water spreads");
        c.ok(!Waterlogging.hasWater(world, spring.east()), "but not into a dry fence beside it");
        c.ok(!Waterlogging.hasWater(world, spring.west()), "nor into a dry bottom slab");
        c.ok(!Waterlogging.hasWater(world, spring.north()), "nor into a dry soul lantern");
        BlockPos logged = flat.add(20, 0, 20);
        Waterlogging.placeWithWater(world, logged, slab.getDefaultState(), 3);
        settle(world);
        c.ok(Waterlogging.hasWaterSource(world, logged), "a waterlogged bottom slab keeps its source");
        c.ok(Waterlogging.hasWater(world, logged.east()) && !Waterlogging.hasWaterSource(world, logged.east()),
                "and its water flows out beside it");

        // 4. Buckets -------------------------------------------------------------------------------------------------
        BlockPos cups = c.area();
        EntityPlayerMP player = Players.real(world, "waterbucket");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        BlockPos s = cup(world, cups.add(2, 0, 2));
        world.setBlockState(s, stairs.getDefaultState(), 3);
        c.eq(Items.BUCKET, bucket(player, s, Items.WATER_BUCKET).getItem(), "a water bucket on dry stairs comes back empty");
        c.ok(world.getBlockState(s).getBlock() == stairs && Waterlogging.hasWaterSource(world, s), "and waterlogs them");
        c.ok(!Waterlogging.hasWater(world, s.up()), "putting no water above them");
        c.eq(Items.WATER_BUCKET, bucket(player, s, Items.BUCKET).getItem(), "an empty bucket takes the water back");
        c.ok(world.getBlockState(s).getBlock() == stairs && !Waterlogging.hasWater(world, s), "leaving the stairs dry");

        BlockPos d = cup(world, cups.add(6, 0, 2));
        world.setBlockState(d, doubleSlab.getDefaultState(), 3);
        bucket(player, d, Items.WATER_BUCKET);
        c.ok(!Waterlogging.hasWater(world, d) && Waterlogging.hasWater(world, d.up()), "a water bucket on a double slab pours the water on top of it");

        // A lit soul campfire with food: the bucket waterlogs it, puts it out, and the food stays.
        BlockPos f = cup(world, cups.add(10, 0, 2));
        world.setBlockState(f, campfire.getDefaultState().withProperty(BlockSoulCampfire.LIT, Boolean.TRUE), 3);
        TileEntitySoulCampfire tile = (TileEntitySoulCampfire) world.getTileEntity(f);
        c.ok(tile != null && tile.placeFood(new ItemStack(Items.BEEF), 600), "beef goes on the soul campfire");
        c.ok(BlockSoulCampfire.isLit(world.getBlockState(f)), "lit");
        c.eq(Items.BUCKET, bucket(player, f, Items.WATER_BUCKET).getItem(), "a water bucket on a lit soul campfire comes back empty");
        c.ok(Waterlogging.hasWaterSource(world, f) && world.getBlockState(f).getBlock() == campfire, "and waterlogs it");
        c.eq(false, world.getBlockState(f).getValue(BlockSoulCampfire.LIT), "which puts it out");
        TileEntitySoulCampfire after = (TileEntitySoulCampfire) world.getTileEntity(f);
        c.ok(after != null && after.items().get(0).getItem() == Items.BEEF || after != null && containsBeef(after),
                "the beef stays on it");
        // Waterlogged, it cannot be lit.
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        boolean used = campfire.onBlockActivated(world, f, world.getBlockState(f), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(!used && !BlockSoulCampfire.isLit(world.getBlockState(f)), "flint and steel does not light a waterlogged soul campfire");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FIRE_CHARGE));
        used = campfire.onBlockActivated(world, f, world.getBlockState(f), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(!used && !BlockSoulCampfire.isLit(world.getBlockState(f)), "nor does a fire charge");
        BlockSoulCampfire.ignite(world, f, world.getBlockState(f));
        c.ok(!BlockSoulCampfire.isLit(world.getBlockState(f)), "nor a burning arrow");
        c.eq(Items.WATER_BUCKET, bucket(player, f, Items.BUCKET).getItem(), "an empty bucket takes its water");
        c.ok(!Waterlogging.hasWater(world, f) && !BlockSoulCampfire.isLit(world.getBlockState(f)), "and it stays out, dry");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        used = campfire.onBlockActivated(world, f, world.getBlockState(f), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(used && BlockSoulCampfire.isLit(world.getBlockState(f)), "dry again, flint and steel lights it");
        // An unlit dry one is waterlogged by the bucket too.
        BlockPos u = cup(world, cups.add(14, 0, 2));
        world.setBlockState(u, campfire.getDefaultState().withProperty(BlockSoulCampfire.LIT, Boolean.FALSE), 3);
        bucket(player, u, Items.WATER_BUCKET);
        c.ok(Waterlogging.hasWaterSource(world, u) && !BlockSoulCampfire.isLit(world.getBlockState(u)), "an unlit soul campfire takes water from a bucket");
        // Water arriving any other way (a command, a source) puts a lit one out too.
        BlockPos w = cup(world, cups.add(18, 0, 2));
        world.setBlockState(w, campfire.getDefaultState().withProperty(BlockSoulCampfire.LIT, Boolean.TRUE), 3);
        Waterlogging.setWater(world, w, true, 3);
        c.ok(Waterlogging.hasWaterSource(world, w) && !BlockSoulCampfire.isLit(world.getBlockState(w)), "water set by a command puts a lit soul campfire out");

        // 5. A slab placed onto a waterlogged slab: a double slab, and the water is gone ---------------------------
        BlockPos m = cup(world, cups.add(2, 0, 8));
        Waterlogging.placeWithWater(world, m, slab.getDefaultState(), 3);
        c.ok(Waterlogging.hasWaterSource(world, m), "a waterlogged bottom slab");
        ItemStack slabs = new ItemStack(Item.getItemFromBlock(slab), 4);
        player.setHeldItem(EnumHand.MAIN_HAND, slabs);
        player.setLocationAndAngles(m.getX() + 0.5D, m.getY() + 1.6D, m.getZ() + 0.5D, 0.0F, 90.0F);
        slabs.onItemUse(player, world, m, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.eq(doubleSlab, world.getBlockState(m).getBlock(), "a second slab makes it a double slab");
        c.ok(!Waterlogging.hasWater(world, m), "with no water left in it");
        settle(world);
        c.ok(!Waterlogging.hasWater(world, m.up()), "and none put above it");

        // 6. Fire and water: water runs over soul fire; a lit campfire beside water stays lit and dry ---------------
        BlockPos fireRow = c.area();
        BlockPos fireAt = fireRow.add(6, 0, 6);
        world.setBlockState(fireAt.down(), soulSoil.getDefaultState(), 2);
        world.setBlockState(fireAt, soulFire.getDefaultState(), 3);
        c.eq(soulFire, world.getBlockState(fireAt).getBlock(), "soul fire burns on soul soil");
        BlockPos beside = fireAt.add(4, 0, 0);
        world.setBlockState(beside, campfire.getDefaultState().withProperty(BlockSoulCampfire.LIT, Boolean.TRUE), 3);
        world.setBlockState(fireAt.west(), Blocks.FLOWING_WATER.getDefaultState(), 3);
        world.setBlockState(beside.west(), Blocks.FLOWING_WATER.getDefaultState(), 3);
        settle(world);
        c.ok(world.getBlockState(fireAt).getBlock() != soulFire && Waterlogging.hasWater(world, fireAt), "water flowing in puts soul fire out");
        c.ok(BlockSoulCampfire.isLit(world.getBlockState(beside)) && !Waterlogging.hasWater(world, beside),
                "a lit soul campfire beside water stays lit and dry");
    }

    private static boolean containsBeef(TileEntitySoulCampfire tile) {
        for (ItemStack stack : tile.items()) {
            if (stack.getItem() == Items.BEEF) {
                return true;
            }
        }
        return false;
    }

    /** Uses a bucket on the block at pos, looking straight down at it from above. @return the bucket after */
    private static ItemStack bucket(EntityPlayerMP player, BlockPos pos, Item bucket) {
        player.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D, 0.0F, 90.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(bucket));
        ActionResult<ItemStack> result = bucket.onItemRightClick(player.world, player, EnumHand.MAIN_HAND);
        return result.getResult();
    }

    /** A stone cup: stone below and on the four sides of pos, air above. @return pos */
    private static BlockPos cup(WorldServer world, BlockPos pos) {
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            world.setBlockState(pos.offset(side), Blocks.STONE.getDefaultState(), 2);
        }
        return pos;
    }

    /** A 12x12 pool of still water one deep, walled with stone, on a fresh area's floor. @return its floor corner */
    private static BlockPos pool(Check c) {
        BlockPos corner = c.area();
        for (int x = 0; x < 14; x++) {
            for (int z = 0; z < 14; z++) {
                boolean edge = x == 0 || z == 0 || x == 13 || z == 13;
                c.world.setBlockState(corner.add(x, 0, z), (edge ? Blocks.STONE : Blocks.WATER).getDefaultState(), 2 | 16);
            }
        }
        return corner.add(0, -1, 0);
    }

    /** Runs every pending block tick now, a few rounds, so water finishes flowing. */
    private static void settle(WorldServer world) {
        for (int i = 0; i < 40; i++) {
            world.tickUpdates(true);
        }
    }
}
