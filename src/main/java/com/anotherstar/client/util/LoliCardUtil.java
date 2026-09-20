package com.anotherstar.client.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.anotherstar.common.LoliPickaxe;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * 萝莉卡片图片列表。
 *
 * <p>1.12.2 直接扫描模组 jar / 类路径与资源包目录收集 lolicards/*.png；
 * 1.20.1 优先使用资源管理器（ResourceManager.listResources），
 * 但 ResourceLocation 只允许 {@code [a-z0-9/._-]}，
 * 原始资源中形如 {@code 小莫女儿''1.png}、{@code 召唤祭坛摆放方式.png} 的文件名会被资源管理器忽略，
 * 因此这些文件仍按原做法从模组自身读取并注册成动态纹理，以保证卡片册分组（' 前缀）逻辑不变。
 */
public class LoliCardUtil {

	public static String[] customArtNames = null;
	public static int[] customArtHeights = null;
	public static int[] customArtWidths = null;
	public static ResourceLocation[] customArtResources = null;

	private static final String CARD_DIR = "lolicards";
	private static final String ASSET_ROOT = "assets/" + LoliPickaxe.MODID + "/" + CARD_DIR + "/";

	private static int dynamicIndex = 0;

	private static final List<ResourceLocation> dynamicTextures = new ArrayList<>();

	public static void updateCustomArtDatas() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getResourceManager() == null) {
			return;
		}
		// 每次重载都会为名字非法的卡片重新注册动态纹理，这里先释放上一批，避免显存泄漏
		for (ResourceLocation texture : dynamicTextures) {
			mc.getTextureManager().release(texture);
		}
		dynamicTextures.clear();
		List<CardArt> arts = new ArrayList<>();
		for (Map.Entry<ResourceLocation, Resource> entry : mc.getResourceManager().listResources(CARD_DIR, location -> location.getPath().endsWith(".png")).entrySet()) {
			String name = entry.getKey().getPath().substring(CARD_DIR.length() + 1);
			try (InputStream in = entry.getValue().open()) {
				try (NativeImage image = NativeImage.read(in)) {
					arts.add(new CardArt(name, entry.getKey(), image.getWidth(), image.getHeight()));
				}
			} catch (IOException e) {
				LoliPickaxe.LOGGER.error("Failed to read loli card art {}", entry.getKey(), e);
			}
		}
		for (String name : listOwnCardFiles()) {
			if (findArt(arts, name) != null) {
				continue;
			}
			try (InputStream in = LoliPickaxe.class.getResourceAsStream("/" + ASSET_ROOT + name)) {
				if (in == null) {
					continue;
				}
				NativeImage image = NativeImage.read(in);
				ResourceLocation texture = mc.getTextureManager().register("loli_card_art_" + (dynamicIndex++), new DynamicTexture(image));
				dynamicTextures.add(texture);
				arts.add(new CardArt(name, texture, image.getWidth(), image.getHeight()));
			} catch (IOException e) {
				LoliPickaxe.LOGGER.error("Failed to read loli card art {}", name, e);
			}
		}
		arts.sort(Comparator.comparing(art -> art.name));
		customArtNames = new String[arts.size()];
		customArtResources = new ResourceLocation[arts.size()];
		customArtWidths = new int[arts.size()];
		customArtHeights = new int[arts.size()];
		for (int i = 0; i < arts.size(); i++) {
			CardArt art = arts.get(i);
			customArtNames[i] = art.name;
			customArtResources[i] = art.resource;
			customArtWidths[i] = art.width;
			customArtHeights[i] = art.height;
		}
	}

	private static CardArt findArt(List<CardArt> arts, String name) {
		for (CardArt art : arts) {
			if (art.name.equals(name)) {
				return art;
			}
		}
		return null;
	}

	/**
	 * 列出模组自身 assets/lolipickaxe/lolicards 下的 png 文件名（含 ResourceLocation 非法的中文名）。
	 */
	private static List<String> listOwnCardFiles() {
		List<String> names = new ArrayList<>();
		URL url = LoliPickaxe.class.getResource("/" + ASSET_ROOT);
		if (url == null) {
			return names;
		}
		try {
			if ("file".equals(url.getProtocol())) {
				File[] files = new File(url.toURI()).listFiles();
				if (files != null) {
					for (File file : files) {
						if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
							names.add(file.getName());
						}
					}
				}
			} else if ("jar".equals(url.getProtocol())) {
				JarURLConnection connection = (JarURLConnection) url.openConnection();
				File source = new File(connection.getJarFileURL().toURI());
				try (JarFile jar = new JarFile(source)) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						String entryName = entries.nextElement().getName();
						if (entryName.startsWith(ASSET_ROOT) && entryName.toLowerCase(Locale.ROOT).endsWith(".png")) {
							String name = entryName.substring(ASSET_ROOT.length());
							if (!name.contains("/")) {
								names.add(name);
							}
						}
					}
				}
			}
		} catch (URISyntaxException e) {
			LoliPickaxe.LOGGER.error("Failed to locate loli card art directory", e);
		} catch (IOException e) {
			LoliPickaxe.LOGGER.error("Failed to list loli card art", e);
		}
		return names;
	}

	private static class CardArt {

		private final String name;
		private final ResourceLocation resource;
		private final int width;
		private final int height;

		private CardArt(String name, ResourceLocation resource, int width, int height) {
			this.name = name;
			this.resource = resource;
			this.width = width;
			this.height = height;
		}

	}

}
