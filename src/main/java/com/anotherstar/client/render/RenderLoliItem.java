package com.anotherstar.client.render;

import org.joml.Matrix4f;

import com.anotherstar.client.util.LoliCardOnlineUtil;
import com.anotherstar.client.util.LoliCardUtil;
import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.item.ItemLoliCard;
import com.anotherstar.common.item.ItemLoliCardAlbum;
import com.anotherstar.common.item.ItemLoliCardOnline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 萝莉卡片类物品的自定义物品渲染器。
 *
 * <p>1.12.2 通过自定义 RenderItem / TileEntityItemStackRenderer 绘制卡片；
 * 1.20.1 对应实现是 {@link BlockEntityWithoutLevelRenderer} + {@link IClientItemExtensions#getCustomRenderer()}。
 * 物品包只需在自己的 initializeClient 中调用：
 *
 * <pre>
 * &#64;Override
 * public void initializeClient(java.util.function.Consumer&lt;IClientItemExtensions&gt; consumer) {
 * 	consumer.accept(RenderLoliItem.CLIENT_EXTENSION);
 * }
 * </pre>
 *
 * 渲染空间与物品模型一致（0~1 的立方体），这里绘制与原版 item/generated 相同的平面方片：
 * 卡片/卡片册/网络卡片分别取各自的图片，取不到时退回物品贴图。
 */
public class RenderLoliItem extends BlockEntityWithoutLevelRenderer {

	private static final ResourceLocation DEFAULT_CARD = new ResourceLocation(LoliPickaxe.MODID, "textures/items/loli_card.png");
	private static final ResourceLocation DEFAULT_ALBUM = new ResourceLocation(LoliPickaxe.MODID, "textures/items/loli_card_album.png");
	private static final ResourceLocation DEFAULT_ONLINE = new ResourceLocation(LoliPickaxe.MODID, "textures/items/loli_card_online.png");

	public static final IClientItemExtensions CLIENT_EXTENSION = new IClientItemExtensions() {

		@Override
		public BlockEntityWithoutLevelRenderer getCustomRenderer() {
			return RenderLoliItem.getInstance();
		}

	};

	private static RenderLoliItem instance;

	private RenderLoliItem() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	public static RenderLoliItem getInstance() {
		if (instance == null) {
			instance = new RenderLoliItem();
		}
		return instance;
	}

	/**
	 * 保留原 1.12.2 的静态入口。1.20.1 下实例改为首次渲染时懒加载
	 * （FMLClientSetupEvent 阶段 Minecraft 实例尚未就绪），因此这里是空实现。
	 */
	public static void init() {
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		ResourceLocation texture = DEFAULT_CARD;
		int width = 16;
		int height = 16;
		if (stack.getItem() instanceof ItemLoliCard) {
			texture = DEFAULT_CARD;
			int index = artIndex(stack.hasTag() ? stack.getTag().getString("picture") : "");
			if (index >= 0) {
				texture = LoliCardUtil.customArtResources[index];
				width = LoliCardUtil.customArtWidths[index];
				height = LoliCardUtil.customArtHeights[index];
			}
		} else if (stack.getItem() instanceof ItemLoliCardAlbum) {
			texture = DEFAULT_ALBUM;
			String group = (stack.hasTag() ? stack.getTag().getString("PictureGroup") : "") + "'";
			if (LoliCardUtil.customArtNames != null) {
				for (int i = 0; i < LoliCardUtil.customArtNames.length; i++) {
					if (LoliCardUtil.customArtNames[i].startsWith(group)) {
						texture = LoliCardUtil.customArtResources[i];
						width = LoliCardUtil.customArtWidths[i];
						height = LoliCardUtil.customArtHeights[i];
						break;
					}
				}
			}
		} else if (stack.getItem() instanceof ItemLoliCardOnline) {
			texture = DEFAULT_ONLINE;
			String url = stack.hasTag() ? stack.getTag().getString("ImageUrl") : "";
			if (LoliCardOnlineUtil.isLoad(url)) {
				ResourceLocation online = LoliCardOnlineUtil.getTexture(url);
				if (online != null) {
					texture = online;
					width = LoliCardOnlineUtil.getWidth(url);
					height = LoliCardOnlineUtil.getHeight(url);
				}
			} else if (!url.isEmpty()) {
				LoliCardOnlineUtil.load(url);
			}
		} else {
			return;
		}
		if (width <= 0 || height <= 0) {
			width = 16;
			height = 16;
		}
		double ratio = (double) width / (double) height;
		double halfWidth = 0.5D;
		double halfHeight = 0.5D;
		if (ratio < 1.0D) {
			halfWidth = 0.5D * ratio;
		} else {
			halfHeight = 0.5D / ratio;
		}
		float x0 = (float) (0.5D - halfWidth);
		float x1 = (float) (0.5D + halfWidth);
		float y0 = (float) (0.5D - halfHeight);
		float y1 = (float) (0.5D + halfHeight);
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
		Matrix4f matrix = poseStack.last().pose();
		vertexConsumer.vertex(matrix, x0, y1, 0.5F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(0.0F, 0.0F, 1.0F).endVertex();
		vertexConsumer.vertex(matrix, x0, y0, 0.5F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(0.0F, 0.0F, 1.0F).endVertex();
		vertexConsumer.vertex(matrix, x1, y0, 0.5F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(0.0F, 0.0F, 1.0F).endVertex();
		vertexConsumer.vertex(matrix, x1, y1, 0.5F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(packedOverlay).uv2(packedLight).normal(0.0F, 0.0F, 1.0F).endVertex();
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

}
