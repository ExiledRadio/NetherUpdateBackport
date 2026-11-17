package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * 1.16.2's spawn costs (20w28a): soul sand valleys and warped forests give some of their mobs a charge, and a natural
 * spawn is refused when the potential energy it would add to the charges already around exceeds its budget.
 *
 * <ul>
 *   <li>Soul sand valley: skeletons, ghasts, endermen and striders, charge 0.7, budget 0.15.</li>
 *   <li>Warped forest: endermen, charge 1.0, budget 0.12.</li>
 * </ul>
 * As 1.16.5's {@code WorldEntitySpawner}: once per spawn pass the charges of every loaded mob that may despawn (and is
 * riding nothing), each by the biome it stands in; each natural spawn attempt sums {@code charge / distance} over
 * them, times its own charge; a spawned mob adds its charge for the rest of the pass. Spawners, eggs and commands
 * pay nothing, as in 1.16.
 */
public final class SpawnCosts {

    /** One mob's cost in one biome. */
    static final class Cost {
        final double charge;
        final double budget;

        Cost(double charge, double budget) {
            this.charge = charge;
            this.budget = budget;
        }
    }

    private static final ResourceLocation SOUL_SAND_VALLEY = Content.id("soul_sand_valley");
    private static final ResourceLocation WARPED_FOREST = Content.id("warped_forest");
    private static final ResourceLocation STRIDER = Content.id("strider");

    /** Per world: the pass the charges were gathered for, and the charges. */
    private final WeakHashMap<World, Pass> passes = new WeakHashMap<World, Pass>();

    @Nullable
    private Entity lastAllowed;
    private double lastCharge;
    @Nullable
    private BlockPos lastPos;

    static final class Pass {
        long tick = -1L;
        final List<BlockPos> positions = new ArrayList<BlockPos>();
        final List<Double> charges = new ArrayList<Double>();

        void add(BlockPos pos, double charge) {
            if (charge != 0.0D) {
                this.positions.add(pos);
                this.charges.add(charge);
            }
        }

        /** {@code MobDensityTracker.getPotentialEnergyChange}. */
        double energyChange(BlockPos pos, double charge) {
            if (charge == 0.0D) {
                return 0.0D;
            }
            double sum = 0.0D;
            for (int i = 0; i < this.positions.size(); i++) {
                double d = this.positions.get(i).distanceSq(pos);
                sum += d == 0.0D ? Double.POSITIVE_INFINITY : this.charges.get(i) / Math.sqrt(d);
            }
            return sum * charge;
        }
    }

    static boolean active() {
        return ModConfig.changes.enabled && ModConfig.changes.spawnCosts;
    }

    /** @return the cost of this mob at this position (by the biome there), or null */
    @Nullable
    static Cost cost(Entity entity, World world, BlockPos pos) {
        Class<?> c = entity.getClass();
        boolean skeleton = c == EntitySkeleton.class;
        boolean ghast = c == EntityGhast.class;
        boolean enderman = c == EntityEnderman.class;
        boolean strider = !skeleton && !ghast && !enderman && STRIDER.equals(EntityList.getKey(entity));
        if (!skeleton && !ghast && !enderman && !strider) {
            return null;
        }
        Biome biome = world.getBiome(pos);
        ResourceLocation id = biome.getRegistryName();
        if (SOUL_SAND_VALLEY.equals(id)) {
            return new Cost(0.7D, 0.15D);
        }
        if (WARPED_FOREST.equals(id) && enderman) {
            return new Cost(1.0D, 0.12D);
        }
        return null;
    }

    /** {@code WorldEntitySpawner.createState}: the charges of the mobs already in the world, once per tick. */
    private Pass pass(World world) {
        Pass pass = this.passes.get(world);
        long now = world.getTotalWorldTime();
        if (pass != null && pass.tick == now) {
            return pass;
        }
        pass = new Pass();
        pass.tick = now;
        for (Entity e : new ArrayList<Entity>(world.loadedEntityList)) {
            if (!(e instanceof EntityLiving) || e.isDead) {
                continue;
            }
            EntityLiving mob = (EntityLiving) e;
            if (mob.isNoDespawnRequired() || mob.isRiding()) {
                continue;
            }
            BlockPos pos = new BlockPos(mob);
            Cost cost = cost(mob, world, pos);
            if (cost != null) {
                pass.add(pos, cost.charge);
            }
        }
        this.passes.put(world, pass);
        return pass;
    }

    /** {@code EntityDensityManager.canSpawn}, for natural spawns only. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        World world = event.getWorld();
        if (world.isRemote || event.isSpawner() || event.getResult() == Event.Result.DENY || !active()) {
            return;
        }
        EntityLiving mob = event.getEntityLiving() instanceof EntityLiving ? (EntityLiving) event.getEntityLiving() : null;
        if (mob == null) {
            return;
        }
        BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
        Cost cost = cost(mob, world, pos);
        if (cost == null) {
            return;
        }
        Pass pass = pass(world);
        if (pass.energyChange(pos, cost.charge) > cost.budget) {
            event.setResult(Event.Result.DENY);
            return;
        }
        this.lastAllowed = mob;
        this.lastCharge = cost.charge;
        this.lastPos = pos;
    }

    /** {@code EntityDensityManager.afterSpawn}: the mob just let through adds its charge. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void joined(EntityJoinWorldEvent event) {
        if (event.getEntity() != this.lastAllowed || event.isCanceled()) {
            return;
        }
        this.lastAllowed = null;
        Pass pass = this.passes.get(event.getWorld());
        if (pass != null && this.lastPos != null) {
            pass.add(new BlockPos(event.getEntity()), this.lastCharge);
        }
    }

    /** For the self-test: forget this tick's charges, so the next check gathers them again. */
    public void forget(World world) {
        this.passes.remove(world);
    }

    /** For the self-test: the potential change a spawn of this charge would add at this position, this tick. */
    public double energyChange(World world, BlockPos pos, double charge) {
        return pass(world).energyChange(pos, charge);
    }
}
