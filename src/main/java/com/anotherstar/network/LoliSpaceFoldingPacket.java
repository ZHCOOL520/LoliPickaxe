package com.anotherstar.network;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
	 * 维度黑名单判定。
	 *
	 * <p><b>原实现的问题</b>：1.12.2 的维度是数字 id，而 1.20.1 改为 {@link ResourceKey}。
	 * 原实现只把原版三维度硬编码映射回 0/-1/1，导致在大型整合包里
	 * <b>所有模组维度都返回 false</b> —— 配置项「跨世界传送黑名单」形同虚设，
	 * 管理员无法阻止玩家传送到模组维度。
	 *
	 * <p><b>本实现如何保持兼容且不改变原逻辑</b>：
	 * <ol>
	 *   <li>原版三维度仍按原来的 0/-1/1 数字语义匹配，<b>旧配置文件完全不用改</b>；</li>
	 *   <li>新增支持：把黑名单里的字符串项与维度的 {@code ResourceLocation} 比较，
	 *       使模组维度也能被列入黑名单；</li>
	 *   <li>原有命中规则、优先级、返回值语义不变 —— 只是「可识别的维度范围」变大了，
	 *       原先能拦住的三维度依然能拦住。</li>
	 * </ol>
	 *
	 * @param dimension 目标维度键
	 * @return true 表示该维度被列入黑名单，禁止跨世界传送
	 */
	private static boolean isBlacklisted(ResourceKey<Level> dimension) {
		if (ConfigLoader.loliPickaxeWorldBlacklist == null || ConfigLoader.loliPickaxeWorldBlacklist.isEmpty()) {
			return false;
		}
		// 1) 原版三维度：保持旧配置的数字语义（0=主世界、-1=下界、1=末地）
		if (dimension.equals(Level.OVERWORLD)) {
			return ConfigLoader.loliPickaxeWorldBlacklist.contains(0);
		}
		if (dimension.equals(Level.NETHER)) {
			return ConfigLoader.loliPickaxeWorldBlacklist.contains(-1);
		}
		if (dimension.equals(Level.END)) {
			return ConfigLoader.loliPickaxeWorldBlacklist.contains(1);
		}
		// 2) 模组维度：原版三维度之外，改用「资源路径字符串」匹配更直观且稳定，
		//    但配置项当前声明为 List<Integer>，因此这里额外支持两种表达：
		//      a) 数字项：与原版维度 id 对比（模组维度无固定约定值时不会误命中）；
		//      b) 字符串项：与维度 ResourceLocation 对比（需配置改为字符串列表，见配置注释）。
		//    两者都不命中则视为「不在黑名单」，与改动前对模组维度的行为保持一致（不误拦）。
		ResourceLocation location = dimension.location();
		for (Integer entry : ConfigLoader.loliPickaxeWorldBlacklist) {
			if (entry == null) {
				continue;
			}
			if (entry.intValue() == dimensionId(dimension)) {
				return true;
			}
		}
		return isBlacklistedByName(location);
	}

	/**
	 * 按资源路径字符串匹配黑名单，供配置以字符串形式登记模组维度时使用。
	 *
	 * <p>由于配置项 {@code loliPickaxeWorldBlacklist} 声明为整数列表，
	 * 这里读取同名兼容项 {@code loliPickaxeWorldBlacklistNames}（若存在）进行匹配；
	 * 不存在时直接返回 false，保证行为与改动前一致。
	 *
	 * @param location 维度资源路径
	 * @return true 表示该维度按名称被列入黑名单
	 */
	private static boolean isBlacklistedByName(ResourceLocation location) {
		List<String> names = ConfigLoader.loliPickaxeWorldBlacklistNames;
		if (names == null || names.isEmpty()) {
			return false;
		}
		return names.contains(location.toString());
	}

	/**
	 * 取得维度的数字 id（用于兼容旧式数字配置）。
	 *
	 * <p>1.20.1 的 {@link ResourceKey} 本身不再是数字，但原版三维度仍有约定的数字 id
	 * （主世界 0、下界 -1、末地 1），模组维度通常也有自己的注册序号。
	 * 这里优先使用原版约定值；无法确定时返回一个不可能匹配的值。
	 *
	 * @param dimension 维度键
	 * @return 维度的数字 id；未知时返回 {@link Integer#MIN_VALUE}
	 */
	private static int dimensionId(ResourceKey<Level> dimension) {
		if (dimension.equals(Level.OVERWORLD)) {
			return 0;
		}
		if (dimension.equals(Level.NETHER)) {
			return -1;
		}
		if (dimension.equals(Level.END)) {
			return 1;
		}
		return Integer.MIN_VALUE;
	}

}
