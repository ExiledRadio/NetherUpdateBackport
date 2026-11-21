package com.exiledradio.netherupdatebackport.babies.client.model;

import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;
import com.exiledradio.netherupdatebackport.piglin.PiglinAction;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

import com.google.common.collect.ImmutableSet;

/**
 * 26.3's baby piglins on {@code HumanoidModel.setupAnim}: {@code BabyPiglinModel} with {@code PiglinModel}'s arm
 * poses (dancing, melee, crossbow, admiring), {@code BabyZombifiedPiglinModel} with
 * {@code AnimationUtils.animateZombieArms}, both with {@code AbstractPiglinModel}'s ear flap at 5 degrees, and the
 * baby piglin armor meshes, which pose themselves the same way. 1.12 has no swimming, elytra or spear poses; arms
 * hold items with the {@code ITEM} pose.
 */
public final class BabyHumanoidModel extends BabyModel<EntityLiving> implements HeadedModel {

    /** Which of 26.3's two baby piglin models this is. */
    public enum Kind {
        PIGLIN, ZOMBIFIED_PIGLIN
    }

    private static final CubeDeformation BABY_PIGLIN_ARMOR_DEFORMATION = new CubeDeformation(0.7F);
    private static final PartPose BABY_PIGLIN_ARMOR_ARM_OFFSET = new PartPose(0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    /** {@code AbstractPiglinModel.BABY_EAR_ANGLE_IN_DEGREES}. */
    private static final float BABY_EAR_ANGLE = 5.0F * RAD;

    private final Kind kind;
    private final boolean translucent;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightEar;
    private final ModelPart leftEar;

    private BabyHumanoidModel(LayerDefinition layer, Kind kind, boolean translucent) {
        super(layer);
        this.kind = kind;
        this.translucent = translucent;
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightEar = head.getChild("right_ear");
        leftEar = head.getChild("left_ear");
    }

    /** 26.3's {@code BabyPiglinModel} or {@code BabyZombifiedPiglinModel} (one mesh); both render translucent. */
    public static BabyHumanoidModel create(Kind kind) {
        return new BabyHumanoidModel(createPiglinLayer(), kind, true);
    }

    /** {@code AbstractPiglinModel.createBabyArmorMeshSet}: the model for one slot, earless. */
    public static BabyHumanoidModel armor(EntityEquipmentSlot slot, Kind kind) {
        MeshDefinition mesh = createBabyArmorMesh(BABY_PIGLIN_ARMOR_DEFORMATION, BABY_PIGLIN_ARMOR_ARM_OFFSET);
        PartDefinition root = mesh.getRoot();
        switch (slot) {
            case HEAD:
                root.retainPartsAndChildren(ImmutableSet.of("head"));
                break;
            case CHEST:
                root.retainExactParts(ImmutableSet.of("body", "left_arm", "right_arm"));
                break;
            case LEGS:
                root.retainExactParts(ImmutableSet.of("left_leg", "right_leg", "waist"));
                break;
            default:
                root.retainExactParts(ImmutableSet.<String>of("left_foot", "right_foot"));
                break;
        }
        // AbstractPiglinModel.removeEars
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.ZERO);
        return new BabyHumanoidModel(LayerDefinition.create(mesh, 64, 64), kind, false);
    }

    @Override
    public ModelPart getHead() {
        return head;
    }

    public ModelPart getArm(EnumHandSide side) {
        return side == EnumHandSide.LEFT ? leftArm : rightArm;
    }

