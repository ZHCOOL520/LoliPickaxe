package com.anotherstar.network;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class LoliSpaceFoldingPacket {

	private ResourceKey<Level> dimension;
	private double x;
	private double y;
	private double z;

	public LoliSpaceFoldingPacket() {
	}

	public LoliSpaceFoldingPacket(ResourceKey<Level> dimension, double x, double y, double z) {
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public LoliSpaceFoldingPacket(FriendlyByteBuf buf) {
		dimension = buf.readResourceKey(Registries.DIMENSION);
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceKey(dimension);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
	}

	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public static void handle(LoliSpaceFoldingPacket msg, Supplier<NetworkEvent.Context> ctx) {
		if (ConfigLoader.loliPickaxeSpaceFolding) {
			ServerPlayer player = ctx.get().getSender();
			if (LoliPickaxeUtil.invHaveLoliPickaxe(player)) {
				ResourceKey<Level> dimension = msg.getDimension();
				double x = msg.getX();
				double y = msg.getY();
				double z = msg.getZ();
				double proportion = Math.sqrt(x * x + y * y + z * z) / ConfigLoader.loliPickaxeMaxTeleportDistance;
				if (proportion > 1) {
					x /= proportion;
					y /= proportion;
					z /= proportion;
				}
				if (player.level().dimension().equals(dimension)) {
					player.stopRiding();
					Set<RelativeMovement> set = EnumSet.of(RelativeMovement.X, RelativeMovement.Y, RelativeMovement.Z);
					player.connection.teleport(x, y, z, player.getYRot(), player.getXRot(), set);
				} else if (!isBlacklisted(dimension)) {
					ServerLevel level = player.getServer().getLevel(dimension);
					if (level != null) {
						player.teleportTo(level, player.getX() + x, player.getY() + y, player.getZ() + z, player.getYRot(), player.getXRot());
					}
				}
			}
		}
		ctx.get().setPacketHandled(true);
	}

	/**
	 * 1.12 的维度是数字 id，1.20.1 改为 ResourceKey，而配置里的黑名单仍然是数字列表，
	 * 因此这里只对原版三个维度（0=主世界、-1=下界、1=末地）保持兼容。
	 */
	private static boolean isBlacklisted(ResourceKey<Level> dimension) {
		if (ConfigLoader.loliPickaxeWorldBlacklist == null) {
			return false;
		}
		if (dimension.equals(Level.OVERWORLD)) {
			return ConfigLoader.loliPickaxeWorldBlacklist.contains(0);
		}
		if (dimension.equals(Level.NETHER)) {
			return ConfigLoader.loliPickaxeWorldBlacklist.contains(-1);
		}
		if (dimension.equals(Level.END)) {
			return ConfigLoader.loliPickaxeWorldBlacklist.contains(1);
		}
		return false;
	}

}
