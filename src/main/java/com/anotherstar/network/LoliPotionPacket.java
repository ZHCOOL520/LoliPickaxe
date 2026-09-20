package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.tool.ILoli;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class LoliPotionPacket {

	CompoundTag potion;

	public LoliPotionPacket() {
	}

	public LoliPotionPacket(CompoundTag potion) {
		this.potion = potion;
	}

	public LoliPotionPacket(FriendlyByteBuf buf) {
		potion = buf.readNbt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeNbt(potion);
	}

	public CompoundTag getPotion() {
		return potion;
	}

	public static void handle(LoliPotionPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack stack = player.getMainHandItem();
		if (!stack.isEmpty() && stack.getItem() instanceof ILoli && msg.getPotion() != null) {
			if (msg.getPotion().contains("LoliPotion")) {
				ListTag list = msg.getPotion().getList("LoliPotion", 10);
				for (int i = 0; i < list.size(); i++) {
					CompoundTag element = list.getCompound(i);
					MobEffect potion = readPotion(element);
					if (potion == null) {
						continue;
					}
					int lvl = element.getByte("lvl");
					ResourceLocation name = ForgeRegistries.MOB_EFFECTS.getKey(potion);
					Integer limit = name == null ? null : ConfigLoader.loliPickaxePotionLimit.get(name.toString());
					if (limit != null) {
						if (lvl > limit) {
							element.putByte("lvl", (byte) (int) limit);
						}
					} else if (lvl > ConfigLoader.loliPickaxePotionDefaultLimit) {
						element.putByte("lvl", (byte) ConfigLoader.loliPickaxePotionDefaultLimit);
					}
				}
				stack.getOrCreateTag().put("LoliPotion", list);
			} else if (stack.hasTag() && stack.getTag().contains("LoliPotion")) {
				stack.getTag().remove("LoliPotion");
			}
		}
		ctx.get().setPacketHandled(true);
	}

	private static MobEffect readPotion(CompoundTag element) {
		String name = element.getString("id");
		if (name.isEmpty()) {
			// 1.12.2 的数字 id 与 1.20.1 注册表分配的数字 id 完全不对应，按数字 id 查表会解析出错误的药水，
			// 因此不再兼容数字 id，只支持 "id" 为资源路径字符串的写法。
			return null;
		}
		return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(name));
	}

}
