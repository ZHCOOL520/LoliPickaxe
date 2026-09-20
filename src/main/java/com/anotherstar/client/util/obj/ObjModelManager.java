package com.anotherstar.client.util.obj;

import java.util.concurrent.ExecutionException;

import com.anotherstar.common.LoliPickaxe;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * OBJ 模型缓存。
 *
 * <p>1.12.2 在类加载时就创建默认模型，1.20.1 里资源管理器在客户端初始化完成后才可用，
 * 因此改为首次使用时再解析，重载时仅清空缓存。
 */
public class ObjModelManager {

	public static final ResourceLocationRaw DEFAULT_MODEL_RESOURCE = new ResourceLocationRaw(LoliPickaxe.MODID, "models/entity/loli/loli.obj");

	private static WavefrontObject defaultModel;

	private static final LoadingCache<ResourceLocationRaw, WavefrontObject> CACHE = CacheBuilder.newBuilder().build(new CacheLoader<ResourceLocationRaw, WavefrontObject>() {

		@Override
		public WavefrontObject load(ResourceLocationRaw key) throws Exception {
			try {
				return new WavefrontObject(key);
			} catch (Exception e) {
				return getDefaultModel();
			}
		}

	});

	public static void reload() {
		defaultModel = null;
		CACHE.invalidateAll();
	}

	public static WavefrontObject getModel(ResourceLocationRaw loc) {
		try {
			return CACHE.get(loc);
		} catch (ExecutionException e) {
			LoliPickaxe.LOGGER.error("Failed to load obj model {}", loc, e);
			return getDefaultModel();
		}
	}

	public static WavefrontObject getDefaultModel() {
		if (defaultModel == null) {
			defaultModel = new WavefrontObject(DEFAULT_MODEL_RESOURCE);
		}
		return defaultModel;
	}

}
