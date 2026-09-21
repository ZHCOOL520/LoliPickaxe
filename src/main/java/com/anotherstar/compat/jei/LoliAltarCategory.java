package com.anotherstar.compat.jei;

import com.anotherstar.common.block.BlockLoader;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 「萝莉祭坛摆放方式」的 JEI 展示类别。
 *
 * <h2>为什么需要这个类别</h2>
 *
 * <p>祭坛的建造规则写在 {@code BlockLoliAltar#initAltarStructure()} 里，
 * 是一个 <b>63×63 的 {@code Block[][]} 图案</b>，以<b>被右键的那个祭坛方块</b>为中心
 * （代码中偏移写法为 {@code pos.offset(dx, 0, dz)}，{@code dx}/{@code dz} 取 {@code -31..31}）。
 *
 * <p>在此之前玩家只能靠资源里的一张图片（{@code lolicards/召唤祭坛摆放方式.png}）猜测摆法，
 * 而那张图因文件名含中文<b>不会被资源管理器加载</b>，等于根本没地方查。本类别把图案直接画进 JEI。
 *
 * <h2>渲染策略</h2>
 *
 * <p>63×63 太大，且图案具有四重旋转对称、中心是唯一不对称的部分。
 * 因此采用「<b>整体缩略图 + 中心放大图 + 图例</b>」三段式：
 * 缩略图让玩家看出整体轮廓，放大图解决最容易摆错的中心 9×9。
 *
 * <p>本类别是<b>纯展示</b>，不参与任何方块校验逻辑。
 */
public class LoliAltarCategory extends AbstractLoliCategory<LoliJeiRecipes.AltarLayout> {

	/** 缩略图每格的像素边长（63 × 2 = 126px）。 */
	private static final int THUMB_CELL = 2;
	/** 中心放大图每格的像素边长（9 × 8 = 72px）。 */
	private static final int ZOOM_CELL = 8;
	/** 中心放大图取中心附近多少格（9 表示 9×9）。 */
	private static final int ZOOM_SPAN = 9;

	/** 顶部标题行高度。 */
	private static final int TITLE_H = 11;
	/** 底部图例区高度。 */
	private static final int LEGEND_H = 24;

	/** 缩略图宽度。 */
	private static final int THUMB_W = LoliAltarPattern.SIZE * THUMB_CELL;
	/** 放大图宽度。 */
	private static final int ZOOM_W = ZOOM_SPAN * ZOOM_CELL;

	/** 图面宽度：左边缩略图、右边放大图，中间留一个内边距。 */
	private static final int WIDTH = PAD + THUMB_W + PAD + ZOOM_W + PAD;
	/** 图面高度：标题 + 图像 + 图例。 */
	private static final int HEIGHT = TITLE_H + Math.max(THUMB_W, ZOOM_W) + LEGEND_H;

	/** 祭坛方块格子的颜色（不透明青蓝色）。 */
	private static final int COLOR_BLOCK = 0xFF3FB6C8;
	/** 中心格的强调色（金色）。 */
	private static final int COLOR_CENTER = 0xFFFFC53D;
	/** 图像背景色（半透明深色）。 */
	private static final int COLOR_BG = 0x40000000;

	/** 由 IGuiHelper 生成的空白绘制对象，用于拼出图案。 */
	private final IGuiHelper helper;

	public LoliAltarCategory(IGuiHelper helper, RecipeType<LoliJeiRecipes.AltarLayout> type) {
		// 以祭坛方块本身作为类别图标
		super(helper, type, Component.translatable("jei.lolipickaxe.category.altar"), new ItemStack(BlockLoader.itemLoliAltar()));
		this.helper = helper;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LoliJeiRecipes.AltarLayout recipe, IFocusGroup focuses) {
		// 本类别不使用槽位，全部内容由 createRecipeExtras 绘制；
		// 这里放一个可见的祭坛方块槽，方便玩家点击方块后定位到本类别。
		builder.addInputSlot(PAD, TITLE_H).addItemStack(new ItemStack(BlockLoader.itemLoliAltar()));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, LoliJeiRecipes.AltarLayout recipe, IFocusGroup focuses) {
		int thumbX = PAD;
		int thumbY = TITLE_H;

		// 1) 缩略图背景
		addRect(builder, thumbX - 1, thumbY - 1, THUMB_W + 2, THUMB_W + 2, COLOR_BG);

		// 2) 缩略图：整张 63×63，逐格绘制
		for (int row = 0; row < LoliAltarPattern.SIZE; row++) {
			for (int col = 0; col < LoliAltarPattern.SIZE; col++) {
				if (LoliAltarPattern.isAltar(row, col)) {
					addRect(builder, thumbX + col * THUMB_CELL, thumbY + row * THUMB_CELL, THUMB_CELL, THUMB_CELL, COLOR_BLOCK);
				}
			}
		}
		// 在缩略图上标出中心位置
		int centerCell = LoliAltarPattern.SIZE / 2;
		addRect(builder, thumbX + centerCell * THUMB_CELL, thumbY + centerCell * THUMB_CELL, THUMB_CELL, THUMB_CELL, COLOR_CENTER);

		// 3) 中心放大图
		int zoomX = PAD + THUMB_W + PAD;
		int zoomY = TITLE_H;
		addRect(builder, zoomX - 1, zoomY - 1, ZOOM_W + 2, ZOOM_W + 2, COLOR_BG);
		int half = ZOOM_SPAN / 2;
		for (int row = -half; row <= half; row++) {
			for (int col = -half; col <= half; col++) {
				if (LoliAltarPattern.isAltarAtOffset(col, row)) {
					int color = (row == 0 && col == 0) ? COLOR_CENTER : COLOR_BLOCK;
					addRect(builder, zoomX + (col + half) * ZOOM_CELL, zoomY + (row + half) * ZOOM_CELL, ZOOM_CELL, ZOOM_CELL, color);
				}
			}
		}

		// 4) 图例与操作提示
		int legendY = TITLE_H + Math.max(THUMB_W, ZOOM_W) + 2;
		builder.addText(Component.translatable("jei.lolipickaxe.altar.legend"), PAD, legendY);
		builder.addText(Component.translatable("jei.lolipickaxe.altar.hint"), PAD, legendY + 11);
	}

	/**
	 * 往图面上叠加一个纯色矩形。
	 *
	 * <p>用 {@code IGuiHelper#createBlankDrawable} 生成空白绘制对象，
	 * 再由 {@link TintedRect} 填充颜色，
	 * 避免依赖任何外部贴图文件（与基类的做法一致）。
	 *
	 * @param builder JEI 附加元素构造器
	 * @param x       左上角 X
	 * @param y       左上角 Y
	 * @param w       宽度
	 * @param h       高度
	 * @param color   颜色（ARGB）
	 */
	private void addRect(IRecipeExtrasBuilder builder, int x, int y, int w, int h, int color) {
		builder.addDrawable(new TintedRect(helper.createBlankDrawable(w, h), color), x, y);
	}

	/** @return 图面宽度 */
	@Override
	public int getWidth() {
		return WIDTH;
	}

	/** @return 图面高度 */
	@Override
	public int getHeight() {
		return HEIGHT;
	}

	/**
	 * 纯色矩形绘制对象。
	 *
	 * <p>{@code IDrawable#draw} 本身不支持着色，因此这里在空白绘制对象上直接填充颜色。
	 */
	private static final class TintedRect implements IDrawable {

		private final IDrawable base;
		private final int color;

		private TintedRect(IDrawable base, int color) {
			this.base = base;
			this.color = color;
		}

		@Override
		public int getWidth() {
			return base.getWidth();
		}

		@Override
		public int getHeight() {
			return base.getHeight();
		}

		@Override
		public void draw(net.minecraft.client.gui.GuiGraphics guiGraphics, int xOffset, int yOffset) {
			guiGraphics.fill(xOffset, yOffset, xOffset + getWidth(), yOffset + getHeight(), color);
		}

	}

}
