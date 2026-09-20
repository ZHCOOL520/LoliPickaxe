package com.anotherstar.client.gui;

import java.util.List;

import com.anotherstar.client.util.LoliCardUtil;
import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12.2 里这些纯客户端界面由 {@code LoliGUIHandler.getClientGuiElement} 打开；
 * 1.20.1 没有 IGuiHandler，统一改由物品/按键在客户端直接 {@code setScreen}。
 */
public final class ClientGuiOpener {

	private ClientGuiOpener() {
	}

	public static void openCard(ItemStack stack) {
		String name = stack.hasTag() ? stack.getTag().getString("picture") : "";
		if (name.isEmpty() || LoliCardUtil.customArtNames == null) {
			return;
		}
		for (int i = 0; i < LoliCardUtil.customArtNames.length; i++) {
			if (LoliCardUtil.customArtNames[i].equals(name)) {
				Minecraft.getInstance().setScreen(new GUILoliCard(name, LoliCardUtil.customArtResources[i], LoliCardUtil.customArtWidths[i], LoliCardUtil.customArtHeights[i]));
				return;
			}
		}
	}

	public static void openCardAlbum(ItemStack stack) {
		String groupName = stack.hasTag() ? stack.getTag().getString("PictureGroup") : "";
		if (groupName.isEmpty() || LoliCardUtil.customArtNames == null) {
			return;
		}
		String prefix = groupName + "'";
		List<ResourceLocation> resources = Lists.newArrayList();
		List<Integer> widths = Lists.newArrayList();
		List<Integer> heights = Lists.newArrayList();
		for (int i = 0; i < LoliCardUtil.customArtNames.length; i++) {
			if (LoliCardUtil.customArtNames[i].startsWith(prefix)) {
				resources.add(LoliCardUtil.customArtResources[i]);
				widths.add(LoliCardUtil.customArtWidths[i]);
				heights.add(LoliCardUtil.customArtHeights[i]);
			}
		}
		if (!resources.isEmpty()) {
			Minecraft.getInstance().setScreen(new GUILoliCardAlbum(groupName, resources, widths, heights));
		}
	}

	public static void openCardOnline(ItemStack stack) {
		String url = stack.hasTag() ? stack.getTag().getString("ImageUrl") : "";
		if (!url.isEmpty()) {
			Minecraft.getInstance().setScreen(new GUILoliCardOnline(url));
		}
	}

	public static void openCardOnlineConfig(ItemStack stack) {
		String url = stack.hasTag() ? stack.getTag().getString("ImageUrl") : "";
		Minecraft.getInstance().setScreen(new GUILoliCardOnlineConfig(url));
	}

}