    /** 26.3's {@code BabyPiglinModel.createBodyLayer}, which {@code BabyZombifiedPiglinModel} uses. */
    static LayerDefinition createPiglinLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, -0.5F));
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(21, 30).addBox(-1.5F, -3.0F, -4.5F, 3.0F, 3.0F, 1.0F)
                        .texOffs(0, 0).addBox(-4.5F, -6.0F, -3.5F, 9.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(4.2F, -4.0F, 0.0F));
        leftEar.addOrReplaceChild("left_ear_r1", CubeListBuilder.create().texOffs(0, 21).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 1.75F, 0.0F, 0.0F, 0.0F, -0.6109F));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-4.2F, -4.0F, 0.0F));
        rightEar.addOrReplaceChild("right_ear_r1", CubeListBuilder.create().texOffs(18, 13).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-1.0F, 1.75F, 0.0F, 0.0F, 0.0F, 0.6109F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 13).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offset(4.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(10, 30).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(22, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-1.5F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(10, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(1.5F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 26.3's {@code HumanoidModel.createBabyArmorMesh}, part names as the jar has them. */
    private static MeshDefinition createBabyArmorMesh(CubeDeformation g, PartPose armOffset) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -7.0F, -4.5F, 9.0F, 8.0F, 8.0F, g), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 5.0F, 3.0F, g),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(0, 36).addBox(-3.0F, -1.2F, -1.49F, 5.9F, 2.0F, 2.9F, g.extend(-0.1F)),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(30, 25).addBox(-1.0F, 0.0F, -1.53F, 2.0F, 5.0F, 3.0F, g),
                PartPose.offset(-3.5F - armOffset.x, 15.5F + armOffset.y, 0.0F + armOffset.z));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(30, 17).addBox(-1.0F, 0.0F, -1.53F, 2.0F, 5.0F, 3.0F, g),
                PartPose.offset(3.5F + armOffset.x, 15.5F + armOffset.y, 0.0F + armOffset.z));
        root.addOrReplaceChild("inner_body", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 5.0F, 3.0F, g),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(18, 24).addBox(-2.0F, -0.2F, -2.0F, 3.0F, 4.0F, 3.0F, g.extend(-0.1F)),
                PartPose.offset(1.5F, 20.0F, 0.5F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(18, 17).addBox(-1.0F, -0.2F, -2.0F, 3.0F, 4.0F, 3.0F, g.extend(-0.1F)),
                PartPose.offset(-1.5F, 20.0F, 0.5F));
        // As in the jar, the left leg carries "right_foot" and the right leg "left_foot".
        leftLegDef.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, 2.9F, -2.0F, 3.0F, 1.0F, 3.0F, g),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        rightLegDef.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-1.0F, 2.9F, -2.0F, 3.0F, 1.0F, 3.0F, g).mirror(false),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scale) {
        if (translucent) {
            // AbstractPiglinModel: RenderTypes::entityTranslucent.
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (translucent) {
            GlStateManager.disableBlend();
        }
    }

    private PiglinAction action(EntityLiving e) {
        return kind == Kind.PIGLIN && e instanceof EntityPiglin ? ((EntityPiglin) e).getArmPose() : PiglinAction.DEFAULT;
    }

    @Override
    protected void setupAnim(EntityLiving e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        PiglinAction action = action(e);
        // HumanoidModel.setupAnim
        head.xRot = xRot * RAD;
        head.yRot = yRot * RAD;
        rightArm.xRot = cos(pos * 0.6662F + (float) Math.PI) * 2.0F * speed * 0.5F;
        leftArm.xRot = cos(pos * 0.6662F) * 2.0F * speed * 0.5F;
        rightLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
        leftLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        rightLeg.yRot = 0.005F;
        leftLeg.yRot = -0.005F;
        rightLeg.zRot = 0.005F;
        leftLeg.zRot = -0.005F;
        if (e.isRiding()) {
            rightArm.xRot += (float) (-Math.PI / 5);
            leftArm.xRot += (float) (-Math.PI / 5);
            rightLeg.xRot = -1.4137167F;
            rightLeg.yRot = (float) (Math.PI / 10);
            rightLeg.zRot = 0.07853982F;
            leftLeg.xRot = -1.4137167F;
            leftLeg.yRot = (float) (-Math.PI / 10);
            leftLeg.zRot = -0.07853982F;
        }
        EnumHandSide mainArm = e.getPrimaryHand();
        poseArm(rightArm, !handItem(e, EnumHandSide.RIGHT, mainArm).isEmpty());
        poseArm(leftArm, !handItem(e, EnumHandSide.LEFT, mainArm).isEmpty());
        if (kind == Kind.PIGLIN && swingProgress > 0.0F && action == PiglinAction.ATTACKING_WITH_MELEE_WEAPON) {
            // PiglinModel.setupAttackAnimation
            swingWeaponDown(mainArm, swingProgress, ageInTicks);
        } else {
            setupAttackAnimation(e, mainArm);
            if (kind == Kind.ZOMBIFIED_PIGLIN) {
                // ZombifiedPiglinModel.setupAttackAnimation
                animateZombieArms(e);
            }
        }
        if (e.isSneaking()) {
            body.xRot = 0.5F;
            rightArm.xRot += 0.4F;
            leftArm.xRot += 0.4F;
            rightLeg.z += 4.0F;
            leftLeg.z += 4.0F;
            head.y += 4.2F;
            body.y += 3.2F;
            leftArm.y += 3.2F;
            rightArm.y += 3.2F;
        }
        bobModelPart(rightArm, ageInTicks, 1.0F);
        bobModelPart(leftArm, ageInTicks, -1.0F);
        // AbstractPiglinModel.setupAnim: the ears.
        float frequency = ageInTicks * 0.1F + pos * 0.5F;
        float amplitude = 0.08F + speed * 0.4F;
        leftEar.zRot = -BABY_EAR_ANGLE - cos(frequency * 1.2F) * amplitude;
        rightEar.zRot = BABY_EAR_ANGLE + cos(frequency) * amplitude;
        if (kind == Kind.PIGLIN) {
            piglinPose(e, action, mainArm, ageInTicks);
        }
    }

    /** {@code PiglinModel.setupAnim} after its super call. */
    private void piglinPose(EntityLiving e, PiglinAction action, EnumHandSide mainArm, float ageInTicks) {
        boolean right = mainArm == EnumHandSide.RIGHT;
        if (action == PiglinAction.DANCING) {
            float dancePos = ageInTicks / 60.0F;
            // The adult's 30 degrees, as 26.3 has it for babies too.
            rightEar.zRot = (float) (Math.PI / 6) + RAD * sin(dancePos * 30.0F) * 10.0F;
            leftEar.zRot = (float) (-Math.PI / 6) - RAD * cos(dancePos * 30.0F) * 10.0F;
            head.x += sin(dancePos * 10.0F);
            head.y += sin(dancePos * 40.0F) + 0.4F;
            rightArm.zRot = RAD * (70.0F + cos(dancePos * 40.0F) * 10.0F);
            leftArm.zRot = rightArm.zRot * -1.0F;
            rightArm.y += sin(dancePos * 40.0F) * 0.5F - 0.5F;
            leftArm.y += sin(dancePos * 40.0F) * 0.5F + 0.5F;
            body.y += sin(dancePos * 40.0F) * 0.35F;
        } else if (action == PiglinAction.ATTACKING_WITH_MELEE_WEAPON && swingProgress == 0.0F) {
            // holdWeaponHigh
            if (right) {
                rightArm.xRot = -1.8F;
            } else {
                leftArm.xRot = -1.8F;
            }
        } else if (action == PiglinAction.CROSSBOW_HOLD) {
            ModelPart holding = right ? rightArm : leftArm;
            ModelPart shooting = right ? leftArm : rightArm;
            holding.yRot = (right ? -0.3F : 0.3F) + head.yRot;
            shooting.yRot = (right ? 0.6F : -0.6F) + head.yRot;
            holding.xRot = (float) (-Math.PI / 2) + head.xRot + 0.1F;
            shooting.xRot = -1.5F + head.xRot;
        } else if (action == PiglinAction.CROSSBOW_CHARGE) {
            ModelPart holding = right ? rightArm : leftArm;
            ModelPart pulling = right ? leftArm : rightArm;
            holding.yRot = right ? -0.8F : 0.8F;
            holding.xRot = -0.97079635F;
            pulling.xRot = holding.xRot;
            float alpha = ((EntityPiglin) e).chargeProgress(partialTicks);
            pulling.yRot = lerp(alpha, 0.4F, 0.85F) * (right ? 1 : -1);
            pulling.xRot = lerp(alpha, pulling.xRot, (float) (-Math.PI / 2));
        } else if (action == PiglinAction.ADMIRING_ITEM) {
            head.xRot = 0.5F;
            head.yRot = 0.0F;
            if (!right) {
                rightArm.yRot = -0.5F;
                rightArm.xRot = -0.9F;
            } else {
                leftArm.yRot = 0.5F;
                leftArm.xRot = -0.9F;
            }
        }
    }

    static ItemStack handItem(EntityLiving e, EnumHandSide side, EnumHandSide mainArm) {
        return side == mainArm ? e.getHeldItemMainhand() : e.getHeldItemOffhand();
    }

    /** {@code ArmPose.EMPTY} or {@code ArmPose.ITEM}. */
    private static void poseArm(ModelPart arm, boolean holding) {
        if (holding) {
            arm.xRot = arm.xRot * 0.5F - (float) (Math.PI / 10);
        }
        arm.yRot = 0.0F;
    }

    /** {@code HumanoidModel.setupAttackAnimation}: a plain swing. */
    private void setupAttackAnimation(EntityLiving e, EnumHandSide mainArm) {
        float swing = swingProgress;
        if (swing <= 0.0F) {
            return;
        }
        EnumHand hand = e.swingingHand == null ? EnumHand.MAIN_HAND : e.swingingHand;
        EnumHandSide swingArm = hand == EnumHand.MAIN_HAND ? mainArm : mainArm.opposite();
        body.yRot = sin(MathHelper.sqrt(swing) * (float) (Math.PI * 2)) * 0.2F;
        if (swingArm == EnumHandSide.LEFT) {
            body.yRot *= -1.0F;
        }
        rightArm.z = sin(body.yRot) * 5.0F * AGE_SCALE;
        rightArm.x = -cos(body.yRot) * 5.0F * AGE_SCALE;
        leftArm.z = -sin(body.yRot) * 5.0F * AGE_SCALE;
        leftArm.x = cos(body.yRot) * 5.0F * AGE_SCALE;
        rightArm.yRot += body.yRot;
        leftArm.yRot += body.yRot;
        leftArm.xRot += body.yRot;
        float eased = 1.0F - (1.0F - swing) * (1.0F - swing) * (1.0F - swing) * (1.0F - swing);
        float aa = sin(eased * (float) Math.PI);
        float bb = sin(swing * (float) Math.PI) * -(head.xRot - 0.7F) * 0.75F;
        ModelPart attackArm = getArm(swingArm);
        attackArm.xRot -= aa * 1.2F + bb;
        attackArm.yRot += body.yRot * 2.0F;
        attackArm.zRot += sin(swing * (float) Math.PI) * -0.4F;
    }

    /** {@code AnimationUtils.animateZombieArms} for a baby: arms raised only with nothing in the main hand. */
    private void animateZombieArms(EntityLiving e) {
        float swing = swingProgress;
        boolean aggressive = e instanceof EntityZombie && ((EntityZombie) e).isArmsRaised();
        boolean raiseArms = e.getHeldItemMainhand().isEmpty();
        float armDrop = raiseArms ? (float) -Math.PI / (aggressive ? 1.5F : 2.25F) : 0.0F;
        float attackYRotModifier = (raiseArms ? 1.0F : -1.0F) * sin(swing * (float) Math.PI);
        float attackXRotModifier = sin((1.0F - (1.0F - swing) * (1.0F - swing)) * (float) Math.PI);
        float xRotArms = armDrop + attackYRotModifier * 1.2F - attackXRotModifier * 0.4F;
        float yRotArms = 0.1F - attackYRotModifier * 0.6F;
        rightArm.xRot = xRotArms;
        rightArm.yRot = raiseArms ? -yRotArms : yRotArms;
        rightArm.zRot = 0.0F;
        leftArm.xRot = xRotArms;
        leftArm.yRot = raiseArms ? yRotArms : -yRotArms;
        leftArm.zRot = 0.0F;
    }

    /** {@code AnimationUtils.swingWeaponDown}, with its {@code bobArms}. */
    private void swingWeaponDown(EnumHandSide mainArm, float attackTime, float ageInTicks) {
        float attack2 = sin(attackTime * (float) Math.PI);
        float attack = sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * (float) Math.PI);
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.yRot = (float) (Math.PI / 20);
        leftArm.yRot = (float) (-Math.PI / 20);
        if (mainArm == EnumHandSide.RIGHT) {
            rightArm.xRot = -1.8849558F + cos(ageInTicks * 0.09F) * 0.15F;
            leftArm.xRot = cos(ageInTicks * 0.19F) * 0.5F;
            rightArm.xRot += attack2 * 2.2F - attack * 0.4F;
            leftArm.xRot += attack2 * 1.2F - attack * 0.4F;
        } else {
            rightArm.xRot = cos(ageInTicks * 0.19F) * 0.5F;
            leftArm.xRot = -1.8849558F + cos(ageInTicks * 0.09F) * 0.15F;
            rightArm.xRot += attack2 * 1.2F - attack * 0.4F;
            leftArm.xRot += attack2 * 2.2F - attack * 0.4F;
        }
        bobModelPart(rightArm, ageInTicks, 1.0F);
        bobModelPart(leftArm, ageInTicks, -1.0F);
    }

    private static void bobModelPart(ModelPart part, float ageInTicks, float scale) {
        part.zRot += scale * (cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
        part.xRot += scale * sin(ageInTicks * 0.067F) * 0.05F;
    }
}
