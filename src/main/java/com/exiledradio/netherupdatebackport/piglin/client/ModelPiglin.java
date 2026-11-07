package com.exiledradio.netherupdatebackport.piglin.client;

import com.exiledradio.netherupdatebackport.piglin.AbstractPiglin;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglinBrute;
import com.exiledradio.netherupdatebackport.piglin.PiglinAction;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

/**
 * 1.16.5's {@code PiglinModel}: the player model with a piglin's wide head, snout, tusks and flapping ears, and
 * its poses (dancing, admiring gold held in the off hand, a melee weapon raised high and swung down, a crossbow
 * held or being loaded), with 1.16's {@code ModelHelper} arm math.
 */
public class ModelPiglin extends ModelPlayer {

    public final ModelRenderer earRight;
    public final ModelRenderer earLeft;

    private final float[] bodyDefault;
    private final float[] headDefault;
    private final float[] leftArmDefault;
    private final float[] rightArmDefault;

    public ModelPiglin(float scale) {
        super(scale, false);
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, scale);
        this.bipedHead = new ModelRenderer(this);
        this.bipedHead.setTextureOffset(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10, 8, 8, scale);
        this.bipedHead.setTextureOffset(31, 1).addBox(-2.0F, -4.0F, -5.0F, 4, 4, 1, scale);
        this.bipedHead.setTextureOffset(2, 4).addBox(2.0F, -2.0F, -5.0F, 1, 2, 1, scale);
        this.bipedHead.setTextureOffset(2, 0).addBox(-3.0F, -2.0F, -5.0F, 1, 2, 1, scale);
        this.earRight = new ModelRenderer(this);
        this.earRight.setRotationPoint(4.5F, -6.0F, 0.0F);
        this.earRight.setTextureOffset(51, 6).addBox(0.0F, 0.0F, -2.0F, 1, 5, 4, scale);
        this.bipedHead.addChild(this.earRight);
        this.earLeft = new ModelRenderer(this);
        this.earLeft.setRotationPoint(-4.5F, -6.0F, 0.0F);
        this.earLeft.setTextureOffset(39, 6).addBox(-1.0F, 0.0F, -2.0F, 1, 5, 4, scale);
        this.bipedHead.addChild(this.earLeft);
        this.bipedHeadwear = new ModelRenderer(this);
        this.bodyDefault = save(this.bipedBody);
        this.headDefault = save(this.bipedHead);
        this.leftArmDefault = save(this.bipedLeftArm);
        this.rightArmDefault = save(this.bipedRightArm);
    }

    private static float[] save(ModelRenderer r) {
        return new float[]{r.rotateAngleX, r.rotateAngleY, r.rotateAngleZ, r.rotationPointX, r.rotationPointY, r.rotationPointZ};
    }

    private static void load(ModelRenderer r, float[] v) {
        r.rotateAngleX = v[0];
        r.rotateAngleY = v[1];
        r.rotateAngleZ = v[2];
        r.rotationPointX = v[3];
        r.rotationPointY = v[4];
        r.rotationPointZ = v[5];
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        load(this.bipedBody, this.bodyDefault);
        load(this.bipedHead, this.headDefault);
        load(this.bipedLeftArm, this.leftArmDefault);
        load(this.bipedRightArm, this.rightArmDefault);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);

        float f1 = ageInTicks * 0.1F + limbSwing * 0.5F;
        float f2 = 0.08F + limbSwingAmount * 0.4F;
        this.earRight.rotateAngleZ = -(float) Math.PI / 6.0F - MathHelper.cos(f1 * 1.2F) * f2;
        this.earLeft.rotateAngleZ = (float) Math.PI / 6.0F + MathHelper.cos(f1) * f2;

        if (entity instanceof AbstractPiglin) {
            AbstractPiglin piglin = (AbstractPiglin) entity;
            boolean rightHanded = piglin.getPrimaryHand() == EnumHandSide.RIGHT;
            PiglinAction action = entity instanceof EntityPiglin ? ((EntityPiglin) entity).getArmPose()
                    : entity instanceof EntityPiglinBrute ? ((EntityPiglinBrute) entity).getArmPose() : PiglinAction.DEFAULT;
            if (action == PiglinAction.DANCING) {
                float f3 = ageInTicks / 60.0F;
                this.earLeft.rotateAngleZ = (float) Math.PI / 6.0F + 0.017453292F * MathHelper.sin(f3 * 30.0F) * 10.0F;
                this.earRight.rotateAngleZ = -(float) Math.PI / 6.0F - 0.017453292F * MathHelper.cos(f3 * 30.0F) * 10.0F;
                this.bipedHead.rotationPointX = MathHelper.sin(f3 * 10.0F);
                this.bipedHead.rotationPointY = MathHelper.sin(f3 * 40.0F) + 0.4F;
                this.bipedRightArm.rotateAngleZ = 0.017453292F * (70.0F + MathHelper.cos(f3 * 40.0F) * 10.0F);
                this.bipedLeftArm.rotateAngleZ = -this.bipedRightArm.rotateAngleZ;
                this.bipedRightArm.rotationPointY = MathHelper.sin(f3 * 40.0F) * 0.5F + 1.5F;
                this.bipedLeftArm.rotationPointY = MathHelper.sin(f3 * 40.0F) * 0.5F + 1.5F;
                this.bipedBody.rotationPointY = MathHelper.sin(f3 * 40.0F) * 0.35F;
            } else if (action == PiglinAction.ATTACKING_WITH_MELEE_WEAPON) {
                if (this.swingProgress > 0.0F) {
                    swingWeaponDown(piglin, this.swingProgress, ageInTicks);
                } else if (rightHanded) {
                    this.bipedRightArm.rotateAngleX = -1.8F;
                } else {
                    this.bipedLeftArm.rotateAngleX = -1.8F;
                }
            } else if (action == PiglinAction.CROSSBOW_HOLD) {
                crossbowHold(rightHanded);
            } else if (action == PiglinAction.CROSSBOW_CHARGE) {
                float progress = entity instanceof EntityPiglin ? ((EntityPiglin) entity).chargeProgress(ageInTicks - entity.ticksExisted) : 1.0F;
                crossbowCharge(rightHanded, progress);
            } else if (action == PiglinAction.ADMIRING_ITEM) {
                this.bipedHead.rotateAngleX = 0.5F;
                this.bipedHead.rotateAngleY = 0.0F;
                if (!rightHanded) {
                    this.bipedRightArm.rotateAngleY = -0.5F;
                    this.bipedRightArm.rotateAngleX = -0.9F;
                } else {
                    this.bipedLeftArm.rotateAngleY = 0.5F;
                    this.bipedLeftArm.rotateAngleX = -0.9F;
                }
            }
        }

        copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
        copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
        copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
        copyModelAngles(this.bipedBody, this.bipedBodyWear);
        copyModelAngles(this.bipedHead, this.bipedHeadwear);
    }

    /** 1.16's {@code ModelHelper.animateCrossbowHold}. */
    private void crossbowHold(boolean rightHanded) {
        ModelRenderer main = rightHanded ? this.bipedRightArm : this.bipedLeftArm;
        ModelRenderer other = rightHanded ? this.bipedLeftArm : this.bipedRightArm;
        main.rotateAngleY = (rightHanded ? -0.3F : 0.3F) + this.bipedHead.rotateAngleY;
        other.rotateAngleY = (rightHanded ? 0.6F : -0.6F) + this.bipedHead.rotateAngleY;
        main.rotateAngleX = -(float) Math.PI / 2.0F + this.bipedHead.rotateAngleX + 0.1F;
        other.rotateAngleX = -1.5F + this.bipedHead.rotateAngleX;
    }

    /** 1.16's {@code ModelHelper.animateCrossbowCharge}. */
    private void crossbowCharge(boolean rightHanded, float progress) {
        ModelRenderer main = rightHanded ? this.bipedRightArm : this.bipedLeftArm;
        ModelRenderer other = rightHanded ? this.bipedLeftArm : this.bipedRightArm;
        main.rotateAngleY = rightHanded ? -0.8F : 0.8F;
        main.rotateAngleX = -0.97079635F;
        other.rotateAngleX = main.rotateAngleX;
        other.rotateAngleY = lerp(progress, 0.4F, 0.85F) * (rightHanded ? 1 : -1);
        other.rotateAngleX = lerp(progress, other.rotateAngleX, -(float) Math.PI / 2.0F);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    /** 1.16's {@code ModelHelper.swingWeaponDown} and {@code bobArms}. */
    private void swingWeaponDown(EntityLiving mob, float attackTime, float ageInTicks) {
        float f = MathHelper.sin(attackTime * (float) Math.PI);
        float f1 = MathHelper.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * (float) Math.PI);
        ModelRenderer right = this.bipedRightArm;
        ModelRenderer left = this.bipedLeftArm;
        right.rotateAngleZ = 0.0F;
        left.rotateAngleZ = 0.0F;
        right.rotateAngleY = 0.15707964F;
        left.rotateAngleY = -0.15707964F;
        if (mob.getPrimaryHand() == EnumHandSide.RIGHT) {
            right.rotateAngleX = -1.8849558F + MathHelper.cos(ageInTicks * 0.09F) * 0.15F;
            left.rotateAngleX = MathHelper.cos(ageInTicks * 0.19F) * 0.5F;
            right.rotateAngleX += f * 2.2F - f1 * 0.4F;
            left.rotateAngleX += f * 1.2F - f1 * 0.4F;
        } else {
            right.rotateAngleX = MathHelper.cos(ageInTicks * 0.19F) * 0.5F;
            left.rotateAngleX = -1.8849558F + MathHelper.cos(ageInTicks * 0.09F) * 0.15F;
            right.rotateAngleX += f * 1.2F - f1 * 0.4F;
            left.rotateAngleX += f * 2.2F - f1 * 0.4F;
        }
        right.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        left.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        right.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        left.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
    }
}
