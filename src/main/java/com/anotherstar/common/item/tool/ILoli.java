package com.anotherstar.common.item.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ILoli extends IContainer {

	String CONFIG = "LoliConfig";

	boolean hasOwner(ItemStack stack);

	boolean isOwner(ItemStack stack, Player player);

	int getRange(ItemStack stack);

}
