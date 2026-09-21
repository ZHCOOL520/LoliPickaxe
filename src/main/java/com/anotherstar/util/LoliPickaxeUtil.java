package com.anotherstar.util;

import java.util.Collection;
import java.util.List;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.entity.IEntityLoli;
import com.anotherstar.common.event.LoliPickaxeEvent;
import com.anotherstar.common.event.LoliTickEvent;
import com.anotherstar.common.item.tool.ILoli;
import com.anotherstar.api.ILoliDataHolder;
import com.anotherstar.api.ILoliPlayerData;
import com.anotherstar.core.util.EventUtil;
import com.anotherstar.network.LoliDeadPacket;
import com.anotherstar.network.LoliKillEntityPacket;
import com.anotherstar.network.NetworkHandler;
import com.google.common.collect.Lists;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.registries.ForgeRegistries;

public class LoliPickaxeUtil {

	public static void kill(Collection<Entity> entitys, LivingEntity source) {
		for (Entity entity : entitys) {
			kill(entity, source);
		}
	}

	public static void kill(Entity entity, LivingEntity source) {
		if (entity instanceof Player) {
			killPlayer((Player) entity, source);
		} else if (entity instanceof LivingEntity) {
			killEntityLiving((LivingEntity) entity, source);
		} else if (ConfigLoader.getBoolean(getLoliPickaxe(source), "loliPickaxeValidToAllEntity")) {
			killEntity(entity);
		}
	}

	public static void killFacing(LivingEntity source) {
		Level world = source.level();
		ItemStack stack = getLoliPickaxe(source);
		List<Entity> entitys = Lists.newArrayList();
		int range = ConfigLoader.getInt(stack, "loliPickaxeKillFacingRange");
		double slope = ConfigLoader.getDouble(stack, "loliPickaxeKillFacingSlope");
		boolean all = ConfigLoader.getBoolean(stack, "loliPickaxeValidToAllEntity");
		Vec3 look = source.getLookAngle().normalize();
		for (int dist = 0; dist <= range; dist += 2) {
			AABB bb = source.getBoundingBox();
			bb = bb.inflate(slope * dist + 2.0, slope * dist + 0.25, slope * dist + 2.0);
			bb = bb.move(look.x * dist, look.y * dist, look.z * dist);
			List<Entity> list = all ? world.getEntitiesOfClass(Entity.class, bb) : Lists.newArrayList(world.getEntitiesOfClass(LivingEntity.class, bb));
			list.removeAll(entitys);
			list.removeIf(entity -> entity.distanceTo(source) > range);
			entitys.addAll(list);
		}
		entitys.remove(source);
		if (!ConfigLoader.getBoolean(stack, "loliPickaxeValidToAmityEntity")) {
			entitys.removeIf(en -> en instanceof Player || en instanceof ArmorStand || en instanceof AmbientCreature || (en instanceof PathfinderMob && !(en instanceof Monster)));
		}
		LoliPickaxeUtil.kill(entitys, source);
	}

	public static void killPlayer(Player player, LivingEntity source) {
		if (invHaveLoliPickaxe(player) || ((ILoliDataHolder) player).isLoliDead() || player instanceof FakePlayer) {
			return;
		}
		ItemStack stack = getLoliPickaxe(source);
		if (ConfigLoader.getBoolean(stack, "loliPickaxeClearInventory")) {
			player.getInventory().clearContent();
			PlayerEnderChestContainer ec = player.getEnderChestInventory();
			for (int i = 0; i < ec.getContainerSize(); i++) {
				ec.setItem(i, ItemStack.EMPTY);
			}
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeDropItems")) {
			player.getInventory().dropAll();
		}
		DamageSource ds = EventUtil.loliDamage(player.level(), source);
		player.getCombatTracker().recordDamage(ds, Float.MAX_VALUE);
		player.setHealth(0.0F);
		player.die(ds);
		boolean remove = ConfigLoader.getBoolean(stack, "loliPickaxeCompulsoryRemove");
		if (remove) {
			((ILoliDataHolder) player).setLoliDead(true);
			delayKill(player);
		}
		if (player instanceof ServerPlayer) {
			ServerPlayer playerMP = (ServerPlayer) player;
			NetworkHandler.sendToPlayer(new LoliDeadPacket(remove, ConfigLoader.getBoolean(stack, "loliPickaxeBlueScreenAttack"), ConfigLoader.getBoolean(stack, "loliPickaxeExitAttack"), ConfigLoader.getBoolean(stack, "loliPickaxeFailRespondAttack")), playerMP);
			if (ConfigLoader.getBoolean(stack, "loliPickaxeBeyondRedemption")) {
				ConfigLoader.addPlayerToBeyondRedemption(playerMP);
			}
			if (ConfigLoader.getBoolean(stack, "loliPickaxeKickPlayer")) {
				playerMP.connection.disconnect(Component.literal(ConfigLoader.getString(stack, "loliPickaxeKickMessage")));
			}
			if (ConfigLoader.getBoolean(stack, "loliPickaxeReincarnation")) {
				ConfigLoader.addPlayerToReincarnation(playerMP);
			}
		}
	}

