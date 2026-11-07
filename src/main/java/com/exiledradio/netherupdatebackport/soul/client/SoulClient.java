package com.exiledradio.netherupdatebackport.soul.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client side of stream L: 1.16's soul fire flame and soul particles, drawn from the block atlas with 1.18's
 * textures (1.12.2 has no particle registry to add them to).
 */
public final class SoulClient {

    private static TextureAtlasSprite flame;
    private static final TextureAtlasSprite[] SOUL = new TextureAtlasSprite[11];

    private SoulClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new SoulClient());
    }

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        flame = event.getMap().registerSprite(new ResourceLocation(NetherUpdateBackport.MOD_ID, "particle/soul_fire_flame"));
        for (int i = 0; i < SOUL.length; i++) {
            SOUL[i] = event.getMap().registerSprite(new ResourceLocation(NetherUpdateBackport.MOD_ID, "particle/soul_" + i));
        }
    }

    /** A soul fire flame, as vanilla's torch flame moves. */
    public static void soulFlame(World world, double x, double y, double z, double vx, double vy, double vz) {
        if (flame != null) {
            Minecraft.getMinecraft().effectRenderer.addEffect(new Flame(world, x, y, z, vx, vy, vz));
        }
    }

    /** A soul rising from soul blocks under Soul Speed boots or from soul fire. */
    public static void soul(World world, double x, double y, double z, double vx, double vy, double vz) {
        if (SOUL[0] != null) {
            Minecraft.getMinecraft().effectRenderer.addEffect(new Soul(world, x, y, z, vx, vy, vz));
        }
    }

    private static final class Flame extends ParticleFlame {
        Flame(World world, double x, double y, double z, double vx, double vy, double vz) {
            super(world, x, y, z, vx, vy, vz);
            setParticleTexture(flame);
        }

        @Override
        public int getFXLayer() {
            return 1;
        }
    }

    /** 1.16's {@code SoulParticle}: rises slowly, its eleven frames played over its life. */
    private static final class Soul extends ParticleFlame {
        Soul(World world, double x, double y, double z, double vx, double vy, double vz) {
            super(world, x, y, z, vx, vy, vz);
            this.particleScale *= 1.5F;
            setParticleTexture(SOUL[0]);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            int frame = Math.min(SOUL.length - 1, this.particleAge * SOUL.length / Math.max(1, this.particleMaxAge));
            setParticleTexture(SOUL[frame]);
        }

        @Override
        public int getFXLayer() {
            return 1;
        }
    }
}
