package com.anotherstar.client.event;

import com.anotherstar.client.gui.GUILoliConfig;
import com.anotherstar.client.gui.GUILoliEnchantment;
import com.anotherstar.client.gui.GUILoliPotion;
import com.anotherstar.client.gui.GUILoliSpaceFolding;
import com.anotherstar.client.key.KeyLoader;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.gui.MenuLoader;
import com.anotherstar.common.item.tool.ILoli;
import com.anotherstar.network.LoliPickaxeContainerOpenPackte;
import com.anotherstar.network.LoliPickaxeDropAllPacket;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 1.12.2 使用 org.lwjgl.input.Keyboard + KeyInputEvent + player.openGui，
 * 1.20.1 改为 InputEvent.Key + KeyMapping#consumeClick：
 * 纯客户端界面（配置/附魔/药水/跨世界）直接 setScreen，
 * 需要服务端容器的（储藏室/黑名单）通过 LoliPickaxeContainerOpenPackte 请服务端 openScreen。
 */
public class LoliKeyEvent {

	@SubscribeEvent
	public void onKeyInput(InputEvent.Key event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		if (KeyLoader.LOLI_CONFIG.consumeClick()) {
			if (!stack.isEmpty() && stack.getItem() instanceof ILoli && !ConfigLoader.loliPickaxeGuiChangeList.isEmpty()) {
				mc.setScreen(new GUILoliConfig(stack));
			}
		}
		if (KeyLoader.LOLI_ENCHANTMENT.consumeClick()) {
			if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
				mc.setScreen(new GUILoliEnchantment(stack));
			}
		}
		if (KeyLoader.LOLI_POTION.consumeClick()) {
			if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
				mc.setScreen(new GUILoliPotion(stack));
			}
		}
		if (KeyLoader.LOLI_SPACE_FOLDING.consumeClick() && ConfigLoader.loliPickaxeSpaceFolding) {
			if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
				mc.setScreen(new GUILoliSpaceFolding(player));
			}
		}
		if (KeyLoader.LOLI_PICKAXE_CONTAINER.consumeClick()) {
			if (Screen.hasShiftDown()) {
				NetworkHandler.sendToServer(new LoliPickaxeDropAllPacket());
			} else {
				NetworkHandler.sendToServer(new LoliPickaxeContainerOpenPackte(MenuLoader.GUI_LOLI_PICKAXE_CONTAINER));
			}
		}
		if (KeyLoader.LOLI_PICKAXE_CONTAINER_BLACKLIST.consumeClick()) {
			NetworkHandler.sendToServer(new LoliPickaxeContainerOpenPackte(MenuLoader.GUI_LOLI_PICKAXE_CONTAINER_BLACKLIST));
		}
	}

}
