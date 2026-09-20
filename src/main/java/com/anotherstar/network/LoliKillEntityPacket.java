package com.anotherstar.network;

import java.util.function.Supplier;

import com.anotherstar.api.ILoliDataHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class LoliKillEntityPacket {

	private ResourceKey<Level> dimension;
	private int entityID;

	public LoliKillEntityPacket() {
	}

	public LoliKillEntityPacket(ResourceKey<Level> dimension, int entityID) {
		this.dimension = dimension;
		this.entityID = entityID;
	}

	public LoliKillEntityPacket(FriendlyByteBuf buf) {
		dimension = buf.readResourceKey(Registries.DIMENSION);
		entityID = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceKey(dimension);
		buf.writeInt(entityID);
	}

	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	public int getEntityID() {
		return entityID;
	}

	public static void handle(LoliKillEntityPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || !mc.level.dimension().equals(msg.getDimension())) {
				return;
			}
			Entity entity = mc.level.getEntity(msg.getEntityID());
			if (entity instanceof LivingEntity) {
				ILoliDataHolder holder = (ILoliDataHolder) entity;
				holder.setLoliDead(true);
				holder.setLoliCool(true);
				entity.discard();
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
