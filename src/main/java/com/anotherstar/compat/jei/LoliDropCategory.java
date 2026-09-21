package com.anotherstar.compat.jei;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;

/**
 * 「生物掉落」的 JEI 展示类别。
 *
 * <h2>为什么需要这个类别</h2>
 *
 * <p>本模组有一批物品<b>不是合成得到的</b>，而是击杀生物时按概率掉落，
 * 逻辑写在 {@code com.anotherstar.common.event.LoliDropEvent}：
 *
 * <ul>
 *   <li>萝莉卡片、卡片册、生物灵魂 —— 任意生物；</li>
 *   <li>萝莉唱片 —— <b>仅苦力怕</b>。</li>
 * </ul>
 *
 * <p>这些掉落不产生配方，因此 JEI 原本完全看不到，
 * 玩家只知道「有这个物品」却不知道「从哪来」——
 * 这正是反馈中「最基本物品不知道怎么获取，这也不显示」的直接原因。
 * 本类别把 {@code LoliDropEvent} 的掉率可视化出来。
 *
 * <p>概率由插件在注册时从配置读取，改配置并重启游戏后会同步更新。
 */
public class LoliDropCategory extends AbstractLoliCategory<LoliJeiRecipes.Drop> {

	public LoliDropCategory(IGuiHelper helper, RecipeType<LoliJeiRecipes.Drop> type) {
		// 以「铁剑」作为类别图标，表示「击杀生物掉落」
		super(helper, type, Component.translatable("jei.lolipickaxe.category.mob_drop"), new ItemStack(Items.IRON_SWORD));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LoliJeiRecipes.Drop recipe, IFocusGroup focuses) {
		// 【修正错位】这里必须用「单输入紧凑布局」而不是通用的 layout(...)。
		// 通用 layout 的几何常量是按 3 列工作台固定的（输出槽 X=88），
		// 而本类别只有 1 个来源，套用后来源与产物之间会空出约 40px（2 格），
		// 即玩家反馈的「掉落界面错位」。layoutSingle 让三者紧贴排布。
		layoutSingle(builder, recipe.source(), recipe.result());
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, LoliJeiRecipes.Drop recipe, IFocusGroup focuses) {
		// 箭头紧贴来源槽右侧，与 layoutSingle 的槽位保持同一行、竖直居中
		builder.addRecipeArrow().setPosition(singleArrowX(), singleSlotY() + (SLOT - 16) / 2);
		// 概率文字放在槽位下方，用推导出的 Y 而不是硬编码的 getHeight()-14
		builder.addText(Component.translatable("jei.lolipickaxe.probability", formatPercent(recipe.probability())),
				PAD, singleTextY());
	}

	/** 单输入紧凑布局下的图面宽度（比通用类别窄）。 */
	@Override
	public int getWidth() {
		return singleWidth();
	}

	/** 单输入紧凑布局下的图面高度（比通用类别矮）。 */
	@Override
	public int getHeight() {
		return singleHeight();
	}

	/**
	 * 把 {@code 0~1} 的概率格式化为百分数。
	 *
	 * <p>固定使用 {@link Locale#ROOT}，避免在部分语言环境下小数点变成逗号（如 {@code 0,01%}）。
	 *
	 * @param probability 概率，取值 {@code [0, 1]}
	 * @return 形如 {@code 1.00%} 的字符串
	 */
	public static String formatPercent(double probability) {
		return String.format(Locale.ROOT, "%.2f%%", probability * 100.0D);
	}

}
