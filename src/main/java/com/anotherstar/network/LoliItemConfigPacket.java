package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.tool.ILoli;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoliItemConfigPacket {

	private CompoundTag data;

	public LoliItemConfigPacket() {
	}

	public LoliItemConfigPacket(CompoundTag data) {
		this.data = data;
	}

	public LoliItemConfigPacket(FriendlyByteBuf buf) {
		data = buf.readNbt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeNbt(data);
	}

	public CompoundTag getData() {
		return data;
	}

	public static void handle(LoliItemConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack stack = player.getMainHandItem();
		if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
			ConfigLoader.setItemConfigs(stack, msg.getData());
		}
		ctx.get().setPacketHandled(true);
	}

}
