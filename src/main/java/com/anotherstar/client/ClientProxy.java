package com.anotherstar.client;

import com.anotherstar.client.event.LoliCardAlbumSwitchEvent;
import com.anotherstar.client.event.LoliKeyEvent;
import com.anotherstar.client.event.LoliPickaxeAntiClientRemoveEntity;
import com.anotherstar.client.event.LoliPickaxeRenderPlayerEvent;
import com.anotherstar.client.event.LoliPickaxeTooltipEvent;
import com.anotherstar.client.event.ResourcePackReloadEvent;
import com.anotherstar.client.gui.GUIContainerBlaceListLoliPickaxe;
import com.anotherstar.client.gui.GUIContainerLoliPickaxe;
import com.anotherstar.client.gui.GUIPasswordCrafting;
import com.anotherstar.client.key.KeyLoader;
import com.anotherstar.client.model.ModelLoli;
import com.anotherstar.client.model.ModelNevermore;
import com.anotherstar.client.model.ModelPaperLoli;
import com.anotherstar.client.render.RenderLoli;
import com.anotherstar.client.render.RenderLoliBuffAttackTNT;
import com.anotherstar.client.render.RenderLoliItem;
import com.anotherstar.common.CommonProxy;
import com.anotherstar.common.entity.EntityLoader;
import com.anotherstar.common.gui.MenuLoader;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端代理。
 *
 * <p>1.12.2 通过 preInit/init/postInit 三个生命周期事件注册客户端内容；
 * 1.20.1 由主类通过 {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 调用本类的静态
 * {@link #clientSetup(IEventBus)}，语义与原 init 阶段一致。
 */
public class ClientProxy extends CommonProxy {

	public ClientProxy() {
	}

	public static void clientSetup(IEventBus modBus) {
		// preInit 语义：键位、实体渲染器与模型层定义
		KeyLoader.init(modBus);
		modBus.addListener(ClientProxy::onRegisterRenderers);
		modBus.addListener(ClientProxy::onRegisterLayerDefinitions);
		modBus.addListener(ClientProxy::onClientSetup);
		modBus.addListener(ResourcePackReloadEvent::onRegisterReloadListeners);
		// init 语义：客户端事件
		MinecraftForge.EVENT_BUS.register(new LoliKeyEvent());
		MinecraftForge.EVENT_BUS.register(new LoliPickaxeTooltipEvent());
		MinecraftForge.EVENT_BUS.register(new LoliCardAlbumSwitchEvent());
		MinecraftForge.EVENT_BUS.register(new LoliPickaxeAntiClientRemoveEntity());
		MinecraftForge.EVENT_BUS.register(new LoliPickaxeRenderPlayerEvent());
		// 卡片图片与 OBJ 模型数据在 ResourcePackReloadEvent（客户端资源重载）里刷新；
		// 卡片物品渲染器使用懒加载，首次渲染时创建。
		RenderLoliItem.init();
	}

	private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(EntityLoader.LOLI_TYPE, context -> new RenderLoli(context, new ModelLoli(context.bakeLayer(ModelLoli.LAYER_LOCATION)), 0.3F));
		event.registerEntityRenderer(EntityLoader.LOLI_BUFF_ATTACK_TNT_TYPE, RenderLoliBuffAttackTNT::new);
	}

	private static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelLoli.LAYER_LOCATION, ModelLoli::createBodyLayer);
		event.registerLayerDefinition(ModelPaperLoli.LAYER_LOCATION, ModelPaperLoli::createBodyLayer);
		event.registerLayerDefinition(ModelNevermore.LAYER_LOCATION, ModelNevermore::createBodyLayer);
	}

	/** 容器界面注册（1.12.2 由 IGuiHandler 承担）。 */
	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(MenuLoader.LOLI_PICKAXE_MENU, GUIContainerLoliPickaxe::new);
			MenuScreens.register(MenuLoader.LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU, GUIContainerBlaceListLoliPickaxe::new);
			MenuScreens.register(MenuLoader.PASSWORD_WORK_BENCH_MENU, GUIPasswordCrafting::new);
		});
	}

}
