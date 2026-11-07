package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.soul.BlockSoulLantern;
import com.exiledradio.netherupdatebackport.soul.SoulContent;
import com.exiledradio.netherupdatebackport.soul.SoulSpeed;
import com.exiledradio.netherupdatebackport.soul.campfire.BlockSoulCampfire;
import com.exiledradio.netherupdatebackport.soul.campfire.SoulCampfireContent;
import com.exiledradio.netherupdatebackport.soul.campfire.TileEntitySoulCampfire;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collections;

/** Stream L: soul fire and its light sources, Soul Speed. */
public final class SoulSelfTest implements SelfTest {

    @Override
    public String name() {
        return "soul";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        world.getMinecraftServer().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
        for (String name : SoulContent.NAMES) {
            Block block = Owners.ourBlock(name);
            if (!c.ok(block != null, name + " is registered")) {
                continue;
            }
            c.eq(!"soul_fire".equals(name), Owners.ours(name) != null, name + " item");
            for (IBlockState state : block.getBlockState().getValidStates()) {
                int meta = block.getMetaFromState(state);
                c.eq(meta, block.getMetaFromState(block.getStateFromMeta(meta)), name + " metadata round trip " + state);
            }
        }
        c.eq(0.5F, SoulContent.soulSoil.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "soul soil hardness");
        c.eq("shovel", SoulContent.soulSoil.getHarvestTool(SoulContent.soulSoil.getDefaultState()), "soul soil wants a shovel");
        for (Block b : new Block[] {SoulContent.soulFire, SoulContent.soulTorch, SoulContent.soulLantern}) {
            c.eq(10, b.getDefaultState().getLightValue(world, BlockPos.ORIGIN), b.getRegistryName() + " gives light 10");
        }

        EntityPlayerMP player = Players.real(world, "soul");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        BlockPos area = c.area();
        TickEvent.WorldTickEvent end = new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world);

        // Soul fire ---------------------------------------------------------------------------------
        BlockPos sand = area.add(1, -1, 1);
        world.setBlockState(sand, Blocks.SOUL_SAND.getDefaultState());
        world.setBlockState(sand.up(), Blocks.FIRE.getDefaultState(), 3);
        SoulContent.worldTick(end);
        c.eq(SoulContent.soulFire, world.getBlockState(sand.up()).getBlock(), "fire on soul sand becomes soul fire");
        BlockPos soil = area.add(3, -1, 1);
        world.setBlockState(soil, SoulContent.soulSoil.getDefaultState());
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        Items.FLINT_AND_STEEL.onItemUse(player, world, soil, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1F, 0.5F);
        SoulContent.worldTick(end);
        c.eq(SoulContent.soulFire, world.getBlockState(soil.up()).getBlock(), "flint and steel on soul soil lights soul fire");
        BlockPos rack = area.add(5, -1, 1);
        world.setBlockState(rack, Blocks.NETHERRACK.getDefaultState());
        world.setBlockState(rack.up(), Blocks.FIRE.getDefaultState(), 3);
        SoulContent.worldTick(end);
        c.eq(Blocks.FIRE, world.getBlockState(rack.up()).getBlock(), "fire on netherrack stays fire");

        // A pig: no armor, so the damage is the fire's own.
        net.minecraft.entity.passive.EntityPig zombie = new net.minecraft.entity.passive.EntityPig(world);
        zombie.setPosition(sand.getX() + 0.5, sand.getY() + 1, sand.getZ() + 0.5);
        world.spawnEntity(zombie);
        float health = zombie.getHealth();
        SoulContent.soulFire.onEntityCollision(world, sand.up(), world.getBlockState(sand.up()), zombie);
        c.eq(health - 2.0F, zombie.getHealth(), "soul fire hurts for 2");
        c.ok(zombie.isBurning(), "and sets the pig alight");
        zombie.setDead();

        PlayerInteractEvent.LeftClickBlock punch = new PlayerInteractEvent.LeftClickBlock(player, soil, EnumFacing.UP, new Vec3d(0.5, 1, 0.5));
        MinecraftForge.EVENT_BUS.post(punch);
        c.ok(world.isAirBlock(soil.up()), "punching soul fire puts it out");
        world.setBlockState(sand, Blocks.STONE.getDefaultState());
        c.ok(world.isAirBlock(sand.up()), "soul fire goes out without its soul block");

        // Soul lantern -------------------------------------------------------------------------------
        BlockPos ceiling = area.add(8, 3, 1);
        world.setBlockState(ceiling, Blocks.STONE.getDefaultState());
        place(player, Owners.ours("soul_lantern"), ceiling, EnumFacing.DOWN);
        IBlockState hung = world.getBlockState(ceiling.down());
        c.eq(SoulContent.soulLantern, hung.getBlock(), "a soul lantern goes under a block");
        c.ok(hung.getValue(BlockSoulLantern.HANGING), "hanging");
        place(player, Owners.ours("soul_lantern"), area.add(10, -1, 1), EnumFacing.UP);
        IBlockState standing = world.getBlockState(area.add(10, 0, 1));
        c.ok(standing.getBlock() == SoulContent.soulLantern && !standing.getValue(BlockSoulLantern.HANGING), "and stands on the floor");
        world.setBlockToAir(ceiling);
        c.ok(world.getBlockState(ceiling.down()).getBlock() != SoulContent.soulLantern, "a hanging lantern falls with its ceiling");
        Block chain = Owners.ourBlock("chain");
        if (chain != null) {
            BlockPos link = area.add(12, 3, 1);
            world.setBlockState(link.up(), Blocks.STONE.getDefaultState());
            world.setBlockState(link, chain.getDefaultState());
            c.ok(SoulContent.soulLantern.canPlaceBlockAt(world, link.down()), "a soul lantern can go under a chain: "
                    + world.getBlockState(link).getBlockFaceShape(world, link, EnumFacing.DOWN));
            place(player, Owners.ours("soul_lantern"), link, EnumFacing.DOWN);
            c.eq(SoulContent.soulLantern, world.getBlockState(link.down()).getBlock(), "a soul lantern hangs from a chain");
        }

