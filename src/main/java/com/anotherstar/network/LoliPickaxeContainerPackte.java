package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.gui.ContainerLoliPickaxe;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

public class LoliPickaxeContainerPackte {

	private boolean next;

	public LoliPickaxeContainerPackte() {
	}

	public LoliPickaxeContainerPackte(boolean next) {
		this.next = next;
	}

	public LoliPickaxeContainerPackte(FriendlyByteBuf buf) {
		next = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(next);
	}

	public boolean isNext() {
		return next;
	}

	public static void handle(LoliPickaxeContainerPackte msg, Supplier<NetworkEvent.Context> ctx) {
		AbstractContainerMenu container = ctx.get().getSender().containerMenu;
		if (container instanceof ContainerLoliPickaxe) {
			if (msg.isNext()) {
				((ContainerLoliPickaxe) container).nextPage();
			} else {
				((ContainerLoliPickaxe) container).prePage();
			}
		}
		ctx.get().setPacketHandled(true);
	}

}
