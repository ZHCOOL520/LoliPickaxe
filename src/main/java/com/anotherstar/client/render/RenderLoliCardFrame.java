package com.anotherstar.client.render;

import java.util.ArrayList;
import java.util.List;

import com.anotherstar.client.util.LoliCardOnlineUtil;
import com.anotherstar.client.util.LoliCardUtil;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.ItemLoliCard;
import com.anotherstar.common.item.ItemLoliCardAlbum;
import com.anotherstar.common.item.ItemLoliCardOnline;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 萝莉卡片展示框渲染器。
 *
 * <p>原 1.12.2 通过 coremod(ASM) 把原版 RenderItemFrame 替换成本类；1.20.1 不再做渲染器替换，
 * 本类因此是一个独立的、可被自由注册的 {@link EntityRenderer}（默认不替换原版展示框渲染器），
 * ConfigLoader.loliCardRenderFrame 在原版展示框上不再产生效果。
 * 这里的实现按原逻辑绘制卡片/卡片册/网络卡片的贴图面片。
 */
public class RenderLoliCardFrame extends EntityRenderer<ItemFrame> {

	public static int step = 0;

	private static final ResourceLocation MAP_BACKGROUND_TEXTURES = new ResourceLocation("textures/map/map_background.png");

	public RenderLoliCardFrame(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(ItemFrame entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		ItemStack stack = entity.getItem();
		if (stack.isEmpty()) {
			return;
		}
		boolean isCard = stack.getItem() instanceof ItemLoliCard && stack.hasTag() && stack.getTag().contains("picture");
		boolean isAlbum = stack.getItem() instanceof ItemLoliCardAlbum && stack.hasTag() && stack.getTag().contains("PictureGroup");
		boolean isOnline = stack.getItem() instanceof ItemLoliCardOnline && stack.hasTag() && stack.getTag().contains("ImageUrl");
		if (!isCard && !isAlbum && !isOnline) {
			return;
		}
		ResourceLocation texture = MAP_BACKGROUND_TEXTURES;
		int width = 1;
		int height = 1;
		if (isCard) {
			int index = artIndex(stack.getTag().getString("picture"));
			if (index >= 0) {
				texture = LoliCardUtil.customArtResources[index];
				width = LoliCardUtil.customArtWidths[index];
				height = LoliCardUtil.customArtHeights[index];
			}
		} else if (isAlbum) {
			String group = stack.getTag().getString("PictureGroup") + "'";
			List<Integer> indexes = new ArrayList<>();
			if (LoliCardUtil.customArtNames != null) {
				for (int i = 0; i < LoliCardUtil.customArtNames.length; i++) {
					if (LoliCardUtil.customArtNames[i].startsWith(group)) {
						indexes.add(i);
					}
				}
			}
			if (!indexes.isEmpty()) {
				int index = indexes.get(step % indexes.size());
				texture = LoliCardUtil.customArtResources[index];
				width = LoliCardUtil.customArtWidths[index];
				height = LoliCardUtil.customArtHeights[index];
			}
		} else {
			String url = stack.getTag().getString("ImageUrl");
			if (LoliCardOnlineUtil.isLoad(url)) {
				texture = LoliCardOnlineUtil.getTexture(url);
				width = LoliCardOnlineUtil.getWidth(url);
				height = LoliCardOnlineUtil.getHeight(url);
			} else {
				LoliCardOnlineUtil.load(url);
			}
		}
		if (texture == null) {
			texture = MAP_BACKGROUND_TEXTURES;
			width = 1;
			height = 1;
		}
		double ratio = (double) width / (double) height;
		double scale = ConfigLoader.loliCardScale;
		if (stack.hasCustomHoverName()) {
			try {
				scale = Double.parseDouble(stack.getHoverName().getString());
			} catch (NumberFormatException e) {
			}
		}
		double dx;
		double dy;
		if (ratio < 1.0D) {
			dy = 0.5D;
			dx = dy * ratio;
		} else {
			dx = 0.5D;
			dy = dx / ratio;
		}
		dx *= scale;
		dy *= scale;
		BlockPos hangingPos = entity.getPos();
		Vec3 entityPos = entity.getPosition(partialTicks);
		int rotation = entity.getRotation() % 4 * 2;
		poseStack.pushPose();
		poseStack.translate(hangingPos.getX() + 0.5D - entityPos.x, hangingPos.getY() + 0.5D - entityPos.y, hangingPos.getZ() + 0.5D - entityPos.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getVisualRotationYInDegrees()));
		poseStack.translate(0.0D, 0.0D, 0.4375D);
		poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 360.0F / 8.0F));
		renderQuad(poseStack, buffer, texture, dx, dy);
		poseStack.popPose();
	}

	private static void renderQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, double dx, double dy) {
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
		Matrix4f matrix = poseStack.last().pose();
		int light = LightTexture.FULL_BRIGHT;
		vertexConsumer.vertex(matrix, (float) -dx, (float) -dy, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
		vertexConsumer.vertex(matrix, (float) -dx, (float) dy, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
		vertexConsumer.vertex(matrix, (float) dx, (float) dy, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
		vertexConsumer.vertex(matrix, (float) dx, (float) -dy, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
	}

	private static int artIndex(String name) {
		if (name == null || name.isEmpty() || LoliCardUtil.customArtNames == null) {
			return -1;
		}
		for (int i = 0; i < LoliCardUtil.customArtNames.length; i++) {
			if (LoliCardUtil.customArtNames[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public ResourceLocation getTextureLocation(ItemFrame entity) {
		return MAP_BACKGROUND_TEXTURES;
	}

}
