package com.anotherstar.common.recipe.password;

import com.anotherstar.common.registry.RegistryLoader;
import com.anotherstar.common.registry.recipe.IPasswordRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public class PasswordRecipeManager {

	public static ItemStack findMatchingResult(CraftingContainer craftMatrix, Player player, String password) {
		for (IPasswordRecipe recipe : RegistryLoader.PASSWORD_RECIPES.values()) {
			ItemStack result = recipe.getResult(craftMatrix, player, password);
			if (!result.isEmpty()) {
				return result;
			}
		}
		return ItemStack.EMPTY;
	}

	public static void register(ResourceLocation id, IPasswordRecipe recipe) {
		RegistryLoader.register(id, recipe);
	}

}
