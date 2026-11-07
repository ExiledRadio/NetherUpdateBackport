package com.exiledradio.netherupdatebackport.strider.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.strider.EntityStrider;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/**
 * Client side of stream R: 1.16.5's {@code StriderRenderer}: the warm or cold texture, shaking while cold, half size
 * for a baby, and the saddle drawn as a second model over it.
 */
public final class StriderClient {

    private static final ResourceLocation WARM = tex("strider");
    private static final ResourceLocation COLD = tex("strider_cold");
    private static final ResourceLocation SADDLE = tex("strider_saddle");

    private StriderClient() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/entity/strider/" + name + ".png");
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityStrider.class, Render::new);
    }

    static final class Render extends RenderLiving<EntityStrider> {
        Render(RenderManager manager) {
            super(manager, new ModelStrider(), 0.5F);
            addLayer(new Saddle(this));
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityStrider strider) {
            return strider.isCold() ? COLD : WARM;
        }

        @Override
        protected void preRenderCallback(EntityStrider strider, float partialTicks) {
            if (strider.isChild()) {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                this.shadowSize = 0.25F;
            } else {
                this.shadowSize = 0.5F;
            }
        }

        @Override
        protected void applyRotations(EntityStrider strider, float ageInTicks, float rotationYaw, float partialTicks) {
            if (strider.isCold()) {
                rotationYaw += (float) (Math.cos(strider.ticksExisted * 3.25) * Math.PI * 0.4);
            }
            super.applyRotations(strider, ageInTicks, rotationYaw, partialTicks);
        }
    }

    /** 1.16's {@code SaddleLayer}: the same model drawn again over it with the saddle texture. */
    static final class Saddle implements LayerRenderer<EntityStrider> {
        private final Render renderer;
        private final ModelStrider model = new ModelStrider();

        Saddle(Render renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityStrider strider, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (!strider.isSaddled()) {
                return;
            }
            this.renderer.bindTexture(SADDLE);
            this.model.setModelAttributes(this.renderer.getMainModel());
            this.model.render(strider, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
