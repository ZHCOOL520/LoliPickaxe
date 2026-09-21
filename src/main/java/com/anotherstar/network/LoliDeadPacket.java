package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public class LoliDeadPacket {

	private boolean gui;
	private boolean blueScreen;
	private boolean exit;
	private boolean failRespond;

	public LoliDeadPacket() {
	}

	public LoliDeadPacket(boolean gui, boolean blueScreen, boolean exit, boolean failRespond) {
		this.gui = gui;
		this.blueScreen = blueScreen;
		this.exit = exit;
		this.failRespond = failRespond;
	}

	public LoliDeadPacket(FriendlyByteBuf buf) {
		gui = buf.readBoolean();
		blueScreen = buf.readBoolean();
		exit = buf.readBoolean();
		failRespond = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(gui);
		buf.writeBoolean(blueScreen);
		buf.writeBoolean(exit);
		buf.writeBoolean(failRespond);
	}

	public boolean isGui() {
		return gui;
	}

	public boolean isBlueScreen() {
		return blueScreen;
	}

	public boolean isExit() {
		return exit;
	}

	public boolean isFailRespond() {
		return failRespond;
	}

	public static void handle(LoliDeadPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (msg.isGui() && mc.player != null && !(mc.screen instanceof DeathScreen)) {
				mc.setScreen(new DeathScreen(mc.player.getCombatTracker().getDeathMessage(), false));
			}
			// 1.20.1 降级：不再释放并执行 BlueScreen.exe。
			// 表现方式改为「强制掉线」（见 disconnectClient 说明），既保留原版
			// 「被打击后客户端被打断」的玩法体感，又不会像 Runtime.halt 那样
			// 直接杀死 JVM（那样会跳过所有关闭钩子、可能损坏存档，
			// 且在专用服务器侧会连带杀掉整个服务端进程）。
			if (msg.isBlueScreen()) {
				LoliPickaxe.LOGGER.warn("LoliPickaxe: blue screen attack");
				disconnectClient("LoliPickaxe: blue screen");
			}
			// 1.20.1 降级：不再真的让客户端死循环（会导致无法调试），只记录日志。
			if (msg.isFailRespond()) {
				LoliPickaxe.LOGGER.warn("LoliPickaxe: fail respond attack");
			}
			if (msg.isExit()) {
				LoliPickaxe.LOGGER.warn("LoliPickaxe: exit attack");
				disconnectClient("LoliPickaxe: exit");
			}
		});
		ctx.get().setPacketHandled(true);
	}

	/**
	 * 让客户端安全断开与服务器的连接。
	 *
	 * <p><b>为什么不沿用 {@code Runtime.halt(0)}</b>：{@code halt} 会<b>立即</b>终止 JVM，
	 * 不执行任何关闭钩子、不给存档刷盘机会。在大型整合包中这会带来两个真实风险：
	 * <ol>
	 *   <li>客户端正在写存档/日志时被硬杀，可能造成存档损坏；</li>
	 *   <li>若该包在专用服务器侧被处理到，会直接杀死<b>整个服务端进程</b>，
	 *       影响所有在线玩家（这是不可接受的服务器级风险）。</li>
	 * </ol>
	 *
	 * <p>改用原版的「断开连接」通道：玩家会被踢回主菜单并看到提示。
	 * 对玩家而言，「被打击 → 突然掉线」的玩法体感与原版一致，
	 * 但进程与存档是安全的。这里<b>只改变实现手段，不改变玩法结果</b>。
	 *
	 * @param reason 断开提示文本（显示在客户端的断开界面）
	 */
	private static void disconnectClient(String reason) {
		Minecraft mc = Minecraft.getInstance();
		// 仅在客户端上下文中有意义；服务端侧不应有任何副作用
		if (mc.getConnection() != null) {
			mc.getConnection().getConnection().disconnect(Component.literal(reason));
		}
	}

}
