package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.gui.ContainerBlaceListLoliPickaxe;
import com.anotherstar.common.gui.ContainerLoliPickaxe;
import com.anotherstar.common.gui.ILoliInventory;
import com.anotherstar.common.gui.InventoryLoliBase;
import com.anotherstar.common.gui.MenuLoader;
import com.anotherstar.common.item.tool.IContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public class LoliPickaxeContainerOpenPackte {

	private int id;

	public LoliPickaxeContainerOpenPackte() {
	}

	public LoliPickaxeContainerOpenPackte(int id) {
		this.id = id;
	}

	public LoliPickaxeContainerOpenPackte(FriendlyByteBuf buf) {
		id = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(id);
	}

	public int getId() {
		return id;
	}

	public static void handle(LoliPickaxeContainerOpenPackte msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof IContainer)) {
			stack = player.getOffhandItem();
		}
		if (stack.isEmpty() || !(stack.getItem() instanceof IContainer) || !((IContainer) stack.getItem()).hasInventory(stack)) {
			ctx.get().setPacketHandled(true);
			return;
		}
		ILoliInventory inventory = ((IContainer) stack.getItem()).getInventory(stack);
		// 沿用原版从物品 NBT 读回的当前页，等价于 1.12.2 的 IInventory#openInventory
		int page = 0;
		if (inventory instanceof InventoryLoliBase) {
			InventoryLoliBase storage = (InventoryLoliBase) inventory;
			storage.startOpen(player);
			page = storage.getCurrentPage();
		}
		final ItemStack held = stack;
		final int targetPage = page;
		if (msg.getId() == MenuLoader.GUI_LOLI_PICKAXE_CONTAINER_BLACKLIST) {
			NetworkHooks.openScreen(player, new SimpleMenuProvider((id, playerInventory, playerIn) -> new ContainerBlaceListLoliPickaxe(id, playerInventory, held, 0), Component.empty()), buf -> buf.writeItem(held));
		} else if (msg.getId() == MenuLoader.GUI_LOLI_PICKAXE_CONTAINER) {
			NetworkHooks.openScreen(player, new SimpleMenuProvider((id, playerInventory, playerIn) -> new ContainerLoliPickaxe(id, playerInventory, held, targetPage), Component.empty()), buf -> {
				buf.writeItem(held);
				buf.writeVarInt(targetPage);
			});
		} else {
			// 原 LoliGUIHandler 只对储藏室/黑名单/密码工作台返回服务端容器，
			// 其余（配置/卡片/附魔/药水/空间折叠）都是纯客户端界面，由客户端自行 setScreen。
			LoliPickaxe.LOGGER.warn("LoliPickaxe: gui id {} has no server side menu", msg.getId());
		}
		ctx.get().setPacketHandled(true);
	}

}
