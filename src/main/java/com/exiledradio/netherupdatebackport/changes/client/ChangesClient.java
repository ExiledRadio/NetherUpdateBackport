package com.exiledradio.netherupdatebackport.changes.client;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.changes.LavaVision;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GLContext;

/**
 * Client side of stream C: the lava fog of {@link LavaVision}.
 *
 * <p>1.12.2's {@code EntityRenderer.setupFog} asks Forge's fog density event before anything else; for a
 * camera in lava this sets linear fog itself and cancels the event, so vanilla's exponential lava fog is
 * skipped. As in 1.16 and later, lava's fog comes before Blindness's.
 */
public final class ChangesClient {

    private ChangesClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ChangesClient());
    }

    @SubscribeEvent
    public void fog(EntityViewRenderEvent.FogDensity event) {
        if (!ModConfig.changes.enabled || !ModConfig.changes.lavaFog || event.getState().getMaterial() != Material.LAVA) {
            return;
        }
        Entity entity = event.getEntity();
        boolean spectator = entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator();
        boolean fireResistant = entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(MobEffects.FIRE_RESISTANCE);
        float renderDistance = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16;
        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        GlStateManager.setFogStart(LavaVision.start(spectator, fireResistant, renderDistance));
        GlStateManager.setFogEnd(LavaVision.end(spectator, fireResistant, renderDistance));
        if (GLContext.getCapabilities().GL_NV_fog_distance) {
            // Radial fog, as vanilla's own linear fogs ask for.
            GlStateManager.glFogi(34138, 34139);
        }
        // Cancelled, the event's density is applied too; linear fog ignores it.
        event.setCanceled(true);
    }
}
