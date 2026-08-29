package com.csykes.searchlight.features.teddy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class TeddyEntityModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("searchlight", "teddy_bear"), "main");
	private final ModelPart Body;
	private final ModelPart head2;
	private final ModelPart eyes;
	private final ModelPart Muzzle;
	private final ModelPart head;
	private final ModelPart hood;
	private final ModelPart pawFR;
	private final ModelPart pawFL;
	private final ModelPart pawHR;
	private final ModelPart honey_pot;
	private final ModelPart pawHL;
	private final ModelPart coat;

	public TeddyEntityModel(ModelPart root) {
		this.Body = root.getChild("Body");
		this.head2 = this.Body.getChild("head2");
		this.head = this.head2.getChild("head");
		this.eyes = this.head.getChild("eyes");
		this.Muzzle = this.head.getChild("Muzzle");
		this.hood = this.head.getChild("hood");
		this.pawFR = this.Body.getChild("pawFR");
		this.pawFL = this.Body.getChild("pawFL");
		this.pawHR = this.Body.getChild("pawHR");
		this.honey_pot = this.pawHR.getChild("honey_pot");
		this.pawHL = this.Body.getChild("pawHL");
		this.coat = this.Body.getChild("coat");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(1.0F, 11.0F, -3.0F));

		PartDefinition head2 = Body.addOrReplaceChild("head2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition head = head2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(38, 29).addBox(-1.7F, -9.25F, -1.5F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 9).addBox(-2.7F, -8.25F, -1.5F, 8.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.3F, 5.25F, 1.5F));

		PartDefinition eyes = head.addOrReplaceChild("eyes", CubeListBuilder.create()
				.texOffs(38, 31).addBox(2.3F, -7.25F, -2.5F, 1.5F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(18, 51).addBox(-1.7F, -6.25F, -2.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(24, 51).addBox(2.3F, -6.25F, -2.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(32, 17).addBox(-1.2F, -7.25F, -2.5F, 1.5F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

		PartDefinition Muzzle = head.addOrReplaceChild("Muzzle", CubeListBuilder.create()
				.texOffs(26, 14).addBox(-0.7F, -3.25F, -3.5F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 0).addBox(-0.7F, -4.25F, -3.5F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(48, 49).addBox(-0.2F, -4.25F, -4.5F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(40, 49).addBox(0.3F, -5.25F, -3.5F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

		PartDefinition hood = head.addOrReplaceChild("hood", CubeListBuilder.create()
				.texOffs(38, 22).addBox(-0.7F, -3.25F, -1.5F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(38, 14).addBox(-2.7F, -3.25F, -3.5F, 2.0F, 0.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(34, 42).addBox(-2.7F, -9.25F, 3.5F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-2.7F, -10.25F, -3.5F, 7.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(36, 33).addBox(3.3F, -3.25F, -3.5F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(16, 42).addBox(4.3F, -10.25F, -3.5F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(20, 19).addBox(5.3F, -9.25F, -3.5F, 1.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(30, 0).addBox(-3.7F, -9.25F, -3.5F, 1.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(16, 33).addBox(-2.7F, -3.25F, -3.5F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(52, 8).addBox(4.3F, -9.25F, -3.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(52, 11).addBox(-2.7F, -9.25F, -3.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

		PartDefinition pawFR = Body.addOrReplaceChild("pawFR", CubeListBuilder.create().texOffs(48, 3).addBox(-2.2F, 4.75F, -0.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.3F, 5.25F, 1.5F));

		PartDefinition pawFL = Body.addOrReplaceChild("pawFL", CubeListBuilder.create().texOffs(48, 3).addBox(2.8F, 4.75F, -0.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.3F, 5.25F, 1.5F));

		PartDefinition pawHR = Body.addOrReplaceChild("pawHR", CubeListBuilder.create().texOffs(8, 45).addBox(-3.7F, -1.25F, -1.5F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.3F, 5.25F, 1.5F));

		PartDefinition honey_pot = pawHR.addOrReplaceChild("honey_pot", CubeListBuilder.create(), PartPose.offset(-3.7F, 4.75F, -1.5F));

		PartDefinition cube_r1 = honey_pot.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-5.9F, 1.85F, 1.8F, 0.2F, 1.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(52, 29).addBox(-5.9F, 1.85F, 1.3F, 0.2F, 1.5F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9F, 1.85F, 1.3F, 0.2F, 1.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(26, 9).addBox(-2.7F, 1.85F, -1.7F, 0.2F, 1.5F, 2.2F, new CubeDeformation(0.0F))
		.texOffs(0, 51).addBox(-5.9F, 1.85F, -1.7F, 0.2F, 1.5F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.7F, 1.85F, 1.5F, 0.0F, 1.5F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(44, 31).addBox(-5.7F, 1.85F, 1.55F, 2.0F, 1.5F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(26, 17).addBox(-5.7F, 1.85F, -1.7F, 3.0F, 1.5F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(6, 51).addBox(-5.7F, 0.75F, 1.2F, 2.7F, 4.0F, 0.35F, new CubeDeformation(0.0F))
		.texOffs(48, 8).addBox(-3.0F, 0.75F, -1.2F, 0.3F, 4.0F, 2.4F, new CubeDeformation(0.0F))
		.texOffs(34, 49).addBox(-5.7F, 0.75F, -1.5F, 0.4F, 4.0F, 2.7F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.0F, 0.75F, 1.2F, 0.3F, 4.0F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(12, 51).addBox(-5.3F, 0.75F, -1.5F, 2.6F, 4.0F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(0, 45).addBox(-5.3F, 1.05F, -1.2F, 2.3F, 3.7F, 2.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.7F, -3.75F, -2.5F, 0.7854F, 0.0F, 0.0F));

		PartDefinition pawHL = Body.addOrReplaceChild("pawHL", CubeListBuilder.create().texOffs(8, 45).addBox(4.3F, -1.25F, -1.5F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.3F, 5.25F, 1.5F));

		PartDefinition coat = Body.addOrReplaceChild("coat", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -10.0F, -5.0F, 2.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(0, 19).addBox(-4.0F, -10.0F, -5.0F, 4.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(48, 31).addBox(-4.0F, -10.0F, -6.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(30, 51).addBox(1.0F, -10.0F, -6.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-1.175F, -6.925F, -5.25F, 0.35F, 4.0F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-1.525F, -10.025F, -5.25F, 0.35F, 4.0F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(16, 32).addBox(-1.0F, -8.7F, -5.25F, 0.5F, 0.75F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(26, 13).addBox(-0.6F, -6.7F, -5.25F, 0.5F, 0.75F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(28, 13).addBox(-0.6F, -4.5F, -5.25F, 0.5F, 0.75F, 0.25F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.0F, 4.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		assert entity instanceof TeddyBearEntity;
		TeddyBearEntity bear = (TeddyBearEntity) entity;

		// 1. Reset baseline poses
		this.Body.y = 11.0F;
		this.Body.xRot = 0.0F;
		this.Body.yRot = 0.0F;
		this.Body.zRot = 0.0F;

		this.head.xRot = 0.0F;
		this.head.yRot = 0.0F;
		this.head.zRot = 0.0F;
		this.eyes.xRot = 0.0F;
		this.eyes.yRot = 0.0F;
		this.eyes.zRot = 0.0F;
		this.Muzzle.x = 0.0F;
		this.Muzzle.y = 0.0F;
		this.Muzzle.z = 0.0F;
		this.Muzzle.xRot = 0.0F;
		this.Muzzle.yRot = 0.0F;
		this.Muzzle.zRot = 0.0F;
		this.hood.xRot = 0.0F;
		this.hood.yRot = 0.0F;
		this.hood.zRot = 0.0F;

		this.pawFR.x = -2.3F;
		this.pawFR.y = 5.25F;
		this.pawFR.z = 1.5F;
		this.pawFR.xRot = 0.0F;
		this.pawFR.yRot = 0.0F;
		this.pawFR.zRot = 0.0F;

		this.pawFL.x = -2.3F;
		this.pawFL.y = 5.25F;
		this.pawFL.z = 1.5F;
		this.pawFL.xRot = 0.0F;
		this.pawFL.yRot = 0.0F;
		this.pawFL.zRot = 0.0F;

		this.pawHR.x = -2.3F;
		this.pawHR.y = 5.25F;
		this.pawHR.z = 1.5F;
		this.pawHR.xRot = 0.0F;
		this.pawHR.yRot = 0.0F;
		this.pawHR.zRot = 0.0F;

		this.pawHL.x = -2.3F;
		this.pawHL.y = 5.25F;
		this.pawHL.z = 1.5F;
		this.pawHL.xRot = 0.0F;
		this.pawHL.yRot = 0.0F;
		this.pawHL.zRot = 0.0F;

		this.honey_pot.visible = true;
		this.honey_pot.xRot = 0.0F;
		this.honey_pot.zRot = 0.0F;

		// 2. Head Tracking & Hood Cloth Dampening
		this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);
		this.hood.yRot = -0.15F * this.head.yRot;
		this.hood.xRot = -0.10F * this.head.xRot;

		// 3. Walk Cycle & Honey Pot Physics
		// pawFR/pawFL are LEGS (feet). pawHR/pawHL are ARMS (hands).
		if (limbSwingAmount > 0.01F) {
			// Legs (feet) swing for walking
			this.pawFR.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
			this.pawFL.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
			// Arms (hands) swing opposite legs
			this.pawHL.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
			this.pawHR.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;

			// Honey jar sloshing physics while walking
			this.honey_pot.xRot = Mth.cos(limbSwing * 0.6662F) * 0.25F * limbSwingAmount;
			this.honey_pot.zRot = Mth.sin(limbSwing * 0.6662F) * 0.15F * limbSwingAmount;
		} else {
			// Gentle arm breathing when standing still (pawHR/pawHL are ARMS)
			this.pawHR.zRot = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
			this.pawHL.zRot = Mth.cos(ageInTicks * 0.09F) * -0.05F - 0.05F;
		}

		// 4. Sitting Pose (Legs tucked compactly against torso without floor clipping)
		if (bear.isInSittingPose() || bear.isOrderedToSit()) {
			this.Body.y = 12.5F;
			// Lift leg joints slightly and angle forward at 40 degrees
			this.pawFR.y = 4.0F;
			this.pawFL.y = 4.0F;
			this.pawFR.z = 0.0F;
			this.pawFL.z = 0.0F;
			this.pawFR.xRot = -0.7F;
			this.pawFL.xRot = -0.7F;
			this.pawFR.yRot = -0.15F;
			this.pawFL.yRot = 0.15F;
			// Arms (hands: pawHR, pawHL) rest on lap
			this.pawHR.xRot = -0.4F;
			this.pawHL.xRot = -0.4F;
			this.pawHR.zRot = 0.1F;
			this.pawHL.zRot = -0.1F;
			// Drowsy head dip
			this.head.xRot += Mth.sin(ageInTicks * 0.04F) * 0.05F + 0.05F;
		}

		// 5. Hugging Animation (Warm Cuddle Squeeze)
		if (bear.isHugging()) {
			this.honey_pot.visible = false;
			// Arms (hands: pawHR, pawHL) raise forward to hug!
			this.pawHR.xRot = (float) -Math.PI / 2F;
			this.pawHL.xRot = (float) -Math.PI / 2F;
			// Soft pulsing cuddle squeeze
			this.pawHR.zRot = -0.15F + Mth.sin(ageInTicks * 0.2F) * 0.05F;
			this.pawHL.zRot = 0.15F - Mth.sin(ageInTicks * 0.2F) * 0.05F;
			// Happy cuddle head nudge
			this.head.zRot = Mth.cos(ageInTicks * 0.12F) * 0.08F;
		}

		// 6. Eating Honey Licking Feast Animation
		if (bear.isEatingHoney()) {
			// Right arm (pawHR holding honey pot) brings pot to mouth
			this.pawHR.xRot = -1.2F;
			this.pawHR.yRot = 0.3F;
			this.head.xRot += 0.35F;
			this.Muzzle.y = Mth.sin(ageInTicks * 0.5F) * 0.15F;
			// Left arm (pawHL) pats belly
			this.pawHL.xRot = -0.6F;
			this.pawHL.zRot = 0.2F + Mth.cos(ageInTicks * 0.3F) * 0.1F;
		}

		// 7. Begging for Honey Animation (When player holds honey item nearby)
		if (!bear.isEatingHoney() && !bear.isHugging() && bear.isBeggingForHoney()) {
			// Arms (hands: pawHR, pawHL) wave up and down in begging motion!
			this.pawHR.xRot = -0.8F + Mth.sin(ageInTicks * 0.3F) * 0.2F;
			this.pawHL.xRot = -0.8F + Mth.cos(ageInTicks * 0.3F) * 0.2F;
			this.head.xRot -= 0.2F;
			this.head.zRot = 0.12F;
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		Body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}
}