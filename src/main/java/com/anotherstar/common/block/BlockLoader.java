package com.anotherstar.common.block;

import java.util.List;

import javax.annotation.Nullable;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 方块与方块物品注册中心。
 *
 * <h2>为什么全部改成「延迟构造」（关键修复）</h2>
 *
 * <p>与 {@link com.anotherstar.common.item.ItemLoader} 同因同源：原实现把方块与方块物品
 * 写成静态字段直接 {@code new}，导致类初始化时（{@code <clinit>}）立即构造它们。
 *
 * <p>{@link Block} 与 {@link Item} 的构造器内部都会向对应的内置注册表登记 intrusive holder
 * （{@code BuiltInRegistries.BLOCK.createIntrusiveHolder(this)} /
 * {@code BuiltInRegistries.ITEM.createIntrusiveHolder(this)}），
 * 而该操作要求注册表<b>尚未冻结</b>。在 {@code preInit} 阶段首次触碰本类时注册表已冻结，
 * 于是抛出 {@code java.lang.IllegalStateException: Registry is already frozen}，模组加载失败。
 *
 * <p>修复方式与物品侧完全一致：只登记供应器，把实际构造推迟到 Forge 调用供应器时，
 * 并以懒加载方法保证单例语义不变。所有注册名一个未改。
 */
