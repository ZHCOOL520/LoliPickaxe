package com.anotherstar.common.gui;

import com.anotherstar.common.block.BlockLoader;
import com.anotherstar.common.recipe.password.PasswordRecipeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ContainerPasswordWorkbench extends AbstractContainerMenu {

	/** 容器自身的 MenuType，供开放界面与客户端注册 Screen 使用。 */
	public static final MenuType<ContainerPasswordWorkbench> PASSWORD_WORK_BENCH_MENU = MenuLoader.PASSWORD_WORK_BENCH_MENU;

	public TransientCraftingContainer craftMatrix = new TransientCraftingContainer(this, 3, 3);
	public ResultContainer craftResult = new ResultContainer();
	private final Level world;
	private final BlockPos pos;
	private final Player player;
	private String password;

	public ContainerPasswordWorkbench(int id, Inventory playerInventory, BlockPos posIn) {
		super(PASSWORD_WORK_BENCH_MENU, id);
		this.player = playerInventory.player;
		this.world = this.player.level();
		this.pos = posIn;
		this.addSlot(new SlotPasswordCrafting(this.player, this.craftMatrix, this.craftResult, 0, 124, 65));
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				this.addSlot(new Slot(this.craftMatrix, j + i * 3, 30 + j * 18, 47 + i * 18));
			}
		}
		for (int k = 0; k < 3; ++k) {
			for (int i1 = 0; i1 < 9; ++i1) {
				this.addSlot(new Slot(playerInventory, i1 + k * 9 + 9, 8 + i1 * 18, 114 + k * 18));
			}
		}
		for (int l = 0; l < 9; ++l) {
			this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 172));
		}
		password = "";
	}

	/** 供 {@code NetworkHooks.openScreen(player, provider, pos)} 使用。 */
	public ContainerPasswordWorkbench(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
		this(id, playerInventory, extraData == null ? BlockPos.ZERO : extraData.readBlockPos());
	}

	public void setPassword(String password) {
		this.password = password;
		slotsChanged(craftMatrix);
	}

	@Override
	public void slotsChanged(Container inventoryIn) {
		if (!world.isClientSide) {
			ItemStack stack = PasswordRecipeManager.findMatchingResult(craftMatrix, player, password);
			craftResult.setItem(0, stack);
			this.setRemoteSlot(0, stack);
			if (player instanceof ServerPlayer) {
				((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, stack));
			}
		}
	}

	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		if (!world.isClientSide) {
			clearContainer(playerIn, craftMatrix);
		}
	}

	@Override
	public boolean stillValid(Player playerIn) {
		if (world.getBlockState(pos).getBlock() != BlockLoader.passwordWorkBench()) {
			return false;
		} else {
			return playerIn.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
		}
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack resultStack = ItemStack.EMPTY;
		// 越界保护：避免整理/转移类模组传入异常索引导致服务端 IndexOutOfBoundsException 崩溃
		if (index < 0 || index >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			resultStack = slotStack.copy();
			if (index == 0) {
				slotStack.onCraftedBy(this.world, playerIn, slotStack.getCount());
				if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
					return ItemStack.EMPTY;
				}
			} else if (index >= 10 && index < 37) {
				if (!this.moveItemStackTo(slotStack, 37, 46, false)) {
					return ItemStack.EMPTY;
				}
			} else if (index >= 37 && index < 46) {
				if (!this.moveItemStackTo(slotStack, 10, 37, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(slotStack, 10, 46, false)) {
				return ItemStack.EMPTY;
			}
			if (slotStack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (slotStack.getCount() == resultStack.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(playerIn, slotStack);
			if (index == 0) {
				playerIn.drop(slotStack, false);
			}
		}
		return resultStack;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
		return slotIn.container != craftResult && super.canTakeItemForPickAll(stack, slotIn);
	}

}
