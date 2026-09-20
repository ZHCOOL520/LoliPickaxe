package com.anotherstar.client.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.anotherstar.common.LoliPickaxe;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 萝莉网络卡片的运行时贴图。
 *
 * <p>1.12.2 使用 TextureUtil.glGenTextures + uploadTextureImage 直接操作 GL 纹理 id；
 * 1.20.1 改为注册 {@link DynamicTexture} 并把得到的 {@link ResourceLocation} 交给渲染层使用
 * （RenderType 会自行按 ResourceLocation 绑定贴图）。
 */
public class LoliCardOnlineUtil {

	private static final Set<String> loading = Sets.newHashSet();
	private static final Map<String, ResourceLocation> urlToTexture = Maps.newHashMap();
	private static final Map<String, Integer> urlToWidth = Maps.newHashMap();
	private static final Map<String, Integer> urlToHeight = Maps.newHashMap();
	private static final AtomicInteger index = new AtomicInteger();

	public static boolean isLoad(String url) {
		return urlToTexture.containsKey(url);
	}

	public static void load(String url) {
		if (loading.contains(url)) {
			return;
		}
		add(url);
		new Thread(() -> {
			try {
				URLConnection connection = new URL(url).openConnection();
				connection.setDoOutput(true);
				NativeImage image;
				try (InputStream in = connection.getInputStream()) {
					image = NativeImage.read(in);
				}
				Minecraft.getInstance().execute(() -> {
					ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("loli_card_online_" + index.incrementAndGet(), new DynamicTexture(image));
					urlToTexture.put(url, texture);
					urlToWidth.put(url, image.getWidth());
					urlToHeight.put(url, image.getHeight());
					remove(url);
				});
			} catch (Exception e) {
				remove(url);
				LoliPickaxe.LOGGER.error("Failed to load loli card online image {}", url, e);
			}
		}).start();
	}

	public synchronized static void add(String url) {
		loading.add(url);
	}

	public synchronized static void remove(String url) {
		loading.remove(url);
	}

	public static void unload(String url) {
		ResourceLocation texture = urlToTexture.remove(url);
		if (texture != null) {
			Minecraft.getInstance().getTextureManager().release(texture);
		}
	}

	/**
	 * 绑定贴图到 0 号贴图单元（保留原方法名，供仍按旧写法绘制的地方调用）。
	 */
	public static void bind(String url) {
		ResourceLocation texture = urlToTexture.get(url);
		if (texture != null) {
			RenderSystem.setShaderTexture(0, texture);
		}
	}

	public static ResourceLocation getTexture(String url) {
		return urlToTexture.get(url);
	}

	public static int getWidth(String url) {
		if (urlToWidth.containsKey(url)) {
			return urlToWidth.get(url);
		}
		return 0;
	}

	public static int getHeight(String url) {
		if (urlToHeight.containsKey(url)) {
			return urlToHeight.get(url);
		}
		return 0;
	}

}
