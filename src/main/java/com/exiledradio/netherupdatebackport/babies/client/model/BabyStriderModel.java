package com.exiledradio.netherupdatebackport.babies.client.model;

import com.exiledradio.netherupdatebackport.strider.EntityStrider;

/**
 * 26.3's {@code BabyStriderModel}: {@code StriderModel.setupAnim} with the baby's bob and its three bristles, which
 * swing forward and back ({@code xRot}) where the adult's six swing sideways.
 */
public final class BabyStriderModel extends BabyModel<EntityStrider> {

    private static final float SPEED = 1.5F;

    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart body;
    private final ModelPart frontBristle;
    private final ModelPart middleBristle;
    private final ModelPart bottomBristle;

    public BabyStriderModel() {
        super(createBodyLayer());
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        body = root.getChild("body");
        frontBristle = body.getChild("bristle2");
        middleBristle = body.getChild("bristle1");
        bottomBristle = body.getChild("bristle0");
    }

    /** 26.3's {@code BabyStriderModel.createBodyLayer}. */
    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.75F, -4.0F, 7.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.75F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.5F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(8, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.5F, 20.0F, 0.0F));
        body.addOrReplaceChild("bristle0",
                CubeListBuilder.create().texOffs(0, 21).addBox(-3.5F, -2.5F, 0.0F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -4.25F, 2.0F));
        body.addOrReplaceChild("bristle1",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.5F, -2.5F, 0.0F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -4.25F, 0.0F));
        body.addOrReplaceChild("bristle2",
                CubeListBuilder.create().texOffs(0, 15).addBox(-3.5F, -2.5F, 0.0F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -4.25F, -2.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void setupAnim(EntityStrider e, float pos, float walkSpeed, float ageInTicks, float yRot, float xRot) {
        float speed = Math.min(walkSpeed, 0.25F);
        if (e.getPassengers().isEmpty()) {
            body.xRot = xRot * RAD;
            body.yRot = yRot * RAD;
        } else {
            body.xRot = 0.0F;
            body.yRot = 0.0F;
        }
        body.zRot = 0.1F * sin(pos * SPEED) * 4.0F * speed;
        leftLeg.xRot = sin(pos * SPEED * 0.5F) * 2.0F * speed;
        rightLeg.xRot = sin(pos * SPEED * 0.5F + (float) Math.PI) * 2.0F * speed;
        leftLeg.zRot = (float) (Math.PI / 18) * cos(pos * SPEED * 0.5F) * speed;
        rightLeg.zRot = (float) (Math.PI / 18) * cos(pos * SPEED * 0.5F + (float) Math.PI) * speed;
        // BabyStriderModel.customAnimations
        body.y = 17.25F - 1.0F * cos(pos * 1.5F) * 2.0F * speed;
        leftLeg.y = 20.0F + 2.0F * sin(pos * 1.5F * 0.5F + (float) Math.PI) * 2.0F * speed;
        rightLeg.y = 20.0F + 2.0F * sin(pos * 1.5F * 0.5F) * 2.0F * speed;
        float bristleFlow = cos(pos * 1.5F + (float) Math.PI) * speed;
        // StriderModel.animateBristle, adding to xRot.
        frontBristle.xRot += bristleFlow * 0.6F;
        middleBristle.xRot += bristleFlow * 1.2F;
        bottomBristle.xRot += bristleFlow * 1.3F;
        frontBristle.xRot += 0.1F * sin(ageInTicks * 0.4F);
        middleBristle.xRot += 0.1F * sin(ageInTicks * 0.2F);
        bottomBristle.xRot += 0.05F * sin(ageInTicks * -0.4F);
    }
}
