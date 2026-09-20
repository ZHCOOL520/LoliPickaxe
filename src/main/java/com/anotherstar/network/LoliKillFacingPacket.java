package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

public class LoliKillFacingPacket {

	public LoliKillFacingPacket() {
	}

	public LoliKillFacingPacket(FriendlyByteBuf buf) {
	}

	public void encode(FriendlyByteBuf buf) {
	}

	public static void handle(LoliKillFacingPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		LoliPickaxeUtil.killFacing(player);
		player.level().playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MODID, "lolisuccess")), SoundSource.BLOCKS, 1.0F, 1.0F);
		ctx.get().setPacketHandled(true);
	}

}
