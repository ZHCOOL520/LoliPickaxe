package com.anotherstar.common.command;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.config.annotation.ConfigField.ValurType;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class ConfigCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("loli")
				.requires(source -> source.hasPermission(4))
				.then(Commands.literal("reload").executes(context -> reload(context.getSource())))
				.then(Commands.literal("listFlag")
						.executes(context -> sendFlags(context.getSource(), 1))
						.then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(context -> sendFlags(context.getSource(), IntegerArgumentType.getInteger(context, "page")))))
				.then(Commands.literal("listValue")
						.executes(context -> sendValues(context.getSource(), 1))
						.then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(context -> sendValues(context.getSource(), IntegerArgumentType.getInteger(context, "page")))))
				.then(Commands.argument("flag", StringArgumentType.word())
						.suggests((context, builder) -> suggestFlags(builder))
						.executes(context -> sendFlag(context.getSource(), StringArgumentType.getString(context, "flag")))
						.then(Commands.argument("value", StringArgumentType.word())
								.suggests((context, builder) -> suggestValue(builder, StringArgumentType.getString(context, "flag")))
								.executes(context -> setAndSendFlag(context.getSource(), StringArgumentType.getString(context, "flag"), StringArgumentType.getString(context, "value"))))));
	}

	private static int reload(CommandSourceStack source) {
		ConfigLoader.load(true);
		ConfigLoader.sandChange(null);
		source.sendSuccess(() -> Component.translatable("commands.loli.reload"), false);
		return 1;
	}

	private static int setAndSendFlag(CommandSourceStack source, String flag, String value) {
		if (!ConfigLoader.commandFlags.contains(flag)) {
			source.sendFailure(Component.translatable("commands.loli.notfound"));
			return 0;
		}
		try {
			switch (ConfigLoader.flagAnnotations.get(flag).valueType()) {
			case INT:
				ConfigLoader.flagFields.get(flag).setInt(null, Integer.parseInt(value));
				break;
			case DOUBLE:
				ConfigLoader.flagFields.get(flag).setDouble(null, Double.parseDouble(value));
				break;
			case BOOLEAN:
				ConfigLoader.flagFields.get(flag).setBoolean(null, Boolean.parseBoolean(value));
				break;
			case STRING:
				ConfigLoader.flagFields.get(flag).set(null, value);
				break;
			default:
				source.sendFailure(Component.translatable("commands.loli.errortype"));
				return 0;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			source.sendFailure(Component.literal(String.valueOf(e.getMessage())));
			return 0;
		}
		ConfigLoader.save();
		ConfigLoader.sandChange(null);
		MutableComponent flagText = Component.literal(flag).withStyle(ChatFormatting.AQUA);
		MutableComponent commentText = Component.literal(ConfigLoader.flagAnnotations.get(flag).comment()).withStyle(ChatFormatting.LIGHT_PURPLE);
		MutableComponent valueText = Component.literal(value).withStyle(ChatFormatting.RED);
		source.sendSuccess(() -> Component.translatable("commands.loli.set", flagText, commentText, valueText), false);
		return 1;
	}

	private static int sendFlag(CommandSourceStack source, String flag) {
		if (!ConfigLoader.commandFlags.contains(flag)) {
			source.sendFailure(Component.translatable("commands.loli.notfound"));
			return 0;
		}
		try {
			Field field = ConfigLoader.flagFields.get(flag);
			Object value;
			switch (ConfigLoader.flagAnnotations.get(flag).valueType()) {
			case INT:
				value = String.valueOf(field.getInt(null));
				break;
			case DOUBLE:
				value = String.valueOf(field.getDouble(null));
				break;
			case BOOLEAN:
				value = String.valueOf(field.getBoolean(null));
				break;
			case STRING:
				value = field.get(null);
				break;
			default:
				source.sendFailure(Component.translatable("commands.loli.errortype"));
				return 0;
			}
			String valueText = String.valueOf(value);
			MutableComponent flagText = Component.literal(flag).withStyle(ChatFormatting.AQUA);
			MutableComponent commentText = Component.literal(ConfigLoader.flagAnnotations.get(flag).comment()).withStyle(ChatFormatting.LIGHT_PURPLE);
			MutableComponent valueComponent = Component.literal(valueText).withStyle(ChatFormatting.RED);
			source.sendSuccess(() -> Component.translatable("commands.loli.get", flagText, commentText, valueComponent), false);
			return 1;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			source.sendFailure(Component.literal(String.valueOf(e.getMessage())));
			return 0;
		}
	}

	private static int sendFlags(CommandSourceStack source, int page) {
		int maxPage = (ConfigLoader.commandFlags.size() - 1) / 18 + 1;
		page = Mth.clamp(page, 1, maxPage);
		int currentPage = page;
		source.sendSuccess(() -> Component.literal(String.format("§a%39d/%-39d", currentPage, maxPage).replaceAll(" ", "-")), false);
		for (int i = (page - 1) * 18; i < page * 18; i++) {
			if (i < ConfigLoader.commandFlags.size()) {
				String flag = ConfigLoader.commandFlags.get(i);
				MutableComponent flagText = Component.literal(flag).withStyle(ChatFormatting.AQUA).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/loli " + flag + " ")));
				MutableComponent commentText = Component.literal(ConfigLoader.flagAnnotations.get(flag).comment()).withStyle(ChatFormatting.LIGHT_PURPLE);
				source.sendSuccess(() -> Component.translatable("commands.loli.list", flagText, commentText), false);
			} else {
				source.sendSuccess(() -> Component.literal(""), false);
			}
		}
		sendPageButtons(source, page, maxPage, "listFlag");
		return 1;
	}

	private static int sendValues(CommandSourceStack source, int page) {
		int maxPage = (ConfigLoader.commandFlags.size() - 1) / 18 + 1;
		page = Mth.clamp(page, 1, maxPage);
		int currentPage = page;
		source.sendSuccess(() -> Component.literal(String.format("§a%39d/%-39d", currentPage, maxPage).replaceAll(" ", "-")), false);
		for (int i = (page - 1) * 18; i < page * 18; i++) {
			if (i < ConfigLoader.commandFlags.size()) {
				String flag = ConfigLoader.commandFlags.get(i);
				try {
					Field field = ConfigLoader.flagFields.get(flag);
					Object value;
					switch (ConfigLoader.flagAnnotations.get(flag).valueType()) {
					case INT:
						value = String.valueOf(field.getInt(null));
						break;
					case DOUBLE:
						value = String.valueOf(field.getDouble(null));
						break;
					case BOOLEAN:
						value = String.valueOf(field.getBoolean(null));
						break;
					case STRING:
						value = field.get(null);
						break;
					default:
						source.sendFailure(Component.translatable("commands.loli.errortype"));
						return 0;
					}
					String valueText = String.valueOf(value);
					MutableComponent flagText = Component.literal(flag).withStyle(ChatFormatting.AQUA).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/loli " + flag + " " + valueText)));
					MutableComponent commentText = Component.literal(ConfigLoader.flagAnnotations.get(flag).comment()).withStyle(ChatFormatting.LIGHT_PURPLE);
					MutableComponent valueComponent = Component.literal(valueText).withStyle(ChatFormatting.RED);
					source.sendSuccess(() -> Component.translatable("commands.loli.get", flagText, commentText, valueComponent), false);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					source.sendFailure(Component.literal(String.valueOf(e.getMessage())));
					return 0;
				}
			} else {
				source.sendSuccess(() -> Component.literal(""), false);
			}
		}
		sendPageButtons(source, page, maxPage, "listValue");
		return 1;
	}

	private static void sendPageButtons(CommandSourceStack source, int page, int maxPage, String listCommand) {
		MutableComponent preButton = Component.translatable("commands.page.button.pre");
		if (page > 1) {
			int target = page - 1;
			preButton.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loli " + listCommand + " " + target)).withColor(ChatFormatting.GOLD));
		} else {
			preButton.withStyle(ChatFormatting.GRAY);
		}
		MutableComponent nextButton = Component.translatable("commands.page.button.next");
		if (page < maxPage) {
			int target = page + 1;
			nextButton.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/loli " + listCommand + " " + target)).withColor(ChatFormatting.GOLD));
		} else {
			nextButton.withStyle(ChatFormatting.GRAY);
		}
		MutableComponent bottom = Component.translatable("--------------------------------%1$s/%2$s--------------------------------", preButton, nextButton).withStyle(ChatFormatting.GREEN);
		source.sendSuccess(() -> bottom, false);
	}

	private static CompletableFuture<Suggestions> suggestFlags(SuggestionsBuilder builder) {
		List<String> list = Lists.newArrayList(ConfigLoader.commandFlags);
		list.add("reload");
		list.add("listFlag");
		list.add("listValue");
		String remaining = builder.getRemainingLowerCase();
		for (String element : list) {
			if (element.toLowerCase().startsWith(remaining)) {
				builder.suggest(element);
			}
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestValue(SuggestionsBuilder builder, String flag) {
		if (ConfigLoader.commandFlags.contains(flag) && ConfigLoader.flagAnnotations.get(flag).valueType() == ValurType.BOOLEAN) {
			String remaining = builder.getRemainingLowerCase();
			for (String element : new String[] { "true", "false" }) {
				if (element.startsWith(remaining)) {
					builder.suggest(element);
				}
			}
		}
		return builder.buildFuture();
	}

}
