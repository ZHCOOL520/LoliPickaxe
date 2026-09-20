package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.item.ItemLoliCardOnline;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoliCardOnlinePacket {

	private String name;

	public LoliCardOnlinePacket() {
	}

	public LoliCardOnlinePacket(String name) {
		this.name = name;
	}

	public LoliCardOnlinePacket(FriendlyByteBuf buf) {
		name = buf.readUtf();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(name);
	}

	public String getName() {
		return name;
	}

	public static void handle(LoliCardOnlinePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack stack = player.getMainHandItem();
		if (!stack.isEmpty() && stack.getItem() instanceof ItemLoliCardOnline) {
			stack.getOrCreateTag().putString("ImageUrl", msg.getName());
		}
		ctx.get().setPacketHandled(true);
	}

}
