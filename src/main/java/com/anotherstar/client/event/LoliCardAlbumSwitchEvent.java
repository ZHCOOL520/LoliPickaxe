package com.anotherstar.client.event;

import com.anotherstar.client.render.RenderLoliCardFrame;
import com.anotherstar.common.config.ConfigLoader;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LoliCardAlbumSwitchEvent {

	private int tick = 0;

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			if (++tick >= ConfigLoader.loliCardAlbumSwitchSpeed) {
				tick = 0;
				if (++RenderLoliCardFrame.step == Integer.MAX_VALUE) {
					RenderLoliCardFrame.step = 0;
				}
			}
		}
	}

}