        // Soul torch ---------------------------------------------------------------------------------
        world.setBlockState(area.add(14, 0, 1), Blocks.STONE.getDefaultState());
        place(player, Owners.ours("soul_torch"), area.add(14, 0, 1), EnumFacing.SOUTH);
        c.eq(SoulContent.soulTorch, world.getBlockState(area.add(14, 0, 2)).getBlock(), "a soul torch goes on a wall");

        // Soul campfire --------------------------------------------------------------------------------
        BlockSoulCampfire fire = SoulCampfireContent.campfire;
        BlockPos camp = area.add(1, 0, 6);
        world.setBlockState(camp, fire.getDefaultState());
        c.eq(10, world.getBlockState(camp).getLightValue(world, camp), "a lit soul campfire gives light 10");
        net.minecraft.entity.passive.EntityPig cook = new net.minecraft.entity.passive.EntityPig(world);
        cook.setPosition(camp.getX() + 0.5, camp.getY(), camp.getZ() + 0.5);
        world.spawnEntity(cook);
        health = cook.getHealth();
        fire.onEntityCollision(world, camp, world.getBlockState(camp), cook);
        c.eq(health - 2.0F, cook.getHealth(), "standing in it hurts for 2");
        cook.setDead();
        NonNullList<ItemStack> drops = NonNullList.create();
        fire.getDrops(drops, world, camp, world.getBlockState(camp), 0);
        c.ok(drops.size() == 1 && drops.get(0).getItem() == Owners.ours("soul_soil"), "it drops one soul soil: " + drops);
        TileEntitySoulCampfire tile = (TileEntitySoulCampfire) world.getTileEntity(camp);
        c.ok(tile != null && tile.placeFood(new ItemStack(Items.BEEF), 3), "raw beef goes on it");
        for (int i = 0; i < 10; i++) {
            tile.update();
        }
        c.ok(tile.items().get(0).isEmpty() || tile.items().get(0).getItem() == Items.COOKED_BEEF, "and cooks");
        c.ok(Drops.count(world, camp.up(), Items.COOKED_BEEF) + (tile.items().get(0).getItem() == Items.COOKED_BEEF ? 1 : 0) == 1,
                "into one steak");

        // Soul Speed -----------------------------------------------------------------------------------
        c.ok(SoulContent.soulSpeed.isTreasureEnchantment(), "Soul Speed is a treasure enchantment");
        c.eq(3, SoulContent.soulSpeed.getMaxLevel(), "up to III");
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        c.ok(SoulContent.soulSpeed.canApply(boots), "on boots");
        c.ok(!SoulContent.soulSpeed.canApply(new ItemStack(Items.IRON_HELMET)), "not on a helmet");
        EnchantmentHelper.setEnchantments(Collections.singletonMap(SoulContent.soulSpeed, 3), boots);
        BlockPos run = area.add(1, -1, 12);
        c.fill(run, run.add(6, 0, 0), Blocks.SOUL_SAND.getDefaultState());
        EntityZombie runner = new EntityZombie(world);
        runner.setItemStackToSlot(EntityEquipmentSlot.FEET, boots);
        runner.setPosition(run.getX() + 0.5, run.getY() + 0.875, run.getZ() + 0.5);
        world.spawnEntity(runner);
        c.eq(3, SoulSpeed.level(runner), "the zombie wears Soul Speed III");
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent(runner));
        double base = runner.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
        double now = runner.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        c.ok(Math.abs(now - base - 0.03 * (1 + 3 * 0.35)) < 1e-4, "on soul sand it is faster by 0.03 x 2.05: " + (now - base));
        runner.motionX = 0.04;
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent(runner));
        c.ok(Math.abs(runner.motionX - 0.1) < 1e-9, "and soul sand's 0.4 slowdown is undone: " + runner.motionX);
        world.setBlockState(run.add(6, 0, 0), Blocks.STONE.getDefaultState());
        runner.setPosition(run.getX() + 6.5, run.getY() + 1.0, run.getZ() + 0.5);
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent(runner));
        c.ok(runner.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() == base, "off soul blocks the boost goes");
        runner.setDead();

        Recipes.checkAll(c);
        ItemStack coal = new ItemStack(Items.COAL);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack ss = new ItemStack(Blocks.SOUL_SAND);
        Crafting.expect(c, Crafting.grid(1, 3, coal, stick, ss), new ItemStack(Owners.ours("soul_torch"), 4), "4 soul torches");
        ItemStack n = new ItemStack(Items.IRON_NUGGET);
        Crafting.expect(c, Crafting.grid(3, 3, n, n, n, n, new ItemStack(Owners.ours("soul_torch")), n, n, n, n),
                new ItemStack(Owners.ours("soul_lantern")), "a soul lantern");
        ItemStack log = new ItemStack(Blocks.LOG, 1, 2);
        ItemStack none = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 3, none, stick, none, stick, new ItemStack(SoulContent.soulSoil), stick, log, log, log),
                new ItemStack(Owners.ours("soul_campfire")), "a soul campfire from birch logs and soul soil");
    }

    private static void place(EntityPlayerMP player, net.minecraft.item.Item item, BlockPos on, EnumFacing face) {
        ItemStack stack = new ItemStack(item);
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, player.world, on, EnumHand.MAIN_HAND, face, 0.5F, 0.5F, 0.5F);
    }
}
