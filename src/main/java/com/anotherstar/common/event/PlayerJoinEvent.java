package com.anotherstar.common.event;

import com.anotherstar.common.config.ConfigLoader;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerJoinEvent {

	/**
	 * 本项目（1.20.1 Forge 移植版）的开源地址。
	 *
	 * <p>玩家进服提示与点击跳转均使用此常量。
	 * 原 1.12.2 作者 Is_GK 的仓库地址见语言文件与 {@code mods.toml} 的 credits，
	 * 署名与 GPL-3.0 许可声明保持不变。
	 */
	private static final String PROJECT_URL = "https://github.com/ZHCOOL520/LoliPickaxe";

	@SubscribeEvent
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			ConfigLoader.sandChange(player);
			MutableComponent message = Component.literal("§2LoliPickaxe§f开源地址: ");
			MutableComponent submsg = Component.literal("§9§n" + PROJECT_URL);
			submsg.setStyle(submsg.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, PROJECT_URL)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击前往链接"))));
			message.append(submsg);
			player.sendSystemMessage(message);
		}
	}

}
