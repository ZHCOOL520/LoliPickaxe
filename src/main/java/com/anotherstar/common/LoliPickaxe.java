package com.anotherstar.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.anotherstar.client.ClientProxy;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.network.NetworkHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 模组主入口类（对应 1.12.2 的 {@code @Mod} 主类 {@code LoliPickaxe}）。
 * <p>
 * 在 1.20.1 Forge 中，模组通过 {@link Mod} 注解声明，并由 Forge 在模组构造阶段实例化；
 * 原有 1.12.2 的 {@code @Mod(modid = ..., name = ..., version = ...)} + {@code @Mod.EventHandler} 回调体系
 * 被替换为「构造器 + {@code modBus.addListener(...)}」的事件监听注册方式。
 * <p>
 * 职责：
 * <ul>
 *   <li>注册配置文件（{@code ForgeConfigSpec} 取代 1.12.2 的 {@code Configuration}）；</li>
 *   <li>根据运行端选择客户端/通用代理（{@code DistExecutor} 取代 1.12.2 的 {@code @SidedProxy}）；</li>
 *   <li>触发各注册器的 {@code DeferredRegister} 注册与事件总线监听器注册；</li>
 *   <li>初始化网络通道。</li>
 * </ul>
 */
@Mod(LoliPickaxe.MODID)
public class LoliPickaxe {

	/** 模组 ID，用于资源命名空间（resource location）与配置文件命名，必须与 mods.toml 保持一致。 */
	public static final String MODID = "lolipickaxe";
	/** 模组显示名称，仅用于日志与元数据展示。 */
	public static final String NAME = "LoliPickaxe Mod";
	/** 模组版本号；1.12.2 中该值由 mcmod.info / @Mod 注解携带，此处改为代码常量。 */
	public static final String VERSION = "1.2.16f";

	/** 全局日志器，供整个模组统一输出日志。 */
	public static final Logger LOGGER = LogManager.getLogger(NAME);

	/**
	 * 当前运行端的代理实例：客户端为 {@code ClientProxy}，服务端/专用服务器为 {@link CommonProxy}。
	 * 静态字段以便全局（含静态注册流程）访问，在构造器中被赋值。
	 */
	public static CommonProxy proxy;

	/**
	 * 模组构造器：由 Forge 在模组加载早期调用（此时注册表尚未冻结，可安全注册各种对象）。
	 * <p>
	 * 顺序说明：必须先注册配置、再创建代理并调用 {@code preInit}，
	 * 因为 {@code preInit} 中会向事件总线注册 {@code DeferredRegister}，
	 * 而这些注册器的注册表条目会在后续的 {@link FMLCommonSetupEvent} 之前完成。
	 */
	public LoliPickaxe() {
		// 获取模组事件总线（mod bus）：与 MinecraftForge.EVENT_BUS 不同，它只分发模组生命周期与注册事件
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		// 注册通用（COMMON）配置：ConfigLoader.init() 构建 ForgeConfigSpec，文件名对应 1.12.2 的 lolipickaxe.cfg
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigLoader.init(), "lolipickaxe-common.toml");

		// 端侧代理：unsafeRunForDist 会在物理客户端返回第一个工厂结果，否则返回第二个，
		// 以此保证 ClientProxy 类不会在服务端被加载（等价于 1.12.2 的 @SidedProxy）
		proxy = DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
		// 预初始化：注册物品/方块/实体/附魔/菜单/配方序列化器等
		proxy.preInit(modBus);

		// 生命周期与配置事件监听（方法引用注册，取代 1.12.2 的 @Mod.EventHandler 注解方法）
		modBus.addListener(this::commonSetup);
		modBus.addListener(this::onConfigLoad);
		modBus.addListener(this::onConfigReload);

		// 仅客户端执行的初始化钩子（例如屏幕、渲染器注册），避免在服务端触发客户端类加载
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.clientSetup(modBus));

		// 初始化 SimpleChannel 网络通道（取代 1.12.2 的 SimpleNetworkWrapper）
		NetworkHandler.init();
	}

	/**
	 * 通用初始化阶段回调（Forge 的 {@link FMLCommonSetupEvent}）。
	 * <p>
	 * 对应 1.12.2 的 {@code FMLCommonSetupEvent} / {@code FMLInitializationEvent} 阶段，
	 * 此时所有注册表条目已完成注册，适合做跨端的后期绑定（如事件总线的业务监听器注册）。
	 *
	 * @param event Forge 的通用初始化事件（本方法未直接使用其内容，仅作为阶段触发点）
	 */
	private void commonSetup(FMLCommonSetupEvent event) {
		proxy.init();
	}

	/**
	 * 配置首次加载完成回调（{@link ModConfigEvent.Loading}）。
	 * <p>
	 * 该事件在配置文件被读取（首次加载）后触发，此时才能安全读取配置值并写入静态字段。
	 *
	 * @param event 配置加载事件，可由 {@code event.getConfig()} 获取具体配置对象
	 */
	private void onConfigLoad(ModConfigEvent.Loading event) {
		proxy.onConfigLoad();
	}

	/**
	 * 配置重载回调（{@link ModConfigEvent.Reloading}）。
	 * <p>
	 * 玩家执行 {@code /reload} 或外部修改配置文件后触发，用于把新的配置值刷回静态字段，
	 * 相当于 1.12.2 的 {@code Configuration} 热重载逻辑。
	 *
	 * @param event 配置重载事件
	 */
	private void onConfigReload(ModConfigEvent.Reloading event) {
		proxy.onConfigReload();
	}

}
