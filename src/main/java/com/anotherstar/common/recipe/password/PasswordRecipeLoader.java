package com.anotherstar.common.recipe.password;

import com.anotherstar.common.registry.RegistryLoader;
import com.anotherstar.common.registry.recipe.IPasswordRecipe;

import net.minecraft.resources.ResourceLocation;

public class PasswordRecipeLoader {

	/**
	 * 1.20.1 的密码配方保存在 {@link RegistryLoader#PASSWORD_RECIPES} 中，这里保留注册入口。
	 */
	public static void register(ResourceLocation id, IPasswordRecipe recipe) {
		RegistryLoader.register(id, recipe);
	}

}
