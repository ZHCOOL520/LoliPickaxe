package com.anotherstar.client.gui;

import java.util.Random;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 氪金萝莉「蓝屏攻击」的客户端表现。
 *
 * <p><b>为什么不是原版实现</b>：1.12.2 原版把一段 38.7 KB 的 PE 可执行文件以字节数组内嵌在
 * {@code LoliDeadPacket} 里，运行时写盘并用 {@code Runtime.exec} 启动。对该二进制做静态分析后确认，
 * 它申请 Windows 的 {@code SeShutdownPrivilege} 提权，再调用 {@code ntdll.dll!ZwRaiseHardError}
 * —— 这是触发<b>真实系统蓝屏（BSOD）</b>的未公开 NT 调用。也就是说它唯一的用途是让玩家的电脑宕机。
 *
 * <p>因此本移植版改为<b>在 Minecraft 内渲染一个仿真蓝屏界面</b>：不写盘、不执行任何外部程序、
 * 不调用任何 Windows API，玩家看到的画面与真实蓝屏高度一致，但宿主系统完全不受影响。
 *
 * <p>流程刻意还原原版「被打中后掉线」的体感：
 * <ol>
 *   <li>进入本界面，进度从 0% 递增到 100%（{@code loliBlueScreenDuration} 毫秒，默认 6 秒）</li>
 *   <li>进度满后<b>真实断开</b>与服务器的连接</li>
 *   <li>停留在此界面，由玩家自行关闭游戏</li>
 * </ol>
 *
 * <p>本类只在客户端加载（{@link OnlyIn}），服务端不会走到这里。
 */
@OnlyIn(Dist.CLIENT)
public class GUILoliBlueScreen extends Screen {

	/** Windows 10 蓝屏的背景色（#0078D7）。 */
	private static final int BG_COLOR = 0xFF0078D7;
	/** 蓝屏正文用的白色。 */
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	/** 进度从 0% 走到 100% 的总时长（毫秒）。 */
	private final long durationMillis;

	/** 界面创建时刻（{@link System#nanoTime()}）。 */
	private final long startNanos;

	/** 进度满后是否已经执行断线，保证只断一次。 */
	private boolean disconnected;

	/** 二维码的伪随机点阵，构造时生成一次，保证每帧一致。 */
	private final boolean[][] qrMatrix;

	/**
	 * @param durationMillis 进度动画时长（毫秒），小于等于 0 时取默认 6000
	 */
	public GUILoliBlueScreen(int durationMillis) {
		super(Component.literal(":( "));
		this.durationMillis = durationMillis > 0 ? durationMillis : 6000L;
		this.startNanos = System.nanoTime();
		// 21x21 的伪随机点阵，仅用于视觉还原，不承载任何真实信息
		this.qrMatrix = new boolean[21][21];
		Random random = new Random(0x10C0FFEEL);
		for (int y = 0; y < 21; y++) {
			for (int x = 0; x < 21; x++) {
				this.qrMatrix[y][x] = random.nextBoolean();
			}
		}
		// 三个定位角：真实二维码的显著特征，画出来才像
		markFinderPattern(0, 0);
		markFinderPattern(14, 0);
		markFinderPattern(0, 14);
	}

	/** 把二维码左上/右上/左下三个 7x7 定位图案涂实。 */
	private void markFinderPattern(int originX, int originY) {
		for (int y = 0; y < 7; y++) {
			for (int x = 0; x < 7; x++) {
				boolean edge = x == 0 || x == 6 || y == 0 || y == 6;
				boolean core = x >= 2 && x <= 4 && y >= 2 && y <= 4;
				this.qrMatrix[originY + y][originX + x] = edge || core;
			}
		}
	}

