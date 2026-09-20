package com.anotherstar.common.command;

import java.util.Collection;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.network.LoliDeadPacket;
import com.anotherstar.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LoliBuffAttackCommand {

	private static final String[] ATTACKS = { "loliPickaxeBlueScreenAttack", "loliPickaxeExitAttack", "loliPickaxeFailRespondAttack" };

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("loliattack")
				.requires(source -> source.hasPermission(4))
				.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("attack", StringArgumentType.word())
								.suggests((context, builder) -> {
									String remaining = builder.getRemainingLowerCase();
									for (String attack : ATTACKS) {
										if (attack.toLowerCase().startsWith(remaining)) {
											builder.suggest(attack);
										}
									}
									return builder.buildFuture();
								})
								.executes(context -> execute(context)))));
	}

	private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		if (!ConfigLoader.loliEnableBuffAttackTNT) {
			throw new SimpleCommandExceptionType(Component.translatable("commands.loliattack.disable")).create();
		}
		String attack = StringArgumentType.getString(context, "attack");
		boolean blueScreen = false;
		boolean exit = false;
		boolean failRespond = false;
		switch (attack) {
		case "loliPickaxeBlueScreenAttack":
			blueScreen = true;
			break;
		case "loliPickaxeExitAttack":
			exit = true;
			break;
		case "loliPickaxeFailRespondAttack":
			failRespond = true;
			break;
		default:
			throw new SimpleCommandExceptionType(Component.translatable("commands.loliattack.notfound")).create();
		}
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
		for (ServerPlayer player : players) {
			NetworkHandler.sendToPlayer(new LoliDeadPacket(false, blueScreen, exit, failRespond), player);
		}
		return players.size();
	}

}
