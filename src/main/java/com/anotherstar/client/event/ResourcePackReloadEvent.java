package com.anotherstar.client.event;

import com.anotherstar.client.util.LoliCardUtil;
import com.anotherstar.client.util.obj.ObjModelManager;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

/**
 * 1.12.2 通过 TextureStitchEvent.Post 在贴图集重建后刷新卡片与 OBJ 模型数据；
 * 1.20.1 改为注册客户端资源重载监听器（首次加载与 F3+T 都会触发）。
 */
public class ResourcePackReloadEvent implements ResourceManagerReloadListener {

	public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new ResourcePackReloadEvent());
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		LoliCardUtil.updateCustomArtDatas();
		ObjModelManager.reload();
	}

}
