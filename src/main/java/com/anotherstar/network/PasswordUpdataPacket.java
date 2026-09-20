package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.gui.ContainerPasswordWorkbench;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

public class PasswordUpdataPacket {

	private String password;

	public PasswordUpdataPacket() {
	}

	public PasswordUpdataPacket(String password) {
		this.password = password;
	}

	public PasswordUpdataPacket(FriendlyByteBuf buf) {
		password = buf.readUtf();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(password);
	}

	public String getPassword() {
		return password;
	}

	public static void handle(PasswordUpdataPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		AbstractContainerMenu container = player.containerMenu;
		if (container instanceof ContainerPasswordWorkbench) {
			((ContainerPasswordWorkbench) container).setPassword(msg.getPassword());
		}
		ctx.get().setPacketHandled(true);
	}

}
