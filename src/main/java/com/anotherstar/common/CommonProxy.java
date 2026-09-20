package com.anotherstar.common;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.common.block.BlockLoader;
import com.anotherstar.common.command.ConfigCommand;
import com.anotherstar.common.command.LoliBuffAttackCommand;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.enchantment.EnchantmentLoader;
import com.anotherstar.common.entity.EntityLoader;
import com.anotherstar.common.event.DestroyBedrockEvent;
import com.anotherstar.common.event.LoliDropEvent;
import com.anotherstar.common.event.LoliPickaxeEvent;
import com.anotherstar.common.event.LoliTickEvent;
import com.anotherstar.common.event.PlayerJoinEvent;
import com.anotherstar.common.event.SmallLoliBlockDropEvent;
import com.anotherstar.common.event.SmallLoliFlyEvent;
import com.anotherstar.common.gui.MenuLoader;
import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.recipe.RecipeLoader;
import com.anotherstar.core.util.EventUtil;

import net.minecraftforge.event.RegisterCommandsEvent;

public class CommonProxy {

	public void preInit(IEventBus modBus) {
		ItemLoader.ITEMS.register(modBus);
		BlockLoader.BLOCKS.register(modBus);
		BlockLoader.ITEMS.register(modBus);
		EntityLoader.ENTITY_TYPES.register(modBus);
		EnchantmentLoader.ENCHANTMENTS.register(modBus);
		MenuLoader.MENUS.register(modBus);
		RecipeLoader.RECIPE_SERIALIZERS.register(modBus);
		CreativeTabLoader.TABS.register(modBus);
	}

	public void init() {
		MinecraftForge.EVENT_BUS.register(new DestroyBedrockEvent());
		MinecraftForge.EVENT_BUS.register(new LoliPickaxeEvent());
		MinecraftForge.EVENT_BUS.register(new LoliTickEvent());
		MinecraftForge.EVENT_BUS.register(new PlayerJoinEvent());
		MinecraftForge.EVENT_BUS.register(new LoliDropEvent());
		MinecraftForge.EVENT_BUS.register(new SmallLoliBlockDropEvent());
		MinecraftForge.EVENT_BUS.register(new SmallLoliFlyEvent());
		MinecraftForge.EVENT_BUS.register(EventUtil.class);
	}

	public void postInit() {
	}

	public void onRegisterCommands(RegisterCommandsEvent event) {
		ConfigCommand.register(event.getDispatcher());
		LoliBuffAttackCommand.register(event.getDispatcher());
	}

	/** 配置加载完成后把值刷进静态字段。 */
	public void onConfigLoad() {
		ConfigLoader.load(false);
	}

	public void onConfigReload() {
		ConfigLoader.load(true);
	}

}
