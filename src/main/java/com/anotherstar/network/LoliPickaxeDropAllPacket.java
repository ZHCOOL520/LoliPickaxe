package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.gui.ILoliInventory;
import com.anotherstar.common.item.tool.IContainer;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoliPickaxeDropAllPacket {

	public LoliPickaxeDropAllPacket() {
	}

	public LoliPickaxeDropAllPacket(FriendlyByteBuf buf) {
	}

	public void encode(FriendlyByteBuf buf) {
	}

	public static void handle(LoliPickaxeDropAllPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack loli = player.getMainHandItem();
		if (loli.isEmpty() || !(loli.getItem() instanceof IContainer)) {
			loli = player.getOffhandItem();
		}
		if (!loli.isEmpty() && loli.getItem() instanceof IContainer && ((IContainer) loli.getItem()).hasInventory(loli)) {
			ILoliInventory inventory = ((IContainer) loli.getItem()).getInventory(loli);
			inventory.startOpen(player);
			NonNullList<ItemStack> stacks = NonNullList.create();
			for (int i = 0; i < inventory.getMaxPage(); i++) {
				for (ItemStack stack : inventory.getPage(i)) {
					if (!stack.isEmpty()) {
						stacks.add(stack);
					}
				}
			}
			inventory.clearContent();
			inventory.stopOpen(player);
			for (ItemStack stack : stacks) {
				player.drop(stack, true, false);
			}
		}
		ctx.get().setPacketHandled(true);
	}

}
