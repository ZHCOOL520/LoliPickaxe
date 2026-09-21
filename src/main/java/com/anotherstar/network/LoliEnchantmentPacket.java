package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.tool.ILoli;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class LoliEnchantmentPacket {

	CompoundTag ench;

	public LoliEnchantmentPacket() {
	}

	public LoliEnchantmentPacket(CompoundTag ench) {
		this.ench = ench;
	}

	public LoliEnchantmentPacket(FriendlyByteBuf buf) {
		ench = buf.readNbt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeNbt(ench);
	}

	public CompoundTag getEnch() {
		return ench;
	}

	public static void handle(LoliEnchantmentPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack stack = player.getMainHandItem();
		if (!stack.isEmpty() && stack.getItem() instanceof ILoli && msg.getEnch() != null) {
			if (msg.getEnch().contains("Enchantments")) {
				ListTag list = msg.getEnch().getList("Enchantments", 10);
				for (int i = 0; i < list.size(); i++) {
					CompoundTag element = list.getCompound(i);
					Enchantment enchantment = readEnchantment(element);
					if (enchantment == null) {
						continue;
					}
					int lvl = element.getShort("lvl");
					ResourceLocation name = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
					Integer limit = name == null || ConfigLoader.loliPickaxeEnchantmentLimit == null ? null : ConfigLoader.loliPickaxeEnchantmentLimit.get(name.toString());
					if (limit != null) {
						if (lvl > limit) {
							element.putShort("lvl", (short) (int) limit);
						}
					} else if (lvl > ConfigLoader.loliPickaxeEnchantmentDefaultLimit) {
						element.putShort("lvl", (short) ConfigLoader.loliPickaxeEnchantmentDefaultLimit);
					}
				}
				stack.getOrCreateTag().put("Enchantments", list);
			} else if (stack.hasTag() && stack.getTag().contains("Enchantments")) {
				stack.getTag().remove("Enchantments");
			}
		}
		ctx.get().setPacketHandled(true);
	}

	private static Enchantment readEnchantment(CompoundTag element) {
		String name = element.getString("id");
		if (name.isEmpty()) {
			// 1.12.2 的数字 id 与 1.20.1 注册表分配的数字 id 完全不对应，按数字 id 查表会解析出错误的附魔，
			// 因此不再兼容数字 id，只支持 "id" 为资源路径字符串的写法。
			return null;
		}
		return ForgeRegistries.ENCHANTMENTS.getValue(parseId(name));
	}

	/**
	 * 安全解析资源路径。
	 *
	 * <p>{@code "id"} 来自客户端提交的 NBT，构造 {@link ResourceLocation} 时遇到非法字符
	 * （例如含空格、大写或缺省命名空间格式错误）会抛 {@code ResourceLocationException}，
	 * 使包处理线程崩溃。这里统一捕获并返回 {@code null}，由调用方按「未知附魔」处理。
	 *
	 * @param name 资源路径字符串
	 * @return 解析成功时的 ResourceLocation，非法时为 null
	 */
	private static ResourceLocation parseId(String name) {
		try {
			return new ResourceLocation(name);
		} catch (Exception e) {
			return null;
		}
	}

}
