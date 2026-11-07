package com.exiledradio.netherupdatebackport.hoglin.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.hoglin.EntityZoglin;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client side of stream H: renderers for hoglins (shaking while they turn into zoglins) and zoglins. */
public final class HoglinClient {

    static final ResourceLocation HOGLIN = new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/entity/hoglin/hoglin.png");
    static final ResourceLocation ZOGLIN = new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/entity/hoglin/zoglin.png");

    private HoglinClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityHoglin.class, m -> new Boar<EntityHoglin>(m, HOGLIN));
        RenderingRegistry.registerEntityRenderingHandler(EntityZoglin.class, m -> new Boar<EntityZoglin>(m, ZOGLIN));
    }

    static final class Boar<T extends EntityLiving> extends RenderLiving<T> {
        private final ResourceLocation texture;

        Boar(RenderManager manager, ResourceLocation texture) {
            super(manager, new ModelBoar(), 0.7F);
            this.texture = texture;
        }

        @Override
        protected ResourceLocation getEntityTexture(T entity) {
            return this.texture;
        }

        /** 1.16's {@code LivingRenderer.isShaking}: a converting hoglin trembles side to side. */
        @Override
        protected void applyRotations(T entity, float ageInTicks, float rotationYaw, float partialTicks) {
            if (entity instanceof EntityHoglin && ((EntityHoglin) entity).isConverting()) {
                rotationYaw += (float) (Math.cos(entity.ticksExisted * 3.25) * Math.PI * 0.4);
            }
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
        }
    }
}