public class BlockLoader {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LoliPickaxe.MODID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LoliPickaxe.MODID);

	/*
	 * ========================================================================================
	 * 与 ItemLoader 相同的模式：私有实例槽 + 公开懒加载方法，绝不在 <clinit> 中构造。
	 * 注意方块与其方块物品必须共用同一延迟构造时机，保证 BlockItem 拿到的是同一个 Block 实例。
	 * ========================================================================================
	 */

	private static BlockBuffAttackTNT loliBlueScreenTNTInstance;

	/** @return 蓝屏 TNT 方块（注册名 {@code loli_blue_screen_tnt}） */
	public static BlockBuffAttackTNT loliBlueScreenTNT() {
		if (loliBlueScreenTNTInstance == null) {
			loliBlueScreenTNTInstance = new BlockBuffAttackTNT("LoliBlueScreenTNT", true, false, false);
		}
		return loliBlueScreenTNTInstance;
	}

	private static BlockItem itemLoliBlueScreenTNTInstance;

	/** @return 蓝屏 TNT 的方块物品 */
	public static BlockItem itemLoliBlueScreenTNT() {
		if (itemLoliBlueScreenTNTInstance == null) {
			itemLoliBlueScreenTNTInstance = new LoliBlockItem(loliBlueScreenTNT(), null, true);
		}
		return itemLoliBlueScreenTNTInstance;
	}

	private static BlockBuffAttackTNT loliExitTNTInstance;

	/** @return 退出 TNT 方块（注册名 {@code loli_exit_tnt}） */
	public static BlockBuffAttackTNT loliExitTNT() {
		if (loliExitTNTInstance == null) {
			loliExitTNTInstance = new BlockBuffAttackTNT("LoliExitTNT", false, true, false);
		}
		return loliExitTNTInstance;
	}

	private static BlockItem itemLoliExitTNTInstance;

	/** @return 退出 TNT 的方块物品 */
	public static BlockItem itemLoliExitTNT() {
		if (itemLoliExitTNTInstance == null) {
			itemLoliExitTNTInstance = new LoliBlockItem(loliExitTNT(), null, true);
		}
		return itemLoliExitTNTInstance;
	}

	private static BlockBuffAttackTNT loliFailRespondTNTInstance;

	/** @return 无响应 TNT 方块（注册名 {@code loli_fail_respond_tnt}） */
	public static BlockBuffAttackTNT loliFailRespondTNT() {
		if (loliFailRespondTNTInstance == null) {
			loliFailRespondTNTInstance = new BlockBuffAttackTNT("LoliFailRespondTNT", false, false, true);
		}
		return loliFailRespondTNTInstance;
	}

	private static BlockItem itemLoliFailRespondTNTInstance;

	/** @return 无响应 TNT 的方块物品 */
	public static BlockItem itemLoliFailRespondTNT() {
		if (itemLoliFailRespondTNTInstance == null) {
			itemLoliFailRespondTNTInstance = new LoliBlockItem(loliFailRespondTNT(), null, true);
		}
		return itemLoliFailRespondTNTInstance;
	}

	private static BlockLoliAltar loliAltarInstance;

	/** @return 萝莉祭坛方块（注册名 {@code loli_altar}） */
	public static BlockLoliAltar loliAltar() {
		if (loliAltarInstance == null) {
			loliAltarInstance = new BlockLoliAltar();
		}
		return loliAltarInstance;
	}

	private static BlockItem itemLoliAltarInstance;

	/** @return 萝莉祭坛的方块物品 */
	public static BlockItem itemLoliAltar() {
		if (itemLoliAltarInstance == null) {
			itemLoliAltarInstance = new LoliBlockItem(loliAltar(), "loliAltar.use", false);
		}
		return itemLoliAltarInstance;
	}

	private static BlockPasswordWorkBench passwordWorkBenchInstance;

	/** @return 密码工作台方块（注册名 {@code password_work_bench}） */
	public static BlockPasswordWorkBench passwordWorkBench() {
		if (passwordWorkBenchInstance == null) {
			passwordWorkBenchInstance = new BlockPasswordWorkBench();
		}
		return passwordWorkBenchInstance;
	}

	private static BlockItem itemPasswordWorkBenchInstance;

	/** @return 密码工作台的方块物品 */
	public static BlockItem itemPasswordWorkBench() {
		if (itemPasswordWorkBenchInstance == null) {
			itemPasswordWorkBenchInstance = new BlockItem(passwordWorkBench(), new Item.Properties());
		}
		return itemPasswordWorkBenchInstance;
	}

	static {
		// 只登记「怎么造」，不在这里真的造：构造时机被推迟到 Forge 调用供应器时（注册表尚未冻结）
		BLOCKS.register("loli_blue_screen_tnt", BlockLoader::loliBlueScreenTNT);
		ITEMS.register("loli_blue_screen_tnt", BlockLoader::itemLoliBlueScreenTNT);
		BLOCKS.register("loli_exit_tnt", BlockLoader::loliExitTNT);
		ITEMS.register("loli_exit_tnt", BlockLoader::itemLoliExitTNT);
		BLOCKS.register("loli_fail_respond_tnt", BlockLoader::loliFailRespondTNT);
		ITEMS.register("loli_fail_respond_tnt", BlockLoader::itemLoliFailRespondTNT);
		BLOCKS.register("loli_altar", BlockLoader::loliAltar);
		ITEMS.register("loli_altar", BlockLoader::itemLoliAltar);
		BLOCKS.register("password_work_bench", BlockLoader::passwordWorkBench);
		ITEMS.register("password_work_bench", BlockLoader::itemPasswordWorkBench);
	}

	/**
	 * 1.20.1 中方块物品的提示信息由 BlockItem 提供，这里补上原有提示。
	 */
	public static class LoliBlockItem extends BlockItem {

		@Nullable
		private final String tooltipKey;
		private final boolean buffAttackTNT;

		public LoliBlockItem(Block block, @Nullable String tooltipKey, boolean buffAttackTNT) {
			super(block, new Item.Properties());
			this.tooltipKey = tooltipKey;
			this.buffAttackTNT = buffAttackTNT;
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
			if (this.buffAttackTNT) {
				tooltip.add(Component.translatable(ConfigLoader.loliEnableBuffAttackTNT ? "buffAttackTNT.enable" : "buffAttackTNT.disable"));
			} else if (this.tooltipKey != null) {
				tooltip.add(Component.translatable(this.tooltipKey));
			}
		}

	}

}
