package com.exiledradio.netherupdatebackport.mobs;

import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * A zombified piglin's anger: 1.16's {@code IAngerable} state plus {@code ZombifiedPiglinEntity}'s two timers,
 * kept as a capability on vanilla's {@code EntityPigZombie} and saved with it (as 1.16's {@code AngerTime} and
 * {@code AngryAt}).
 *
 * <p>The rules that change it are in {@link ZombifiedPiglins}; this class only holds and saves the numbers.
 */
public final class Anger {

    static final ResourceLocation KEY = Content.id("anger");

    @CapabilityInject(Anger.class)
    static Capability<Anger> CAP = null;

    /** 1.16's {@code remainingPersistentAngerTime}, in ticks; angry while above zero. */
    int remaining;
    /** 1.16's {@code persistentAngerTarget}: the one entity it is angry at, by id. */
    @Nullable
    UUID target;
    /** Ticks until the first angry grunt after taking a target ({@code playFirstAngerSoundIn}). */
    int firstSoundIn;
    /** Ticks until it next raises the alarm ({@code ticksUntilNextAlert}). */
    int alertIn;

    /** Not saved: the attack target as last set, for 1.16's "target was null" test in {@code setTarget}. */
    @Nullable
    EntityLivingBase knownTarget;
    /** Not saved: 1.16's goals are installed in this entity object. */
    boolean installed;

    public boolean isAngry() {
        return this.remaining > 0;
    }

    /** 1.16's {@code isAngryAt}, with {@code universalAnger} off (1.12.2 has no such game rule). */
    public boolean isAngryAt(@Nullable EntityLivingBase entity) {
        return entity != null && EntitySelectors.CAN_AI_TARGET.apply(entity) && entity.getUniqueID().equals(this.target);
    }

    public int remaining() {
        return this.remaining;
    }

    @Nullable
    public UUID target() {
        return this.target;
    }

    @Nullable
    public static Anger of(Entity entity) {
        return CAP == null ? null : entity.getCapability(CAP, null);
    }

    NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("AngerTime", this.remaining);
        if (this.target != null) {
            tag.setUniqueId("AngryAt", this.target);
        }
        return tag;
    }

    void read(NBTTagCompound tag) {
        this.remaining = tag.getInteger("AngerTime");
        this.target = tag.hasUniqueId("AngryAt") ? tag.getUniqueId("AngryAt") : null;
    }

    static void register() {
        CapabilityManager.INSTANCE.register(Anger.class, new Capability.IStorage<Anger>() {
            @Override
            public NBTBase writeNBT(Capability<Anger> capability, Anger instance, EnumFacing side) {
                return instance.write();
            }

            @Override
            public void readNBT(Capability<Anger> capability, Anger instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.read((NBTTagCompound) nbt);
                }
            }
        }, new Callable<Anger>() {
            @Override
            public Anger call() {
                return new Anger();
            }
        });
    }

    /** Attaches one {@link Anger} to one pigman and saves it. */
    static final class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final Anger anger = new Anger();

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAP;
        }

        @Override
        @Nullable
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CAP ? CAP.<T>cast(this.anger) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return this.anger.write();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            this.anger.read(nbt);
        }
    }
}
