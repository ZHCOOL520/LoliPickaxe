package com.anotherstar.client.render;

import com.anotherstar.client.model.ModelLoli;
import com.anotherstar.client.model.ModelNevermore;
import com.anotherstar.client.model.ModelPaperLoli;
import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.entity.EntityLoli;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 原 1.12.2 继承 RenderLiving 并在 doRender 里按配置切换主模型；
 * 1.20.1 的 LivingEntityRenderer 模型在构造时固定，因此这里改为普通 EntityRenderer，
 * 手动完成与原版相同的姿态变换后绘制选中的模型。
 */
public class RenderLoli extends EntityRenderer<EntityLoli> {

	private static final ResourceLocation TEXTURE_LOLI = new ResourceLocation(LoliPickaxe.MODID, "textures/entities/loli.png");
	private static final ResourceLocation TEXTURE_PAPER_LOLI = new ResourceLocation(LoliPickaxe.MODID, "textures/entities/paper_loli.png");

	private final ModelLoli loli;
	private final ModelNevermore nevermore;
	private final ModelPaperLoli paperLoli;

	public RenderLoli(EntityRendererProvider.Context context, ModelLoli model, float shadowSize) {
		super(context);
		this.loli = model;
		this.nevermore = new ModelNevermore(context.bakeLayer(ModelNevermore.LAYER_LOCATION));
		this.paperLoli = new ModelPaperLoli(context.bakeLayer(ModelPaperLoli.LAYER_LOCATION));
		this.shadowRadius = shadowSize;
	}

	@Override
	public void render(EntityLoli entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		EntityModel<EntityLoli> model = this.loli;
		ResourceLocation texture = TEXTURE_LOLI;
		switch (ConfigLoader.loliModelType) {
		case 1:
			model = this.nevermore;
			break;
		case 2:
			model = this.paperLoli;
			texture = TEXTURE_PAPER_LOLI;
			break;
		// case 3 为车万女仆自定义模型，1.20.1 无该依赖，退回默认模型
		default:
			break;
		}
		float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
		float headRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
		float netHeadYaw = headRot - bodyRot;
		float headPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
		float ageInTicks = entity.tickCount + partialTicks;
		float limbSwing = entity.walkAnimation.position(partialTicks);
		float limbSwingAmount = entity.walkAnimation.speed(partialTicks);
		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
		model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityLoli entity) {
		return ConfigLoader.loliModelType == 2 ? TEXTURE_PAPER_LOLI : TEXTURE_LOLI;
	}

}