	/** 当前进度百分比（0~100）。 */
	private int percent() {
		long elapsed = (System.nanoTime() - this.startNanos) / 1_000_000L;
		return (int) Mth.clamp(elapsed * 100L / this.durationMillis, 0L, 100L);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 整屏铺蓝：不调用 super.render，避免渲染任何原版界面元素
		graphics.fill(0, 0, this.width, this.height, BG_COLOR);

		int percent = percent();
		if (percent >= 100 && !this.disconnected) {
			this.disconnected = true;
			disconnectFromServer();
		}

		// 版面按屏幕高度等比缩放，保证小窗口与大分辨率下观感一致
		float scale = Math.max(1.0F, Math.min(this.width / 640.0F, this.height / 360.0F));
		int left = (int) (this.width * 0.10F);
		int top = (int) (this.height * 0.16F);
		int lineHeight = (int) (18 * scale);

		// 巨大的 :( 表情
		graphics.pose().pushPose();
		graphics.pose().scale(scale * 4.0F, scale * 4.0F, 1.0F);
		graphics.drawString(this.font, ":(", (int) (left / (scale * 4.0F)), (int) (top / (scale * 4.0F)), TEXT_COLOR, false);
		graphics.pose().popPose();

		int y = top + (int) (lineHeight * 5.5F);
		y = drawLine(graphics, "你的电脑遇到问题，需要重新启动。我们只收集某些错误信息，然后为你重新启动。", left, y, scale);
		y += lineHeight / 2;
		y = drawLine(graphics, percent + "% 完成", left, y, scale);

		y += lineHeight;
		y = drawLine(graphics, "如需了解更多信息，请稍后访问以下网址：", left, y, scale);
		y = drawLine(graphics, "https://github.com/ZHCOOL520/LoliPickaxe", left, y, scale);

		y += lineHeight;
		y = drawLine(graphics, "停止代码：LOLI_PICKAXE_BLUE_SCREEN", left, y, scale);
		y = drawLine(graphics, "失败的操作：lolipickaxe.sys", left, y, scale);

		// 右下角二维码
		drawQrCode(graphics, this.width - (int) (140 * scale), this.height - (int) (140 * scale), (int) (7 * scale));

		// 顶部提示：告诉玩家这只是游戏内效果，避免真的以为电脑坏了
		String hint = "游戏内特效 · 按 ESC 退出蓝屏界面";
		graphics.drawString(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 14, 0xFF9CC3E5, false);
	}

	/** 画一行正文，返回下一行的 y 坐标。 */
	private int drawLine(GuiGraphics graphics, String text, int x, int y, float scale) {
		graphics.pose().pushPose();
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, (int) (x / scale), (int) (y / scale), TEXT_COLOR, false);
		graphics.pose().popPose();
		return y + (int) (12 * scale);
	}

	/** 画二维码点阵（白色方块）。 */
	private void drawQrCode(GuiGraphics graphics, int x, int y, int cell) {
		for (int row = 0; row < 21; row++) {
			for (int col = 0; col < 21; col++) {
				if (this.qrMatrix[row][col]) {
					graphics.fill(x + col * cell, y + row * cell, x + col * cell + cell, y + row * cell + cell, TEXT_COLOR);
				}
			}
		}
	}

	/**
	 * 断开与服务器的连接，对应原版「被打中后掉线」的表现。
	 *
	 * <p>只操作客户端的连接对象；服务端会把这次断开当作普通掉线处理，不受任何影响。
	 */
	private void disconnectFromServer() {
		try {
			if (this.minecraft != null && this.minecraft.getConnection() != null) {
				this.minecraft.getConnection().getConnection().disconnect(Component.literal("LoliPickaxe: blue screen"));
			}
		} catch (Exception e) {
			LoliPickaxe.LOGGER.warn("LoliPickaxe: blue screen disconnect failed", e);
		}
	}

	/**
	 * 是否允许 ESC 关闭界面。
	 *
	 * <p>返回 {@code true}：玩家可以按 ESC 退出蓝屏界面。
	 * 这是刻意保留的<b>逃生通道</b> —— 原版依赖真实系统蓝屏，玩家除了重启电脑别无选择；
	 * 本实现既然不触碰宿主系统，就必须给玩家一个不用强杀进程的退出方式。
	 */
	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	/** 屏蔽 F1~F12、E、T 等原版快捷键，避免玩家在"蓝屏"时还能操作。 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// 只允许 ESC（256）退出，其余按键一律吞掉
		if (keyCode == 256) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		return true;
	}

	/** 蓝屏界面暂停游戏渲染之外的逻辑（与原版死亡界面一致）。 */
	@Override
	public boolean isPauseScreen() {
		return false;
	}

}
