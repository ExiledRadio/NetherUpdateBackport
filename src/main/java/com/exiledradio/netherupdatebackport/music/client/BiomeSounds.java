package com.exiledradio.netherupdatebackport.music.client;

import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.music.NetherAmbience;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * 1.16.5's {@code BiomeSoundHandler} for the five nether biomes: the biome's sound loop fading in and out over 2 s,
 * its additions at 1.11% a tick, and its mood sound after long darkness. One per client player, as in 1.16 (a
 * respawn or a new dimension makes a new player, and a new handler).
 *
 * <p>Other biomes have no 1.16 ambience but the cave mood, which 1.12.2 plays its own way and is left alone.
 */
final class BiomeSounds {

    private final EntityPlayer player;
    private final SoundHandler sounds;
    private final Random random;
    private final Map<NetherBiomes.Kind, Loop> loops = new EnumMap<NetherBiomes.Kind, Loop>(NetherBiomes.Kind.class);
    @Nullable
    private NetherAmbience.Settings settings;
    @Nullable
    private NetherBiomes.Kind previous;
    private float moodiness;

    BiomeSounds(EntityPlayer player, SoundHandler sounds) {
        this.player = player;
        this.sounds = sounds;
        this.random = player.world.rand;
    }

    EntityPlayer player() {
        return this.player;
    }

    /** @return the ambience playing now, or null outside the nether biomes */
    @Nullable
    NetherAmbience.Settings settings() {
        return this.settings;
    }

    /** One player tick. With {@code on} false every loop fades out and nothing new plays. */
    void tick(boolean on) {
        for (Iterator<Loop> it = this.loops.values().iterator(); it.hasNext(); ) {
            if (it.next().isDonePlaying()) {
                it.remove();
            }
        }
        World world = this.player.world;
        NetherBiomes.Kind kind = on ? NetherBiomes.at(world, new BlockPos(this.player.posX, this.player.posY, this.player.posZ)) : null;
        if (kind != this.previous) {
            this.previous = kind;
            this.settings = NetherAmbience.of(kind);
            for (Loop loop : this.loops.values()) {
                loop.fadeOut();
            }
            if (this.settings != null) {
                Loop loop = this.loops.get(kind);
                if (loop == null) {
                    loop = new Loop(this.settings.loop, this.player);
                    this.loops.put(kind, loop);
                    this.sounds.playSound(loop);
                }
                loop.fadeIn();
            }
        }
        NetherAmbience.Settings s = this.settings;
        if (s == null) {
            return;
        }
        if (this.random.nextDouble() < s.additionsChance) {
            // 1.16's forAmbientAddition: relative to the listener, no attenuation.
            this.sounds.playSound(new PositionedSoundRecord(s.additions.getSoundName(), SoundCategory.AMBIENT, 1.0F, 1.0F,
                    false, 0, ISound.AttenuationType.NONE, (float) this.player.posX, (float) eyeY(), (float) this.player.posZ));
        }
        int extent = s.moodSearchExtent;
        int span = extent * 2 + 1;
        BlockPos pos = new BlockPos(this.player.posX + (double) this.random.nextInt(span) - (double) extent,
                eyeY() + (double) this.random.nextInt(span) - (double) extent,
                this.player.posZ + (double) this.random.nextInt(span) - (double) extent);
        this.moodiness = NetherAmbience.mood(this.moodiness, world.getLightFor(EnumSkyBlock.SKY, pos),
                world.getLightFor(EnumSkyBlock.BLOCK, pos), s.moodTickDelay);
        if (this.moodiness >= 1.0F) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D;
            double dx = x - this.player.posX;
            double dy = y - eyeY();
            double dz = z - this.player.posZ;
            double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            double far = distance + s.moodOffset;
            // 1.16's forAmbientMood: linear attenuation, beyond the dark spot as seen from the player.
            this.sounds.playSound(new PositionedSoundRecord(s.mood.getSoundName(), SoundCategory.AMBIENT, 1.0F, 1.0F, false, 0,
                    ISound.AttenuationType.LINEAR, (float) (this.player.posX + dx / distance * far),
                    (float) (eyeY() + dy / distance * far), (float) (this.player.posZ + dz / distance * far)));
            this.moodiness = 0.0F;
        } else {
            this.moodiness = Math.max(this.moodiness, 0.0F);
        }
    }

    /** Fades every loop out, for a handler being replaced (1.16 drops it with its player). */
    void retire() {
        for (Loop loop : this.loops.values()) {
            loop.fadeOut();
        }
        this.settings = null;
    }

    private double eyeY() {
        return this.player.posY + (double) this.player.getEyeHeight();
    }

    /** 1.16's {@code BiomeSoundHandler.Sound}: a looping ambient sound that follows the listener and fades over 40 ticks. */
    static final class Loop extends MovingSound {
        private final EntityPlayer listener;
        private int fadeDirection;
        private int fade;

        Loop(SoundEvent event, EntityPlayer listener) {
            super(event, SoundCategory.AMBIENT);
            this.listener = listener;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 1.0F;
            // 1.16 plays it relative to the listener; 1.12.2 has no relative sounds, so it follows the player.
            this.attenuationType = ISound.AttenuationType.NONE;
            follow();
        }

        @Override
        public void update() {
            if (this.fade < 0) {
                this.donePlaying = true;
            }
            this.fade += this.fadeDirection;
            this.volume = MathHelper.clamp((float) this.fade / 40.0F, 0.0F, 1.0F);
            follow();
        }

        void fadeOut() {
            this.fade = Math.min(this.fade, 40);
            this.fadeDirection = -1;
        }

        void fadeIn() {
            this.fade = Math.max(0, this.fade);
            this.fadeDirection = 1;
        }

        private void follow() {
            this.xPosF = (float) this.listener.posX;
            this.yPosF = (float) (this.listener.posY + (double) this.listener.getEyeHeight());
            this.zPosF = (float) this.listener.posZ;
        }
    }
}