	public static void killEntityLiving(LivingEntity entity, LivingEntity source) {
		if (invHaveLoliPickaxe(entity)) {
			return;
		}
		if (!(entity.level().isClientSide || ((ILoliDataHolder) entity).isLoliDead() || entity.isDeadOrDying() || entity.getHealth() == 0.0F)) {
			entity.hurtTime = 10;
			DamageSource ds = EventUtil.loliDamage(entity.level(), source);
			entity.getCombatTracker().recordDamage(ds, Float.MAX_VALUE);
			entity.setHealth(0.0F);
			Class<? extends LivingEntity> clazz = entity.getClass();
			LoliPickaxeEvent.antiEntity.add(clazz);
			try {
				entity.die(ds);
			} finally {
				// 必须用 finally：若 die() 抛出异常（例如某个模组的死亡钩子出错），
				// 原实现会让该实体类型永久留在 antiEntity 中，导致此后该类型实体
				// 一旦加入世界就被 EntityJoinLevelEvent 取消（表现为实体神秘消失）。
				LoliPickaxeEvent.antiEntity.remove(clazz);
			}
			if (ConfigLoader.getBoolean(getLoliPickaxe(source), "loliPickaxeCompulsoryRemove")) {
				((ILoliDataHolder) entity).setLoliDead(true);
				delayKill(entity);
			}
		}
	}

	public static void killEntity(Entity entity) {
		entity.discard();
	}

