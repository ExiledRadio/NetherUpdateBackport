package com.exiledradio.netherupdatebackport.piglin.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.piglin.AbstractPiglin;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglinBrute;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/**
 * Client side of stream P: 1.16.5's {@code PiglinRenderer} for piglins and brutes - the piglin model, armor at
 * 1.16's sizes (0.5 and 1.02), held items, and the shaking of a piglin turning into a zombified piglin.
 */
public final class PiglinClient {

    static final ResourceLocation PIGLIN = new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/entity/piglin/piglin.png");
    static final ResourceLocation BRUTE = new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/entity/piglin/piglin_brute.png");

    private PiglinClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityPiglin.class, m -> new Render<EntityPiglin>(m, PIGLIN));
        RenderingRegistry.registerEntityRenderingHandler(EntityPiglinBrute.class, m -> new Render<EntityPiglinBrute>(m, BRUTE));
    }

    static final class Render<T extends AbstractPiglin> extends RenderBiped<T> {
        private final ResourceLocation texture;

        Render(RenderManager manager, ResourceLocation texture) {
            super(manager, new ModelPiglin(0.0F), 0.5F);
            this.texture = texture;
            addLayer(new LayerBipedArmor(this) {
                @Override
                protected void initArmor() {
                    this.modelLeggings = new ModelBiped(0.5F);
                    this.modelArmor = new ModelBiped(1.02F);
                }
            });
        }

        @Override
        protected ResourceLocation getEntityTexture(T entity) {
            return this.texture;
        }

        /** 1.16's {@code isShaking}: a converting piglin trembles. */
        @Override
        protected void applyRotations(T entity, float ageInTicks, float rotationYaw, float partialTicks) {
            if (entity.isConverting()) {
                rotationYaw += (float) (Math.cos(entity.ticksExisted * 3.25) * Math.PI * 0.4);
            }
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
        }
    }
}
