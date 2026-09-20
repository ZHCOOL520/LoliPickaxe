package com.anotherstar.client.key;

import org.lwjgl.glfw.GLFW;

import com.anotherstar.common.LoliPickaxe;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.IEventBus;

public class KeyLoader {

	public static final String CATEGORY = "key.category." + LoliPickaxe.MODID;

	public static final KeyMapping LOLI_CONFIG = new KeyMapping("key." + LoliPickaxe.MODID + ".loli_config", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
	public static final KeyMapping LOLI_ENCHANTMENT = new KeyMapping("key." + LoliPickaxe.MODID + ".loli_enchantment", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
	public static final KeyMapping LOLI_POTION = new KeyMapping("key." + LoliPickaxe.MODID + ".loli_potion", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);
	public static final KeyMapping LOLI_SPACE_FOLDING = new KeyMapping("key." + LoliPickaxe.MODID + ".loli_space_folding", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
	public static final KeyMapping LOLI_PICKAXE_CONTAINER = new KeyMapping("key." + LoliPickaxe.MODID + ".loli_container", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
	public static final KeyMapping LOLI_PICKAXE_CONTAINER_BLACKLIST = new KeyMapping("key." + LoliPickaxe.MODID + ".loli_container_blacklist", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);

	/**
	 * 1.20.1 里按键需要在 mod 事件总线通过 RegisterKeyMappingsEvent 注册（原 ClientRegistry.registerKeyBinding）。
	 */
	public static void init(IEventBus modBus) {
		modBus.addListener(KeyLoader::onRegisterKeyMappings);
	}

	private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(LOLI_CONFIG);
		event.register(LOLI_ENCHANTMENT);
		event.register(LOLI_POTION);
		event.register(LOLI_SPACE_FOLDING);
		event.register(LOLI_PICKAXE_CONTAINER);
		event.register(LOLI_PICKAXE_CONTAINER_BLACKLIST);
	}

}
