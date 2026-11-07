package com.exiledradio.netherupdatebackport.piglin;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A piglin's memories: 1.16's {@code Brain} memory map, without the brain.
 *
 * <p>Each memory is a value with an optional time to live in ticks, counted down once per server tick
 * ({@link #tick()}, 1.16's {@code forgetOutdatedMemories}) and erased when it reaches zero. An entity held in a
 * memory is forgotten when it dies or is removed, as 1.16's memories of living entities effectively are once its
 * sensors refresh.
 */
public final class Memories {

    /** 1.16's memory module types this port uses. */
    public enum Mem {
        // Kept across a save, as 1.16's brain codec keeps them.
        ANGRY_AT(true), UNIVERSAL_ANGER(true), ADMIRING_DISABLED(true), HUNTED_RECENTLY(true), HOME(true),
        // Behavior state.
        AVOID_TARGET, ADMIRING_ITEM, DISABLE_WALK_TO_ADMIRE_ITEM, TIME_TRYING_TO_REACH_ADMIRE_ITEM, CELEBRATE_LOCATION,
        DANCING, ATE_RECENTLY, RIDE_TARGET, ATTACK_COOLING_DOWN, CANT_REACH_WALK_TARGET_SINCE, INTERACTION_TARGET,
        // Written by the sensors.
        NEAREST_VISIBLE_PLAYER, NEAREST_VISIBLE_TARGETABLE_PLAYER, NEAREST_VISIBLE_WANTED_ITEM, NEAREST_VISIBLE_NEMESIS,
        NEAREST_VISIBLE_HUNTABLE_HOGLIN, NEAREST_VISIBLE_BABY_HOGLIN, NEAREST_VISIBLE_ZOMBIFIED,
        NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, NEAREST_PLAYER_HOLDING_WANTED_ITEM, NEAREST_REPELLENT,
        NEAREST_VISIBLE_ADULT_PIGLINS, NEARBY_ADULT_PIGLINS, VISIBLE_ADULT_PIGLIN_COUNT, VISIBLE_ADULT_HOGLIN_COUNT,
        VISIBLE_LIVING_ENTITIES, HURT_BY;

        final boolean saved;

        Mem() {
            this(false);
        }

        Mem(boolean saved) {
            this.saved = saved;
        }
    }

    private static final long FOREVER = Long.MAX_VALUE;

    private final Map<Mem, Object> values = new EnumMap<Mem, Object>(Mem.class);
    private final Map<Mem, Long> ttl = new EnumMap<Mem, Long>(Mem.class);

    /** Sets a memory with no expiry; a null value erases it. */
    public void set(Mem key, @Nullable Object value) {
        setFor(key, value, FOREVER);
    }

    /** Sets a memory that is forgotten after this many ticks; a null value erases it. */
    public void setFor(Mem key, @Nullable Object value, long ticks) {
        if (value == null) {
            erase(key);
        } else {
            this.values.put(key, value);
            this.ttl.put(key, ticks);
        }
    }

    public void erase(Mem key) {
        this.values.remove(key);
        this.ttl.remove(key);
    }

    public boolean has(Mem key) {
        Object v = this.values.get(key);
        if (v instanceof Entity && !((Entity) v).isEntityAlive()) {
            erase(key);
            return false;
        }
        return v != null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(Mem key) {
        return has(key) ? (T) this.values.get(key) : null;
    }

    /** @return the ticks left before a memory is forgotten, or -1 when it never is (or is absent) */
    public long timeLeft(Mem key) {
        Long t = this.ttl.get(key);
        return t == null || t == FOREVER ? -1 : t;
    }

    /** 1.16's {@code isMemoryValue}. */
    public boolean is(Mem key, Object value) {
        return has(key) && this.values.get(key).equals(value);
    }

    /** Counts every expiring memory down by one tick and forgets those that ran out. */
    public void tick() {
        for (Mem key : Mem.values()) {
            Long t = this.ttl.get(key);
            if (t != null && t != FOREVER) {
                if (t <= 1) {
                    erase(key);
                } else {
                    this.ttl.put(key, t - 1);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Saving: the memories 1.16 saves (anger, its universal flag, admiring disabled, hunted recently, home)

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        for (Mem key : Mem.values()) {
            if (!key.saved || !has(key)) {
                continue;
            }
            NBTTagCompound m = new NBTTagCompound();
            Object v = this.values.get(key);
            if (v instanceof UUID) {
                m.setUniqueId("value", (UUID) v);
            } else if (v instanceof BlockPos) {
                m.setLong("pos", ((BlockPos) v).toLong());
            } else if (v instanceof Boolean) {
                m.setBoolean("flag", (Boolean) v);
            } else {
                continue;
            }
            long left = timeLeft(key);
            if (left >= 0) {
                m.setLong("ttl", left);
            }
            tag.setTag(key.name().toLowerCase(java.util.Locale.ROOT), m);
        }
        return tag;
    }

    public void read(NBTTagCompound tag) {
        for (Mem key : Mem.values()) {
            String name = key.name().toLowerCase(java.util.Locale.ROOT);
            if (!key.saved || !tag.hasKey(name, 10)) {
                continue;
            }
            NBTTagCompound m = tag.getCompoundTag(name);
            Object v = m.hasUniqueId("value") ? m.getUniqueId("value")
                    : m.hasKey("pos") ? BlockPos.fromLong(m.getLong("pos"))
                    : m.hasKey("flag") ? (Object) m.getBoolean("flag") : null;
            setFor(key, v, m.hasKey("ttl") ? m.getLong("ttl") : FOREVER);
        }
    }
}
