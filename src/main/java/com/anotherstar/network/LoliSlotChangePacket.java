package com.anotherstar.network;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoliSlotChangePacket {

	private int windowId;
	private int slotIndex;
	private ItemStack stack;

	public LoliSlotChangePacket() {
	}

	public LoliSlotChangePacket(int windowId, int slotIndex, ItemStack stack) {
		this.windowId = windowId;
		this.slotIndex = slotIndex;
		this.stack = stack.copy();
	}

	public LoliSlotChangePacket(FriendlyByteBuf buf) {
		windowId = buf.readByte();
		slotIndex = buf.readShort();
		stack = buf.readItem();
		// 【必须显式补发数量】原版 writeItem/readItem（FriendlyByteBuf.writeItemStack）内部用
		// writeByte(count) 传输堆叠数，readItem 用 readByte() 还原，上限仅 127（甚至会被解释为负数）。
		// 本包的存在意义正是同步「超过原版上限的堆叠数」，因此必须在此额外用 VarInt 传递真实数量；
		// 否则 2000000000 会被 writeByte 截断成 0，客户端反而把 0 当作真实值写回槽位。
		if (!stack.isEmpty()) {
			stack.setCount(buf.readVarInt());
		}
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeByte(windowId);
		buf.writeShort(slotIndex);
		buf.writeItem(stack);
		// 与 LoliSlotChangePacket(FriendlyByteBuf) 中的读取顺序严格对应
		if (!stack.isEmpty()) {
			buf.writeVarInt(stack.getCount());
		}
	}

	public int getWindowId() {
		return windowId;
	}

	public int getSlotIndex() {
		return slotIndex;
	}

	public ItemStack getStack() {
		return stack;
	}

	public static void handle(LoliSlotChangePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if (player == null) {
				return;
			}
			ItemStack itemstack = msg.getStack();
			int i = msg.getSlotIndex();
			mc.getTutorial().onGetItem(itemstack);
			if (msg.getWindowId() == -1) {
				player.containerMenu.setCarried(itemstack);
			} else if (msg.getWindowId() == -2) {
				if (i >= 0 && i < player.getInventory().getContainerSize()) {
					player.getInventory().setItem(i, itemstack);
				}
			} else {
				// 1.20.1 没有 CreativeTabs.INVENTORY.getTabIndex()，直接按 windowId 判断容器
				AbstractContainerMenu container = null;
				if (msg.getWindowId() == 0) {
					container = player.inventoryMenu;
				} else if (msg.getWindowId() == player.containerMenu.containerId) {
					container = player.containerMenu;
				}
				if (container != null && i >= 0 && i < container.slots.size()) {
					if (msg.getWindowId() == 0 && msg.getSlotIndex() >= 36) {
						ItemStack old = container.getSlot(i).getItem();
						if (!itemstack.isEmpty() && (old.isEmpty() || old.getCount() < itemstack.getCount())) {
							itemstack.setPopTime(5);
						}
					}
					container.getSlot(i).set(itemstack);
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
