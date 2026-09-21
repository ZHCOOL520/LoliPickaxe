package com.anotherstar.common.item;

import java.util.List;
import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.entity.EntityLoader;
import com.anotherstar.common.item.tool.ItemLoliPickaxe;
import com.anotherstar.common.item.tool.ItemSmallLoliPickaxe;
import com.google.common.collect.Lists;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册中心，对应原 1.12.2 的 {@code com.anotherstar.common.item.ItemLoader}。
 *
 * <h2>为什么全部改成「延迟构造」（关键修复）</h2>
 *
 * <p>原实现把 21 个物品写成静态字段直接 new：
 * <pre>{@code public final static ItemLoliPickaxe loliPickaxe = new ItemLoliPickaxe();}</pre>
 *
 * <p>静态字段在类首次被引用时由 {@code <clinit>} 初始化，因此一旦有人触碰本类
 * （例如 {@code CommonProxy.preInit()} 引用 {@code ItemLoader.ITEMS}），
 * <b>所有物品会在那一刻被一次性构造</b>。
 *
 * <p>而 {@code Item} 的构造器内部会执行
 * {@code BuiltInRegistries.ITEM.createIntrusiveHolder(this)}，
 * 该调用要求注册表<b>尚未冻结</b>。在触发时机上，物品注册表此时已经冻结，于是抛出：
 *
 * <pre>java.lang.IllegalStateException: Registry is already frozen</pre>
 *
 * <p>这会导致模组加载失败、游戏直接崩溃（在 Forge 47.4.x 上必现）。
 *
 * <h2>修复方式</h2>
 *
 * <p>把「在 {@code <clinit>} 中构造」改为「由 {@link DeferredRegister} 的供应器在
 * 合法窗口内构造」，并缓存实例以保证<b>单例语义不变</b>：
 * <ul>
 *   <li>{@link #ITEMS}{@code .register(名字, 供应器)} —— Forge 只会在允许注册的时刻调用供应器；</li>
 *   <li>供应器内部走 {@code instance()} 懒加载方法，首次调用才 {@code new}，之后复用同一实例。</li>
 * </ul>
 *
 * <p><b>不变的部分</b>：所有注册名字符串与 1.12.2 逐一对应、一个未改；
 * 物品的类型、单例语义、对外提供的获取方法名均保持一致。
 * 本次改动只影响「什么时候 new」，不影响任何玩法逻辑。
 */
public class ItemLoader {

	/** 物品（{@code forge:item}）延迟注册器，命名空间为 {@link LoliPickaxe#MODID}。 */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LoliPickaxe.MODID);

	/*
	 * ========================================================================================
	 * 以下每个物品都遵循同一模式：
	 *   private static X xxxInstance;   ← 唯一的实例持有者，绝不在 <clinit> 中赋值
	 *   public static X xxx() { ... }   ← 懒加载获取（首次调用才 new，之后返回同一实例）
	 * 然后由下方 static 块统一 ITEMS.register("注册名", ItemLoader::xxx)
	 * ========================================================================================
	 */

	private static ItemLoliPickaxe loliPickaxeInstance;

	/** @return 罗莉镐本体（注册名 {@code loli_pickaxe}），全局单例 */
	public static ItemLoliPickaxe loliPickaxe() {
		if (loliPickaxeInstance == null) {
			loliPickaxeInstance = new ItemLoliPickaxe();
		}
		return loliPickaxeInstance;
	}

	private static ItemSmallLoliPickaxe smallLoliPickaxeInstance;

	/** @return 插槽式小罗莉镐（注册名 {@code small_loli_pickaxe}），全局单例 */
	public static ItemSmallLoliPickaxe smallLoliPickaxe() {
		if (smallLoliPickaxeInstance == null) {
			smallLoliPickaxeInstance = new ItemSmallLoliPickaxe();
		}
		return smallLoliPickaxeInstance;
	}

	private static ItemLoliPickaxeMaterial coalAddonInstance;

	/** @return 煤炭附加件（注册名 {@code loli_coal_addon}，10 级，对应 NBT 键 LoliDodge 闪避） */
	public static ItemLoliPickaxeMaterial coalAddon() {
		if (coalAddonInstance == null) {
			coalAddonInstance = new ItemLoliPickaxeMaterial("loliCoalAddon", 10, false);
		}
		return coalAddonInstance;
	}

	private static ItemLoliPickaxeMaterial ironAddonInstance;

	/** @return 铁附加件（注册名 {@code loli_iron_addon}，10 级，LoliDiggingSpeed 挖掘速度） */
	public static ItemLoliPickaxeMaterial ironAddon() {
		if (ironAddonInstance == null) {
			ironAddonInstance = new ItemLoliPickaxeMaterial("loliIronAddon", 10, false);
		}
		return ironAddonInstance;
	}

	private static ItemLoliPickaxeMaterial goldAddonInstance;

	/** @return 金附加件（注册名 {@code loli_gold_addon}，7 级，LoliAttackDamage 攻击伤害） */
	public static ItemLoliPickaxeMaterial goldAddon() {
		if (goldAddonInstance == null) {
			goldAddonInstance = new ItemLoliPickaxeMaterial("loliGoldAddon", 7, false);
		}
		return goldAddonInstance;
	}

	private static ItemLoliPickaxeMaterial redstoneAddonInstance;

	/** @return 红石附加件（注册名 {@code loli_redstone_addon}，4 级，LoliAttackSpeed 攻击速度） */
	public static ItemLoliPickaxeMaterial redstoneAddon() {
		if (redstoneAddonInstance == null) {
			redstoneAddonInstance = new ItemLoliPickaxeMaterial("loliRedstoneAddon", 4, false);
		}
		return redstoneAddonInstance;
	}

	private static ItemLoliPickaxeMaterial lapisAddonInstance;

	/** @return 青金石附加件（注册名 {@code loli_lapis_addon}，6 级，LoliFortuneLevel 时运等级） */
	public static ItemLoliPickaxeMaterial lapisAddon() {
		if (lapisAddonInstance == null) {
			lapisAddonInstance = new ItemLoliPickaxeMaterial("loliLapisAddon", 6, false);
		}
		return lapisAddonInstance;
	}

	private static ItemLoliPickaxeMaterial diamondAddonInstance;

	/** @return 钻石附加件（注册名 {@code loli_diamond_addon}，6 级，LoliDiggingLevel 挖掘等级） */
	public static ItemLoliPickaxeMaterial diamondAddon() {
		if (diamondAddonInstance == null) {
			diamondAddonInstance = new ItemLoliPickaxeMaterial("loliDiamondAddon", 6, false);
		}
		return diamondAddonInstance;
	}

	private static ItemLoliPickaxeMaterial emeraldAddonInstance;

	/** @return 绿宝石附加件（注册名 {@code loli_emerald_addon}，5 级，LoliDiggingRange 挖掘范围） */
	public static ItemLoliPickaxeMaterial emeraldAddon() {
		if (emeraldAddonInstance == null) {
			emeraldAddonInstance = new ItemLoliPickaxeMaterial("loliEmeraldAddon", 5, false);
		}
		return emeraldAddonInstance;
	}

	private static ItemLoliPickaxeMaterial obsidianAddonInstance;

	/** @return 黑曜石附加件（注册名 {@code loli_obsidian_addon}，10 级，LoliAntiInjury 伤害减免） */
	public static ItemLoliPickaxeMaterial obsidianAddon() {
		if (obsidianAddonInstance == null) {
			obsidianAddonInstance = new ItemLoliPickaxeMaterial("loliObsidianAddon", 10, false);
		}
		return obsidianAddonInstance;
	}

	private static ItemLoliPickaxeMaterial glowAddonInstance;

	/** @return 荧石附加件（注册名 {@code loli_glow_addon}，3 级，LoliBuff 增益效果等级） */
	public static ItemLoliPickaxeMaterial glowAddon() {
		if (glowAddonInstance == null) {
			glowAddonInstance = new ItemLoliPickaxeMaterial("loliGlowAddon", 3, false);
		}
		return glowAddonInstance;
	}

	private static ItemLoliPickaxeMaterial quartzAddonInstance;

	/** @return 石英附加件（注册名 {@code loli_quartz_addon}，3 级，LoliHitRange 攻击范围） */
	public static ItemLoliPickaxeMaterial quartzAddon() {
		if (quartzAddonInstance == null) {
			quartzAddonInstance = new ItemLoliPickaxeMaterial("loliQuartzAddon", 3, false);
		}
		return quartzAddonInstance;
	}

	private static ItemLoliPickaxeMaterial netherStarAddonInstance;

	/** @return 下界之星附加件（注册名 {@code loli_nether_star_addon}，5 级，LoliBackpackPage 背包页数） */
	public static ItemLoliPickaxeMaterial netherStarAddon() {
		if (netherStarAddonInstance == null) {
			netherStarAddonInstance = new ItemLoliPickaxeMaterial("loliNetherStarAddon", 5, false);
		}
		return netherStarAddonInstance;
	}

	private static ItemLoliPickaxeMaterial autoFurnaceAddonInstance;

	/** @return 自动熔炼附加件（注册名 {@code loli_auto_furnace_addon}，单子类型，0 表示已启用） */
	public static ItemLoliPickaxeMaterial autoFurnaceAddon() {
		if (autoFurnaceAddonInstance == null) {
			autoFurnaceAddonInstance = new ItemLoliPickaxeMaterial("loliAutoFurnaceAddon", 1, false);
		}
		return autoFurnaceAddonInstance;
	}

	private static ItemLoliPickaxeMaterial flyAddonInstance;

	/** @return 飞行附加件（注册名 {@code loli_fly_addon}，单子类型，0 表示已启用） */
	public static ItemLoliPickaxeMaterial flyAddon() {
		if (flyAddonInstance == null) {
			flyAddonInstance = new ItemLoliPickaxeMaterial("loliFlyAddon", 1, false);
		}
		return flyAddonInstance;
	}

	private static ItemLoliPickaxeMaterial entitySoulInstance;

	/**
	 * @return 生物灵魂附加件（注册名 {@code loli_entity_soul_addon}，7 级）；
	 *         {@code differentEnd=true} 使最后一级使用独立贴图（击杀生物掉落）
	 */
	public static ItemLoliPickaxeMaterial entitySoul() {
		if (entitySoulInstance == null) {
			entitySoulInstance = new ItemLoliPickaxeMaterial("loliEntitySoulAddon", 7, true);
		}
		return entitySoulInstance;
	}

	private static ItemLoliDispersal loliDispersalInstance;

	/** @return 让罗莉消散的道具（注册名 {@code loli_dispersal}） */
	public static ItemLoliDispersal loliDispersal() {
		if (loliDispersalInstance == null) {
			loliDispersalInstance = new ItemLoliDispersal();
		}
		return loliDispersalInstance;
	}

	private static ItemBugEntityClear bugEntityClearInstance;

	/** @return 卡死实体清除器（注册名 {@code bug_entity_clear}） */
	public static ItemBugEntityClear bugEntityClear() {
		if (bugEntityClearInstance == null) {
			bugEntityClearInstance = new ItemBugEntityClear();
		}
		return bugEntityClearInstance;
	}

	private static ForgeSpawnEggItem loliSpawnEggInstance;

	/**
	 * 萝莉刷怪蛋（注册名 {@code loli_spawn_egg}）。
	 *
	 * <p>对应 1.12.2 的 {@code EntityRegistry.registerEgg(lolipickaxe:loli, 0xFFFFFF, 0x000000)}。
	 * 该调用在移植到 1.20.1 时被遗漏，导致萝莉实体没有任何合法的生成方式。
	 * 颜色值与 1.12.2 完全一致：主色 {@code 0xFFFFFF}、副色 {@code 0x000000}。
	 *
	 * <p>注意：1.12.2 只给 {@code loli} 注册了刷怪蛋，{@code loli_buff_attack_tnt} 是 TNT 实体、
	 * 从来没有刷怪蛋，因此这里也不为它添加。
	 *
	 * @return 萝莉刷怪蛋物品（全局单例）
	 */
	public static ForgeSpawnEggItem loliSpawnEgg() {
		if (loliSpawnEggInstance == null) {
			loliSpawnEggInstance = new ForgeSpawnEggItem(EntityLoader::LOLI_TYPE, 0xFFFFFF, 0x000000, new Item.Properties());
		}
		return loliSpawnEggInstance;
	}

	private static ItemLoliCard loliCardInstance;

	/** @return 单张立绘卡片（注册名 {@code loli_card}） */
	public static ItemLoliCard loliCard() {
		if (loliCardInstance == null) {
			loliCardInstance = new ItemLoliCard();
		}
		return loliCardInstance;
	}

	private static ItemLoliCardAlbum loliCardAlbumInstance;

	/** @return 立绘卡册（注册名 {@code loli_card_album}，按组） */
	public static ItemLoliCardAlbum loliCardAlbum() {
		if (loliCardAlbumInstance == null) {
			loliCardAlbumInstance = new ItemLoliCardAlbum();
		}
		return loliCardAlbumInstance;
	}

	private static ItemLoliCardOnline loliCardOnlineInstance;

	/** @return 在线立绘卡片（注册名 {@code loli_card_online}） */
	public static ItemLoliCardOnline loliCardOnline() {
		if (loliCardOnlineInstance == null) {
			loliCardOnlineInstance = new ItemLoliCardOnline();
		}
		return loliCardOnlineInstance;
	}

	/** 运行时按配置动态生成的唱片物品，注册名由配置项决定。 */
	public static final List<ItemLoliRecord> loliRecords = Lists.newArrayList();

	/**
	 * 所有升级材料物品的注册名清单，供客户端统一登记模型属性使用
	 * （见 {@code com.anotherstar.client.event.ItemModelPropertyLoader}）。
	 *
	 * <p>这里保存的是注册名而不是物品实例：因为物品实例是懒加载构造的，
	 * 客户端注册事件触发时未必已经全部构造完成；用注册名可以在需要时再解析。
	 */
	private static final List<String> MATERIAL_NAMES = Lists.newArrayList("loli_coal_addon", "loli_iron_addon", "loli_gold_addon", "loli_redstone_addon", "loli_lapis_addon", "loli_diamond_addon", "loli_emerald_addon", "loli_obsidian_addon", "loli_glow_addon", "loli_quartz_addon", "loli_nether_star_addon", "loli_auto_furnace_addon", "loli_fly_addon", "loli_entity_soul_addon");

	/**
	 * 登记一个升级材料物品，并记录其注册名 → 注册对象的映射。
	 *
	 * <p>与 {@code ITEMS.register(...)} 的唯一区别是额外写入 {@link #MATERIALS}，
	 * 使客户端能够遍历到所有材料实例，从而批量注册 {@code lolipickaxe:end} 模型属性。
	 * 注册名与供应器语义与直接调用 {@code register} 完全一致。
	 *
	 * @param name     注册名（如 {@code loli_coal_addon}）
	 * @param supplier 物品构造供应器
	 */
	private static void registerMaterial(String name, Supplier<? extends Item> supplier) {
		MATERIALS.put(name, ITEMS.register(name, supplier));
	}

	/**
	 * 解析所有已注册的升级材料物品实例。
	 *
	 * @return 材料物品实例列表；尚未注册完成的项会被跳过（不会返回 {@code null} 元素）
	 */
	public static List<Item> allMaterials() {
		List<Item> list = Lists.newArrayList();
		for (String name : MATERIAL_NAMES) {
			RegistryObject<Item> holder = MATERIALS.get(name);
			if (holder != null && holder.isPresent()) {
				list.add(holder.get());
			}
		}
		return list;
	}

	/**
	 * 升级材料的名称 → 注册对象映射，在静态块中随注册一同建立。
	 * <p>
	 * 用途：让客户端能够遍历到所有材料实例以登记模型属性，
	 * 而不必在客户端硬编码具体是哪一个物品。
	 */
	public static final java.util.Map<String, RegistryObject<Item>> MATERIALS = new java.util.HashMap<>();

	static {
		// 以下注册名与 1.12.2 中 setRegistryName 传入的字符串逐一对应，保持升级后世界/物品 ID 不变。
		// 注意：这里只登记「怎么造」，绝不在这里真的造 —— 构造被推迟到 Forge 调用供应器时，
		// 那时注册表尚未冻结，createIntrusiveHolder 才不会抛 "Registry is already frozen"。
		ITEMS.register("loli_pickaxe", ItemLoader::loliPickaxe);
		ITEMS.register("small_loli_pickaxe", ItemLoader::smallLoliPickaxe);
		// 材料类物品：统一用 registerMaterial 登记，同时记录到 MATERIALS 映射，
		// 供客户端批量注册模型属性（lolipickaxe:end）使用。
		registerMaterial("loli_coal_addon", ItemLoader::coalAddon);
		registerMaterial("loli_iron_addon", ItemLoader::ironAddon);
		registerMaterial("loli_gold_addon", ItemLoader::goldAddon);
		registerMaterial("loli_redstone_addon", ItemLoader::redstoneAddon);
		registerMaterial("loli_lapis_addon", ItemLoader::lapisAddon);
		registerMaterial("loli_diamond_addon", ItemLoader::diamondAddon);
		registerMaterial("loli_emerald_addon", ItemLoader::emeraldAddon);
		registerMaterial("loli_obsidian_addon", ItemLoader::obsidianAddon);
		registerMaterial("loli_glow_addon", ItemLoader::glowAddon);
		registerMaterial("loli_quartz_addon", ItemLoader::quartzAddon);
		registerMaterial("loli_nether_star_addon", ItemLoader::netherStarAddon);
		registerMaterial("loli_auto_furnace_addon", ItemLoader::autoFurnaceAddon);
		registerMaterial("loli_fly_addon", ItemLoader::flyAddon);
		registerMaterial("loli_entity_soul_addon", ItemLoader::entitySoul);
		ITEMS.register("loli_dispersal", ItemLoader::loliDispersal);
		ITEMS.register("bug_entity_clear", ItemLoader::bugEntityClear);
		// 萝莉刷怪蛋：1.12.2 用 EntityRegistry.registerEgg 注册，移植时遗漏，此处补回。
		ITEMS.register("loli_spawn_egg", ItemLoader::loliSpawnEgg);
		ITEMS.register("loli_card", ItemLoader::loliCard);
		ITEMS.register("loli_card_album", ItemLoader::loliCardAlbum);
		ITEMS.register("loli_card_online", ItemLoader::loliCardOnline);
		// 配置此时可能尚未加载，未加载时使用注解默认值
		List<String> names = ConfigLoader.loliRecodeNames == null || ConfigLoader.loliRecodeNames.isEmpty() ? Lists.newArrayList("lolirecord:loliRecord:loli_record") : ConfigLoader.loliRecodeNames;
		for (String loliRecordName : names) {
			// 每条配置的格式为「声音事件名:唱片显示名:唱片注册名」，即 name[0]=声音、name[1]=显示名、name[2]=注册名
			String[] name = loliRecordName.split(":");
			// 这里同样只登记供应器：唱片的实际构造推迟到注册窗口内进行
			Supplier<Item> recordSupplier = () -> {
				ItemLoliRecord record = new ItemLoliRecord(name[1], name[0]);
				// 保持与原实现一致：构造完成后登记进 loliRecords 列表，供其它代码遍历使用
				if (!loliRecords.contains(record)) {
					loliRecords.add(record);
				}
				return record;
			};
			ITEMS.register(name[2], recordSupplier);
		}
	}

}
