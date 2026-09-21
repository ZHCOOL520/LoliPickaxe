package com.anotherstar.compat.jei;

import com.anotherstar.common.item.ItemLoader;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 材料拆分（{@code SuperpositionRecipe} 的反向）的 JEI 展示类别。
 *
 * <p>同一个 {@code SuperpositionRecipe} 同时支持两个方向：
 * 9 个低级 → 1 个高级，以及 1 个高级 → 9 个低级。
 * 拆成两个 JEI 类别展示，是为了让玩家在两个方向上都能查到。
 */
public class LoliSplitCategory extends AbstractLoliCategory<LoliJeiRecipes.Display> {

	public LoliSplitCategory(IGuiHelper helper, RecipeType<LoliJeiRecipes.Display> type) {
		super(helper, type, Component.translatable("jei.lolipickaxe.category.split"), new ItemStack(ItemLoader.netherStarAddon()));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LoliJeiRecipes.Display recipe, IFocusGroup focuses) {
		layout(builder, recipe.inputs(), recipe.output());
	}

}
