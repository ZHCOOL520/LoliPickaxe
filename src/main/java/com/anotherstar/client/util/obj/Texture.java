package com.anotherstar.client.util.obj;

import net.minecraft.resources.ResourceLocation;

/**
 * OBJ 材质定义（mtl）。原实现直接调用 GL11 的材质设置，
 * 1.20.1 固定管线已移除，这里只保留解析结果供渲染层使用。
 */
public class Texture {

	public String name;
	public ResourceLocation texture;
	public float[] ka;
	public float[] kd;
	public float[] ks;
	public float[] ns;

	public Texture(String name) {
		this(name, null, null, null, null);
	}

	public Texture(String name, ResourceLocation texture) {
		this(name, texture, null, null, null);
	}

	public Texture(String name, ResourceLocation texture, float[] ka, float[] kd, float[] ks) {
		this.name = name;
		this.texture = texture;
		this.ka = ka;
		this.kd = kd;
		this.ks = ks;
		this.ns = null;
	}

	public Texture(String name, ResourceLocation texture, float[] ka, float[] kd, float[] ks, float ns) {
		this(name, texture, ka, kd, ks);
		this.ns = new float[] { ns };
	}

}
