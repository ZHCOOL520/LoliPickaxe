package com.anotherstar.common.recipe;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 1.20.1 的配方改为数据驱动，动态配方的实例由对应的 JSON（data/lolipickaxe/recipes/）创建。
 */
public class RecipeLoader {

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, LoliPickaxe.MODID);

	public static final RegistryObject<RecipeSerializer<SuperpositionRecipe>> SUPERPOSITION_SERIALIZER = RECIPE_SERIALIZERS.register("loli_superposition", () -> new SimpleCraftingRecipeSerializer<>(SuperpositionRecipe::new));

	public static final RegistryObject<RecipeSerializer<SmallLoliPickaxeRecipe>> SMALL_LOLI_PICKAXE_SERIALIZER = RECIPE_SERIALIZERS.register("small_loli_pickaxe_up", () -> new SimpleCraftingRecipeSerializer<>(SmallLoliPickaxeRecipe::new));

	public static final RegistryObject<RecipeSerializer<LoliPickaxeRecipe>> LOLI_PICKAXE_SERIALIZER = RECIPE_SERIALIZERS.register("loli_pickaxe", () -> new SimpleCraftingRecipeSerializer<>(LoliPickaxeRecipe::new));

}
