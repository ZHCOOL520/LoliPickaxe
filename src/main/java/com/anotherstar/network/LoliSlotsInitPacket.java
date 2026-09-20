package com.anotherstar.network;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoliSlotsInitPacket {

	private int windowId;
	private List<ItemStack> stacks;

	public LoliSlotsInitPacket() {
	}

	public LoliSlotsInitPacket(int windowId, List<ItemStack> stacks) {
		this.windowId = windowId;
		this.stacks = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
		for (int i = 0; i < this.stacks.size(); ++i) {
			ItemStack stack = stacks.get(i);
			this.stacks.set(i, stack.copy());
		}
	}

	public LoliSlotsInitPacket(FriendlyByteBuf buf) {
		windowId = buf.readUnsignedByte();
		int size = Math.max(buf.readShort(), 0);
		stacks = NonNullList.withSize(size, ItemStack.EMPTY);
		for (int j = 0; j < size; ++j) {
			stacks.set(j, buf.readItem());
		}
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeByte(windowId);
		buf.writeShort(stacks.size());
		for (ItemStack stack : stacks) {
			buf.writeItem(stack);
		}
	}

	public int getWindowId() {
		return windowId;
	}

	public List<ItemStack> getStacks() {
		return stacks;
	}

	public static void handle(LoliSlotsInitPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if (player == null) {
				return;
			}
			AbstractContainerMenu container = null;
			if (msg.getWindowId() == 0) {
				container = player.inventoryMenu;
			} else if (msg.getWindowId() == player.containerMenu.containerId) {
				container = player.containerMenu;
			}
			if (container == null) {
				return;
			}
			List<ItemStack> stacks = msg.getStacks();
			for (int i = 0; i < stacks.size() && i < container.slots.size(); i++) {
				container.getSlot(i).set(stacks.get(i));
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
