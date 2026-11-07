package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.blocks.BlockRespawnAnchor;
import com.exiledradio.netherupdatebackport.blocks.BlockTarget;
import com.exiledradio.netherupdatebackport.blocks.BlocksContent;
import com.exiledradio.netherupdatebackport.blocks.ItemLodestoneCompass;
import com.exiledradio.netherupdatebackport.blocks.RespawnAnchors;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/** Stream U: the smaller blocks. */
public final class BlocksSelfTest implements SelfTest {

    @Override
    public String name() {
        return "blocks";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        for (String name : BlocksContent.NAMES) {
            Block block = Owners.ourBlock(name);
            if (!c.ok(block != null, name + " is registered")) {
                continue;
            }
            c.ok(Owners.ours(name) != null, name + " has an item");
            for (IBlockState state : block.getBlockState().getValidStates()) {
                c.eq(state, block.getStateFromMeta(block.getMetaFromState(state)), name + " metadata round trip " + state);
            }
        }
        c.ok(Owners.ours("lodestone_compass") != null, "the lodestone compass is registered");
        hardness(c, "chain", 5.0F);
        hardness(c, "crying_obsidian", 50.0F);
        hardness(c, "respawn_anchor", 50.0F);
        hardness(c, "lodestone", 3.5F);
        hardness(c, "nether_gold_ore", 3.0F);
        hardness(c, "target", 0.5F);
        hardness(c, "quartz_bricks", 0.8F);
        hardness(c, "cracked_nether_bricks", 2.0F);
        c.eq(3, BlocksContent.cryingObsidian.getHarvestLevel(BlocksContent.cryingObsidian.getDefaultState()), "crying obsidian needs a diamond pickaxe");
        c.eq(10, BlocksContent.cryingObsidian.getDefaultState().getLightValue(world, BlockPos.ORIGIN), "crying obsidian gives light 10");

        EntityPlayerMP player = Players.real(world, "blocks");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        BlockPos area = c.area();

        // Chain ---------------------------------------------------------------------------------------------
        world.setBlockState(area, Blocks.STONE.getDefaultState());
        place(player, Owners.ours("chain"), area, EnumFacing.NORTH);
        IBlockState chain = world.getBlockState(area.north());
        c.eq(BlocksContent.chain, chain.getBlock(), "a chain placed on a block's side");
        c.eq(EnumFacing.Axis.Z, chain.getValue(BlockRotatedPillar.AXIS), "runs along that axis");
        AxisAlignedBB box = chain.getBoundingBox(world, area.north());
        c.ok(Math.abs(box.minX - 6.5 / 16) < 1e-6 && Math.abs(box.maxY - 9.5 / 16) < 1e-6 && box.minZ == 0 && box.maxZ == 1,
                "1.16's thin box along Z: " + box);
        c.eq(BlockFaceShape.CENTER_SMALL, chain.getBlockFaceShape(world, area.north(), EnumFacing.NORTH), "its ends are a small centre");
        c.ok(!chain.isOpaqueCube(), "a chain does not hide its neighbors' faces");

        // Target ------------------------------------------------------------------------------------------
        c.eq(15, BlockTarget.strength(EnumFacing.UP, new Vec3d(0.5, 1.0, 0.5)), "centre of the top: 15");
        c.eq(1, BlockTarget.strength(EnumFacing.UP, new Vec3d(0.99, 1.0, 0.5)), "edge of the top: 1");
        c.eq(8, BlockTarget.strength(EnumFacing.NORTH, new Vec3d(0.5, 0.5 + 0.25, 0.0)), "half way out on a side: 8");
        BlockPos targetAt = area.add(6, 1, 0);
        world.setBlockState(targetAt, BlocksContent.target.getDefaultState());
        // A real arrow flying at the target's west face.
        EntityTippedArrow arrow = new EntityTippedArrow(world, targetAt.getX() - 2.5, targetAt.getY() + 0.5, targetAt.getZ() + 0.5);
        arrow.shootingEntity = player;
        arrow.motionX = 1.5;
        arrow.motionY = 0.0;
        arrow.motionZ = 0.0;
        world.spawnEntity(arrow);
        for (int i = 0; i < 10 && world.getBlockState(targetAt).getValue(BlockTarget.POWER) == 0; i++) {
            arrow.onUpdate();
        }
        int power = world.getBlockState(targetAt).getValue(BlockTarget.POWER);
        c.ok(power >= 12, "an arrow near the centre powers the target strongly: " + power);
        c.eq(power, world.getRedstonePowerFromNeighbors(targetAt.east()), "and the power reaches the next block");
        c.ok(world.isUpdateScheduled(targetAt, BlocksContent.target), "a reset is scheduled");
        c.eq(0, BlockTarget.hit(world, targetAt, EnumFacing.WEST, new Vec3d(targetAt.getX(), targetAt.getY() + 0.5, targetAt.getZ() + 0.5), arrow),
                "a second hit while counting down is ignored");
        arrow.setDead();
        BlocksContent.target.updateTick(world, targetAt, world.getBlockState(targetAt), world.rand);
        c.eq(0, world.getBlockState(targetAt).getValue(BlockTarget.POWER).intValue(), "the reset turns it off");

        // Respawn anchor ------------------------------------------------------------------------------------
        respawnAnchor(c, world, player, area.add(12, 1, 0));

        // Lodestone compass -------------------------------------------------------------------------------
        BlockPos stone = area.add(0, 1, 8);
        world.setBlockState(stone, BlocksContent.lodestone.getDefaultState());
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.COMPASS));
        c.ok(useCompass(player, world, stone), "a compass used on a lodestone is taken");
        ItemStack bound = player.getHeldItem(EnumHand.MAIN_HAND);
        c.eq(BlocksContent.lodestoneCompass, bound.getItem(), "a single compass becomes a lodestone compass in place");
        c.eq(stone, ItemLodestoneCompass.target(bound), "bound to that lodestone");
        c.eq(0, ItemLodestoneCompass.dimension(bound), "in the overworld");
        player.inventory.clear();
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.COMPASS, 3));
        useCompass(player, world, stone);
        c.eq(2, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "from a stack, one compass is used");
        c.ok(player.inventory.hasItemStack(new ItemStack(BlocksContent.lodestoneCompass)), "and the lodestone compass goes to the inventory");
        world.setBlockToAir(stone);
        bound.getItem().onUpdate(bound, world, player, 0, false);
        c.ok(ItemLodestoneCompass.target(bound) == null, "once the lodestone is gone the compass forgets it");
        c.ok(bound.getItem() == BlocksContent.lodestoneCompass, "but stays a lodestone compass");
        player.inventory.clear();

        // Nether gold ore -------------------------------------------------------------------------------------
        int min = 99;
        int max = 0;
        for (int i = 0; i < 400; i++) {
            NonNullList<ItemStack> d = NonNullList.create();
            BlocksContent.netherGoldOre.getDrops(d, world, area, BlocksContent.netherGoldOre.getDefaultState(), 0);
            c.eq(Items.GOLD_NUGGET, d.get(0).getItem(), "nether gold ore drops nuggets");
            min = Math.min(min, d.get(0).getCount());
            max = Math.max(max, d.get(0).getCount());
        }
        c.ok(min == 2 && max == 6, "2 to 6 without Fortune: " + min + "-" + max);
        int fortuneMax = 0;
        for (int i = 0; i < 400; i++) {
            NonNullList<ItemStack> d = NonNullList.create();
            BlocksContent.netherGoldOre.getDrops(d, world, area, BlocksContent.netherGoldOre.getDefaultState(), 3);
            fortuneMax = Math.max(fortuneMax, d.get(0).getCount());
        }
        c.ok(fortuneMax > 6 && fortuneMax <= 24, "Fortune III gives more, up to 24: " + fortuneMax);
        c.eq(Items.GOLD_INGOT, FurnaceRecipes.instance().getSmeltingResult(new ItemStack(BlocksContent.netherGoldOre)).getItem(), "smelts to a gold ingot");
        c.eq(Owners.ours("cracked_nether_bricks"), FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.NETHER_BRICK)).getItem(),
                "nether bricks smelt to cracked nether bricks");

        // Recipes ---------------------------------------------------------------------------------------------
        Recipes.checkAll(c);
        ItemStack none = ItemStack.EMPTY;
        ItemStack nug = new ItemStack(Items.IRON_NUGGET);
        Crafting.expect(c, Crafting.grid(1, 3, nug, new ItemStack(Items.IRON_INGOT), nug), new ItemStack(Owners.ours("chain")), "chain");
        ItemStack q = new ItemStack(Blocks.QUARTZ_BLOCK);
        Crafting.expect(c, Crafting.grid(2, 2, q, q, q, q), new ItemStack(Owners.ours("quartz_bricks"), 4), "4 quartz blocks make 4 quartz bricks");
        ItemStack slab = new ItemStack(Blocks.STONE_SLAB, 1, 6);
        Crafting.expect(c, Crafting.grid(1, 2, slab, slab), new ItemStack(Owners.ours("chiseled_nether_bricks")), "chiseled nether bricks");
        ItemStack cry = new ItemStack(Owners.ours("crying_obsidian"));
        ItemStack glow = new ItemStack(Blocks.GLOWSTONE);
        Crafting.expect(c, Crafting.grid(3, 3, cry, cry, cry, glow, glow, glow, cry, cry, cry), new ItemStack(Owners.ours("respawn_anchor")), "respawn anchor");
        ItemStack red = new ItemStack(Items.REDSTONE);
        ItemStack hay = new ItemStack(Blocks.HAY_BLOCK);
        Crafting.expect(c, Crafting.grid(3, 3, none, red, none, red, hay, red, none, red, none), new ItemStack(Owners.ours("target")), "target");

        // Advancements ----------------------------------------------------------------------------------------
        net.minecraft.advancements.AdvancementManager advancements = world.getAdvancementManager();
        for (String a : new String[] {"nether/obtain_crying_obsidian", "nether/charge_respawn_anchor", "adventure/bullseye"}) {
            c.ok(advancements.getAdvancement(new ResourceLocation("netherupdatebackport", a)) != null, "advancement " + a + " loaded");
        }

        // Generation ------------------------------------------------------------------------------------------
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        int cx = -1600;
        int cz = 1600;
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 2; z++) {
                nether.getChunkProvider().provideChunk(cx + x, cz + z);
            }
        }
        int ore = 0;
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                Chunk chunk = nether.getChunk(cx + x, cz + z);
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int y = 0; y < 128; y++) {
                            if (chunk.getBlockState(bx, y, bz).getBlock() == BlocksContent.netherGoldOre) {
                                ore++;
                            }
                        }
                    }
                }
            }
        }
        c.ok(ore > 20, "four new nether chunks have nether gold ore: " + ore);
    }

    private static void respawnAnchor(Check c, WorldServer world, EntityPlayerMP player, BlockPos at) {
        BlockRespawnAnchor anchor = BlocksContent.respawnAnchor;
        world.setBlockState(at, anchor.getDefaultState());
        c.eq(0, anchor.getDefaultState().getLightValue(world, at), "an empty anchor gives no light");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Blocks.GLOWSTONE, 5));
        // Four clicks; a fifth with glowstone on a full anchor would use it, and blow it up here.
        for (int i = 0; i < 4; i++) {
            anchor.onBlockActivated(world, at, world.getBlockState(at), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1F, 0.5F);
        }
        c.eq(4, world.getBlockState(at).getValue(BlockRespawnAnchor.CHARGE).intValue(), "glowstone charges it up to 4");
        c.eq(1, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "using four glowstone");
        c.eq(15, world.getBlockState(at).getLightValue(world, at), "a full anchor gives light 15");
        c.eq(15, world.getBlockState(at).getComparatorInputOverride(world, at), "and comparator output 15");
        int[] light = {0, 3, 7, 11, 15};
        for (int i = 0; i <= 4; i++) {
            c.eq(light[i], BlockRespawnAnchor.scaled(i), "light at charge " + i);
        }

        // In the overworld a charged anchor explodes.
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        anchor.onBlockActivated(world, at, world.getBlockState(at), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1F, 0.5F);
        c.ok(world.getBlockState(at).getBlock() != anchor, "a charged anchor used in the overworld explodes");

        // A dispenser of glowstone charges one.
        BlockPos disp = at.add(0, 0, 3);
        world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST));
        world.setBlockState(disp.east(), anchor.getDefaultState());
        Item glowstone = Item.getItemFromBlock(Blocks.GLOWSTONE);
        ItemStack left = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(glowstone).dispense(new BlockSourceImpl(world, disp), new ItemStack(glowstone, 2));
        c.eq(1, world.getBlockState(disp.east()).getValue(BlockRespawnAnchor.CHARGE).intValue(), "a dispenser of glowstone charges an anchor");
        c.eq(1, left.getCount(), "using one glowstone");

        // In the nether: sets the respawn point; respawning uses a charge.
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        BlockPos n = new BlockPos(2000, 100, 2000);
        nether.getChunkProvider().provideChunk(n.getX() >> 4, n.getZ() >> 4);
        for (BlockPos p : BlockPos.getAllInBox(n.add(-3, -2, -3), n.add(3, 4, 3))) {
            nether.setBlockState(p, p.getY() < n.getY() ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState(), 2 | 16);
        }
        nether.setBlockState(n, anchor.getDefaultState().withProperty(BlockRespawnAnchor.CHARGE, 2));
        EntityPlayerMP netherPlayer = Players.real(nether, "anchor");
        netherPlayer.dimension = -1;
        RespawnAnchors.forget(netherPlayer);
        anchor.onBlockActivated(nether, n, nether.getBlockState(n), netherPlayer, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1F, 0.5F);
        c.eq(anchor, nether.getBlockState(n).getBlock(), "in the nether a charged anchor does not explode");
        c.eq(n, RespawnAnchors.anchor(netherPlayer), "and becomes the player's respawn point");
        Vec3d spot = RespawnAnchors.useAnchor(nether, n);
        c.ok(spot != null && Math.abs(spot.y - n.getY()) < 0.01 && spot.distanceTo(new Vec3d(n.getX() + 0.5, n.getY(), n.getZ() + 0.5)) < 1.5,
                "respawning stands the player beside it: " + spot);
        c.eq(1, nether.getBlockState(n).getValue(BlockRespawnAnchor.CHARGE).intValue(), "using a charge");
        for (BlockPos p : BlockPos.getAllInBox(n.add(-1, -1, -1), n.add(1, 2, 1))) {
            if (!p.equals(n)) {
                nether.setBlockState(p, Blocks.OBSIDIAN.getDefaultState(), 2 | 16);
            }
        }
        c.ok(RespawnAnchors.useAnchor(nether, n) == null, "a walled-in anchor cannot be used");
        c.eq(1, nether.getBlockState(n).getValue(BlockRespawnAnchor.CHARGE).intValue(), "and keeps its charge");
        netherPlayer.setSpawnPoint(new BlockPos(0, 64, 0), false);
        c.ok(RespawnAnchors.anchor(netherPlayer) == null, "choosing a bed spawn forgets the anchor");
    }

    private static boolean useCompass(EntityPlayerMP player, WorldServer world, BlockPos pos) {
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event =
                new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.UP, new Vec3d(0.5, 1, 0.5));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    private static void hardness(Check c, String name, float hardness) {
        Block block = Owners.ourBlock(name);
        c.eq(hardness, block.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), name + " hardness");
    }

    private static void place(EntityPlayerMP player, Item item, BlockPos on, EnumFacing face) {
        ItemStack stack = new ItemStack(item);
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, player.world, on, EnumHand.MAIN_HAND, face, 0.5F, 0.5F, 0.5F);
    }
}
