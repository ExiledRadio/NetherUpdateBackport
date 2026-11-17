package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.Content;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 1.16's fishing (20w12a, 20w13a): treasure comes only from open water, and lily pads are junk, not treasure.
 *
 * <p>1.16.5's {@code FishingBobberEntity} keeps an {@code openWater} flag: true while nothing bites, and while a
 * fish is on its way or nibbling it stays true only if the bobber has not been out of water for 10 ticks and
 * every layer from 1 below to 2 above the bobber, 5 by 5 around it, is all water sources without collision
 * ("inside") or all air and lily pads ("above"), with no inside layer over an above one. The treasure entry of
 * {@code gameplay/fishing} has the {@code in_open_water} condition. Here the flag is worked out each tick after
 * the hook's own tick, for every hook in a server world, and the condition reads it.
 */
public final class Fishing {

    static final ResourceLocation JUNK = new ResourceLocation("minecraft", "gameplay/fishing/junk");
    static final ResourceLocation TREASURE = new ResourceLocation("minecraft", "gameplay/fishing/treasure");

    private static final Field CATCHABLE = ReflectionHelper.findField(EntityFishHook.class, "ticksCatchable", "field_146045_ax");
    private static final Field CAUGHT_DELAY = ReflectionHelper.findField(EntityFishHook.class, "ticksCaughtDelay", "field_146040_ay");
    private static final Field STATE = ReflectionHelper.findField(EntityFishHook.class, "currentState", "field_190627_av");

    /** Server hooks and their 1.16 state. */
    private static final Map<EntityFishHook, Water> HOOKS = Collections.synchronizedMap(new WeakHashMap<EntityFishHook, Water>());

    static final class Water {
        boolean open = true;
        int outOfWater;
    }

    static boolean active() {
        return ModConfig.changes.enabled && ModConfig.changes.fishing;
    }

    static void init() {
        LootConditionManager.registerCondition(new InOpenWater.Serializer());
    }

