package com.anotherstar.common.item;

import java.util.List;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.tool.ItemLoliPickaxe;
import com.anotherstar.common.item.tool.ItemSmallLoliPickaxe;
import com.google.common.collect.Lists;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 物品注册中心，对应原 1.12.2 的 {@code com.anotherstar.common.item.ItemLoader}。
 * <p>
 * 1.12.2 是在 {@code RegistryEvent.Register<Item>} 里逐个调用
 * {@code event.getRegistry().register(item.setRegistryName(MODID, name))}；
 * 1.20.1 改为 {@link DeferredRegister}，注册名在下方静态块中统一声明，
 * 由模组主类取用 {@link #ITEMS} 并挂到 mod 事件总线上完成实际注册。
 * <p>
 * 除动态生成的唱片外，本类所有静态字段都是「单例物品实例」，其注册名见各字段行尾注释。
 */
public class ItemLoader {

	/** 物品（{@code forge:item}）延迟注册器，命名空间为 {@link LoliPickaxe#MODID}。 */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LoliPickaxe.MODID);

	public final static ItemLoliPickaxe loliPickaxe = new ItemLoliPickaxe(); // 注册名 loli_pickaxe：罗莉镐本体
	public final static ItemSmallLoliPickaxe smallLoliPickaxe = new ItemSmallLoliPickaxe(); // 注册名 small_loli_pickaxe：插槽式小罗莉镐
	public final static ItemLoliPickaxeMaterial coalAddon = new ItemLoliPickaxeMaterial("loliCoalAddon", 10, false); // 注册名 loli_coal_addon，10 级子类型（对应 NBT 键 LoliDodge 闪避）
	public final static ItemLoliPickaxeMaterial ironAddon = new ItemLoliPickaxeMaterial("loliIronAddon", 10, false); // 注册名 loli_iron_addon，10 级子类型（LoliDiggingSpeed 挖掘速度）
	public final static ItemLoliPickaxeMaterial goldAddon = new ItemLoliPickaxeMaterial("loliGoldAddon", 7, false); // 注册名 loli_gold_addon，7 级子类型（LoliAttackDamage 攻击伤害）
	public final static ItemLoliPickaxeMaterial redstoneAddon = new ItemLoliPickaxeMaterial("loliRedstoneAddon", 4, false); // 注册名 loli_redstone_addon，4 级子类型（LoliAttackSpeed 攻击速度）
	public final static ItemLoliPickaxeMaterial lapisAddon = new ItemLoliPickaxeMaterial("loliLapisAddon", 6, false); // 注册名 loli_lapis_addon，6 级子类型（LoliFortuneLevel 时运等级）
	public final static ItemLoliPickaxeMaterial diamondAddon = new ItemLoliPickaxeMaterial("loliDiamondAddon", 6, false); // 注册名 loli_diamond_addon，6 级子类型（LoliDiggingLevel 挖掘等级）
	public final static ItemLoliPickaxeMaterial emeraldAddon = new ItemLoliPickaxeMaterial("loliEmeraldAddon", 5, false); // 注册名 loli_emerald_addon，5 级子类型（LoliDiggingRange 挖掘范围）
	public final static ItemLoliPickaxeMaterial obsidianAddon = new ItemLoliPickaxeMaterial("loliObsidianAddon", 10, false); // 注册名 loli_obsidian_addon，10 级子类型（LoliAntiInjury 伤害减免）
	public final static ItemLoliPickaxeMaterial glowAddon = new ItemLoliPickaxeMaterial("loliGlowAddon", 3, false); // 注册名 loli_glow_addon，3 级子类型（LoliBuff 增益效果等级）
	public final static ItemLoliPickaxeMaterial quartzAddon = new ItemLoliPickaxeMaterial("loliQuartzAddon", 3, false); // 注册名 loli_quartz_addon，3 级子类型（LoliHitRange 攻击范围）
	public final static ItemLoliPickaxeMaterial netherStarAddon = new ItemLoliPickaxeMaterial("loliNetherStarAddon", 5, false); // 注册名 loli_nether_star_addon，5 级子类型（LoliBackpackPage 背包页数）
	public final static ItemLoliPickaxeMaterial autoFurnaceAddon = new ItemLoliPickaxeMaterial("loliAutoFurnaceAddon", 1, false); // 注册名 loli_auto_furnace_addon，单子类型（LoliAutoFurnace 自动熔炼，0 表示已启用）
	public final static ItemLoliPickaxeMaterial flyAddon = new ItemLoliPickaxeMaterial("loliFlyAddon", 1, false); // 注册名 loli_fly_addon，单子类型（LoliFly 飞行，0 表示已启用）
	public final static ItemLoliPickaxeMaterial entitySoul = new ItemLoliPickaxeMaterial("loliEntitySoulAddon", 7, true); // 注册名 loli_entity_soul_addon，7 级子类型；differentEnd=true 使最后一级使用独立贴图（击杀生物掉落）
	public final static ItemLoliDispersal loliDispersal = new ItemLoliDispersal(); // 注册名 loli_dispersal：让罗莉消散
	public final static ItemBugEntityClear bugEntityClear = new ItemBugEntityClear(); // 注册名 bug_entity_clear：卡死实体清除器
	public final static ItemLoliCard loliCard = new ItemLoliCard(); // 注册名 loli_card：单张立绘卡片
	public final static ItemLoliCardAlbum loliCardAlbum = new ItemLoliCardAlbum(); // 注册名 loli_card_album：立绘卡册（按组）
	public final static ItemLoliCardOnline loliCardOnline = new ItemLoliCardOnline(); // 注册名 loli_card_online：在线立绘卡片
	public final static List<ItemLoliRecord> loliRecords = Lists.newArrayList(); // 运行时按配置动态生成的唱片物品，注册名由配置项决定

	static {
		// 以下注册名与 1.12.2 中 setRegistryName 传入的字符串逐一对应，保持升级后世界/物品 ID 不变
		ITEMS.register("loli_pickaxe", () -> loliPickaxe);
		ITEMS.register("small_loli_pickaxe", () -> smallLoliPickaxe);
		ITEMS.register("loli_coal_addon", () -> coalAddon);
		ITEMS.register("loli_iron_addon", () -> ironAddon);
		ITEMS.register("loli_gold_addon", () -> goldAddon);
		ITEMS.register("loli_redstone_addon", () -> redstoneAddon);
		ITEMS.register("loli_lapis_addon", () -> lapisAddon);
		ITEMS.register("loli_diamond_addon", () -> diamondAddon);
		ITEMS.register("loli_emerald_addon", () -> emeraldAddon);
		ITEMS.register("loli_obsidian_addon", () -> obsidianAddon);
		ITEMS.register("loli_glow_addon", () -> glowAddon);
		ITEMS.register("loli_quartz_addon", () -> quartzAddon);
		ITEMS.register("loli_nether_star_addon", () -> netherStarAddon);
		ITEMS.register("loli_auto_furnace_addon", () -> autoFurnaceAddon);
		ITEMS.register("loli_fly_addon", () -> flyAddon);
		ITEMS.register("loli_entity_soul_addon", () -> entitySoul);
		ITEMS.register("loli_dispersal", () -> loliDispersal);
		ITEMS.register("bug_entity_clear", () -> bugEntityClear);
		ITEMS.register("loli_card", () -> loliCard);
		ITEMS.register("loli_card_album", () -> loliCardAlbum);
		ITEMS.register("loli_card_online", () -> loliCardOnline);
		// 配置此时可能尚未加载，未加载时使用注解默认值
		List<String> names = ConfigLoader.loliRecodeNames == null || ConfigLoader.loliRecodeNames.isEmpty() ? Lists.newArrayList("lolirecord:loliRecord:loli_record") : ConfigLoader.loliRecodeNames;
		for (String loliRecordName : names) {
			// 每条配置的格式为「声音事件名:唱片显示名:唱片注册名」，即 name[0]=声音、name[1]=显示名、name[2]=注册名
			String[] name = loliRecordName.split(":");
			final ItemLoliRecord record = new ItemLoliRecord(name[1], name[0]);
			loliRecords.add(record);
			ITEMS.register(name[2], () -> record);
		}
	}

}
