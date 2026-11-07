package com.exiledradio.netherupdatebackport.mobs.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.math.MathHelper;

/**
 * 1.16.5's {@code PiglinModel} as {@code PiglinRenderer} builds it for the zombified piglin: the player model
 * (64x64, sleeves, trouser legs and jacket) with the piglin's wide head, snout, tusks and ears, its left ear
 * gone, and the zombie's raised arms ({@code ModelHelper.animateZombieArms}, the same as 1.12.2's
 * {@code ModelZombie}) when aggressive.
 */
public final class ModelZombifiedPiglin extends ModelPlayer {

    private final ModelRenderer earRight;
    private final ModelRenderer earLeft;

    public ModelZombifiedPiglin() {
        super(0.0F, false);
        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);
        this.bipedHead = new ModelRenderer(this, 0, 0);
        this.bipedHead.addBox(-5.0F, -8.0F, -4.0F, 10, 8, 8, 0.0F);
        this.bipedHead.setTextureOffset(31, 1).addBox(-2.0F, -4.0F, -5.0F, 4, 4, 1, 0.0F);
        this.bipedHead.setTextureOffset(2, 4).addBox(2.0F, -2.0F, -5.0F, 1, 2, 1, 0.0F);
        this.bipedHead.setTextureOffset(2, 0).addBox(-3.0F, -2.0F, -5.0F, 1, 2, 1, 0.0F);
        this.earRight = new ModelRenderer(this, 51, 6);
        this.earRight.setRotationPoint(4.5F, -6.0F, 0.0F);
        this.earRight.addBox(0.0F, 0.0F, -2.0F, 1, 5, 4, 0.0F);
        this.bipedHead.addChild(this.earRight);
        this.earLeft = new ModelRenderer(this, 39, 6);
        this.earLeft.setRotationPoint(-4.5F, -6.0F, 0.0F);
        this.earLeft.addBox(-1.0F, 0.0F, -2.0F, 1, 5, 4, 0.0F);
        this.bipedHead.addChild(this.earLeft);
        // PiglinRenderer(manager, true): the zombified piglin has lost its left ear.
        this.earLeft.showModel = false;
        this.bipedHeadwear = new ModelRenderer(this);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
        float f = ageInTicks * 0.1F + limbSwing * 0.5F;
        float f1 = 0.08F + limbSwingAmount * 0.4F;
        this.earRight.rotateAngleZ = -(float) Math.PI / 6.0F - MathHelper.cos(f * 1.2F) * f1;
        this.earLeft.rotateAngleZ = (float) Math.PI / 6.0F + MathHelper.cos(f) * f1;

        boolean aggressive = entity instanceof EntityZombie && ((EntityZombie) entity).isArmsRaised();
        float swing = MathHelper.sin(this.swingProgress * (float) Math.PI);
        float swing1 = MathHelper.sin((1.0F - (1.0F - this.swingProgress) * (1.0F - this.swingProgress)) * (float) Math.PI);
        this.bipedRightArm.rotateAngleZ = 0.0F;
        this.bipedLeftArm.rotateAngleZ = 0.0F;
        this.bipedRightArm.rotateAngleY = -(0.1F - swing * 0.6F);
        this.bipedLeftArm.rotateAngleY = 0.1F - swing * 0.6F;
        float raised = -(float) Math.PI / (aggressive ? 1.5F : 2.25F);
        this.bipedRightArm.rotateAngleX = raised + swing * 1.2F - swing1 * 0.4F;
        this.bipedLeftArm.rotateAngleX = raised + swing * 1.2F - swing1 * 0.4F;
        this.bipedRightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedLeftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedRightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        this.bipedLeftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;

        // The sleeves follow the arms as they now stand (ModelPlayer copied them before the zombie pose).
        ModelBase.copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        ModelBase.copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
        ModelBase.copyModelAngles(this.bipedBody, this.bipedBodyWear);
        ModelBase.copyModelAngles(this.bipedHead, this.bipedHeadwear);
    }
}
