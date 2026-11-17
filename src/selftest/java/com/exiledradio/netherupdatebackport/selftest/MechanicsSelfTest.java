package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.asm.MechanicsTransformer;
import com.exiledradio.netherupdatebackport.changes.BlockSounds;
import com.exiledradio.netherupdatebackport.changes.ChangeHooks;
import com.exiledradio.netherupdatebackport.changes.Fishing;
import com.exiledradio.netherupdatebackport.changes.RedstoneDot;
import com.exiledradio.netherupdatebackport.mobs.Anger;
import com.exiledradio.netherupdatebackport.mobs.NeutralMobs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/** Stream C's 1.16 behavior changes (patches P5 to P11) and the neutral mobs' anger. */
public final class MechanicsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mechanics";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(MechanicsTransformer.appliedP5, "P5 knockback is applied");
        c.ok(MechanicsTransformer.appliedP6, "P6 mending is applied");
        c.ok(MechanicsTransformer.appliedP7, "P7 portal search is applied");
        c.ok(MechanicsTransformer.appliedP8, "P8 lava pushing is applied");
        c.ok(MechanicsTransformer.appliedP9, "P9 totem is applied");
        c.ok(MechanicsTransformer.appliedP10, "P10 riders stay is applied");
        c.ok(MechanicsTransformer.appliedP11, "P11 redstone dot is applied");
        knockback(c);
        mending(c);
        portal(c);
        lava(c);
        totem(c);
        riders(c);
        redstone(c);
        fishing(c);
        sounds(c);
        anger(c);
    }

    // P5 ------------------------------------------------------------------------------------------------------

    private static void knockback(Check c) {
        BlockPos a = c.area();
        double[] resist = {0.0D, 0.5D, 0.6D, 1.0D};
        double[] want = {-0.4D, -0.2D, -0.16D, 0.0D};
        for (int i = 0; i < resist.length; i++) {
            EntityZombie z = new EntityZombie(c.world);
            z.setPosition(a.getX() + 2.5 + i, a.getY(), a.getZ() + 2.5);
            z.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(resist[i]);
            int pushed = 0;
            for (int n = 0; n < 20; n++) {
                z.motionX = 0.0D;
                z.motionY = 0.0D;
                z.motionZ = 0.0D;
                z.onGround = true;
                z.knockBack(null, 0.4F, 1.0D, 0.0D);
                if (Math.abs(z.motionX - want[i]) < 1.0E-6) {
                    pushed++;
                }
            }
            c.eq(20, pushed, "knockback resistance " + resist[i] + " gives x motion " + want[i] + " every time (last " + z.motionX + ")");
            if (resist[i] < 1.0D) {
                c.ok(Math.abs(z.motionY - Math.min(0.4D, -want[i])) < 1.0E-6, "and lifts by the scaled strength: " + z.motionY);
            }
        }
    }

    // P6 ------------------------------------------------------------------------------------------------------

    private static void mending(Check c) {
        BlockPos a = c.area();
        EntityPlayerMP player = Players.real(c.world, "mending");
        player.setPosition(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.addEnchantment(Enchantments.MENDING, 1);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        boots.addEnchantment(Enchantments.MENDING, 1);
        boots.setItemDamage(150);
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, helmet);
        player.setItemStackToSlot(EntityEquipmentSlot.FEET, boots);
        int repairs = 0;
        for (int i = 0; i < 20; i++) {
            int before = boots.getItemDamage();
            pickUp(c, player, 2);
            if (boots.getItemDamage() == before - 4) {
                repairs++;
            }
        }
        c.eq(20, repairs, "every 2 XP orb repairs the damaged Mending item by 4, never the undamaged one");
        c.eq(0, helmet.getItemDamage(), "the undamaged one is untouched");
        boots.setItemDamage(0);
        int xp = player.experienceTotal;
        pickUp(c, player, 3);
        c.eq(xp + 3, player.experienceTotal, "with nothing damaged the XP goes to the player");
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemStackToSlot(EntityEquipmentSlot.FEET, ItemStack.EMPTY);
    }

    private static void pickUp(Check c, EntityPlayerMP player, int value) {
        EntityXPOrb orb = new EntityXPOrb(c.world, player.posX, player.posY, player.posZ, value);
        orb.delayBeforeCanPickup = 0;
        player.xpCooldown = 0;
        orb.onCollideWithPlayer(player);
    }

    // P7 ------------------------------------------------------------------------------------------------------

    private static void portal(Check c) throws Exception {
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        c.eq(16, ChangeHooks.portalRadius(nether), "the nether side searches 16 blocks");
        c.eq(128, ChangeHooks.portalRadius(c.world), "the overworld keeps 128");
        Teleporter teleporter = nether.getDefaultTeleporter();
        // Two fresh spots, far apart: a portal 20 blocks away is out of reach, one 12 away is found.
        c.ok(!findsPortalAt(nether, teleporter, new BlockPos(-24000, 70, 24000), 20), "a portal 20 blocks off in the nether is not linked to");
        c.ok(findsPortalAt(nether, teleporter, new BlockPos(-24400, 70, 24400), 12), "one 12 blocks off is");
    }

    private static boolean findsPortalAt(WorldServer nether, Teleporter teleporter, BlockPos from, int dx) {
        for (int x = -1; x <= 3; x++) {
            for (int z = -1; z <= 1; z++) {
                nether.getChunkProvider().provideChunk((from.getX() >> 4) + x, (from.getZ() >> 4) + z);
            }
        }
        for (int x = -17; x <= 17; x++) {
            for (int z = -17; z <= 17; z++) {
                for (int y = 0; y < 128; y++) {
                    BlockPos p = from.add(x, 0, z);
                    p = new BlockPos(p.getX(), y, p.getZ());
                    if (nether.getBlockState(p).getBlock() == Blocks.PORTAL) {
                        nether.setBlockState(p, Blocks.AIR.getDefaultState(), 2 | 16);
                    }
                }
            }
        }
        BlockPos portal = from.add(dx, 0, 0);
        nether.setBlockState(portal.down(), Blocks.OBSIDIAN.getDefaultState(), 2 | 16);
        nether.setBlockState(portal, Blocks.PORTAL.getDefaultState(), 2 | 16);
        nether.setBlockState(portal.up(), Blocks.PORTAL.getDefaultState(), 2 | 16);
        EntityPig pig = new EntityPig(nether);
        pig.setPosition(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        // What walking into a portal sets, which placing the entity at the far portal reads.
        try {
            ReflectionHelper.findField(net.minecraft.entity.Entity.class, "lastPortalVec", "field_181017_ao").set(pig, new Vec3d(0.5D, 0.0D, 0.0D));
            ReflectionHelper.findField(net.minecraft.entity.Entity.class, "teleportDirection", "field_181018_ap").set(pig, EnumFacing.NORTH);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        boolean found = teleporter.placeInExistingPortal(pig, 0.0F);
        nether.setBlockState(portal, Blocks.AIR.getDefaultState(), 2 | 16);
        nether.setBlockState(portal.up(), Blocks.AIR.getDefaultState(), 2 | 16);
        return found;
    }

    // P8 ------------------------------------------------------------------------------------------------------

    private static void lava(Check c) {
        BlockPos a = c.area();
        // A row of flowing lava along +x, levels 1 to 4, on the stone floor.
        for (int i = 0; i < 4; i++) {
            c.world.setBlockState(a.add(i, 0, 0), Blocks.FLOWING_LAVA.getDefaultState().withProperty(BlockLiquid.LEVEL, i + 1), 2 | 16);
        }
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(a.getX() + 1.5, a.getY(), a.getZ() + 0.5);
        pig.motionX = 0.0D;
        pig.motionY = 0.0D;
        pig.motionZ = 0.0D;
        ChangeHooks.lavaPush(pig);
        c.ok(pig.motionX > 0.0D, "flowing lava pushes a mob downstream: " + pig.motionX);
        c.ok(Math.abs(new Vec3d(pig.motionX, pig.motionY, pig.motionZ).length() - 0.0045D) < 1.0E-6,
                "a still mob by at least 0.0045: " + pig.motionX);
        pig.motionX = 0.0D;
        pig.motionY = 0.0D;
        pig.motionZ = 0.05D;
        ChangeHooks.lavaPush(pig);
        c.ok(Math.abs(new Vec3d(pig.motionX, pig.motionY, pig.motionZ - 0.05D).length() - 0.0023333333333333335D) < 1.0E-6,
                "a moving one by 0.0023 a tick in the overworld: " + pig.motionX);
        pig.motionX = 0.0D;
        pig.motionY = 0.0D;
        pig.motionZ = 0.0D;
        pig.onEntityUpdate();
        c.ok(pig.motionX > 0.0D, "and the entity tick does it (P8): " + pig.motionX);
        c.eq(0.007D, ChangeHooks.lavaPushScale(c.world.getMinecraftServer().getWorld(-1)), "0.007 in the nether");
        EntitySquid squid = new EntitySquid(c.world);
        squid.setPosition(a.getX() + 1.5, a.getY(), a.getZ() + 0.5);
        squid.motionX = 0.0D;
        ChangeHooks.lavaPush(squid);
        c.eq(0.0D, squid.motionX, "a mob water does not push is not pushed by lava");
        c.fill(a, a.add(3, 0, 0), Blocks.AIR.getDefaultState());
    }

    // P9 ------------------------------------------------------------------------------------------------------

    private static void totem(Check c) {
        BlockPos a = c.area();
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        c.world.spawnEntity(z);
        z.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        z.setHealth(1.0F);
        z.attackEntityFrom(DamageSource.GENERIC, 100.0F);
        c.ok(!z.isDead && z.getHealth() > 0.0F, "the totem saves the zombie");
        PotionEffect fire = z.getActivePotionEffect(MobEffects.FIRE_RESISTANCE);
        c.ok(fire != null && fire.getDuration() == 800 && fire.getAmplifier() == 0, "and gives Fire Resistance for 40 s: " + fire);
        c.ok(z.isPotionActive(MobEffects.ABSORPTION), "besides Absorption (Regeneration does nothing for the undead)");
        z.setDead();
    }

    // P10 -----------------------------------------------------------------------------------------------------

    private static void riders(Check c) throws Exception {
        BlockPos a = c.area();
        Method despawn = ReflectionHelper.findMethod(EntityLiving.class, "despawnEntity", "func_70623_bb");
        EntityPlayerMP far = Players.real(c.world, "despawn");
        far.setPosition(a.getX() + 200.5, a.getY(), a.getZ() + 0.5);
        c.world.playerEntities.add(far);
        try {
            EntityZombie walker = new EntityZombie(c.world);
            walker.setPosition(a.getX() + 2.5, a.getY(), a.getZ() + 2.5);
            c.world.spawnEntity(walker);
            despawn.invoke(walker);
            c.ok(walker.isDead, "a zombie 200 blocks from the nearest player despawns");

            EntityZombie rider = new EntityZombie(c.world);
            rider.setPosition(a.getX() + 4.5, a.getY(), a.getZ() + 2.5);
            c.world.spawnEntity(rider);
            EntityBoat boat = new EntityBoat(c.world, a.getX() + 4.5, a.getY(), a.getZ() + 2.5);
            c.world.spawnEntity(boat);
            rider.startRiding(boat, true);
            c.ok(rider.isRiding(), "a zombie sits in a boat");
            despawn.invoke(rider);
            c.ok(!rider.isDead, "one riding a boat does not");
            rider.dismountRidingEntity();
            despawn.invoke(rider);
            c.ok(rider.isDead, "and once out of it, it does");
            boat.setDead();
        } finally {
            c.world.playerEntities.remove(far);
        }
    }

    // P11 -----------------------------------------------------------------------------------------------------

    private static void redstone(Check c) {
        WorldServer w = c.world;
        Block dot = RedstoneDot.block();
        if (!c.ok(dot != null, "the redstone dot block is registered")) {
            return;
        }
        BlockPos a = c.area();
        IBlockState wire15 = Blocks.REDSTONE_WIRE.getDefaultState().withProperty(BlockRedstoneWire.POWER, 15);

        // A lone wire: a cross, powering all four sides and below.
        BlockPos p = a.add(4, 1, 4);
        w.setBlockState(p.down(), Blocks.STONE.getDefaultState(), 2 | 16);
        powered(w, p, wire15);
        IBlockState actual = w.getBlockState(p).getActualState(w, p);
        c.eq("side,side,side,side", sides(actual), "a lone wire is drawn as a cross");
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            c.eq(15, w.getBlockState(p).getWeakPower(w, p, f), "the cross powers its " + f.getOpposite() + " side");
        }
        c.eq(15, w.getBlockState(p).getWeakPower(w, p, EnumFacing.UP), "and the block below");
        c.eq(0, w.getBlockState(p).getWeakPower(w, p, EnumFacing.DOWN), "never the block above");

        // Used, it is a dot: powers only the block below.
        EntityPlayerMP player = Players.real(w, "redstone");
        player.setPosition(p.getX() + 0.5, p.getY(), p.getZ() + 2.5);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        use(player, p);
        c.eq(dot, w.getBlockState(p).getBlock(), "using it makes a dot");
        // The dot works out its own power (from nothing here: 0); give it 15 to read its sides.
        w.setBlockState(p, dot.getDefaultState().withProperty(BlockRedstoneWire.POWER, 15), 2 | 16);
        c.eq("none,none,none,none", sides(w.getBlockState(p).getActualState(w, p)), "a dot is drawn as a dot");
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            c.eq(0, w.getBlockState(p).getWeakPower(w, p, f), "the dot does not power its " + f.getOpposite() + " side");
        }
        c.eq(15, w.getBlockState(p).getWeakPower(w, p, EnumFacing.UP), "the dot powers the block below");
        c.eq(new ItemStack(Items.REDSTONE).getItem(), dot.getItem(w, p, w.getBlockState(p)).getItem(), "and is picked as redstone");
        use(player, p);
        c.eq(Blocks.REDSTONE_WIRE, w.getBlockState(p).getBlock(), "using the dot makes a cross again");

        // A dot that gets a neighbor is vanilla's wire again.
        RedstoneDot.toggle(w, p, w.getBlockState(p));
        c.eq(dot, w.getBlockState(p).getBlock(), "a dot again");
        w.setBlockState(p.north().down(), Blocks.STONE.getDefaultState(), 2 | 16);
        w.setBlockState(p.north(), Blocks.REDSTONE_WIRE.getDefaultState(), 3);
        c.eq(Blocks.REDSTONE_WIRE, w.getBlockState(p).getBlock(), "a dot that gets connected is vanilla's wire again");
        // A connected wire is not toggled.
        use(player, p);
        c.eq(Blocks.REDSTONE_WIRE, w.getBlockState(p).getBlock(), "a connected wire does not toggle");
        c.eq(0, RedstoneDot.connected(w, p) & ~(1 << EnumFacing.NORTH.getHorizontalIndex()), "it connects north only");

        // Lines and corners: power where the wire points.
        BlockPos q = a.add(12, 1, 12);
        c.fill(q.add(-1, -1, -1), q.add(1, -1, 1), Blocks.STONE.getDefaultState());
        // A line end: wire q with a wire to the west.
        powered(w, q.west(), wire15);
        powered(w, q, wire15);
        c.eq(15, w.getBlockState(q).getWeakPower(w, q, EnumFacing.WEST), "a line end powers the block it points into (east)");
        c.eq(0, w.getBlockState(q).getWeakPower(w, q, EnumFacing.NORTH), "not the block beside it (south)");
        c.eq(0, w.getBlockState(q).getWeakPower(w, q, EnumFacing.SOUTH), "nor the other (north)");
        // A corner: q joined west and north.
        powered(w, q.north(), wire15);
        // Placing the north wire made its neighbors work their power out (none here): give q its 15 back.
        powered(w, q, wire15);
        c.eq(15, w.getBlockState(q).getWeakPower(w, q, EnumFacing.SOUTH), "a corner powers the north it points into");
        c.eq(15, w.getBlockState(q).getWeakPower(w, q, EnumFacing.EAST), "and the west");
        c.eq(0, w.getBlockState(q).getWeakPower(w, q, EnumFacing.WEST), "not the east, where it does not point");
        c.eq(0, w.getBlockState(q).getWeakPower(w, q, EnumFacing.NORTH), "nor the south");

        // A powered contraption still works: lever, wire, lamp.
        BlockPos r = a.add(20, 1, 4);
        c.fill(r.add(0, -1, 0), r.add(3, -1, 0), Blocks.STONE.getDefaultState());
        w.setBlockState(r.add(3, 0, 0), Blocks.REDSTONE_LAMP.getDefaultState(), 3);
        w.setBlockState(r.add(1, 0, 0), Blocks.REDSTONE_WIRE.getDefaultState(), 3);
        w.setBlockState(r.add(2, 0, 0), Blocks.REDSTONE_WIRE.getDefaultState(), 3);
        w.setBlockState(r, Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.eq(Blocks.LIT_REDSTONE_LAMP, w.getBlockState(r.add(3, 0, 0)).getBlock(), "a redstone block lights a lamp through two wires");
        w.setBlockState(r, Blocks.AIR.getDefaultState(), 3);
        c.eq(0, (int) w.getBlockState(r.add(2, 0, 0)).getValue(BlockRedstoneWire.POWER), "and the wire goes dark without it");
    }

    /** A wire with a set power: placed first (which works its power out), then given the power. */
    private static void powered(WorldServer w, BlockPos p, IBlockState state) {
        w.setBlockState(p, state, 2 | 16);
        w.setBlockState(p, state, 2 | 16);
    }

    private static void use(EntityPlayerMP player, BlockPos p) {
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, p, EnumFacing.UP,
                new Vec3d(p.getX() + 0.5, p.getY() + 0.05, p.getZ() + 0.5));
        RedstoneDot.use(event);
    }

    private static String sides(IBlockState s) {
        return name(s, BlockRedstoneWire.NORTH) + "," + name(s, BlockRedstoneWire.EAST) + ","
                + name(s, BlockRedstoneWire.SOUTH) + "," + name(s, BlockRedstoneWire.WEST);
    }

    private static <T extends Comparable<T>> String name(IBlockState s, net.minecraft.block.properties.IProperty<T> p) {
        return p.getName(s.getValue(p));
    }

    // Fishing ---------------------------------------------------------------------------------------------------

    private static void fishing(Check c) throws Exception {
        WorldServer w = c.world;
        LootTable treasure = w.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING_TREASURE);
        LootTable junk = w.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING_JUNK);
        LootTable fishing = w.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING);
        c.ok(treasure.getPool("main").getEntry("minecraft:waterlily") == null, "lily pads are no longer treasure");
        LootEntry lily = junk.getPool("main").getEntry("minecraft:lily_pad");
        c.ok(lily != null && lily.getEffectiveWeight(0.0F) == 17, "they are junk, weight 17");
        LootPool pool = fishing.getPool("main");
        LootEntry entry = pool.getEntry("minecraft:gameplay/fishing/treasure");
        if (c.ok(entry != null, "the fishing table still has its treasure entry")) {
            c.eq(5, entry.getEffectiveWeight(0.0F), "weight 5");
            c.eq(7, entry.getEffectiveWeight(1.0F), "quality 2");
            Field conditions = ReflectionHelper.findField(LootEntry.class, "conditions", "field_186366_e");
            LootCondition[] cs = (LootCondition[]) conditions.get(entry);
            c.ok(cs.length == 1 && cs[0] instanceof Fishing.InOpenWater, "with the open water condition");
        }

        // Open water: 5x5 of water sources from 1 below to the bobber's block, air above.
        BlockPos a = c.area();
        BlockPos bob = a.add(8, 4, 8);
        c.fill(bob.add(-3, -3, -3), bob.add(3, 0, 3), Blocks.WATER.getDefaultState());
        c.ok(Fishing.openWaterAt(w, bob), "open water around the bobber");
        w.setBlockState(bob.add(2, 1, 2), Blocks.WATERLILY.getDefaultState(), 2 | 16);
        c.ok(Fishing.openWaterAt(w, bob), "a lily pad above still counts as open");
        w.setBlockState(bob.add(-2, 0, 1), Blocks.STONE.getDefaultState(), 2 | 16);
        c.ok(!Fishing.openWaterAt(w, bob), "a stone block in the 5x5 does not");
        w.setBlockState(bob.add(-2, 0, 1), Blocks.WATER.getDefaultState(), 2 | 16);
        w.setBlockState(bob.add(0, 2, 2), Blocks.STONE.getDefaultState(), 2 | 16);
        c.ok(!Fishing.openWaterAt(w, bob), "nor one two blocks above");
        w.setBlockState(bob.add(0, 2, 2), Blocks.AIR.getDefaultState(), 2 | 16);

        // The bobber's flag, and the condition reading it.
        EntityPlayerMP angler = Players.real(w, "fisher");
        angler.setPosition(bob.getX() + 0.5, bob.getY() + 2, bob.getZ() + 6.5);
        EntityFishHook hook = new EntityFishHook(w, angler);
        hook.setPosition(bob.getX() + 0.5, bob.getY() + 0.5, bob.getZ() + 0.5);
        Fishing.follow(hook);
        Field state = ReflectionHelper.findField(EntityFishHook.class, "currentState", "field_190627_av");
        Field catchable = ReflectionHelper.findField(EntityFishHook.class, "ticksCatchable", "field_146045_ax");
        for (Object s : state.getType().getEnumConstants()) {
            if ("BOBBING".equals(((Enum<?>) s).name())) {
                state.set(hook, s);
            }
        }
        catchable.setInt(hook, 20);
        Fishing.tickForTest(hook);
        c.ok(Fishing.openWater(hook), "a bite in open water is open water");
        LootCondition open = new Fishing.InOpenWater();
        LootContext context = new LootContext.Builder(w).withLootedEntity(hook).withPlayer(angler).build();
        c.ok(open.testCondition(new Random(1), context), "the condition lets treasure through");
        w.setBlockState(bob.add(1, -1, 1), Blocks.STONE.getDefaultState(), 2 | 16);
        Fishing.tickForTest(hook);
        c.ok(!Fishing.openWater(hook), "a stone in the water during the bite: not open");
        w.setBlockState(bob.add(1, -1, 1), Blocks.WATER.getDefaultState(), 2 | 16);
        Fishing.tickForTest(hook);
        c.ok(!Fishing.openWater(hook), "and it stays not open for the rest of that bite");
        c.ok(!open.testCondition(new Random(1), context), "so no treasure");
        catchable.setInt(hook, 0);
        Fishing.tickForTest(hook);
        c.ok(Fishing.openWater(hook), "the next wait starts open again");
        hook.setDead();
        angler.fishEntity = null;
    }

    // Sounds ----------------------------------------------------------------------------------------------------

    private static void sounds(Check c) {
        if (!ModConfig.changes.blockSounds) {
            return;
        }
        String[][] want = {
                {"netherrack", "netherupdatebackport:block.netherrack.break"},
                {"nether_brick", "netherupdatebackport:block.nether_bricks.break"},
                {"red_nether_brick", "netherupdatebackport:block.nether_bricks.break"},
                {"quartz_ore", "netherupdatebackport:block.nether_ore.break"},
                {"bone_block", "netherupdatebackport:block.bone_block.break"},
                {"soul_sand", "netherupdatebackport:block.soul_sand.break"},
                {"nether_wart", "netherupdatebackport:block.nether_wart.break"},
                {"nether_wart_block", "netherupdatebackport:block.wart_block.break"},
        };
        for (String[] w : want) {
            Block block = Block.getBlockFromName("minecraft:" + w[0]);
            SoundType type = block.getSoundType(block.getDefaultState(), c.world, BlockPos.ORIGIN, null);
            c.eq(w[1], String.valueOf(type.getBreakSound().getRegistryName()), w[0] + " breaks with 1.16's sound");
        }
        c.eq(10, BlockSounds.types().size(), "ten blocks get 1.16's sounds");
        c.eq("netherupdatebackport:block.netherrack.step", String.valueOf(Blocks.NETHERRACK.getSoundType().getStepSound().getRegistryName()),
                "netherrack's steps too");
    }

    // Neutral mobs ----------------------------------------------------------------------------------------------

    private static void anger(Check c) throws Exception {
        if (!NeutralMobs.active()) {
            c.fail("neutral mob anger is off");
            return;
        }
        WorldServer w = c.world;
        BlockPos a = c.area();
        EntityPlayerMP player = Players.real(w, "angerer");
        player.setPosition(a.getX() + 8.5, a.getY(), a.getZ() + 8.5);
        player.interactionManager.setGameType(GameType.SURVIVAL);

        EntityWolf wolf = new EntityWolf(w);
        wolf.setPosition(a.getX() + 4.5, a.getY(), a.getZ() + 4.5);
        w.spawnEntity(wolf);
        Anger anger = Anger.of(wolf);
        if (!c.ok(anger != null, "a wolf carries 1.16's anger")) {
            return;
        }
        c.ok(hasAngryAtPlayerGoal(wolf), "and a goal to take back the player it is angry at");
        wolf.setAttackTarget(player);
        NeutralMobs.updatePersistentAnger(wolf, anger);
        c.eq(player.getUniqueID(), anger.target(), "a wolf targeting a player is angry at that player");
        c.ok(anger.remaining() >= 400 && anger.remaining() <= 780, "for 20-39 s: " + anger.remaining());
        int held = anger.remaining();
        NeutralMobs.updatePersistentAnger(wolf, anger);
        c.eq(held, anger.remaining(), "the timer holds while it targets the player");
        wolf.setAttackTarget(null);
        NeutralMobs.updatePersistentAnger(wolf, anger);
        c.eq(held - 1, anger.remaining(), "and runs once it has lost the player");
        c.ok(anger.isAngryAt(player), "still angry at the player meanwhile");
        // The anger and its target are saved with the wolf.
        EntityWolf copy = new EntityWolf(w);
        copy.readFromNBT(wolf.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        c.ok(Anger.of(copy) != null && player.getUniqueID().equals(Anger.of(copy).target()) && Anger.of(copy).remaining() == held - 1,
                "AngerTime and AngryAt are saved");
        NeutralMobs.forgive(player);
        c.ok(!anger.isAngry() && anger.target() == null, "a player who dies is forgiven");

        Class<?>[] kinds = {EntityEnderman.class, EntityPolarBear.class, EntityIronGolem.class};
        for (Class<?> kind : kinds) {
            EntityLiving mob = (EntityLiving) kind.getConstructor(net.minecraft.world.World.class).newInstance(w);
            mob.setPosition(a.getX() + 12.5, a.getY(), a.getZ() + 12.5);
            w.spawnEntity(mob);
            Anger an = Anger.of(mob);
            if (!c.ok(an != null, kind.getSimpleName() + " carries 1.16's anger")) {
                continue;
            }
            c.eq(!(mob instanceof EntityEnderman), hasAngryAtPlayerGoal(mob), kind.getSimpleName() + " takes back a player it is angry at (not endermen)");
            mob.setAttackTarget(player);
            NeutralMobs.updatePersistentAnger(mob, an);
            c.ok(an.isAngryAt(player), kind.getSimpleName() + " is angry at the player it targets");
            NeutralMobs.forgive(player);
            c.ok(!an.isAngry() && mob.getAttackTarget() == null, kind.getSimpleName() + " forgives a player who dies");
            mob.setDead();
        }
        wolf.setDead();
    }

    private static boolean hasAngryAtPlayerGoal(EntityLiving mob) {
        for (EntityAITasks.EntityAITaskEntry e : mob.targetTasks.taskEntries) {
            if (e.action.getClass().getName().endsWith("NeutralMobs$AngryAtPlayer")) {
                return true;
            }
        }
        return false;
    }
}
