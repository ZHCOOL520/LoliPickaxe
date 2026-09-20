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

	@SubscribeEvent
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			ConfigLoader.sandChange(player);
			MutableComponent message = Component.literal("§2LoliPickaxe§f开源地址: ");
			MutableComponent submsg = Component.literal("§9§nhttps://github.com/IslenautsGK/LoliPickaxe");
			submsg.setStyle(submsg.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/IslenautsGK/LoliPickaxe")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击前往链接"))));
			message.append(submsg);
			player.sendSystemMessage(message);
		}
	}

}
