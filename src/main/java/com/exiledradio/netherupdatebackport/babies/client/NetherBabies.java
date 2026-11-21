package com.exiledradio.netherupdatebackport.babies.client;

import com.exiledradio.netherupdatebackport.babies.client.model.BabyHoglinModel;
import com.exiledradio.netherupdatebackport.babies.client.model.BabyHumanoidModel;
import com.exiledradio.netherupdatebackport.babies.client.model.BabyStriderModel;
import com.exiledradio.netherupdatebackport.babies.client.render.BabyArmorLayer;
import com.exiledradio.netherupdatebackport.babies.client.render.BabyHeadLayer;
import com.exiledradio.netherupdatebackport.babies.client.render.BabyHeldItemLayer;
import com.exiledradio.netherupdatebackport.babies.client.render.BabyRender;
import com.exiledradio.netherupdatebackport.babies.client.render.BabyWrapper;
import com.exiledradio.netherupdatebackport.piglin.AbstractPiglin;
import com.exiledradio.netherupdatebackport.strider.EntityStrider;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.util.ResourceLocation;

/**
 * 26.1's babies for this mod's mobs (Game Drops Backport's request 1): each 1.16 renderer is wrapped by a
 * {@link BabyWrapper} that sends babies to a 26.3 baby renderer while {@link BabyModels#active}. Shadows are 26.3's
 * adult radii, which 1.12 halves for children as 26.3 does ({@code MobRenderer}'s age scale).
 */
public final class NetherBabies {

    /** {@code PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS}. */
    private static final float PIGLIN_HEAD_SCALE = 1.0019531F;

    private NetherBabies() {
    }

    private static <T> BabyRender.Texture<T> fixed(final ResourceLocation texture) {
        return new BabyRender.Texture<T>() {
            @Override
            public ResourceLocation get(T entity) {
                return texture;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends EntityLiving> Render<T> wrap(Render<T> adult, BabyRender<T> baby) {
        return new BabyWrapper(adult, baby);
    }

    /** {@code HoglinRenderer} / {@code ZoglinRenderer}: {@code hoglin_baby} or {@code zoglin_baby}, shadow 0.7. */
    public static <T extends EntityLiving> Render<T> hoglin(RenderManager manager, Render<T> adult, String texture) {
        BabyRender<T> baby = new BabyRender<T>(manager, new BabyHoglinModel(), 0.7F,
                NetherBabies.<T>fixed(BabyRender.tex("hoglin/" + texture + "_baby"))) {
            @Override
            protected void applyRotations(T entity, float ageInTicks, float rotationYaw, float partialTicks) {
                // HoglinRenderer.isShaking: a converting hoglin.
                if (entity instanceof com.exiledradio.netherupdatebackport.hoglin.EntityHoglin
                        && ((com.exiledradio.netherupdatebackport.hoglin.EntityHoglin) entity).isConverting()) {
                    rotationYaw += (float) (Math.cos(entity.ticksExisted * 3.25) * Math.PI * 0.4);
                }
                super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
            }
        };
        return wrap(adult, baby);
    }

    /** {@code StriderRenderer}: {@code strider_baby}, {@code strider_cold_baby} while cold; a further halved shadow. */
    public static Render<EntityStrider> strider(RenderManager manager, Render<EntityStrider> adult) {
        final ResourceLocation warm = BabyRender.tex("strider/strider_baby");
        final ResourceLocation cold = BabyRender.tex("strider/strider_cold_baby");
        // Babies take no saddle, so the saddle layer has nothing to draw.
        BabyRender<EntityStrider> baby = new BabyRender<EntityStrider>(manager, new BabyStriderModel(), 0.25F,
                new BabyRender.Texture<EntityStrider>() {
                    @Override
                    public ResourceLocation get(EntityStrider strider) {
                        return strider.isCold() ? cold : warm;
                    }
                }) {
            @Override
            protected void applyRotations(EntityStrider strider, float ageInTicks, float rotationYaw, float partialTicks) {
                if (strider.isCold()) {
                    rotationYaw += (float) (Math.cos(strider.ticksExisted * 3.25) * Math.PI * 0.4);
                }
                super.applyRotations(strider, ageInTicks, rotationYaw, partialTicks);
            }
        };
        return wrap(adult, baby);
    }

    /** {@code PiglinRenderer} for a baby piglin: {@code piglin_baby}, armor, head item, held items, shadow 0.5. */
    public static <T extends AbstractPiglin> Render<T> piglin(RenderManager manager, Render<T> adult) {
        BabyHumanoidModel model = BabyHumanoidModel.create(BabyHumanoidModel.Kind.PIGLIN);
        BabyRender<T> baby = new BabyRender<T>(manager, model, 0.5F, NetherBabies.<T>fixed(BabyRender.tex("piglin/piglin_baby"))) {
            @Override
            public boolean handles(T entity) {
                return BabyArmorLayer.isSupported(entity);
            }

            @Override
            protected void applyRotations(T entity, float ageInTicks, float rotationYaw, float partialTicks) {
                if (entity.isConverting()) {
                    rotationYaw += (float) (Math.cos(entity.ticksExisted * 3.25) * Math.PI * 0.4);
                }
                super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
            }
        };
        humanoidLayers(baby, model, BabyHumanoidModel.Kind.PIGLIN);
        return wrap(adult, baby);
    }

    /** {@code ZombifiedPiglinRenderer} for a baby: {@code zombified_piglin_baby}, armor, head item, held items. */
    public static Render<EntityPigZombie> zombifiedPiglin(RenderManager manager, Render<EntityPigZombie> adult) {
        BabyHumanoidModel model = BabyHumanoidModel.create(BabyHumanoidModel.Kind.ZOMBIFIED_PIGLIN);
        BabyRender<EntityPigZombie> baby = new BabyRender<EntityPigZombie>(manager, model, 0.5F,
                NetherBabies.<EntityPigZombie>fixed(BabyRender.tex("piglin/zombified_piglin_baby"))) {
            @Override
            public boolean handles(EntityPigZombie entity) {
                return BabyArmorLayer.isSupported(entity);
            }
        };
        humanoidLayers(baby, model, BabyHumanoidModel.Kind.ZOMBIFIED_PIGLIN);
        return wrap(adult, baby);
    }

    /** {@code HumanoidMobRenderer}'s layers: armor, the head item (piglin transforms), held items. */
    private static void humanoidLayers(BabyRender<?> render, BabyHumanoidModel model, BabyHumanoidModel.Kind kind) {
        render.addLayer(new BabyArmorLayer(render, kind));
        render.addLayer(new BabyHeadLayer(model, 0.0F, 0.0F, PIGLIN_HEAD_SCALE));
        render.addLayer(new BabyHeldItemLayer(model));
    }
}
