package com.anotherstar.common.gui;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface ILoliInventory extends Container {

	int getMaxPage();

	NonNullList<ItemStack> getPage(int index);

	boolean cancelStackLimit();

}
