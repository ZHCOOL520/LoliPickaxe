package com.anotherstar.compat.jei;

import com.anotherstar.common.item.ItemLoader;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 材料叠加（{@code SuperpositionRecipe}）的 JEI 展示类别——合成方向。
 *
 * <p>对应真实配方 {@code com.anotherstar.common.recipe.SuperpositionRecipe}：
 * 9 个同级材料 → 1 个高一级材料。
 */
public class SuperpositionCategory extends AbstractLoliCategory<LoliJeiRecipes.Display> {

	public SuperpositionCategory(IGuiHelper helper, RecipeType<LoliJeiRecipes.Display> type) {
		super(helper, type, Component.translatable("jei.lolipickaxe.category.superposition"), new ItemStack(ItemLoader.diamondAddon()));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LoliJeiRecipes.Display recipe, IFocusGroup focuses) {
		layout(builder, recipe.inputs(), recipe.output());
	}

}
