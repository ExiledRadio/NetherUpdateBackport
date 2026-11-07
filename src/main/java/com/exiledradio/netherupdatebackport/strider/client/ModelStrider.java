package com.exiledradio.netherupdatebackport.strider.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * The strider's model: 1.16.5's {@code StriderModel} (64x128), its boxes, pivots, the six bristles, and its walk:
 * the body sways and bobs, the legs swing and splay, the bristles wave with the walk and drift with time. The body
 * turns with the head only when nothing rides it.
 */
public final class ModelStrider extends ModelBase {

    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;
    private final ModelRenderer body;
    private final ModelRenderer[] bristles = new ModelRenderer[6];
    private static final float[] BRISTLE_Z = {-1.2217305F, -1.134464F, -0.87266463F, 0.87266463F, 1.134464F, 1.2217305F};

    public ModelStrider() {
        this.textureWidth = 64;
        this.textureHeight = 128;
        this.rightLeg = new ModelRenderer(this, 0, 32);
        this.rightLeg.setRotationPoint(-4.0F, 8.0F, 0.0F);
        this.rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 16, 4, 0.0F);
        this.leftLeg = new ModelRenderer(this, 0, 55);
        this.leftLeg.setRotationPoint(4.0F, 8.0F, 0.0F);
        this.leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 16, 4, 0.0F);
        this.body = new ModelRenderer(this, 0, 0);
        this.body.setRotationPoint(0.0F, 1.0F, 0.0F);
        this.body.addBox(-8.0F, -6.0F, -8.0F, 16, 14, 16, 0.0F);
        int[][] tex = {{16, 65}, {16, 49}, {16, 33}, {16, 33}, {16, 49}, {16, 65}};
        float[][] pos = {{-8.0F, 4.0F}, {-8.0F, -1.0F}, {-8.0F, -5.0F}, {8.0F, -6.0F}, {8.0F, -2.0F}, {8.0F, 3.0F}};
        for (int i = 0; i < 6; i++) {
            ModelRenderer b = new ModelRenderer(this, tex[i][0], tex[i][1]);
            b.setRotationPoint(pos[i][0], pos[i][1], -8.0F);
            boolean left = i < 3;
            b.mirror = left;
            b.addBox(left ? -12.0F : 0.0F, 0.0F, 0.0F, 12, 0, 16, 0.0F);
            b.rotateAngleZ = BRISTLE_Z[i];
            this.body.addChild(b);
            this.bristles[i] = b;
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.leftLeg.render(scale);
        this.rightLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        float swing = Math.min(0.25F, limbSwingAmount);
        if (entity.getPassengers().isEmpty()) {
            this.body.rotateAngleX = headPitch * ((float) Math.PI / 180F);
            this.body.rotateAngleY = netHeadYaw * ((float) Math.PI / 180F);
        } else {
            this.body.rotateAngleX = 0.0F;
            this.body.rotateAngleY = 0.0F;
        }
        this.body.rotateAngleZ = 0.1F * MathHelper.sin(limbSwing * 1.5F) * 4.0F * swing;
        this.body.rotationPointY = 2.0F - 2.0F * MathHelper.cos(limbSwing * 1.5F) * 2.0F * swing;
        this.leftLeg.rotateAngleX = MathHelper.sin(limbSwing * 1.5F * 0.5F) * 2.0F * swing;
        this.rightLeg.rotateAngleX = MathHelper.sin(limbSwing * 1.5F * 0.5F + (float) Math.PI) * 2.0F * swing;
        this.leftLeg.rotateAngleZ = 0.17453292F * MathHelper.cos(limbSwing * 1.5F * 0.5F) * swing;
        this.rightLeg.rotateAngleZ = 0.17453292F * MathHelper.cos(limbSwing * 1.5F * 0.5F + (float) Math.PI) * swing;
        this.leftLeg.rotationPointY = 8.0F + 2.0F * MathHelper.sin(limbSwing * 1.5F * 0.5F + (float) Math.PI) * 2.0F * swing;
        this.rightLeg.rotationPointY = 8.0F + 2.0F * MathHelper.sin(limbSwing * 1.5F * 0.5F) * 2.0F * swing;
        float wave = MathHelper.cos(limbSwing * 1.5F + (float) Math.PI) * swing;
        float[] waveFactor = {1.3F, 1.2F, 0.6F, 0.6F, 1.2F, 1.3F};
        float[] drift = {0.05F * MathHelper.sin(ageInTicks * -0.4F), 0.1F * MathHelper.sin(ageInTicks * 0.2F),
                0.1F * MathHelper.sin(ageInTicks * 0.4F), 0.1F * MathHelper.sin(ageInTicks * 0.4F),
                0.1F * MathHelper.sin(ageInTicks * 0.2F), 0.05F * MathHelper.sin(ageInTicks * -0.4F)};
        for (int i = 0; i < 6; i++) {
            this.bristles[i].rotateAngleZ = BRISTLE_Z[i] + wave * waveFactor[i] + drift[i];
        }
    }
}
