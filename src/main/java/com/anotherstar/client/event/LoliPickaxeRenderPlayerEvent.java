package com.anotherstar.client.event;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.tool.ILoli;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LoliPickaxeRenderPlayerEvent {

	@SubscribeEvent
	public void onPlayerRender(RenderPlayerEvent.Pre event) {
		Player player = event.getEntity();
		ItemStack loli = LoliPickaxeUtil.getLoliPickaxe(player);
		if (!loli.isEmpty() && ConfigLoader.getBoolean(loli, "loliPickaxeInvisible")) {
			event.setCanceled(true);
		}
	}

}
