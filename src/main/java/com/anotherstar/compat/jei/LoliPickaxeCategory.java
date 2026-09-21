package com.anotherstar.compat.jei;

import com.anotherstar.common.item.ItemLoader;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;

/**
 * 氪金萝莉合成（{@code LoliPickaxeRecipe}）的 JEI 展示类别。
 *
 * <p>对应真实配方 {@code com.anotherstar.common.recipe.LoliPickaxeRecipe}：
 * 满级小萝莉 + 满级生物灵魂 → 氪金萝莉。
 */
public class LoliPickaxeCategory extends AbstractLoliCategory<LoliJeiRecipes.Display> {

	public LoliPickaxeCategory(IGuiHelper helper, RecipeType<LoliJeiRecipes.Display> type) {
		super(helper, type, Component.translatable("jei.lolipickaxe.category.pickaxe"), new net.minecraft.world.item.ItemStack(ItemLoader.loliPickaxe()));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LoliJeiRecipes.Display recipe, IFocusGroup focuses) {
		layout(builder, recipe.inputs(), recipe.output());
	}

}
