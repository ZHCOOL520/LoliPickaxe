package com.anotherstar.network;

import java.util.Map;
import java.util.function.Supplier;

import com.anotherstar.common.item.ItemLoliCard;
import com.anotherstar.common.item.ItemLoliCardAlbum;
import com.google.common.collect.Maps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoliCardPacket {

	private int slot;
	private ItemType type;
	private String name;

	public LoliCardPacket() {
	}

	public LoliCardPacket(int slot, ItemType type, String name) {
		this.slot = slot;
		this.type = type;
		this.name = name;
	}

	public LoliCardPacket(FriendlyByteBuf buf) {
		slot = buf.readInt();
		type = ItemType.idToElement.get(buf.readInt());
		name = buf.readUtf();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(slot);
		buf.writeInt(type.getId());
		buf.writeUtf(name);
	}

	public int getSlot() {
		return slot;
	}

	public ItemType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public static enum ItemType {

		LOLICARD(0), LOLICARDALBUM(1);

		public static Map<Integer, ItemType> idToElement = Maps.newHashMap();

		private final int id;

		private ItemType(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		static {
			for (ItemType element : values()) {
				idToElement.put(element.id, element);
			}
		}

	}

	public static void handle(LoliCardPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		ItemStack stack = player.getInventory().getItem(msg.getSlot());
		switch (msg.getType()) {
		case LOLICARD:
			if (stack.getItem() instanceof ItemLoliCard) {
				CompoundTag nbt = stack.getOrCreateTag();
				if (!nbt.contains("picture")) {
					nbt.putString("picture", msg.getName());
				}
			}
			break;
		case LOLICARDALBUM:
			if (stack.getItem() instanceof ItemLoliCardAlbum) {
				CompoundTag nbt = stack.getOrCreateTag();
				if (!nbt.contains("PictureGroup")) {
					nbt.putString("PictureGroup", msg.getName());
				}
			}
			break;
		}
		ctx.get().setPacketHandled(true);
	}

}
