package com.anotherstar.client.event;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.anotherstar.common.item.tool.ILoli;
import com.anotherstar.util.LoliRomeDigitalUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LoliPickaxeTooltipEvent {

	private static final Pattern STRING_ENCHANTMENT_PATTERN = Pattern.compile("enchantment\\.level\\.(\\d+)$");

	private int tick = 0;
	private int curColor = 0;
	private ChatFormatting[] colors = { ChatFormatting.GOLD, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW };

	@SubscribeEvent
	public void onLoliPickaxeTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (stack.isEmpty() || !(stack.getItem() instanceof ILoli)) {
			return;
		}
		List<Component> tooltip = event.getToolTip();
		for (int i = 0; i < tooltip.size(); i++) {
			String tip = tooltip.get(i).getString();
			if (tip.endsWith(Component.translatable("attribute.name.generic.attack_damage").getString())) {
				tooltip.set(i, Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0", Component.literal(rainbow("loliPickaxe.damage") + ChatFormatting.GRAY), Component.translatable("attribute.name.generic.attack_damage"))));
			} else if (tip.endsWith(Component.translatable("attribute.name.generic.attack_speed").getString())) {
				tooltip.set(i, Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0", Component.literal(rainbow("loliPickaxe.speed") + ChatFormatting.GRAY), Component.translatable("attribute.name.generic.attack_speed"))));
			} else {
				Matcher matcher = STRING_ENCHANTMENT_PATTERN.matcher(tip);
				if (matcher.find()) {
					try {
						tooltip.set(i, Component.literal(tip.substring(0, matcher.start()) + LoliRomeDigitalUtil.intToRoman(Integer.parseInt(matcher.group(1)))));
					} catch (NumberFormatException e) {
					}
				}
			}
		}
	}

	/**
	 * 彩虹色文本，颜色代码通过 ChatFormatting 的 § 前缀拼接（与 1.12.2 的 TextFormatting 行为一致）。
	 */
	private String rainbow(String key) {
		String str = Component.translatable(key).getString();
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < str.length(); j++) {
			sb.append(colors[(curColor + j) % colors.length].toString());
			sb.append(str.charAt(j));
		}
		return sb.toString();
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			if (++tick >= 3) {
				tick = 0;
				if (--curColor < 0) {
					curColor = colors.length - 1;
				}
			}
		}
	}

}
