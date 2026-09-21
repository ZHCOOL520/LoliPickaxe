package com.anotherstar.compat.jei;

import com.anotherstar.common.item.ItemLoader;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 小萝莉属性升级（{@code SmallLoliPickaxeRecipe}）的 JEI 展示类别。
 *
 * <p>对应真实配方 {@code com.anotherstar.common.recipe.SmallLoliPickaxeRecipe}：
 * 小萝莉 + 比当前等级高 1 级的材料 → 该属性 +1 的小萝莉。
 */
public class SmallLoliPickaxeCategory extends AbstractLoliCategory<LoliJeiRecipes.Display> {

	public SmallLoliPickaxeCategory(IGuiHelper helper, RecipeType<LoliJeiRecipes.Display> type) {
		super(helper, type, Component.translatable("jei.lolipickaxe.category.small_pickaxe"), new ItemStack(ItemLoader.smallLoliPickaxe()));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LoliJeiRecipes.Display recipe, IFocusGroup focuses) {
		layout(builder, recipe.inputs(), recipe.output());
	}

}
