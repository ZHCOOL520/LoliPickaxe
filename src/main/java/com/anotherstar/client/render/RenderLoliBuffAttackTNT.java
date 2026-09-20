package com.anotherstar.client.render;

import com.anotherstar.common.entity.EntityLoliBuffAttackTNT;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.model.data.ModelData;

/**
 * 1.12.2 直接调用 BlockRendererDispatcher.renderBlockBrightness 绘制方块；
 * 1.20.1 改为 BlockRenderDispatcher.renderSingleBlock（方块贴图由方块状态模型提供）。
 */
public class RenderLoliBuffAttackTNT extends EntityRenderer<EntityLoliBuffAttackTNT> {

	public RenderLoliBuffAttackTNT(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.5F;
	}

	@Override
	public void render(EntityLoliBuffAttackTNT entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.5D, 0.0D);
		if ((float) entity.getFuse() - partialTicks + 1.0F < 10.0F) {
			float f = 1.0F - ((float) entity.getFuse() - partialTicks + 1.0F) / 10.0F;
			f = Mth.clamp(f, 0.0F, 1.0F);
			f = f * f;
			f = f * f;
			float f1 = 1.0F + f * 0.3F;
			poseStack.scale(f1, f1, f1);
		}
		poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
		poseStack.translate(-0.5D, -0.5D, 0.5D);
		// 5 参的 renderSingleBlock 已被 Forge 废弃，改用带 ModelData/RenderType 的非废弃重载；
		// 传 ModelData.EMPTY 与 null 与废弃重载内部实现完全一致（不改变渲染行为）。
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(entity.getDefaultState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityLoliBuffAttackTNT entity) {
		// TextureAtlas.LOCATION_BLOCKS 已废弃，InventoryMenu.BLOCK_ATLAS 是同一个常量值。
		return InventoryMenu.BLOCK_ATLAS;
	}

}