	public static int killRangeEntity(Level world, LivingEntity entity, int range) {
		ItemStack stack = entity.getMainHandItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof ILoli)) {
			stack = getLoliPickaxe(entity);
		}
		AABB aabb = new AABB(entity.getX() - range, entity.getY() - range, entity.getZ() - range, entity.getX() + range, entity.getY() + range, entity.getZ() + range);
		List<Entity> list = ConfigLoader.getBoolean(stack, "loliPickaxeValidToAllEntity") ? world.getEntitiesOfClass(Entity.class, aabb) : Lists.newArrayList(world.getEntitiesOfClass(LivingEntity.class, aabb));
		if (!ConfigLoader.getBoolean(stack, "loliPickaxeValidToAmityEntity")) {
			list.removeIf(en -> en instanceof Player || en instanceof ArmorStand || en instanceof AmbientCreature || (en instanceof PathfinderMob && !(en instanceof Monster)));
		}
		list.remove(entity);
		for (Entity en : list) {
			if (en instanceof Player) {
				killPlayer((Player) en, entity);
			} else if (en instanceof LivingEntity) {
				killEntityLiving((LivingEntity) en, entity);
			} else {
				killEntity(en);
			}
		}
		return list.size();
	}

	private static void delayKill(LivingEntity entity) {
		int tick = 21;
		if (!(entity instanceof Player) && ConfigLoader.loliPickaxeDelayRemoveList != null) {
			// 注意：ForgeRegistries.ENTITY_TYPES#getKey 标注为 @Nullable，
			// 当实体类型不在注册表中时返回 null，直接调用 toString()/getPath() 会抛 NPE 并中断击杀流程。
			ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
			if (id != null) {
				String fullName = id.toString();
				String path = id.getPath();
				if (ConfigLoader.loliPickaxeDelayRemoveList.containsKey(fullName)) {
					tick = ConfigLoader.loliPickaxeDelayRemoveList.get(fullName);
				} else if (ConfigLoader.loliPickaxeDelayRemoveList.containsKey(path)) {
					tick = ConfigLoader.loliPickaxeDelayRemoveList.get(path);
				}
			}
		}
		LoliTickEvent.addTask(new LoliTickEvent.TickStartTask(tick, () -> {
			((ILoliDataHolder) entity).setLoliCool(true);
			entity.discard();
			NetworkHandler.sendToAll(new LoliKillEntityPacket(entity.level().dimension(), entity.getId()));
		}), Phase.START);
	}

	/**
	 * 单次背包扫描的结果：是否持有可用萝莉镐 + 最后找到的那一把。
	 *
	 * <p>把「是否持有」与「取出物品」合并成一次扫描，是因为这两件事在热路径上总是成对出现
	 * （例如 {@code onUpdate} 先判断保护、再取物品读配置）。
	 * 原本两个方法各自完整扫一遍背包（36 格 + 光标），在大型整合包里被每 tick 反复调用，
	 * 属于纯粹的重复开销。语义上与原实现<b>逐格等价</b>：遍历顺序、归属校验、
	 * 非本人的镐被丢弃的行为都完全一致，只是不再扫第二遍。
	 */
	public static final class LoliScanResult {

		/** 玩家是否持有至少一把「属于本人」的萝莉镐。 */
		public final boolean hasLoli;
		/** 扫描中最后找到的可用萝莉镐；未找到时为 {@link ItemStack#EMPTY}。 */
		public final ItemStack pickaxe;

		LoliScanResult(boolean hasLoli, ItemStack pickaxe) {
			this.hasLoli = hasLoli;
			this.pickaxe = pickaxe;
		}
	}

	/**
	 * 扫描玩家背包与光标，一次性得出「是否持有可用萝莉镐」与「那一把萝莉镐」。
	 *
	 * <p>行为与原 {@link #invHaveLoliPickaxe} / {@link #getLoliPickaxe} 完全一致：
	 * <ul>
	 *   <li>按快捷栏→主背包→副手的原版顺序遍历全部格子；</li>
	 *   <li>每个格子若是 {@link ILoli}：属于本人则记录（后者覆盖前者，与原实现相同），
	 *       不属于本人则丢弃该物品并清空该格；</li>
	 *   <li>最后再处理光标（{@code containerMenu.getCarried}），同样覆盖式记录；</li>
	 *   <li>{@code hasLoli} 额外包含「萝莉进入冷却计时 ({@code getHodeLoli() > 0})」这一条件，
	 *       与原 {@code invHaveLoliPickaxe} 的返回值定义保持一致。</li>
	 * </ul>
	 *
	 * @param entity 待扫描的实体
	 * @return 扫描结果；非玩家且不是萝莉实体时返回「未持有」
	 */
	public static LoliScanResult scanLoli(LivingEntity entity) {
		if (entity instanceof IEntityLoli) {
			// 萝莉实体自身等价于「持有」；它没有背包，因此没有可返回的物品堆
			return new LoliScanResult(true, ItemStack.EMPTY);
		}
		if (!(entity instanceof Player)) {
			return new LoliScanResult(false, ItemStack.EMPTY);
		}
		Player player = (Player) entity;
		Inventory inventory = player.getInventory();
		boolean hasLoli = false;
		ItemStack iloli = ItemStack.EMPTY;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
				if (checkOwner(player, stack)) {
					hasLoli = true;
					iloli = stack;
				} else {
					dropUnusable(player, stack);
					inventory.setItem(i, ItemStack.EMPTY);
				}
			}
		}
		ItemStack carried = player.containerMenu.getCarried();
		if (!carried.isEmpty() && carried.getItem() instanceof ILoli) {
			if (checkOwner(player, carried)) {
				hasLoli = true;
				iloli = carried;
			} else {
				dropUnusable(player, carried);
				player.containerMenu.setCarried(ItemStack.EMPTY);
			}
		}
		return new LoliScanResult(hasLoli || ((ILoliPlayerData) player).getHodeLoli() > 0, iloli);
	}

	public static boolean invHaveLoliPickaxe(LivingEntity entity) {
		return scanLoli(entity).hasLoli;
	}

	public static ItemStack getLoliPickaxe(LivingEntity entity) {
		return scanLoli(entity).pickaxe;
	}

	private static void dropUnusable(Player player, ItemStack stack) {
		// 只在服务端真正丢弃：客户端调用 player.drop 会生成「幽灵掉落物」，
		// 在大型整合包里会干扰客户端实体同步（表现为掉落物闪烁/吸附异常）。
		if (!player.level().isClientSide) {
			player.drop(stack, true, false);
		}
	}

	private static boolean checkOwner(Player player, ItemStack stack) {
		ILoli loli = (ILoli) stack.getItem();
		if (loli.hasOwner(stack)) {
			if (loli.isOwner(stack, player)) {
				int time = ConfigLoader.loliPickaxeDuration;
				ILoliPlayerData data = (ILoliPlayerData) player;
				if (time > 0 && time > data.getHodeLoli()) {
					data.setHodeLoli(time);
				}
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	public static double clampConfig(double value, double min, double max) {
		return Mth.clamp(value, min, max);
	}

}
