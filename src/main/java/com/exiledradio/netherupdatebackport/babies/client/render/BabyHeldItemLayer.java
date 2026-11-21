package com.exiledradio.netherupdatebackport.babies.client.render;

import com.exiledradio.netherupdatebackport.babies.client.model.BabyHumanoidModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;

/** 26.3's {@code ItemInHandLayer} for babies: items at the hand with the baby offset (0, 1, -4.5)/16. */
public final class BabyHeldItemLayer implements LayerRenderer<EntityLivingBase> {

    private final BabyHumanoidModel model;

    public BabyHeldItemLayer(BabyHumanoidModel model) {
        this.model = model;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        boolean rightMain = entity.getPrimaryHand() == EnumHandSide.RIGHT;
        ItemStack main = entity.getHeldItemMainhand();
        ItemStack off = entity.getHeldItemOffhand();
        render(entity, rightMain ? main : off, EnumHandSide.RIGHT);
        render(entity, rightMain ? off : main, EnumHandSide.LEFT);
    }

    private void render(EntityLivingBase entity, ItemStack stack, EnumHandSide side) {
        if (stack.isEmpty()) {
            return;
        }
        boolean left = side == EnumHandSide.LEFT;
        GlStateManager.pushMatrix();
        model.getArm(side).translateAndRotate(0.0625F);
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, 1.0F / 16.0F, -4.5F / 16.0F);
        Minecraft.getMinecraft().getItemRenderer().renderItemSide(entity, stack, left
                ? ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND
                : ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, left);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
