package com.anotherstar.network;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 1.20.1 网络层。原来基于 1.12.2 的 SimpleNetworkWrapper(IMessage)，
 * 这里改为 Forge SimpleChannel + 每个包自行提供 encode/decode/handle。
 */
public class NetworkHandler {

	/** 协议版本号。客户端与服务端必须一致，否则 Forge 会直接拒绝连接（版本握手失败）。 */
	public static final String PROTOCOL_VERSION = "1";

	/** 全局唯一通道，通道名 lolipickaxe:main。所有本模组的包都通过它收发。 */
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(LoliPickaxe.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	/** 自增的包 id 计数器。注册顺序一旦改变，所有包的 id 都会变化，会导致新旧版本不兼容，故严禁调整 init() 内的注册顺序。 */
	private static int id = 0;

	/**
	 * 网络层初始化，在模组构造阶段（客户端与服务端各一次）调用。
	 * <p>
	 * 注册顺序即包 id 顺序（从 0 开始递增），当前对应关系为：
	 * <ul>
	 * <li>0  LoliKillFacingPacket      C→S</li>
	 * <li>1  LoliDeadPacket            S→C</li>
	 * <li>2  LoliConfigPacket          S→C</li>
	 * <li>3  LoliItemConfigPacket      C→S</li>
	 * <li>4  LoliKillEntityPacket      S→C</li>
	 * <li>5  LoliCardPacket            C→S</li>
	 * <li>6  LoliPickaxeContainerPackte     C→S</li>
	 * <li>7  LoliPickaxeContainerOpenPackte C→S</li>
	 * <li>8  LoliPickaxeDropAllPacket  C→S</li>
	 * <li>9  LoliSlotChangePacket      S→C</li>
	 * <li>10 LoliSlotsInitPacket       S→C</li>
	 * <li>11 LoliCardOnlinePacket      C→S</li>
	 * <li>12 LoliEnchantmentPacket     C→S</li>
	 * <li>13 LoliPotionPacket          C→S</li>
	 * <li>14 LoliSpaceFoldingPacket    C→S</li>
	 * <li>15 PasswordUpdataPacket      C→S</li>
	 * </ul>
	 * 注意：id 必须保持稳定，否则已有的其他模组/存档握手会失败。
	 */
	public static void init() {
		register(LoliKillFacingPacket.class, LoliKillFacingPacket::new, LoliKillFacingPacket::encode, LoliKillFacingPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliDeadPacket.class, LoliDeadPacket::new, LoliDeadPacket::encode, LoliDeadPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
		register(LoliConfigPacket.class, LoliConfigPacket::new, LoliConfigPacket::encode, LoliConfigPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
		register(LoliItemConfigPacket.class, LoliItemConfigPacket::new, LoliItemConfigPacket::encode, LoliItemConfigPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliKillEntityPacket.class, LoliKillEntityPacket::new, LoliKillEntityPacket::encode, LoliKillEntityPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
		register(LoliCardPacket.class, LoliCardPacket::new, LoliCardPacket::encode, LoliCardPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliPickaxeContainerPackte.class, LoliPickaxeContainerPackte::new, LoliPickaxeContainerPackte::encode, LoliPickaxeContainerPackte::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliPickaxeContainerOpenPackte.class, LoliPickaxeContainerOpenPackte::new, LoliPickaxeContainerOpenPackte::encode, LoliPickaxeContainerOpenPackte::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliPickaxeDropAllPacket.class, LoliPickaxeDropAllPacket::new, LoliPickaxeDropAllPacket::encode, LoliPickaxeDropAllPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliSlotChangePacket.class, LoliSlotChangePacket::new, LoliSlotChangePacket::encode, LoliSlotChangePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
		register(LoliSlotsInitPacket.class, LoliSlotsInitPacket::new, LoliSlotsInitPacket::encode, LoliSlotsInitPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
		register(LoliCardOnlinePacket.class, LoliCardOnlinePacket::new, LoliCardOnlinePacket::encode, LoliCardOnlinePacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliEnchantmentPacket.class, LoliEnchantmentPacket::new, LoliEnchantmentPacket::encode, LoliEnchantmentPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliPotionPacket.class, LoliPotionPacket::new, LoliPotionPacket::encode, LoliPotionPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(LoliSpaceFoldingPacket.class, LoliSpaceFoldingPacket::new, LoliSpaceFoldingPacket::encode, LoliSpaceFoldingPacket::handle, NetworkDirection.PLAY_TO_SERVER);
		register(PasswordUpdataPacket.class, PasswordUpdataPacket::new, PasswordUpdataPacket::encode, PasswordUpdataPacket::handle, NetworkDirection.PLAY_TO_SERVER);
	}

	/**
	 * 把一个包类型注册到通道上（对应 1.12.2 的 {@code SimpleNetworkWrapper#registerMessage}）。
	 * 使用 consumerMainThread 保证 handle 在主线程执行，因此各包的 handle 中无需再手动 enqueueWork。
	 *
	 * @param type     包类型，Forge 用它做类型分发
	 * @param decoder  从字节流构造包实例的函数（对应 1.12.2 IMessage 的 fromBytes）
	 * @param encoder  把包写入字节流的函数（对应 1.12.2 IMessage 的 toBytes）
	 * @param handler  收包处理函数，第二个参数为网络上下文
	 * @param direction 传输方向，PLAY_TO_SERVER = 客户端→服务端，PLAY_TO_CLIENT = 服务端→客户端
	 * @param <M>      包的实际类型
	 */
	private static <M> void register(Class<M> type, Function<FriendlyByteBuf, M> decoder, BiConsumer<M, FriendlyByteBuf> encoder, BiConsumer<M, Supplier<NetworkEvent.Context>> handler, NetworkDirection direction) {
		CHANNEL.messageBuilder(type, id++, direction).encoder(encoder).decoder(decoder).consumerMainThread(handler).add();
	}

	/**
	 * 把包发送给指定维度中的所有玩家（服务端调用）。
	 *
	 * @param msg 要发送的包实例
	 * @param dim 目标维度 key
	 */
	public static void sendToDim(Object msg, ResourceKey<Level> dim) {
		CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dim), msg);
	}

	/**
	 * 把包发送给以某坐标为中心、指定半径内的所有玩家（服务端调用）。
	 *
	 * @param msg   要发送的包实例
	 * @param dim   目标维度 key
	 * @param pos   中心坐标（方块坐标）
	 * @param range 半径（方块），以目标点为中心的球形范围
	 */
	public static void sendAroundPos(Object msg, ResourceKey<Level> dim, BlockPos pos, double range) {
		CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX(), pos.getY(), pos.getZ(), range, dim)), msg);
	}

	/**
	 * 把包发送给单个玩家（服务端调用）。
	 *
	 * @param msg    要发送的包实例
	 * @param player 目标玩家
	 */
	public static void sendToPlayer(Object msg, ServerPlayer player) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
	}

	/**
	 * 把包广播给服务端上的所有玩家。
	 *
	 * @param msg 要发送的包实例
	 */
	public static void sendToAll(Object msg) {
		CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
	}

	/**
	 * 把包发送给服务端（客户端调用）。
	 *
	 * @param msg 要发送的包实例，必须是注册为 PLAY_TO_SERVER 的类型
	 */
	public static void sendToServer(Object msg) {
		CHANNEL.sendToServer(msg);
	}

}
