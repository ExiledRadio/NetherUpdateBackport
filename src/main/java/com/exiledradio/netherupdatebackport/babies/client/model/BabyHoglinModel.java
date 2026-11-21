package com.exiledradio.netherupdatebackport.babies.client.model;

import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.hoglin.EntityZoglin;

import net.minecraft.entity.EntityLiving;

/**
 * 26.3's {@code BabyHoglinModel} (hoglin and zoglin): {@code HoglinModel.setupAnim} with the baby's headbutt, which
 * also lowers the head. The baby has no horns and no mane.
 */
public final class BabyHoglinModel extends BabyModel<EntityLiving> {

    private static final float DEFAULT_HEAD_X_ROT = 0.87266463F;
    private static final float ATTACK_HEAD_X_ROT_END = (float) (-Math.PI / 9);
    private static final float EAR_Z_ROT = (float) (Math.PI * 2.0 / 9.0);

    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public BabyHoglinModel() {
        super(createBodyLayer());
        head = root.getChild("head");
        rightEar = head.getChild("right_ear");
        leftEar = head.getChild("left_ear");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
    }

    /** 26.3's {@code BabyHoglinModel.createBodyLayer}. */
    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -2.2605F, -10.547F, 10.0F, 4.0F, 12.0F)
                        .texOffs(44, 29).addBox(-7.0F, -4.0981F, -8.4879F, 2.0F, 5.0F, 2.0F)
                        .texOffs(52, 29).addBox(5.0F, -4.0981F, -8.4879F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, -7.0F, 0.8727F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.0F, -14.0F, -7.0F, 8.0F, 8.0F, 14.0F, new CubeDeformation(0.02F))
                        .texOffs(24, 39).addBox(0.0F, -18.0F, -8.0F, 0.0F, 6.0F, 11.0F, new CubeDeformation(0.02F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(32, 5).addBox(-5.1F, -0.5F, -2.0F, 6.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(-5.0F, -1.0F, -1.5F, 0.0F, 0.0F, -0.8727F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-0.9F, -0.5F, -2.0F, 6.0F, 1.0F, 4.0F).mirror(false),
                PartPose.offsetAndRotation(5.0F, -1.0F, -1.5F, 0.0F, 0.0F, 0.8727F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 47).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-2.5F, 18.0F, 4.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 47).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(2.5F, 18.0F, 4.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 38).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-2.5F, 18.0F, -4.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 38).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(2.5F, 18.0F, -4.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static int attackAnimation(EntityLiving e) {
        return e instanceof EntityHoglin ? ((EntityHoglin) e).attackAnimation()
                : e instanceof EntityZoglin ? ((EntityZoglin) e).attackAnimation() : 0;
    }

    @Override
    protected void setupAnim(EntityLiving e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        rightEar.zRot = -EAR_Z_ROT - speed * sin(pos);
        leftEar.zRot = EAR_Z_ROT + speed * sin(pos);
        head.yRot = yRot * RAD;
        float headbutt = 1.0F - Math.abs(10 - 2 * attackAnimation(e)) / 10.0F;
        // BabyHoglinModel.animateHeadbutt: the adult's head swing, and the head drops.
        head.xRot = lerp(headbutt, DEFAULT_HEAD_X_ROT, ATTACK_HEAD_X_ROT_END);
        head.y += headbutt * 2.5F;
        rightFrontLeg.xRot = cos(pos) * 1.2F * speed;
        leftFrontLeg.xRot = cos(pos + (float) Math.PI) * 1.2F * speed;
        rightHindLeg.xRot = leftFrontLeg.xRot;
        leftHindLeg.xRot = rightFrontLeg.xRot;
    }
}
