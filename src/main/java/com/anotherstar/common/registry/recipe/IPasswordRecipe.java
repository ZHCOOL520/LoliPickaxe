package com.anotherstar.common.registry.recipe;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public interface IPasswordRecipe {

	ItemStack getResult(CraftingContainer inv, Player player, String password);

}
