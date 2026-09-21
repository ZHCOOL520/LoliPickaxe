package com.anotherstar.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 本模组 JEI 类别的通用基类。
 *
 * <p>把「标题、背景、图标、宽高」这些与具体配方无关的部分集中实现，
 * 各个子类只需提供 {@link #getRecipeType()} 与
 * {@link #setRecipe(IRecipeLayoutBuilder, Object, IFocusGroup)}。
 *
 * <p>背景与图标都<b>用 JEI 自带的绘制工具生成</b>，不依赖任何外部贴图，
 * 从而避免出现因贴图缺失导致的紫黑方块。
 *
 * @param <T> 该类别承载的配方数据类型
 */
public abstract class AbstractLoliCategory<T> implements IRecipeCategory<T> {

	/** 槽位尺寸（与原版一致的 18px 单位格）。 */
	protected static final int SLOT = 18;
	/** 输入网格列数（与原版工作台一致的 3 列）。 */
	protected static final int INPUT_COLS = 3;
	/** 图面内边距。 */
	protected static final int PAD = 4;
	/** JEI 内置配方箭头的尺寸（22×16），用于给箭头预留水平空间。 */
	protected static final int ARROW_W = 22;
	/** JEI 内置配方箭头的高度。 */
	protected static final int ARROW_H = 16;

	/** 输入网格左上角 X。 */
	private static final int INPUT_X = PAD;
	/** 输入网格占据的宽度。 */
	private static final int INPUT_W = INPUT_COLS * SLOT;
	/** 箭头 X：紧接输入网格右侧，间隔一个内边距。 */
	private static final int ARROW_X = INPUT_X + INPUT_W + PAD;
	/** 输出槽 X：紧接箭头右侧，间隔一个内边距。 */
	private static final int OUTPUT_X = ARROW_X + ARROW_W + PAD;

	/** 单输入布局的来源槽 X（就是左内边距）。 */
	private static final int SINGLE_INPUT_X = PAD;
	/** 单输入布局中，给底部概率文字预留的高度（约一行 9px 字 + 余量）。 */
	private static final int SINGLE_TEXT_H = 14;

	/**
	 * 图面宽度 = 输出槽右边缘 + 右侧内边距。
	 *
	 * <p>此前宽度误算为 {@code 3*SLOT + 8 = 62}，导致输出槽 X 落在 42px，
	 * 与输入网格第 3 列（40~58px）**完全重叠**，箭头也随之压在第 3 列上，
	 * 表现为玩家看到的「合成表错乱摆放」。这里按
	 * 「输入网格 + 间隔 + 箭头 + 间隔 + 输出槽 + 内边距」重新推算。
	 */
	protected static final int WIDTH = OUTPUT_X + SLOT + PAD;

	/** 图面高度 = 3 行输入 + 上下内边距。 */
	protected static final int HEIGHT = 3 * SLOT + 2 * PAD;

	private final RecipeType<T> recipeType;
	private final Component title;
	private final IDrawable icon;

	/**
	 * @param helper     JEI 的 GUI 辅助工具，用于创建图标；不可为 null
	 * @param recipeType 本类别的配方类型；不可为 null
	 * @param title      类别标题；不可为 null
	 * @param iconStack  类别图标；不可为 null
	 */
	protected AbstractLoliCategory(IGuiHelper helper, RecipeType<T> recipeType, Component title, ItemStack iconStack) {
		this.recipeType = recipeType;
		this.title = title;
		this.icon = helper.createDrawableItemStack(iconStack);
	}

	@Override
	public RecipeType<T> getRecipeType() {
		return recipeType;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	/**
	 * 类别宽度。
	 *
	 * <p>使用 {@code getWidth()/getHeight()} 而非已废弃的 {@code getBackground()}：
	 * 新版 JEI 用尺寸描述背景，无需再自行创建一个空白的 {@code IDrawable}。
	 */
	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	/**
	 * 在配方图面上追加「输入 → 输出」箭头，让玩家一眼看懂合成方向。
	 *
	 * <p>使用 JEI 内置的 {@code addRecipeArrow()} 而非自绘图形：
	 * 该箭头由 JEI 统一渲染，自动适配主题与动画，也不需要任何外部贴图。
	 *
	 * @param builder JEI 的附加元素构造器；不可为 null
	 * @param recipe  当前配方；不可为 null
	 * @param focuses 当前聚焦的原料；不可为 null
	 */
	@Override
	public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
		// 箭头位于输入网格与输出槽之间，竖直居中；三者水平方向互不重叠
		builder.addRecipeArrow().setPosition(ARROW_X, (HEIGHT - ARROW_H) / 2);
	}

	/**
	 * 把若干输入与一个输出填充到布局中。
	 *
	 * <p>输入按 3 列排布（最多 9 格，与工作台 3×3 对应），并在竖直方向<b>居中</b>，
	 * 这样 2 个输入与 9 个输入的类别看起来都是居中的，不会一边倒。
	 *
	 * <p>水平方向的三个区域严格分离，保证互不重叠：
	 * <pre>
	 * | 输入网格(3 列) | 间隔 | 箭头 | 间隔 | 输出槽 | 内边距 |
	 * </pre>
	 *
	 * @param builder JEI 布局构造器；不可为 null
	 * @param inputs  输入槽内容，每个元素占一个槽位；不可为 null
	 * @param output  输出槽内容；不可为 null
	 */
	protected void layout(IRecipeLayoutBuilder builder, List<ItemStack> inputs, ItemStack output) {
		int count = Math.min(inputs.size(), INPUT_COLS * 3);
		int rows = count <= 0 ? 1 : (count + INPUT_COLS - 1) / INPUT_COLS;
		// 输入网格竖直居中：行数越少，起始 Y 越大
		int inputY = (HEIGHT - rows * SLOT) / 2;
		for (int i = 0; i < count; i++) {
			int col = i % INPUT_COLS;
			int row = i / INPUT_COLS;
			builder.addInputSlot(INPUT_X + col * SLOT, inputY + row * SLOT).addItemStack(inputs.get(i));
		}
		// 输出槽：位于箭头右侧、竖直居中，与输入网格无任何重叠
		builder.addOutputSlot(OUTPUT_X, (HEIGHT - SLOT) / 2).addItemStack(output);
	}

	/**
	 * 「单输入」紧凑布局：一个来源槽 → 箭头 → 一个产物槽。
	 *
	 * <h2>为什么需要单独一个方法</h2>
	 *
	 * <p>{@link #layout} 的几何常量是按<b>3 列工作台</b>固定的
	 * （{@code INPUT_W = 3 × 18 = 54}、{@code ARROW_X = 62}、{@code OUTPUT_X = 88}）。
	 * 但「生物掉落」这类条目<b>只有 1 个来源</b>，套用后会变成
	 * 「左侧孤零零一格 → 中间空出 40px（约 2 格）→ 右侧产物」，
	 * 视觉上就是玩家反馈的「掉落界面错位」。
	 *
	 * <p>本方法不复用那套常量，而是<b>紧贴排布</b>：
	 * <pre>
	 * | 来源槽 | 间隔 | 箭头 | 间隔 | 产物槽 | 内边距 |
	 * </pre>
	 * 由于此布局比 {@code WIDTH} 窄，调用方需要同时重写
	 * {@link #getWidth()} 与 {@link #getHeight()}，否则背景与内容会不匹配。
	 *
	 * @param builder JEI 布局构造器；不可为 null
	 * @param source  来源槽内容；不可为 null
	 * @param output  产物槽内容；不可为 null
	 */
	protected void layoutSingle(IRecipeLayoutBuilder builder, ItemStack source, ItemStack output) {
		builder.addInputSlot(SINGLE_INPUT_X, singleSlotY()).addItemStack(source);
		builder.addOutputSlot(singleOutputX(), singleSlotY()).addItemStack(output);
	}

	/**
	 * 单输入布局下的箭头 X 坐标。
	 *
	 * @return 紧接来源槽右侧、间隔一个内边距
	 */
	protected static int singleArrowX() {
		return SINGLE_INPUT_X + SLOT + PAD;
	}

	/**
	 * 单输入布局下的产物槽 X 坐标。
	 *
	 * @return 紧接箭头右侧、间隔一个内边距
	 */
	protected static int singleOutputX() {
		return singleArrowX() + ARROW_W + PAD;
	}

	/**
	 * 单输入布局的图面宽度。
	 *
	 * @return 产物槽右边缘 + 右侧内边距
	 */
	protected static int singleWidth() {
		return singleOutputX() + SLOT + PAD;
	}

	/**
	 * 单输入布局的图面高度。
	 *
	 * <p>比 3 行的 {@code HEIGHT} 矮，只留「一行槽位 + 上下内边距 + 一行概率文字」的空间。
	 *
	 * @return 图面高度
	 */
	protected static int singleHeight() {
		return SLOT + 2 * PAD + SINGLE_TEXT_H;
	}

	/**
	 * 单输入布局中槽位的 Y 坐标（在文字上方的剩余空间里居中）。
	 *
	 * @return 槽位 Y
	 */
	protected static int singleSlotY() {
		return PAD;
	}

	/**
	 * 单输入布局中概率文字的 Y 坐标。
	 *
	 * <p>此前该文字用的是 {@code getHeight() - 14} 这种硬编码，
	 * 在 3 行高的图面上会与产物槽底部只隔 8px、几乎贴在一起。
	 * 这里改为「槽位底部 + 内边距」推导，与槽位保持稳定间距。
	 *
	 * @return 概率文字 Y
	 */
	protected static int singleTextY() {
		return singleSlotY() + SLOT + 1;
	}

}
