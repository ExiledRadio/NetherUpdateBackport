package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.piglin.Crossbows;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;

/**
 * Piglins fight a player who wears no gold, as vanilla: a crossbow piglin charges and shoots, a sword piglin closes in
 * and hits, and neither runs away; a soul torch within 8 blocks does send them off (1.16's repellents).
 */
public final class PiglinFightSelfTest implements SelfTest {

    @Override
    public String name() {
        return "piglinfight";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        nether.getMinecraftServer().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
        int base = 4000 + (int) (System.currentTimeMillis() / 1000 % 3000) * 40;

        String[] scenarios = {"plain", "hitBack", "torchMidFight", "holdingTorch", "group", "zombifiedFar", "moving", "hoglins", "hoglinHunt"};
        int i = 0;
        for (String scenario : scenarios) {
            fightCheck(c, nether, new BlockPos(base + 64 * i++, 100, 4000), true, scenario);
            fightCheck(c, nether, new BlockPos(base + 64 * i++, 100, 4000), false, scenario);
        }
        repellentCheck(c, nether, new BlockPos(base + 64 * i, 100, 4000));
    }

    /** A 40x40 stone floor with air above, its chunks loaded. */
    static void arena(WorldServer w, BlockPos at) {
        for (int x = -20; x <= 20; x += 16) {
            for (int z = -20; z <= 20; z += 16) {
                w.getChunkProvider().provideChunk((at.getX() + x) >> 4, (at.getZ() + z) >> 4);
            }
        }
        for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-20, -1, -20), at.add(20, 6, 20))) {
            w.setBlockState(p, p.getY() == at.getY() - 1 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 2 | 16);
        }
        for (Entity e : w.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(at).grow(24))) {
            if (!(e instanceof EntityPlayerMP)) {
                e.setDead();
            }
        }
    }

    static EntityPlayerMP player(WorldServer w, BlockPos at, String name) {
        EntityPlayerMP player = Players.real(w, name);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.dimension = -1;
        player.setPositionAndUpdate(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        w.spawnEntity(player);
        // A new player is invulnerable for its first 60 ticks, counted down by ticks this test does not run.
        net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(EntityPlayerMP.class, player, 0, "field_147101_bU");
        return player;
    }

    static EntityPiglin piglin(WorldServer w, BlockPos at, boolean crossbow) {
        return piglin(w, at, crossbow, true);
    }

    /** @param spawned as a spawned piglin is: through onInitialSpawn, which gives 1.16's 30-120 s without hunting */
    static EntityPiglin piglin(WorldServer w, BlockPos at, boolean crossbow, boolean spawned) {
        EntityPiglin p = new EntityPiglin(w);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setImmuneToZombification(true);
        if (spawned) {
            p.onInitialSpawn(w.getDifficultyForLocation(at), null);
            p.setChild(false);
            for (EntityEquipmentSlot slot : new EntityEquipmentSlot[] {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
                    EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET}) {
                p.setItemStackToSlot(slot, ItemStack.EMPTY);
            }
        }
        w.spawnEntity(p);
        p.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, crossbow ? new ItemStack(Crossbows.item()) : new ItemStack(Items.GOLDEN_SWORD));
        return p;
    }

    private static void fightCheck(Check c, WorldServer w, BlockPos at, boolean crossbow, String scenario) {
        arena(w, at);
        String kind = scenario + "/" + (crossbow ? "crossbow" : "sword");
        EntityPlayerMP player = player(w, at.add(crossbow ? 9 : 6, 0, 0), "fight" + scenario + crossbow);
        boolean hunt = "hoglinHunt".equals(scenario);
        EntityPiglin p = piglin(w, at, crossbow, !hunt);
        java.util.List<EntityPiglin> others = new java.util.ArrayList<EntityPiglin>();
        if ("group".equals(scenario)) {
            for (int k = 0; k < 3; k++) {
                others.add(piglin(w, at.add(-2, 0, 2 * k - 2), k % 2 == 0));
            }
        }
        if ("holdingTorch".equals(scenario) && Owners.ours("soul_torch") != null) {
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(Owners.ours("soul_torch")));
        }
        java.util.List<net.minecraft.entity.EntityLiving> beasts = new java.util.ArrayList<net.minecraft.entity.EntityLiving>();
        if ("hoglins".equals(scenario) || hunt) {
            for (int k = 0; k < 2; k++) {
                com.exiledradio.netherupdatebackport.hoglin.EntityHoglin h = new com.exiledradio.netherupdatebackport.hoglin.EntityHoglin(w);
                // Behind the piglin, away from its line of fire: a bolt that hits a hoglin turns the herd on the
                // piglin and, outnumbered, it retreats, as in vanilla.
                h.setPosition(at.getX() - 5.5, at.getY(), at.getZ() + 0.5 + 3 * k);
                h.setImmuneToZombification(true);
                h.setNoAI(true);
                w.spawnEntity(h);
                beasts.add(h);
            }
        }
        if ("zombifiedFar".equals(scenario)) {
            net.minecraft.entity.monster.EntityPigZombie z = new net.minecraft.entity.monster.EntityPigZombie(w);
            z.setPosition(at.getX() - 10.5, at.getY(), at.getZ() + 0.5);
            z.setNoAI(true);
            w.spawnEntity(z);
        }
        StringBuilder trace = new StringBuilder();
        int fought = 0;
        int avoided = 0;
        int arrows = 0;
        int hits = 0;
        String last = "";
        java.util.Set<Integer> seen = new java.util.HashSet<Integer>();
        for (int t = 0; t < 400 && !p.isDead; t++) {
            float before = player.getHealth();
            player.hurtResistantTime = 0;
            p.onUpdate();
            for (EntityPiglin o : others) {
                o.onUpdate();
            }
            for (net.minecraft.entity.EntityLiving b : beasts) {
                b.onUpdate();
            }
            if ("moving".equals(scenario)) {
                // Circles the piglin at 3 to 8 blocks, as a player fighting it does.
                double ang = t * 0.05;
                double r = 5.5 + 2.5 * Math.sin(t * 0.03);
                player.setPosition(p.posX + r * Math.cos(ang), at.getY(), p.posZ + r * Math.sin(ang));
            }
            if ("hitBack".equals(scenario) && t % 20 == 10 && p.getDistance(player) < 5) {
                p.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(player), 1.0F);
                p.hurtResistantTime = 0;
                p.setHealth(p.getMaxHealth());
            }
            if ("torchMidFight".equals(scenario) && t == 60 && Owners.ourBlock("soul_torch") != null) {
                w.setBlockState(new BlockPos(p).add(2, 0, 0), Owners.ourBlock("soul_torch").getDefaultState());
            }
            for (Entity e : w.getEntitiesWithinAABB(EntityArrow.class, new AxisAlignedBB(at).grow(30))) {
                if (seen.add(e.getEntityId())) {
                    arrows++;
                }
                e.onUpdate();
            }
            if (player.getHealth() < before) {
                hits++;
                player.setHealth(player.getMaxHealth());
            }
            String a = p.activity().name();
            fought += "FIGHT".equals(a) ? 1 : 0;
            avoided += "AVOID".equals(a) ? 1 : 0;
            if (t == 50) {
                trace.append(" {sees=").append(p.visibleLiving().contains(player)).append(" targetable=").append(p.isTargetable(player))
                        .append(" allowed=").append(com.exiledradio.netherupdatebackport.piglin.AbstractPiglin.isAttackAllowed(player))
                        .append(" inList=").append(w.playerEntities.contains(player)).append(" alive=").append(player.isEntityAlive())
                        .append(" diff=").append(player.world.getDifficulty()).append(" creative=").append(player.isCreative())
                        .append(" child=").append(p.isChild()).append(" gold=").append(com.exiledradio.netherupdatebackport.piglin.PiglinTasks.isWearingGold(player))
                        .append(" offhand=").append(p.getHeldItemOffhand()).append(" players=").append(w.playerEntities.size())
                        .append(" mem=").append(p.memories.get(com.exiledradio.netherupdatebackport.piglin.Memories.Mem.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD) != null)
                        .append(" zomb=").append((Object) p.memories.get(com.exiledradio.netherupdatebackport.piglin.Memories.Mem.NEAREST_VISIBLE_ZOMBIFIED))
                        .append(" nemesis=").append((Object) p.memories.get(com.exiledradio.netherupdatebackport.piglin.Memories.Mem.NEAREST_VISIBLE_NEMESIS))
                        .append(" angry=").append(p.memories.has(com.exiledradio.netherupdatebackport.piglin.Memories.Mem.ANGRY_AT))
                        .append(" near=").append(w.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class, p.getEntityBoundingBox().grow(16)).size())
                        .append("}");
            }
            if (t % 40 == 0) {
                trace.append(String.format(" [t%d d%.1f %s nav=%s]", t, p.getDistance(player), a, p.getNavigator().noPath() ? "none" : "path"));
            }
            if (!a.equals(last)) {
                trace.append(" t").append(t).append(":").append(a).append(p.getAttackTarget() == null ? "" : "->" + p.getAttackTarget().getClass().getSimpleName());
                last = a;
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST piglinfight {}: fight {} avoid {} arrows {} hits {} activities{}", kind, fought, avoided, arrows, hits, trace);
        if (hunt) {
            // 1.16: a piglin whose 30-120 s without hunting has run out goes after the nearest adult hoglin, even
            // with a player about; hit back by a herd that outnumbers it, it retreats. Vanilla's rule.
            c.ok(trace.indexOf("->EntityHoglin") >= 0, kind + ": a piglin free to hunt hunts the hoglin (" + trace + ")");
            p.setDead();
            for (net.minecraft.entity.EntityLiving b : beasts) {
                b.setDead();
            }
            w.removeEntity(player);
            return;
        }
        c.ok(fought > 200, kind + " piglin fights the player most of the time: " + fought + "/400 (" + trace + ")");
        c.eq(0, avoided, kind + " piglin never retreats from the player");
        if (crossbow) {
            c.ok(arrows >= 2, "the crossbow piglin shoots: " + arrows + " arrows");
        }
        if (!"moving".equals(scenario)) {
            c.ok(hits >= 2, kind + " piglin lands hits: " + hits + " (" + trace + ")");
        } else if (crossbow) {
            // A circling player outruns a sword piglin (it moves 0.25 a tick) and dodges some bolts.
            c.ok(hits >= 1, kind + " piglin lands hits: " + hits + " (" + trace + ")");
        }
        p.setDead();
        for (EntityPiglin o : others) {
            o.setDead();
        }
        for (net.minecraft.entity.EntityLiving b : beasts) {
            b.setDead();
        }
        w.removeEntity(player);
    }

    private static void repellentCheck(Check c, WorldServer w, BlockPos at) {
        arena(w, at);
        EntityPiglin p = piglin(w, at, false);
        net.minecraft.block.Block torch = Owners.ourBlock("soul_torch");
        if (torch == null) {
            return;
        }
        w.setBlockState(at.add(5, 0, 0), torch.getDefaultState());
        double start = p.getDistanceSq(at.add(5, 0, 0));
        for (int t = 0; t < 200; t++) {
            p.onUpdate();
        }
        double end = p.getDistanceSq(at.add(5, 0, 0));
        c.ok(end > start + 4, "a soul torch 5 blocks off sends an idle piglin away: from " + Math.sqrt(start) + " to " + Math.sqrt(end));
        p.setDead();
        w.setBlockToAir(at.add(5, 0, 0));

        // 1.16.5's piglin_repellents tag (soul fire, soul torch and wall torch, soul lantern) and a lit soul
        // campfire, found 8 blocks across and 4 up or down; nothing else.
        BlockPos probe = at.add(0, 1, 0);
        String[] repellents = {"soul_fire", "soul_torch", "soul_lantern", "soul_campfire"};
        // Solid blocks under and around the corner spot, so a torch of any facing stays put.
        for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.values()) {
            if (f != net.minecraft.util.EnumFacing.UP) {
                w.setBlockState(probe.add(8, 4, -8).offset(f), net.minecraft.init.Blocks.NETHERRACK.getDefaultState(), 2 | 16);
            }
        }
        for (String name : repellents) {
            net.minecraft.block.Block b = Owners.ourBlock(name);
            if (b == null) {
                continue;
            }
            for (net.minecraft.block.state.IBlockState s : b.getBlockState().getValidStates()) {
                boolean lit = true;
                for (net.minecraft.block.properties.IProperty<?> prop : s.getPropertyKeys()) {
                    if ("lit".equals(prop.getName())) {
                        lit = Boolean.TRUE.equals(s.getValue(prop));
                    }
                }
                w.setBlockState(probe.add(8, 4, -8), s, 2 | 16);
                c.eq(s, w.getBlockState(probe.add(8, 4, -8)), name + " stays where it is set");
                boolean found = probe.add(8, 4, -8).equals(com.exiledradio.netherupdatebackport.piglin.PiglinTasks.findNearestRepellent(w, probe));
                c.eq(lit, found, name + " " + s + " 8 across and 4 up repels: " + found);
            }
            w.setBlockToAir(probe.add(8, 4, -8));
            w.setBlockState(probe.add(9, 0, 0), b.getDefaultState(), 2 | 16);
            w.setBlockState(probe.add(0, 5, 0), b.getDefaultState(), 2 | 16);
            c.ok(com.exiledradio.netherupdatebackport.piglin.PiglinTasks.findNearestRepellent(w, probe) == null,
                    name + " 9 across or 5 up does not repel");
            w.setBlockToAir(probe.add(9, 0, 0));
            w.setBlockToAir(probe.add(0, 5, 0));
        }
        net.minecraft.block.state.IBlockState[] others = {net.minecraft.init.Blocks.TORCH.getDefaultState(),
                net.minecraft.init.Blocks.FIRE.getDefaultState(), net.minecraft.init.Blocks.LIT_PUMPKIN.getDefaultState()};
        for (net.minecraft.block.state.IBlockState s : others) {
            w.setBlockState(probe.add(2, 0, 0), s, 2 | 16);
            c.ok(com.exiledradio.netherupdatebackport.piglin.PiglinTasks.findNearestRepellent(w, probe) == null, s + " does not repel");
            w.setBlockToAir(probe.add(2, 0, 0));
        }
        if (Owners.ours("soul_torch") != null) {
            c.ok(com.exiledradio.netherupdatebackport.piglin.PiglinTasks.isRepellentItem(new ItemStack(Owners.ours("soul_torch"))),
                    "the soul torch item is a repellent item (piglins will not pick it up)");
        }
    }
}
