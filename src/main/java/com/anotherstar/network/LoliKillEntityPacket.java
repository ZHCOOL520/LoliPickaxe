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
			// 必须直接判断 ILoliDataHolder 而非 LivingEntity：
			// 萝莉数据字段由 LivingEntityMixin 通过接口注入，若某个模组的生物实体未实现该接口，
			// 原来的强制转换会抛 ClassCastException 并使客户端崩溃。
			if (entity instanceof ILoliDataHolder) {
				ILoliDataHolder holder = (ILoliDataHolder) entity;
				holder.setLoliDead(true);
				holder.setLoliCool(true);
				entity.discard();
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
