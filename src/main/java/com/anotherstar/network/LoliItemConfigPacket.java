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
		if (player == null) {
			ctx.get().setPacketHandled(true);
			return;
		}
		// 【服务端权威】该包的内容完全来自客户端，必须视为不可信输入。
		// 原实现直接把客户端传来的整个 CompoundTag 写进物品 NBT，存在两个严重后果：
		//   1) 客户端可覆写同一 root tag 下的 Owner/OwnerUUID，从而绕过
		//      checkOwner / invHaveLoliPickaxe 的归属校验，拿到不属于自己的萝莉镐的全部强力效果；
		//   2) 客户端可注入任意配置键（如 loliPickaxeClearInventory / KickPlayer / BeyondRedemption），
		//      而这些键在服务端会被 ConfigLoader.getBoolean(stack, ...) 真实读取。
		// 因此改为「服务端按白名单逐项重建」：只接受 GUI 可改且列入 loliPickaxeGuiChangeList 的项，
		// 且每个值都经 setInt/setDouble/setBoolean/setString 重新做类型与范围收敛。
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof ILoli)) {
			ctx.get().setPacketHandled(true);
			return;
		}
		// 归属校验：不属于该玩家的萝莉镐不接受任何配置写入
		ILoli loli = (ILoli) stack.getItem();
		if (loli.hasOwner(stack) && !loli.isOwner(stack, player)) {
			ctx.get().setPacketHandled(true);
			return;
		}
		ConfigLoader.applyItemConfigs(stack, msg.getData());
		// 【关键】把写入结果同步回客户端。
		// 服务端改动物品 NBT 后，原版不会自动通知客户端，而 GUILoliConfig 读取的是
		// 客户端侧物品堆的 LoliConfig 节。若不同步，玩家会出现：
		//   点击 true → 关闭界面 → 重开界面又变回 false，
		// 表现为「点了 true 或 false 之后都没用」。
		// broadcastChanges 会逐槽比对 remoteSlots 与当前槽位，NBT 有差异时下发更新包，
		// 因此这里调用一次即可把最新的 LoliConfig 送到客户端。
		player.inventoryMenu.broadcastChanges();
		ctx.get().setPacketHandled(true);
	}

}
