package com.anotherstar.common.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

public class SlotPasswordCrafting extends Slot {

	private final CraftingContainer craftMatrix;
	private final Player player;
	private int amountCrafted;

	public SlotPasswordCrafting(Player player, CraftingContainer craftingInventory, Container inventoryIn, int slotIndex, int xPosition, int yPosition) {
		super(inventoryIn, slotIndex, xPosition, yPosition);
		this.player = player;
		this.craftMatrix = craftingInventory;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack remove(int amount) {
		if (hasItem()) {
			amountCrafted += Math.min(amount, getItem().getCount());
		}
		return super.remove(amount);
	}

	protected void onCrafting(ItemStack stack, int amount) {
		amountCrafted += amount;
		onCrafting(stack);
	}

	@Override
	protected void onQuickCraft(ItemStack stack, int amount) {
		amountCrafted += amount;
		onCrafting(stack);
	}

	protected void onCrafting(ItemStack stack) {
		if (amountCrafted > 0) {
			stack.onCraftedBy(player.level(), player, amountCrafted);
			ForgeEventFactory.firePlayerCraftingEvent(player, stack, craftMatrix);
		}
		amountCrafted = 0;
	}

	@Override
	public void onTake(Player thePlayer, ItemStack stackIn) {
		onCrafting(stackIn);
		for (int i = 0; i < craftMatrix.getContainerSize(); ++i) {
			ItemStack stack = craftMatrix.getItem(i);
			if (!stack.isEmpty()) {
				craftMatrix.removeItem(i, 1);
			}
		}
	}

}