    /** @return 1.16's {@code isOpenWaterFishing}: true for a hook this does not follow */
    public static boolean openWater(Entity hook) {
        Water w = hook instanceof EntityFishHook ? HOOKS.get(hook) : null;
        return w == null || w.open;
    }

    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityFishHook) {
            HOOKS.put((EntityFishHook) event.getEntity(), new Water());
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || HOOKS.isEmpty()) {
            return;
        }
        synchronized (HOOKS) {
            for (Iterator<Map.Entry<EntityFishHook, Water>> it = HOOKS.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<EntityFishHook, Water> e = it.next();
                EntityFishHook hook = e.getKey();
                if (hook == null || hook.isDead) {
                    it.remove();
                } else if (hook.world == event.world) {
                    update(hook, e.getValue());
                }
            }
        }
    }

    /** 1.16.5's {@code FishingBobberEntity.tick}, the part in the bobbing state. */
    static void update(EntityFishHook hook, Water w) {
        try {
            if (!"BOBBING".equals(((Enum<?>) STATE.get(hook)).name())) {
                return;
            }
            BlockPos pos = new BlockPos(hook);
            if (CATCHABLE.getInt(hook) <= 0 && CAUGHT_DELAY.getInt(hook) <= 0) {
                w.open = true;
            } else {
                w.open = w.open && w.outOfWater < 10 && calculateOpenWater(hook.world, pos);
            }
            IBlockState here = hook.world.getBlockState(pos);
            boolean inWater = here.getMaterial() == Material.WATER;
            w.outOfWater = inWater ? Math.max(0, w.outOfWater - 1) : Math.min(10, w.outOfWater + 1);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final int INVALID = 0;
    private static final int ABOVE = 1;
    private static final int INSIDE = 2;

    /** 1.16.5's {@code calculateOpenWater}. */
    static boolean calculateOpenWater(World world, BlockPos pos) {
        int type = INVALID;
        for (int dy = -1; dy <= 2; dy++) {
            int layer = layer(world, pos.add(-2, dy, -2), pos.add(2, dy, 2));
            if (layer == INVALID) {
                return false;
            }
            if (layer == ABOVE && type == INVALID) {
                return false;
            }
            if (layer == INSIDE && type == ABOVE) {
                return false;
            }
            type = layer;
        }
        return true;
    }

    private static int layer(World world, BlockPos from, BlockPos to) {
        int type = -1;
        for (BlockPos p : BlockPos.getAllInBoxMutable(from, to)) {
            int t = block(world, p);
            if (type == -1) {
                type = t;
            } else if (type != t) {
                return INVALID;
            }
        }
        return type == -1 ? INVALID : type;
    }

    /** 1.16.5's {@code getOpenWaterTypeForBlock}: air or lily pad above, a still water block without collision inside. */
    static int block(World world, BlockPos p) {
        IBlockState state = world.getBlockState(p);
        if (state.getBlock().isAir(state, world, p) || state.getBlock() == Blocks.WATERLILY) {
            return ABOVE;
        }
        if (state.getMaterial() != Material.WATER) {
            return INVALID;
        }
        boolean source = !(state.getBlock() instanceof BlockLiquid) || state.getValue(BlockLiquid.LEVEL) == 0;
        return source && state.getCollisionBoundingBox(world, p) == net.minecraft.block.Block.NULL_AABB ? INSIDE : INVALID;
    }

    // ---- loot tables -----------------------------------------------------------------------------------------

    /** Lily pads leave the treasure for the junk (weight 17), and treasure gets the open water condition. */
    @SubscribeEvent
    public void loot(LootTableLoadEvent event) {
        if (!active()) {
            return;
        }
        ResourceLocation name = event.getName();
        LootPool pool = event.getTable().getPool("main");
        if (pool == null) {
            return;
        }
        if (TREASURE.equals(name)) {
            pool.removeEntry("minecraft:waterlily");
        } else if (JUNK.equals(name)) {
            if (pool.getEntry("minecraft:lily_pad") == null && pool.getEntry("minecraft:waterlily") == null) {
                pool.addEntry(new LootEntryItem(Item.getItemFromBlock(Blocks.WATERLILY), 17, 0, new LootFunction[0],
                        new LootCondition[0], "minecraft:lily_pad"));
            }
        } else if (LootTableList.GAMEPLAY_FISHING.equals(name)) {
            LootEntry treasure = pool.removeEntry(TREASURE.toString());
            if (treasure != null) {
                int weight = treasure.getEffectiveWeight(0.0F);
                int quality = treasure.getEffectiveWeight(1.0F) - weight;
                pool.addEntry(new LootEntryTable(TREASURE, weight, quality, new LootCondition[] {new InOpenWater()}, TREASURE.toString()));
            }
        }
    }

    /**
     * {@code netherupdatebackport:in_open_water}: 1.16's {@code entity_properties} of "this" with
     * {@code fishing_hook.in_open_water}, true when the looted entity is a hook fishing in open water. With the
     * setting off it always holds.
     */
    public static final class InOpenWater implements LootCondition {

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            if (!active()) {
                return true;
            }
            Entity looted = context.getLootedEntity();
            return looted instanceof EntityFishHook && openWater(looted);
        }

        public static final class Serializer extends LootCondition.Serializer<InOpenWater> {
            public Serializer() {
                super(Content.id("in_open_water"), InOpenWater.class);
            }

            @Override
            public void serialize(JsonObject json, InOpenWater value, JsonSerializationContext context) {
            }

            @Override
            public InOpenWater deserialize(JsonObject json, JsonDeserializationContext context) {
                return new InOpenWater();
            }
        }
    }

    // ---- for the self-test -------------------------------------------------------------------------------------

    /** Follows a hook as if it had just joined a server world. */
    public static void follow(EntityFishHook hook) {
        HOOKS.put(hook, new Water());
    }

    /** Runs one tick of the open water rule for a followed hook. */
    public static void tickForTest(EntityFishHook hook) {
        Water w = HOOKS.get(hook);
        if (w != null) {
            update(hook, w);
        }
    }

    public static boolean openWaterAt(World world, BlockPos pos) {
        return calculateOpenWater(world, pos);
    }
}
