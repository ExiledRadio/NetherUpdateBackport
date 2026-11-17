package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.asm.SmallChangesTransformer;
import com.exiledradio.netherupdatebackport.changes.ChangesContent;
import com.exiledradio.netherupdatebackport.changes.SmallChanges;
import com.exiledradio.netherupdatebackport.changes.SmallHooks;
import com.exiledradio.netherupdatebackport.common.AngerGameRules;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.mobs.Anger;
import com.exiledradio.netherupdatebackport.mobs.WitherSkeletons;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;
import com.exiledradio.netherupdatebackport.piglin.Memories;
import com.exiledradio.netherupdatebackport.piglin.PiglinTasks;
import com.exiledradio.netherupdatebackport.soul.SoulSoil;
import com.exiledradio.netherupdatebackport.soul.campfire.BlockSoulCampfire;
import com.exiledradio.netherupdatebackport.soul.campfire.TileEntitySoulCampfire;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockPattern;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stream C's low-impact 1.16 changes from the 2026-11-16 audit: patches P12 to P25 and their event-side companions. */
public final class SmallChangesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "smallchanges";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(SmallChangesTransformer.appliedP12, "P12 withers on soul soil is applied");
        c.ok(SmallChangesTransformer.appliedP13, "P13 magma pathing is applied");
        c.ok(SmallChangesTransformer.appliedP14, "P14 hoe enchantments is applied");
        c.ok(SmallChangesTransformer.appliedP14Hoe, "P14 hoe enchantability is applied");
        c.ok(SmallChangesTransformer.appliedP15, "P15 compass vanishing is applied");
        c.ok(SmallChangesTransformer.appliedP16, "P16 parrot imitation is applied");
        c.ok(SmallChangesTransformer.appliedP17, "P17 snow golem water is applied");
        c.ok(SmallChangesTransformer.appliedP18, "P18 undead horse leash is applied");
        c.ok(SmallChangesTransformer.appliedP19, "P19 walls and panes is applied");
        c.ok(SmallChangesTransformer.appliedP20, "P20 furnace XP is applied");
        c.eq(2, SmallChangesTransformer.appliedP21, "P21 suffocation is applied to farmland and grass paths");
        c.ok(SmallChangesTransformer.appliedP22, "P22 ghast volume is applied");
        c.ok(SmallChangesTransformer.appliedP23, "P23 armor stands is applied");
        c.ok(SmallChangesTransformer.appliedP24, "P24 universal anger hurt-by is applied");
        c.ok(SmallChangesTransformer.appliedP25, "P25 climbing steps is applied");
        gameRules(c);
        universalAnger(c);
        witherSkeletons(c);
        campfire(c);
        witherSoulSoil(c);
        magma(c);
        dispensers(c);
        enderman(c);
        hoes(c);
        spawnCosts(c);
        caveMood(c);
        witherRose(c);
        parrots(c);
        snowGolem(c);
        leash(c);
        pistons(c);
        walls(c);
        furnace(c);
        vines(c);
        tnt(c);
        compass(c);
        armorStand(c);
        endPlatform(c);
        suffocation(c);
        ghast(c);
    }

    // Game rules and anger ------------------------------------------------------------------------------------------

    private static void gameRules(Check c) {
        net.minecraft.world.GameRules rules = c.world.getGameRules();
        c.ok(rules.hasRule(AngerGameRules.FORGIVE_DEAD_PLAYERS) && rules.getBoolean(AngerGameRules.FORGIVE_DEAD_PLAYERS),
                "the world has forgiveDeadPlayers, on");
        c.ok(rules.hasRule(AngerGameRules.UNIVERSAL_ANGER) && !rules.getBoolean(AngerGameRules.UNIVERSAL_ANGER),
                "the world has universalAnger, off");
        c.ok(rules.areSameType(AngerGameRules.UNIVERSAL_ANGER, net.minecraft.world.GameRules.ValueType.BOOLEAN_VALUE),
                "a true/false rule, as /gamerule shows it");
    }

    private static void universalAnger(Check c) throws Exception {
        WorldServer w = c.world;
        net.minecraft.world.GameRules rules = w.getGameRules();
        BlockPos a = c.area();
        EntityPlayerMP player = Players.real(w, "universal");
        player.setPosition(a.getX() + 6.5, a.getY(), a.getZ() + 4.5);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        w.playerEntities.add(player);
        EntityPlayerMP other = Players.real(w, "bystander");
        other.setPosition(a.getX() + 2.5, a.getY(), a.getZ() + 8.5);
        other.interactionManager.setGameType(GameType.SURVIVAL);
        try {
            EntityWolf wolf = new EntityWolf(w);
            wolf.setPosition(a.getX() + 4.5, a.getY(), a.getZ() + 4.5);
            w.spawnEntity(wolf);
            Anger anger = Anger.of(wolf);
            EntityAIBase reset = task(wolf, "ResetAngerGoal");
            if (!c.ok(anger != null && reset != null, "a wolf has 1.16's anger and its ResetAngerGoal")) {
                return;
            }
            EntityAIHurtByTarget hurtBy = new EntityAIHurtByTarget(wolf, false);
            // Hurt on tick 0 reads as "never" to both goals; any later tick will do.
            wolf.ticksExisted = 100;
            wolf.setRevengeTarget(player);
            c.ok(hurtBy.shouldExecute(), "universalAnger off: a wolf a player hits fights back (HurtByTarget)");
            c.ok(!reset.shouldExecute(), "and the anger reset is idle");
            rules.setOrCreateGameRule(AngerGameRules.UNIVERSAL_ANGER, "true");
            c.ok(!hurtBy.shouldExecute(), "universalAnger on: HurtByTarget leaves a player's hit alone (P24)");
            c.ok(reset.shouldExecute(), "and the anger reset takes it");
            reset.startExecuting();
            c.ok(anger.isAngry() && anger.target() == null && anger.remaining() >= 400 && anger.remaining() <= 780,
                    "the wolf is angry for 20-39 s at no one in particular: " + anger.remaining());
            c.ok(anger.isAngryAt(player) && anger.isAngryAt(other), "so it is angry at every player");
            rules.setOrCreateGameRule(AngerGameRules.UNIVERSAL_ANGER, "false");
            c.ok(!anger.isAngryAt(other), "universalAnger off again: at no one");

            // forgiveDeadPlayers.
            wolf.setAttackTarget(player);
            com.exiledradio.netherupdatebackport.mobs.NeutralMobs.updatePersistentAnger(wolf, anger);
            rules.setOrCreateGameRule(AngerGameRules.FORGIVE_DEAD_PLAYERS, "false");
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, net.minecraft.util.DamageSource.GENERIC));
            c.eq(player.getUniqueID(), anger.target(), "forgiveDeadPlayers off: a dead player stays the wolf's anger");
            rules.setOrCreateGameRule(AngerGameRules.FORGIVE_DEAD_PLAYERS, "true");
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, net.minecraft.util.DamageSource.GENERIC));
            c.ok(anger.target() == null && !anger.isAngry(), "on: forgiven");
            wolf.setDead();

            // Zombified piglins have the reset too, with the alarm.
            EntityPigZombie pigman = new EntityPigZombie(w);
            pigman.setPosition(a.getX() + 8.5, a.getY(), a.getZ() + 8.5);
            w.spawnEntity(pigman);
            c.ok(task(pigman, "ResetAngerGoal") != null, "a zombified piglin has the anger reset");
            pigman.setDead();

            // Piglins: a hit with universalAnger remembers it.
            EntityPiglin piglin = new EntityPiglin(w);
            piglin.setPosition(a.getX() + 10.5, a.getY(), a.getZ() + 10.5);
            w.spawnEntity(piglin);
            rules.setOrCreateGameRule(AngerGameRules.UNIVERSAL_ANGER, "true");
            PiglinTasks.setAngerTarget(piglin, player);
            c.ok(piglin.memories.has(Memories.Mem.UNIVERSAL_ANGER), "a piglin angered by a player with universalAnger remembers it");
            rules.setOrCreateGameRule(AngerGameRules.UNIVERSAL_ANGER, "false");
            piglin.setDead();
        } finally {
            rules.setOrCreateGameRule(AngerGameRules.UNIVERSAL_ANGER, "false");
            rules.setOrCreateGameRule(AngerGameRules.FORGIVE_DEAD_PLAYERS, "true");
            w.playerEntities.remove(player);
        }
    }

    private static EntityAIBase task(EntityLiving mob, String simpleName) {
        for (EntityAITasks.EntityAITaskEntry e : mob.targetTasks.taskEntries) {
            if (e.action.getClass().getSimpleName().equals(simpleName)) {
                return e.action;
            }
        }
        return null;
    }

    private static void witherSkeletons(Check c) {
        BlockPos a = c.area();
        EntityWitherSkeleton skeleton = new EntityWitherSkeleton(c.world);
        skeleton.setPosition(a.getX() + 4.5, a.getY(), a.getZ() + 4.5);
        c.world.spawnEntity(skeleton);
        boolean found = false;
        for (EntityAITasks.EntityAITaskEntry e : skeleton.targetTasks.taskEntries) {
            found |= e.action instanceof WitherSkeletons.TargetPiglins && e.priority == 3;
        }
        c.ok(found, "a wither skeleton targets piglins (priority 3)");
        EntityPiglin piglin = new EntityPiglin(c.world);
        piglin.setPosition(a.getX() + 7.5, a.getY(), a.getZ() + 4.5);
        c.world.spawnEntity(piglin);
        boolean took = false;
        for (EntityAITasks.EntityAITaskEntry e : skeleton.targetTasks.taskEntries) {
            // The goal looks 1 tick in 10.
            for (int i = 0; i < 300 && e.action instanceof WitherSkeletons.TargetPiglins && !took; i++) {
                if (e.action.shouldExecute()) {
                    e.action.startExecuting();
                    took = skeleton.getAttackTarget() == piglin;
                }
            }
        }
        c.ok(took, "and takes one it sees as its target");
        skeleton.setDead();
        piglin.setDead();
    }

    // Soul campfire --------------------------------------------------------------------------------------------------

    private static void campfire(Check c) {
        Block campfire = Owners.ourBlock("soul_campfire");
        if (!c.ok(campfire != null, "the soul campfire is registered")) {
            return;
        }
        BlockPos a = c.area().add(4, 0, 4);
        c.world.setBlockState(a, campfire.getDefaultState(), 3);
        IBlockState state = c.world.getBlockState(a);
        c.ok(BlockSoulCampfire.isLit(state), "a placed soul campfire is lit");
        TileEntitySoulCampfire tile = (TileEntitySoulCampfire) c.world.getTileEntity(a);
        tile.items().set(0, new ItemStack(Items.BEEF));
        BlockSoulCampfire.extinguish(c.world, a, state, true);
        c.ok(tile.items().get(0).isEmpty(), "put out, it throws its food off");
        List<EntityItem> items = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(a).grow(2.0D));
        c.ok(items.size() == 1 && items.get(0).getItem().getItem() == Items.BEEF, "the beef lies by it");
        for (EntityItem i : items) {
            i.setDead();
        }
        c.world.setBlockToAir(a);
    }

    // P12 --------------------------------------------------------------------------------------------------------------

    private static void witherSoulSoil(Check c) throws Exception {
        Block soil = Owners.ourBlock("soul_soil");
        if (!c.ok(soil != null, "soul soil is registered")) {
            return;
        }
        Method method = ReflectionHelper.findMethod(BlockSkull.class, "getWitherPattern", "func_176416_l");
        BlockPattern pattern = (BlockPattern) method.invoke(Blocks.SKULL);
        BlockPos a = c.area().add(4, 0, 4);
        c.eq(true, witherBuilt(c, pattern, a, soil, soil), "a wither of soul soil matches the pattern");
        c.eq(true, witherBuilt(c, pattern, a.add(6, 0, 0), soil, Blocks.SOUL_SAND), "so does one of soul soil and soul sand");
        c.eq(true, witherBuilt(c, pattern, a.add(12, 0, 0), Blocks.SOUL_SAND, Blocks.SOUL_SAND), "and soul sand alone, as always");
        ModConfig.changes.witherSoulSoil = false;
        try {
            c.eq(false, witherBuilt(c, pattern, a.add(18, 0, 0), soil, soil), "with the setting off, soul soil does not");
        } finally {
            ModConfig.changes.witherSoulSoil = true;
        }
    }

    /** A T of base blocks (stem {@code stem}, arms {@code arms}) with three wither skeleton skulls; true if it matches. */
    private static boolean witherBuilt(Check c, BlockPattern pattern, BlockPos at, Block stem, Block arms) {
        c.world.setBlockState(at, stem.getDefaultState(), 2 | 16);
        c.world.setBlockState(at.up().west(), arms.getDefaultState(), 2 | 16);
        c.world.setBlockState(at.up(), stem.getDefaultState(), 2 | 16);
        c.world.setBlockState(at.up().east(), arms.getDefaultState(), 2 | 16);
        for (int dx = -1; dx <= 1; dx++) {
            BlockPos p = at.up(2).east(dx);
            c.world.setBlockState(p, Blocks.SKULL.getDefaultState().withProperty(BlockSkull.FACING, EnumFacing.UP), 2 | 16);
            ((TileEntitySkull) c.world.getTileEntity(p)).setType(1);
        }
        return pattern.match(c.world, at.up(2)) != null;
    }

    // P13 --------------------------------------------------------------------------------------------------------------

    private static void magma(Check c) {
        BlockPos a = c.area().add(8, 0, 8);
        WalkNodeProcessor nodes = new WalkNodeProcessor();
        c.eq(PathNodeType.WALKABLE, nodes.checkNeighborBlocks(c.world, a.getX(), a.getY(), a.getZ(), PathNodeType.WALKABLE),
                "a node on stone is walkable");
        c.world.setBlockState(a.add(1, -1, 1), Blocks.MAGMA.getDefaultState(), 2 | 16);
        c.eq(PathNodeType.DANGER_FIRE, nodes.checkNeighborBlocks(c.world, a.getX(), a.getY(), a.getZ(), PathNodeType.WALKABLE),
                "one beside a magma block underfoot is a fire danger");
        ModConfig.changes.magmaPathing = false;
        try {
            c.eq(PathNodeType.WALKABLE, nodes.checkNeighborBlocks(c.world, a.getX(), a.getY(), a.getZ(), PathNodeType.WALKABLE),
                    "with the setting off, walkable");
        } finally {
            ModConfig.changes.magmaPathing = true;
        }
        c.eq(PathNodeType.OPEN, nodes.checkNeighborBlocks(c.world, a.getX(), a.getY(), a.getZ(), PathNodeType.OPEN),
                "other node types are left alone");
    }

    // Dispensers --------------------------------------------------------------------------------------------------------

    private static void dispensers(Check c) {
        BlockPos a = c.area().add(2, 0, 2);
        WorldServer w = c.world;
        // Saddle on a pig.
        BlockPos front = dispenser(c, a);
        EntityPig pig = new EntityPig(w);
        pig.setPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        w.spawnEntity(pig);
        ItemStack saddle = new ItemStack(Items.SADDLE, 2);
        dispense(c, a, saddle);
        c.ok(pig.getSaddled() && saddle.getCount() == 1, "a dispenser saddles a pig");
        dispense(c, a, saddle);
        List<EntityItem> dropped = w.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(front).grow(1.0D));
        c.ok(saddle.isEmpty() && dropped.size() == 1, "with nothing to saddle, it drops the saddle as before");
        for (EntityItem i : dropped) {
            i.setDead();
        }
        pig.setDead();

        // Horse armor on a tamed horse, a chest on a tamed donkey, a carpet on a tamed llama.
        a = a.add(0, 0, 4);
        front = dispenser(c, a);
        EntityHorse horse = new EntityHorse(w);
        horse.setPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        w.spawnEntity(horse);
        ItemStack armor = new ItemStack(Items.IRON_HORSE_ARMOR);
        dispense(c, a, armor);
        c.ok(horse.getHorseArmorType() == HorseArmorType.NONE, "an untamed horse gets no armor");
        horse.setHorseTamed(true);
        armor = new ItemStack(Items.IRON_HORSE_ARMOR);
        dispense(c, a, armor);
        c.ok(horse.getHorseArmorType() == HorseArmorType.IRON && armor.isEmpty(), "a tamed one gets the iron horse armor");
        ItemStack saddle2 = new ItemStack(Items.SADDLE);
        dispense(c, a, saddle2);
        c.ok(horse.isHorseSaddled() && saddle2.isEmpty(), "and a saddle");
        horse.setDead();
        clearItems(c, front);

        EntityDonkey donkey = new EntityDonkey(w);
        donkey.setPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        donkey.setHorseTamed(true);
        w.spawnEntity(donkey);
        ItemStack chest = new ItemStack(Blocks.CHEST);
        dispense(c, a, chest);
        c.ok(donkey.hasChest() && chest.isEmpty(), "a tamed donkey gets a chest");
        donkey.setDead();

        EntityLlama llama = new EntityLlama(w);
        llama.setPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        llama.setHorseTamed(true);
        w.spawnEntity(llama);
        ItemStack carpet = new ItemStack(Blocks.CARPET, 1, 14);
        dispense(c, a, carpet);
        c.ok(llama.getColor() != null && carpet.isEmpty(), "a tamed llama gets a carpet: " + llama.getColor());
        llama.setDead();
        clearItems(c, front);

        // Shears: a mooshroom and a snow golem.
        a = a.add(0, 0, 4);
        front = dispenser(c, a);
        EntityMooshroom moo = new EntityMooshroom(w);
        moo.setPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        w.spawnEntity(moo);
        ItemStack shears = new ItemStack(Items.SHEARS);
        dispense(c, a, shears);
        List<EntityCow> cows = w.getEntitiesWithinAABB(EntityCow.class, new AxisAlignedBB(front).grow(1.0D));
        boolean cow = false;
        for (EntityCow e : cows) {
            cow |= !(e instanceof EntityMooshroom) && !e.isDead;
            e.setDead();
        }
        c.ok(moo.isDead && cow && shears.getItemDamage() == 1, "a dispenser shears a mooshroom into a cow, wearing the shears");
        clearItems(c, front);
        EntitySnowman golem = new EntitySnowman(w);
        golem.setPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
        w.spawnEntity(golem);
        dispense(c, a, shears);
        c.ok(!golem.isPumpkinEquipped(), "and takes a snow golem's pumpkin");
        golem.setDead();
    }

    /** A dispenser at {@code at} facing east; @return the block in front of it */
    private static BlockPos dispenser(Check c, BlockPos at) {
        c.world.setBlockState(at, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 2 | 16);
        return at.east();
    }

    private static void dispense(Check c, BlockPos at, ItemStack stack) {
        ItemStack out = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(stack.getItem()).dispense(new BlockSourceImpl(c.world, at), stack);
        if (out != stack) {
            stack.setCount(out.getCount());
        }
    }

    private static void clearItems(Check c, BlockPos at) {
        for (EntityItem i : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(3.0D))) {
            i.setDead();
        }
    }

    // Enderman, hoes, compass ---------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void enderman(Check c) throws Exception {
        Field field = ReflectionHelper.findField(net.minecraft.entity.monster.EntityEnderman.class, "CARRIABLE_BLOCKS");
        Set<Block> carriable = (Set<Block>) field.get(null);
        c.ok(!carriable.contains(Blocks.NETHERRACK), "endermen no longer pick up netherrack");
        for (String name : new String[] {"crimson_nylium", "warped_nylium", "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots"}) {
            Block b = Owners.ourBlock(name);
            c.ok(b != null && carriable.contains(b), "endermen pick up " + name);
        }
        c.ok(carriable.contains(Blocks.GRASS) && carriable.contains(Blocks.TNT), "and still grass and TNT");
    }

    private static void hoes(Check c) {
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        c.ok(EnumEnchantmentType.DIGGER.canEnchantItem(Items.DIAMOND_HOE), "a hoe is a digger (P14)");
        c.ok(Enchantments.EFFICIENCY.canApply(hoe) && Enchantments.FORTUNE.canApply(hoe) && Enchantments.SILK_TOUCH.canApply(hoe),
                "a hoe takes Efficiency, Fortune and Silk Touch at the anvil");
        c.ok(Enchantments.EFFICIENCY.canApplyAtEnchantingTable(hoe) && Enchantments.FORTUNE.canApplyAtEnchantingTable(hoe),
                "and at the enchanting table");
        c.ok(!Enchantments.SHARPNESS.canApply(hoe), "not Sharpness");
        c.eq(10, Items.DIAMOND_HOE.getItemEnchantability(), "a diamond hoe has diamond's enchantability, 10");
        c.eq(22, Items.GOLDEN_HOE.getItemEnchantability(), "a golden one gold's, 22");
        java.util.Random rand = new java.util.Random(1);
        Set<net.minecraft.enchantment.Enchantment> offered = new HashSet<net.minecraft.enchantment.Enchantment>();
        for (int i = 0; i < 200; i++) {
            for (net.minecraft.enchantment.EnchantmentData d : net.minecraft.enchantment.EnchantmentHelper.buildEnchantmentList(rand, hoe, 30, false)) {
                offered.add(d.enchantment);
            }
        }
        c.ok(offered.contains(Enchantments.EFFICIENCY) && offered.contains(Enchantments.FORTUNE) && offered.contains(Enchantments.SILK_TOUCH)
                && offered.contains(Enchantments.UNBREAKING), "the enchanting table offers a hoe Efficiency, Fortune, Silk Touch and Unbreaking: " + offered);
        c.ok(EnumEnchantmentType.DIGGER.canEnchantItem(Items.DIAMOND_PICKAXE), "pickaxes still are diggers");
        c.ok(!EnumEnchantmentType.DIGGER.canEnchantItem(Items.DIAMOND_SWORD), "swords still are not");
        ModConfig.changes.hoeEnchanting = false;
        try {
            c.ok(!Enchantments.FORTUNE.canApply(hoe), "with the setting off, no Fortune on a hoe");
            c.eq(0, Items.DIAMOND_HOE.getItemEnchantability(), "and no enchantability, as 1.12.2");
        } finally {
            ModConfig.changes.hoeEnchanting = true;
        }
    }

    private static void compass(Check c) {
        c.ok(Enchantments.VANISHING_CURSE.canApply(new ItemStack(Items.COMPASS)), "Curse of Vanishing goes on a compass (P15)");
        Item lodestone = Owners.ours("lodestone_compass");
        c.ok(lodestone == null || Enchantments.VANISHING_CURSE.canApply(new ItemStack(lodestone)), "and on a lodestone compass");
        c.ok(!Enchantments.MENDING.canApply(new ItemStack(Items.COMPASS)), "Mending does not");
        c.ok(!Enchantments.VANISHING_CURSE.canApply(new ItemStack(Items.STICK)), "the curse still not on a stick");
        ModConfig.changes.compassVanishing = false;
        try {
            c.ok(!Enchantments.VANISHING_CURSE.canApply(new ItemStack(Items.COMPASS)), "with the setting off, not on a compass");
        } finally {
            ModConfig.changes.compassVanishing = true;
        }
    }

    // Spawn costs --------------------------------------------------------------------------------------------------------

    private static void spawnCosts(Check c) {
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        ResourceLocation valley = new ResourceLocation("netherupdatebackport", "soul_sand_valley");
        BlockPos found = null;
        for (int r = 0; r <= 4096 && found == null; r += 64) {
            for (int x = -r; x <= r && found == null; x += 64) {
                for (int z = -r; z <= r && found == null; z += 64) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    Biome b = nether.getBiomeProvider().getBiome(new BlockPos(x + 8, 64, z + 8));
                    if (valley.equals(b.getRegistryName())) {
                        found = new BlockPos(x + 8, 64, z + 8);
                    }
                }
            }
        }
        if (!c.ok(found != null && ChangesContent.spawnCosts != null, "a soul sand valley is found in the nether")) {
            return;
        }
        nether.getChunkProvider().provideChunk(found.getX() >> 4, found.getZ() >> 4);
        if (!c.ok(valley.equals(nether.getBiome(found).getRegistryName()), "and the chunk there is one")) {
            return;
        }
        EntitySkeleton first = new EntitySkeleton(nether);
        first.setPosition(found.getX() + 0.5, found.getY(), found.getZ() + 0.5);
        nether.spawnEntity(first);
        ChangesContent.spawnCosts.forget(nether);
        c.ok(Math.abs(ChangesContent.spawnCosts.energyChange(nether, found.east(), 0.7D) - 0.49D) < 1.0E-9,
                "a skeleton next to a soul sand valley skeleton adds 0.7 x 0.7 / 1 = 0.49");
        c.eq(Event.Result.DENY, checkSpawn(nether, new EntitySkeleton(nether), found.east()), "over the 0.15 budget: refused");
        c.eq(Event.Result.DEFAULT, checkSpawn(nether, new EntitySkeleton(nether), found.east(10)), "10 blocks off (0.049): allowed");
        c.eq(Event.Result.DEFAULT, checkSpawn(nether, new EntityPigZombie(nether), found.east()), "a zombified piglin has no cost");
        ModConfig.changes.spawnCosts = false;
        try {
            c.eq(Event.Result.DEFAULT, checkSpawn(nether, new EntitySkeleton(nether), found.east()), "with the setting off, allowed");
        } finally {
            ModConfig.changes.spawnCosts = true;
        }
        first.setDead();
        ChangesContent.spawnCosts.forget(nether);
    }

    private static Event.Result checkSpawn(WorldServer world, EntityLiving mob, BlockPos at) {
        mob.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        LivingSpawnEvent.CheckSpawn event = new LivingSpawnEvent.CheckSpawn(mob, world, at.getX() + 0.5F, at.getY(), at.getZ() + 0.5F, null);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    // Cave mood ---------------------------------------------------------------------------------------------------------

    private static void caveMood(Check c) {
        float m = 0.0F;
        for (int i = 0; i < 6000; i++) {
            m = SmallChanges.caveMood(m, 0, 0, 6000);
        }
        c.ok(Math.abs(m - 1.0F) < 1.0E-3F, "6000 ticks of darkness fill the cave mood: " + m);
        c.ok(SmallChanges.caveMood(0.5F, 15, 0, 6000) < 0.5F, "sky light lowers it");
        c.ok(SmallChanges.caveMood(0.5F, 0, 8, 6000) < 0.5F, "so does block light");
    }

    // Wither rose on soul soil -----------------------------------------------------------------------------------------

    private static void witherRose(Check c) {
        Block soil = Owners.ourBlock("soul_soil");
        if (!c.ok(soil instanceof SoulSoil, "soul soil is its own block")) {
            return;
        }
        BlockBush rose = new BlockBush() {
        };
        rose.setRegistryName(new ResourceLocation("villagerbackport", "wither_rose"));
        BlockBush poppy = new BlockBush() {
        };
        poppy.setRegistryName(new ResourceLocation("villagerbackport", "cornflower"));
        BlockPos p = c.area();
        c.ok(soil.canSustainPlant(soil.getDefaultState(), c.world, p, EnumFacing.UP, rose), "soul soil holds Village & Pillage's wither rose");
        c.ok(!soil.canSustainPlant(soil.getDefaultState(), c.world, p, EnumFacing.UP, poppy), "not another flower");
        ModConfig.changes.witherRoseSoulSoil = false;
        try {
            c.ok(!soil.canSustainPlant(soil.getDefaultState(), c.world, p, EnumFacing.UP, rose), "with the setting off, not the rose");
        } finally {
            ModConfig.changes.witherRoseSoulSoil = true;
        }
    }

    // P16 -----------------------------------------------------------------------------------------------------------------

    /** Counts parrot sounds played. */
    public static final class Sounds {
        final Set<SoundEvent> imitations = new HashSet<SoundEvent>();
        int imitated;
        int ambient;

        @SubscribeEvent
        public void sound(PlaySoundAtEntityEvent event) {
            if (this.imitations.contains(event.getSound())) {
                this.imitated++;
            } else if (event.getSound() == net.minecraft.init.SoundEvents.ENTITY_PARROT_AMBIENT) {
                this.ambient++;
            }
            event.setCanceled(true);
        }
    }

    @SuppressWarnings("unchecked")
    private static void parrots(Check c) throws Exception {
        BlockPos a = c.area();
        WorldServer w = c.world;
        EntityParrot parrot = new EntityParrot(w);
        parrot.setPosition(a.getX() + 4.5, a.getY(), a.getZ() + 4.5);
        w.spawnEntity(parrot);
        EntityZombie zombie = new EntityZombie(w);
        zombie.setPosition(a.getX() + 8.5, a.getY(), a.getZ() + 4.5);
        w.spawnEntity(zombie);
        Sounds sounds = new Sounds();
        Field map = ReflectionHelper.findField(EntityParrot.class, "MIMIC_SOUNDS");
        sounds.imitations.addAll(((java.util.Map<Class<?>, SoundEvent>) map.get(null)).values());
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            int n = 80000;
            for (int i = 0; i < n; i++) {
                SmallHooks.parrotTick(w, parrot);
            }
            // 1.16: 1/400 x 1/2 = 100 expected; 1.12.2: 1/50 = 1600.
            c.ok(sounds.imitated >= 50 && sounds.imitated <= 170, "a parrot imitates 1 tick in 800 (about 100 of 80000): " + sounds.imitated);
            sounds.imitated = 0;
            sounds.ambient = 0;
            Method shoulder = ReflectionHelper.findMethod(EntityParrot.class, "playAmbientSound", "func_192005_a",
                    net.minecraft.world.World.class, Entity.class);
            for (int i = 0; i < 40000; i++) {
                shoulder.invoke(null, w, zombie);
            }
            int total = sounds.imitated + sounds.ambient;
            // 1.16: 1/200 = 200 expected (half imitations); 1.12.2: about 1000.
            c.ok(total >= 130 && total <= 280, "on a shoulder, a sound 1 tick in 200 (about 200 of 40000): " + total);
            c.ok(sounds.imitated > 0 && sounds.ambient > 0, "imitations and its own call both: " + sounds.imitated + "/" + sounds.ambient);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
            parrot.setDead();
            zombie.setDead();
        }
    }

    // P17 -----------------------------------------------------------------------------------------------------------------

    private static void snowGolem(Check c) throws Exception {
        BlockPos a = c.area();
        EntitySnowman golem = new EntitySnowman(c.world);
        golem.setPosition(a.getX() + 4.5, a.getY(), a.getZ() + 4.5);
        c.world.spawnEntity(golem);
        float before = golem.getHealth();
        ItemStack water = PotionUtils.addPotionToItemStack(new ItemStack(Items.SPLASH_POTION), PotionTypes.WATER);
        EntityPotion potion = new EntityPotion(c.world, golem.posX, golem.posY + 0.5D, golem.posZ, water);
        ReflectionHelper.findMethod(EntityPotion.class, "applyWater", "func_190545_n").invoke(potion);
        c.eq(before - 1.0F, golem.getHealth(), "a water bottle hurts a snow golem by 1");
        golem.setDead();
        Method sensitive = ReflectionHelper.findMethod(EntityPotion.class, "isWaterSensitiveEntity", "func_190544_c", EntityLivingBase.class);
        c.eq(false, sensitive.invoke(null, new EntityZombie(c.world)), "not a zombie");
    }

    // P18 -----------------------------------------------------------------------------------------------------------------

    private static void leash(Check c) {
        EntityPlayerMP player = Players.real(c.world, "leash");
        EntitySkeletonHorse horse = new EntitySkeletonHorse(c.world);
        c.ok(horse.canBeLeashedTo(player), "a skeleton horse takes a lead");
        c.ok(new net.minecraft.entity.passive.EntityZombieHorse(c.world).canBeLeashedTo(player), "so does a zombie horse");
        c.ok(new EntityHorse(c.world).canBeLeashedTo(player), "and a horse, as always");
        ModConfig.changes.undeadHorseLeash = false;
        try {
            c.ok(!horse.canBeLeashedTo(player), "with the setting off, not a skeleton horse");
        } finally {
            ModConfig.changes.undeadHorseLeash = true;
        }
    }

    // Pistons -------------------------------------------------------------------------------------------------------------

    private static void pistons(Check c) {
        BlockPos p = c.area();
        c.ok(SmallChanges.pistonsHarder, "pistons were made harder at startup");
        for (Block b : new Block[] {Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.PISTON_HEAD}) {
            c.eq(1.5F, b.getDefaultState().getBlockHardness(c.world, p), b.getRegistryName() + " hardness 1.5");
            c.eq(1.5F, b.getExplosionResistance(null), b.getRegistryName() + " blast resistance 1.5");
            c.eq("pickaxe", b.getHarvestTool(b.getDefaultState()), b.getRegistryName() + " is mined with a pickaxe");
        }
        c.ok(Items.IRON_PICKAXE.getDestroySpeed(new ItemStack(Items.IRON_PICKAXE), Blocks.PISTON.getDefaultState()) > 1.0F,
                "a pickaxe digs a piston faster than a hand");
    }

    // P19 -----------------------------------------------------------------------------------------------------------------

    private static void walls(Check c) {
        BlockPos p = c.area().add(4, 0, 4);
        c.world.setBlockState(p, Blocks.COBBLESTONE_WALL.getDefaultState(), 2 | 16);
        c.world.setBlockState(p.east(), Blocks.GLASS_PANE.getDefaultState(), 2 | 16);
        c.world.setBlockState(p.west(), Blocks.IRON_BARS.getDefaultState(), 2 | 16);
        IBlockState wall = c.world.getBlockState(p).getActualState(c.world, p);
        c.ok(wall.getValue(BlockWall.EAST) && wall.getValue(BlockWall.WEST), "a wall connects to a glass pane and iron bars");
        IBlockState pane = c.world.getBlockState(p.east()).getActualState(c.world, p.east());
        c.ok(pane.getValue(BlockPane.WEST), "and the pane to the wall");
        ModConfig.changes.wallsPanes = false;
        try {
            IBlockState off = c.world.getBlockState(p).getActualState(c.world, p);
            c.ok(!off.getValue(BlockWall.EAST), "with the setting off, they do not");
        } finally {
            ModConfig.changes.wallsPanes = true;
        }
    }

    // P20 -----------------------------------------------------------------------------------------------------------------

    private static void furnace(Check c) {
        BlockPos p = c.area().add(4, 0, 4);
        c.eq(7, furnaceXp(c, p, true), "breaking a furnace with 10 iron ingots out gives 7 XP (0.7 each)");
        c.eq(0, furnaceXp(c, p.east(4), false), "with the setting off, none");
    }

    private static int furnaceXp(Check c, BlockPos p, boolean on) {
        c.world.setBlockState(p, Blocks.FURNACE.getDefaultState(), 3);
        ((TileEntityFurnace) c.world.getTileEntity(p)).setInventorySlotContents(2, new ItemStack(Items.IRON_INGOT, 10));
        ModConfig.changes.furnaceXp = on;
        try {
            c.world.setBlockToAir(p);
        } finally {
            ModConfig.changes.furnaceXp = true;
        }
        int xp = 0;
        for (EntityXPOrb orb : c.world.getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(p).grow(1.0D))) {
            xp += orb.getXpValue();
            orb.setDead();
        }
        clearItems(c, p);
        return xp;
    }

    // P25 and the vine's sound --------------------------------------------------------------------------------------------

    private static void vines(Check c) {
        c.ok(SmallHooks.climbable(Blocks.VINE, Blocks.LADDER), "climbing a vine counts as steps (P25)");
        c.ok(SmallHooks.climbable(Blocks.LADDER, Blocks.LADDER), "a ladder still does");
        c.ok(!SmallHooks.climbable(Blocks.STONE, Blocks.LADDER), "stone does not");
        Block weeping = Owners.ourBlock("weeping_vines");
        c.ok(weeping == null || SmallHooks.climbable(weeping, Blocks.LADDER), "nor do weeping vines fail to");
        c.eq(new ResourceLocation("netherupdatebackport", "block.vine.step"), Blocks.VINE.getSoundType().getStepSound().getRegistryName(),
                "vines step with 1.16's climbing sound");
        c.eq(net.minecraft.init.SoundEvents.BLOCK_GRASS_BREAK, Blocks.VINE.getSoundType().getBreakSound(), "and break as grass");
        ModConfig.changes.vineClimbing = false;
        try {
            c.ok(!SmallHooks.climbable(Blocks.VINE, Blocks.LADDER), "with the setting off, vines do not count");
        } finally {
            ModConfig.changes.vineClimbing = true;
        }
    }

    // TNT ------------------------------------------------------------------------------------------------------------------

    private static void tnt(Check c) {
        BlockPos p = c.area().add(8, 0, 8);
        c.ok(hitTnt(c, p, true), "a burning snowball lights the TNT it hits");
        c.ok(!hitTnt(c, p.east(6), false), "an unburning one does not");
        ModConfig.changes.burningProjectilesTnt = false;
        try {
            c.ok(!hitTnt(c, p.east(12), true), "with the setting off, neither does a burning one");
        } finally {
            ModConfig.changes.burningProjectilesTnt = true;
        }
    }

    private static boolean hitTnt(Check c, BlockPos p, boolean burning) {
        c.world.setBlockState(p, Blocks.TNT.getDefaultState(), 2 | 16);
        EntitySnowball ball = new EntitySnowball(c.world, p.getX() + 0.5D, p.getY() + 1.5D, p.getZ() + 0.5D);
        if (burning) {
            ball.setFire(5);
        }
        RayTraceResult ray = new RayTraceResult(new Vec3d(p.getX() + 0.5D, p.getY() + 1.0D, p.getZ() + 0.5D), EnumFacing.UP, p);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Throwable(ball, ray));
        boolean lit = c.world.getBlockState(p).getBlock() != Blocks.TNT;
        List<EntityTNTPrimed> primed = c.world.getEntitiesWithinAABB(EntityTNTPrimed.class, new AxisAlignedBB(p).grow(1.0D));
        lit &= !primed.isEmpty();
        for (EntityTNTPrimed e : primed) {
            e.setDead();
        }
        c.world.setBlockToAir(p);
        return lit;
    }

    // P23 ------------------------------------------------------------------------------------------------------------------

    private static void armorStand(Check c) {
        BlockPos p = c.area().add(6, 0, 6);
        c.ok(placeStand(c, p) == Blocks.TALLGRASS, "an armor stand placed in tall grass leaves the grass (P23)");
        ModConfig.changes.armorStandPlacement = false;
        try {
            c.ok(placeStand(c, p.east(4)) == Blocks.AIR, "with the setting off, the grass goes");
        } finally {
            ModConfig.changes.armorStandPlacement = true;
        }
    }

    private static Block placeStand(Check c, BlockPos p) {
        c.world.setBlockState(p.down(), Blocks.GRASS.getDefaultState(), 2 | 16);
        c.world.setBlockState(p, Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), 2 | 16);
        EntityPlayerMP player = c.player("stand" + p.getX(), p.south(2));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.ARMOR_STAND));
        Items.ARMOR_STAND.onItemUse(player, c.world, p, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        List<EntityArmorStand> stands = c.world.getEntitiesWithinAABB(EntityArmorStand.class, new AxisAlignedBB(p).grow(0.5D));
        c.ok(stands.size() == 1, "the armor stand is placed there");
        for (EntityArmorStand s : stands) {
            s.setDead();
        }
        return c.world.getBlockState(p).getBlock();
    }

    // The End platform ------------------------------------------------------------------------------------------------------

    private static void endPlatform(Check c) {
        WorldServer end = c.world.getMinecraftServer().getWorld(1);
        BlockPos center = new BlockPos(100, 48, 0);
        end.setBlockState(center, Blocks.AIR.getDefaultState(), 2 | 16);
        end.setBlockState(center.up(2), Blocks.END_STONE.getDefaultState(), 2 | 16);
        EntityPig pig = new EntityPig(c.world);
        BlockPos a = c.area();
        pig.setPosition(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        MinecraftForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(pig, 1));
        boolean platform = true;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                platform &= end.getBlockState(center.add(x, 0, z)).getBlock() == Blocks.OBSIDIAN;
                for (int y = 1; y <= 3; y++) {
                    platform &= end.isAirBlock(center.add(x, y, z));
                }
            }
        }
        c.ok(platform, "a pig going to the End rebuilds the obsidian platform with air above");
    }

    // P21, P22 -----------------------------------------------------------------------------------------------------------------

    private static void suffocation(Check c) {
        c.ok(Blocks.FARMLAND.getDefaultState().causesSuffocation(), "farmland suffocates (P21)");
        c.ok(Blocks.GRASS_PATH.getDefaultState().causesSuffocation(), "so does a grass path");
        c.ok(Blocks.SOUL_SAND.getDefaultState().causesSuffocation(), "soul sand already does in 1.12.2");
        ModConfig.changes.suffocation = false;
        try {
            c.ok(!Blocks.FARMLAND.getDefaultState().causesSuffocation(), "with the setting off, farmland does not");
        } finally {
            ModConfig.changes.suffocation = true;
        }
    }

    private static void ghast(Check c) throws Exception {
        Method volume = ReflectionHelper.findMethod(EntityLivingBase.class, "getSoundVolume", "func_70599_aP");
        c.eq(5.0F, volume.invoke(new EntityGhast(c.world)), "a ghast's sounds have volume 5 (80 blocks) (P22)");
        ModConfig.changes.ghastSoundRange = false;
        try {
            c.eq(10.0F, volume.invoke(new EntityGhast(c.world)), "with the setting off, 10");
        } finally {
            ModConfig.changes.ghastSoundRange = true;
        }
    }
}
