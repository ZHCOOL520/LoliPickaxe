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
 * 纳文摩尔（shadow fiend）。
 *
 * <p>原 1.12.2 实现是一个 OBJ 模型（models/entity/loli/loli.obj + loli.mtl，
 * 共 5 种材质、4217 个三角面与 1557 个四边形），依赖 {@code client/util/obj} 的解析器
 * 与固定管线绘制。1.20.1 的 EntityModel 只支持方盒模型，多材质三角网格需要自行
 * 实现 RenderType/端板绘制，成本过高，因此这里用一套方盒结构近似还原其外形
 * （人形躯干 + 头部 + 双臂 + 背后双翼），OBJ 解析工具类仍然保留在
 * {@code com.anotherstar.client.util.obj} 供后续接入。
 */
public class ModelNevermore extends EntityModel<EntityLoli> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(LoliPickaxe.MODID, "nevermore"), "main");

	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public ModelNevermore(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftWing = root.getChild("left_wing");
		this.rightWing = root.getChild("right_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4), PartPose.offset(2.5F, 12.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 32).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4), PartPose.offset(-2.5F, 12.0F, 0.0F));
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5F, -12.0F, -3.0F, 9, 12, 6)
				.texOffs(30, 0).addBox(-5.5F, -13.0F, -3.5F, 11, 3, 7),
				PartPose.offset(0.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 48).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
				.texOffs(32, 48).addBox(-4.5F, -8.5F, -4.5F, 9, 4, 9),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 32).addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3), PartPose.offset(6.0F, 1.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 32).addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3), PartPose.offset(-6.0F, 1.0F, 0.0F));
		root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 64).addBox(0.0F, 0.0F, 0.0F, 12, 16, 1), PartPose.offset(1.0F, -1.0F, 3.0F));
		root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(26, 64).addBox(-12.0F, 0.0F, 0.0F, 12, 16, 1), PartPose.offset(-1.0F, -1.0F, 3.0F));
		return LayerDefinition.create(mesh, 64, 96);
	}

	@Override
	public void setupAnim(EntityLoli entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.leftArm.xRot = Mth.cos(limbSwing + (float) Math.PI) * limbSwingAmount * 1.5F;
		this.rightArm.xRot = Mth.cos(limbSwing) * limbSwingAmount * 1.5F;
		this.leftLeg.xRot = Mth.cos(limbSwing) * limbSwingAmount * 1.5F;
		this.rightLeg.xRot = Mth.cos(limbSwing + (float) Math.PI) * limbSwingAmount * 1.5F;
		this.head.yRot = -netHeadYaw * 0.017453292F;
		this.head.xRot = headPitch * 0.017453292F;
		float flap = Mth.cos(ageInTicks * 0.3F) * 0.35F;
		this.leftWing.zRot = flap;
		this.rightWing.zRot = -flap;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay);
		this.body.render(poseStack, buffer, packedLight, packedOverlay);
		this.leftWing.render(poseStack, buffer, packedLight, packedOverlay);
		this.rightWing.render(poseStack, buffer, packedLight, packedOverlay);
		this.head.render(poseStack, buffer, packedLight, packedOverlay);
		this.leftArm.render(poseStack, buffer, packedLight, packedOverlay);
		this.rightArm.render(poseStack, buffer, packedLight, packedOverlay);
	}

}
