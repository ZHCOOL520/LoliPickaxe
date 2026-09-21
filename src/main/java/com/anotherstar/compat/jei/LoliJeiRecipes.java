package com.anotherstar.compat.jei;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * JEI 展示用的配方数据载体。
 *
 * <p>这些类<b>只用于显示</b>，不参与任何实际合成判定。
 * 真实的合成逻辑仍然完全由 {@code com.anotherstar.common.recipe} 下的
 * {@code CustomRecipe} 子类负责，本包不改动它们。
 *
 * <h2>为什么需要独立的展示类型</h2>
 *
 * <p>原版 {@code CustomRecipe} 没有"原料/产物"的数据结构可供 JEI 读取，
 * 因此这里用一个最小的不可变记录类，把"玩家看到什么"表达出来。
 */
public final class LoliJeiRecipes {

	private LoliJeiRecipes() {
	}

	/**
	 * 通用展示配方：若干输入 → 一个输出。
	 *
	 * @param inputs 输入槽内容（按顺序占位）
	 * @param output 输出槽内容
	 */
	public record Display(List<ItemStack> inputs, ItemStack output) {
	}

	/**
	 * 「生物掉落」展示条目：某种生物被击杀后按概率掉落某物品。
	 *
	 * <p>之所以单列一个类型，是因为它与工作台配方语义不同：
	 * 没有固定的原料格子，产出由<b>概率</b>决定。
	 * 这些数据来自 {@code com.anotherstar.common.event.LoliDropEvent}，
	 * 概率在构造时从配置读取，因此改配置后（重启游戏）JEI 会显示新数值。
	 *
	 * @param source      掉落来源的代表性物品（用于占位展示）
	 * @param result      掉落物
	 * @param probability 掉落概率，取值 {@code [0, 1]}
	 */
	public record Drop(ItemStack source, ItemStack result, double probability) {
	}

	/**
	 * 「萝莉祭坛摆放方式」展示条目。
	 *
	 * <p>祭坛不是合成出来的，而是要求在世界上按特定图案摆放 63×63 的祭坛方块，
	 * 再用萝莉镐右键中心的方块。这个条目本身<b>不携带数据</b>，
	 * 图案由 {@link LoliAltarPattern} 提供、由 {@link LoliAltarCategory} 绘制。
	 *
	 * <p>之所以做成"配方条目"而不是信息页，是因为这样玩家可以在 JEI 里
	 * <b>直接搜索祭坛方块并跳到摆法</b>，比藏在物品说明里更好找。
	 */
	public record AltarLayout() {
	}

}
