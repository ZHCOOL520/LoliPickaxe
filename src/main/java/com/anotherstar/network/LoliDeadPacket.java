package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.client.gui.GUILoliBlueScreen;
import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

/**
 * 氪金萝莉「特效攻击」包（服务端 → 客户端）。
 *
 * <p>承载三种攻击：蓝屏、崩溃（蹦溃）、未响应。四个布尔字段的编码顺序
 * 必须与 1.12.2 保持一致（gui, blueScreen, exit, failRespond），不得调整。
 *
 * <h2>与 1.12.2 原版的实现差异</h2>
 *
 * <p><b>蓝屏</b>：原版内嵌了一个 38.7 KB 的 PE 可执行文件并 {@code Runtime.exec} 运行它。对该二进制做
 * 静态分析（提取字节 → MZ 头 → 导入表）后确认：它调用 {@code ADVAPI32!LookupPrivilegeValueA} /
 * {@code AdjustTokenPrivileges} 为自己开启 {@code SeShutdownPrivilege} 关机特权，再调用
 * {@code ntdll!ZwRaiseHardError} —— 这是触发<b>真实系统蓝屏（BSOD）</b>的未公开 NT 调用。
 * 其 PDB 路径为 {@code C:\Users\dhsth\source\repos\BlueScreen\Release\BlueScreen.pdb}，
 * 即一个专门用于「弄崩用户电脑」的程序。
 * <br>本移植版<b>不内嵌、不写盘、不执行</b>该文件，改为游戏内仿真蓝屏界面（{@link GUILoliBlueScreen}），
 * 玩家观感一致，宿主系统不受影响。
 *
 * <p><b>未响应</b>：原版 {@code while(true);} 开满所有 CPU 核心 + 一个主线程死循环，会把整机拖死。
 * 本移植版改为<b>阻塞客户端主线程一个有限时长</b>（默认 15 秒，配置 {@code loliFailRespondDuration}），
 * 足以让操作系统把窗口标记为「未响应」并显示幽灵窗口，但不会永久卡死，也不会占用全部核心。
 *
 * <p><b>崩溃</b>：原版 {@code FMLCommonHandler.exitJava(0, true)} 的语义就是立即终止 JVM，
 * 本移植版用 {@code Runtime.halt(0)} 等价还原。这是原版既有行为，不做"安全化"改写，
 * 但严格限制为<b>只在客户端进程</b>执行，专用服务端不会因此被终止。
 */
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
			// 三种攻击都是纯粹的客户端表现。专用服务端收到此包（理论上不会发生）时直接忽略，
			// 避免在服务端进程里执行 halt 之类的破坏性操作。
			if (FMLEnvironment.dist != Dist.CLIENT) {
				LoliPickaxe.LOGGER.warn("LoliPickaxe: received effect attack packet on a dedicated server, ignored");
				return;
			}
			applyOnClient(msg);
		});
		ctx.get().setPacketHandled(true);
	}

	/** 客户端侧的实际执行逻辑。 */
	private static void applyOnClient(LoliDeadPacket msg) {
		Minecraft mc = Minecraft.getInstance();

		// 原版的"死亡界面"表现（gui 标志），与三种攻击相互独立
		if (msg.isGui() && mc.player != null && !(mc.screen instanceof DeathScreen)) {
			mc.setScreen(new DeathScreen(mc.player.getCombatTracker().getDeathMessage(), false));
		}

		if (msg.isBlueScreen()) {
			LoliPickaxe.LOGGER.warn("LoliPickaxe: blue screen attack");
			mc.setScreen(new GUILoliBlueScreen(ConfigLoader.loliBlueScreenDuration));
		}

		if (msg.isFailRespond()) {
			LoliPickaxe.LOGGER.warn("LoliPickaxe: fail respond attack");
			freezeClientFor(ConfigLoader.loliFailRespondDuration);
		}

		if (msg.isExit()) {
			LoliPickaxe.LOGGER.warn("LoliPickaxe: exit attack");
			// 与原版 FMLCommonHandler.exitJava(0, true) 语义一致：立即终止 JVM。
			// halt 不执行关闭钩子、不刷盘 —— 这与原版行为相同，属于该攻击的固有特性。
			Runtime.getRuntime().halt(0);
		}
	}

	/**
	 * 阻塞客户端主线程，触发操作系统级的「程序未响应」。
	 *
	 * <p>用自旋而非 {@code Thread.sleep}：{@code sleep} 会主动让出时间片，窗口不会进入未响应状态。
	 * 这里刻意只占用<b>当前这一个线程</b>（即客户端主线程），不额外开线程、不占满 CPU 核心，
	 * 因此系统整体仍然可用，玩家可以正常点「关闭程序」。
	 *
	 * @param millis 卡死时长（毫秒）；小于等于 0 时不执行任何操作
	 */
	private static void freezeClientFor(int millis) {
		if (millis <= 0) {
			return;
		}
		long deadline = System.nanoTime() + millis * 1_000_000L;
		// 自旋等待：不释放 CPU，让系统判定该窗口无响应
		while (System.nanoTime() < deadline) {
			// 空转，故意不做任何事
		}
	}

	/**
	 * 让客户端安全断开与服务器的连接。
	 *
	 * @param reason 断开提示文本（显示在客户端的断开界面）
	 */
	@SuppressWarnings("unused")
	private static void disconnectClient(String reason) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() != null) {
			mc.getConnection().getConnection().disconnect(Component.literal(reason));
		}
	}

}
