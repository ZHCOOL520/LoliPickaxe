package com.anotherstar.common.gui;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.20.1 中 {@code IGuiHandler} / {@code NetworkRegistry.INSTANCE.registerGuiHandler} 已被移除，
 * 这里用 DeferredRegister&lt;MenuType&gt; 取代原来的 {@code LoliGUIHandler}：
 * <ul>
 * <li>带有容器的界面（储藏室 / 黑名单 / 密码工作台）注册 MenuType，服务端通过
 * {@code NetworkHooks.openScreen} 打开；</li>
 * <li>纯客户端界面（配置 / 卡片 / 附魔 / 药水 / 空间折叠）保留原来的 GUI ID 常量，
 * 由客户端自行 {@code Minecraft.getInstance().setScreen(...)} 打开。</li>
 * </ul>
 *
 * <p>对应原 1.12.2 类：{@code com.anotherstar.common.gui.LoliGUIHandler}（枚举单例 IGuiHandler）。
 * 原版通过 {@code player.openGui(instance, ID, world, x, y, z)} 一次性完成「服务端建容器 + 客户端建 GUI」，
 * 1.20.1 被拆成服务端 {@code openScreen}（MenuType + AbstractContainerMenu）与客户端
 * {@code MenuScreens.register}（Screen）+ 直接 {@code setScreen} 两条独立路径。
 *
 * <h2>GUI ID 常量与 MenuType 注册名对照表</h2>
 * <table border="1">
 * <caption>ID / 原 LoliGUIHandler 用途 / 1.20.1 打开方式</caption>
 * <tr><th>常量</th><th>值</th><th>界面</th><th>1.20.1 承载方式</th></tr>
 * <tr><td>{@link #GUI_LOLI_CONFIG}</td><td>1</td><td>萝莉镐物品配置（GUILoliConfig）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_CARD}</td><td>2</td><td>单张卡片（GUILoliCard）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_PICKAXE_CONTAINER}</td><td>3</td><td>储藏室（ContainerLoliPickaxe）</td>
 * <td>MenuType 注册名 {@code lolipickaxe:loli_pickaxe_container}，服务端 openScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_PICKAXE_CONTAINER_BLACKLIST}</td><td>4</td><td>黑名单（ContainerBlaceListLoliPickaxe）</td>
 * <td>MenuType 注册名 {@code lolipickaxe:loli_pickaxe_container_blacklist}，服务端 openScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_CARD_ALBUM}</td><td>5</td><td>卡片册（GUILoliCardAlbum）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_CARD_ONLINE}</td><td>6</td><td>网络卡片（GUILoliCardOnline）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_CARD_ONLINE_CONFIG}</td><td>7</td><td>网络卡片 URL 配置（GUILoliCardOnlineConfig）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_ENCHANTMENT}</td><td>8</td><td>附魔编辑（GUILoliEnchantment）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_POTION}</td><td>9</td><td>药水效果编辑（GUILoliPotion）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_LOLI_SPACEF_OLDING}</td><td>10</td><td>空间折叠传送（GUILoliSpaceFolding）</td><td>纯客户端 setScreen</td></tr>
 * <tr><td>{@link #GUI_PASSWORD_WORK_BENCH}</td><td>11</td><td>密码工作台（ContainerPasswordWorkbench）</td>
 * <td>MenuType 注册名 {@code lolipickaxe:password_work_bench}，服务端 openScreen</td></tr>
 * </table>
 *
 * <p>注意：值保持与 1.12.2 完全一致，仅 ID 3 / 4 / 11 真正注册了 MenuType；
 * 其余 ID 只作为 {@code LoliKeyEvent} 等处的语义标记，不再有 IGuiHandler 分发。
 */
public class MenuLoader {

	/** MenuType 延迟注册器，命名空间为本模组 {@code lolipickaxe}；由 CommonProxy 负责挂到 mod 事件总线。 */
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, LoliPickaxe.MODID);

	/** 原 LoliGUIHandler 的 GUI ID，纯客户端界面按 ID 打开时使用。 */
	public static final int GUI_LOLI_CONFIG = 1;
	public static final int GUI_LOLI_CARD = 2;
	public static final int GUI_LOLI_PICKAXE_CONTAINER = 3;
	public static final int GUI_LOLI_PICKAXE_CONTAINER_BLACKLIST = 4;
	public static final int GUI_LOLI_CARD_ALBUM = 5;
	public static final int GUI_LOLI_CARD_ONLINE = 6;
	public static final int GUI_LOLI_CARD_ONLINE_CONFIG = 7;
	public static final int GUI_LOLI_ENCHANTMENT = 8;
	public static final int GUI_LOLI_POTION = 9;
	public static final int GUI_LOLI_SPACEF_OLDING = 10;
	public static final int GUI_PASSWORD_WORK_BENCH = 11;

	/** 萝莉镐储藏室（ContainerLoliPickaxe）。注册名 {@code lolipickaxe:loli_pickaxe_container}，对应 GUI ID 3。 */
	public static final MenuType<ContainerLoliPickaxe> LOLI_PICKAXE_MENU = IForgeMenuType.create(ContainerLoliPickaxe::new);

	/** 萝莉镐黑名单（ContainerBlaceListLoliPickaxe）。注册名 {@code lolipickaxe:loli_pickaxe_container_blacklist}，对应 GUI ID 4。 */
	public static final MenuType<ContainerBlaceListLoliPickaxe> LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU = IForgeMenuType.create(ContainerBlaceListLoliPickaxe::new);

	/** 密码工作台（ContainerPasswordWorkbench）。注册名 {@code lolipickaxe:password_work_bench}，对应 GUI ID 11。 */
	public static final MenuType<ContainerPasswordWorkbench> PASSWORD_WORK_BENCH_MENU = IForgeMenuType.create(ContainerPasswordWorkbench::new);

	static {
		// 注册名必须小写且只含 [a-z0-9/._-]，这里保持与原 LoliGUIHandler 的语义一一对应
		MENUS.register("loli_pickaxe_container", () -> LOLI_PICKAXE_MENU);
		MENUS.register("loli_pickaxe_container_blacklist", () -> LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU);
		MENUS.register("password_work_bench", () -> PASSWORD_WORK_BENCH_MENU);
	}

}
