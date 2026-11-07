package com.exiledradio.netherupdatebackport.mobs.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.mobs.MobsContent;

import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/**
 * Client side of stream Z: vanilla's zombie pigman drawn as 1.16's zombified piglin. Only the renderer is
 * replaced (Forge's entity renderer registry puts it in place of vanilla's {@code RenderPigZombie}); the entity
 * is untouched.
 */
public final class MobsClient {

    static final ResourceLocation TEXTURE = new ResourceLocation(NetherUpdateBackport.MOD_ID,
            "textures/entity/piglin/zombified_piglin.png");

    private MobsClient() {
    }

    public static void init() {
        if (!MobsContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityPigZombie.class, RenderZombifiedPiglin::new);
    }

    /**
     * 1.16.5's {@code PiglinRenderer(manager, true)}: shadow 0.5, the biped layers (head item, elytra, held item)
     * and armor whose arms follow the zombie pose, as its {@code BipedArmorLayer} copies the body's pose.
     */
    static final class RenderZombifiedPiglin extends RenderBiped<EntityPigZombie> {

        RenderZombifiedPiglin(RenderManager manager) {
            super(manager, new ModelZombifiedPiglin(), 0.5F);
            this.addLayer(new LayerBipedArmor(this) {
                @Override
                protected void initArmor() {
                    this.modelLeggings = new ModelZombie(0.5F, true);
                    this.modelArmor = new ModelZombie(1.02F, true);
                }
            });
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityPigZombie entity) {
            return TEXTURE;
        }
    }
}
