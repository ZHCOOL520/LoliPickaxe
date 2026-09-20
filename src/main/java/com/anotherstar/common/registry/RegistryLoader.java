package com.anotherstar.common.registry;

import java.util.Map;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.registry.recipe.IPasswordRecipe;
import com.google.common.collect.Maps;

import net.minecraft.resources.ResourceLocation;

/**
 * 原 1.12.2 用 RegistryBuilder 自建了密码配方注册表，1.20.1 已删除该 API，
 * 这里改用普通容器保存。
 */
public class RegistryLoader {

	public static final Map<ResourceLocation, IPasswordRecipe> PASSWORD_RECIPES = Maps.newLinkedHashMap();

	public static void register(ResourceLocation id, IPasswordRecipe recipe) {
		PASSWORD_RECIPES.put(id, recipe);
	}

	public static void register(String name, IPasswordRecipe recipe) {
		PASSWORD_RECIPES.put(new ResourceLocation(LoliPickaxe.MODID, name), recipe);
	}

	public static IPasswordRecipe get(ResourceLocation id) {
		return PASSWORD_RECIPES.get(id);
	}

}
