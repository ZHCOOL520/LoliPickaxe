package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
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
		if (player == null) {
			ctx.get().setPacketHandled(true);
			return;
		}
		// 【服务端权威】客户端可以伪造任意包，因此必须在此校验：
		// 1) 玩家确实持有属于本人的萝莉镐（否则任意客户端都能远程范围秒杀）；
		// 2) 该玩家当前配置中「左键范围攻击」处于开启状态。
		// 客户端 GUI 的同类判断只是体验优化，不能作为安全依据。
		if (!LoliPickaxeUtil.invHaveLoliPickaxe(player)) {
			ctx.get().setPacketHandled(true);
			return;
		}
		ItemStack stack = LoliPickaxeUtil.getLoliPickaxe(player);
		if (stack.isEmpty() || !ConfigLoader.getBoolean(stack, "loliPickaxeKillFacing")) {
			ctx.get().setPacketHandled(true);
			return;
		}
		LoliPickaxeUtil.killFacing(player);
		player.level().playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MODID, "lolisuccess")), SoundSource.BLOCKS, 1.0F, 1.0F);
		ctx.get().setPacketHandled(true);
	}

}
