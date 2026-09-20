package com.anotherstar.client.event;

import com.anotherstar.api.ILoliDataHolder;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 对应原 1.12.2 中阻止客户端移除（死亡的）持有萝莉的玩家实体。
 * 1.20.1 的删除逻辑由 Mixin 处理，这里只在客户端持续把死亡标记复位，
 * 保证实体不会被客户端本地判定为死亡而剔除。
 */
public class LoliPickaxeAntiClientRemoveEntity {

	@SubscribeEvent
	public void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide()) {
			return;
		}
		if (event.getEntity() instanceof LocalPlayer) {
			return;
		}
		if (event.getEntity() instanceof Player player && LoliPickaxeUtil.invHaveLoliPickaxe(player)) {
			restore(player);
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !LoliPickaxeUtil.invHaveLoliPickaxe(player)) {
			return;
		}
		restore(player);
		ClientLevel level = mc.level;
		if (level != null && !level.players().contains(player)) {
			try {
				level.players().add(player);
			} catch (UnsupportedOperationException ignored) {
				// 客户端玩家列表只读时忽略
			}
		}
	}

	private static void restore(Player player) {
		ILoliDataHolder holder = (ILoliDataHolder) player;
		holder.setLoliDead(false);
		holder.setLoliDeathTime(0);
		if (player.isDeadOrDying()) {
			player.deathTime = 0;
			player.setHealth(player.getMaxHealth());
		}
	}

}
