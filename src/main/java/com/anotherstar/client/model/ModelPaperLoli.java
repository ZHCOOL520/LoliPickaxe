package com.anotherstar.client.model;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.entity.EntityLoli;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 纸片人萝莉：与原 1.12.2 一样全是厚度为 0 的平面方盒，贴图尺寸 32x32，
 * 对应 textures/entities/paper_loli.png。
 */
public class ModelPaperLoli extends EntityModel<EntityLoli> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(LoliPickaxe.MODID, "paper_loli"), "main");

	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;

	public ModelPaperLoli(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(18, 0).addBox(-1.5F, 0.0F, 0.0F, 3, 7, 0), PartPose.offset(1.5F, 17.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, 0.0F, 0.0F, 3, 7, 0), PartPose.offset(-1.5F, 17.0F, 0.0F));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -5.0F, 0.0F, 6, 8, 0), PartPose.offset(0.0F, 14.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-3.0F, -6.0F, 0.0F, 6, 6, 0), PartPose.offset(0.0F, 9.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, 0.0F, 0.0F, 2, 8, 0), PartPose.offset(3.0F, 9.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, 0.0F, 2, 8, 0), PartPose.offset(-3.0F, 9.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(EntityLoli entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.leftArm.xRot = Mth.cos(limbSwing + (float) Math.PI) * limbSwingAmount * 1.5F;
		this.rightArm.xRot = Mth.cos(limbSwing) * limbSwingAmount * 1.5F;
		this.leftLeg.xRot = Mth.cos(limbSwing) * limbSwingAmount * 1.5F;
		this.rightLeg.xRot = Mth.cos(limbSwing + (float) Math.PI) * limbSwingAmount * 1.5F;
		this.head.yRot = netHeadYaw * 0.017453292F;
		this.head.xRot = headPitch * 0.017453292F;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay);
		this.body.render(poseStack, buffer, packedLight, packedOverlay);
		this.head.render(poseStack, buffer, packedLight, packedOverlay);
		this.leftArm.render(poseStack, buffer, packedLight, packedOverlay);
		this.rightArm.render(poseStack, buffer, packedLight, packedOverlay);
	}

}
