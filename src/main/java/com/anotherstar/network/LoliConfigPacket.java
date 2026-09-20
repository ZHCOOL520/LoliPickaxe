package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.config.ConfigLoader;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class LoliConfigPacket {

	private CompoundTag data;

	public LoliConfigPacket() {
	}

	public LoliConfigPacket(CompoundTag data) {
		this.data = data;
	}

	public LoliConfigPacket(FriendlyByteBuf buf) {
		data = buf.readNbt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeNbt(data);
	}

	public CompoundTag getData() {
		return data;
	}

	public static void handle(LoliConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> receptionChange(msg));
		ctx.get().setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private static void receptionChange(LoliConfigPacket msg) {
		ConfigLoader.receptionChange(msg.getData());
	}

}
