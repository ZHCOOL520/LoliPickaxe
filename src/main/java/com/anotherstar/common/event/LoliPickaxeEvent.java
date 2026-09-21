package com.anotherstar.common.event;

import java.util.List;
import java.util.Set;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.tool.ILoli;
import com.anotherstar.api.ILoliPlayerData;
import com.anotherstar.core.util.EventUtil;
import com.anotherstar.network.LoliKillFacingPacket;
import com.anotherstar.network.NetworkHandler;
import com.anotherstar.util.LoliPickaxeUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class LoliPickaxeEvent {

	/**
	 * 「正在被强制击杀」的实体类型集合。
	 *
	 * <p><b>为什么必须是并发集合</b>：{@link LoliPickaxeUtil#killEntityLiving} 在调用
	 * {@code entity.die()} <b>之前</b>加入本集合、<b>之后</b>移除，而 {@code die()} 内部可能触发
	 * 掉落物生成并进而触发 {@link #onEntityItemJoinWorld}（遍历本集合）。
	 * 原实现使用普通 {@code HashSet}，一旦「遍历过程中又发生一次击杀」就会抛出
	 * {@link java.util.ConcurrentModificationException} 并中断服务端 tick（表现为服务端崩溃）。
	 * 改用 {@code ConcurrentHashMap.newKeySet()} 后遍历为弱一致，不再抛出该异常。
	 */
	public static final Set<Class<? extends LivingEntity>> antiEntity = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private Set<String> flyingPlayer = Sets.newHashSet();
	private Set<Player> loliPlayer = Sets.newHashSet();

	@SubscribeEvent
	public void onLiftClick(PlayerInteractEvent.LeftClickEmpty event) {
		Player player = event.getEntity();
		if (ConfigLoader.getBoolean(player.getMainHandItem(), "loliPickaxeKillFacing") && !player.isSpectator() && !player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() instanceof ILoli) {
			if (player.level().isClientSide) {
				NetworkHandler.sendToServer(new LoliKillFacingPacket());
			}
		}
	}

	@SubscribeEvent
	public void onGetHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide) {
			return;
		}
		if (LoliPickaxeUtil.invHaveLoliPickaxe(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onAttack(LivingAttackEvent event) {
		LivingEntity entity = event.getEntity();
		if (!entity.level().isClientSide) {
			if (LoliPickaxeUtil.invHaveLoliPickaxe(entity)) {
				if (ConfigLoader.getBoolean(LoliPickaxeUtil.getLoliPickaxe(entity), "loliPickaxeThorns")) {
					Entity source = event.getSource().getEntity();
					if (source != null) {
						LivingEntity el = null;
						if (source instanceof AbstractArrow) {
							Entity se = ((AbstractArrow) source).getOwner();
							if (se instanceof LivingEntity) {
								el = (LivingEntity) se;
							}
						} else if (source instanceof LivingEntity) {
							el = (LivingEntity) source;
						}
						if (el != null) {
							if (el instanceof Player) {
								LoliPickaxeUtil.killPlayer((Player) el, entity);
							} else {
								LoliPickaxeUtil.killEntityLiving(el, entity);
							}
						}
					}
				}
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerUpdate(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		// 一次扫描同时得到「是否持有」与「那把萝莉镐」。
		// 原实现先 invHaveLoliPickaxe(entity)，随后又 getLoliPickaxe(player)，
		// 等于每个生物每 tick 把玩家背包完整扫两遍；在成千实体的大型整合包里这是纯浪费。
		// 合并后判定顺序、归属校验与丢弃行为逐格等价。
		LoliPickaxeUtil.LoliScanResult scan = LoliPickaxeUtil.scanLoli(entity);
		boolean isLoli = scan.hasLoli;
		if (isLoli && !entity.level().isClientSide) {
			entity.extinguishFire();
			if (ConfigLoader.getBoolean(entity.getMainHandItem(), "loliPickaxeAutoKillRangeEntity")) {
				int range = ConfigLoader.getInt(entity.getMainHandItem(), "loliPickaxeAutoKillRange");
				LoliPickaxeUtil.killRangeEntity(entity.level(), entity, range);
			}
		}
		if (entity instanceof Player) {
			Player player = (Player) entity;
			String name = player.getName().getString();
			ItemStack stack = scan.pickaxe;
			ILoliPlayerData data = (ILoliPlayerData) player;
			if (data.getHodeLoli() > 0) {
				data.setHodeLoli(data.getHodeLoli() - 1);
			}
			if (isLoli) {
				flyingPlayer.add(name);
				loliPlayer.add(player);
				player.getAbilities().setFlyingSpeed(0.05F);
				player.getAbilities().setWalkingSpeed(0.1F);
				EventUtil.applyReachDistance(player, stack);
				if (!player.level().isClientSide) {
					List<MobEffect> potions = Lists.newArrayList();
					if (stack.hasTag() && stack.getTag().contains("LoliPotion")) {
						ListTag list = stack.getTag().getList("LoliPotion", 10);
						for (int i = 0; i < list.size(); i++) {
							CompoundTag element = list.getCompound(i);
							MobEffect potion = readPotion(element);
							if (potion == null) {
								continue;
							}
							byte level = element.getByte("lvl");
							potions.add(potion);
							player.addEffect(new MobEffectInstance(potion, 410, level, false, false));
						}
					}
					for (MobEffectInstance instance : Lists.newArrayList(player.getActiveEffects())) {
						if (!potions.contains(instance.getEffect())) {
							player.removeEffect(instance.getEffect());
						}
					}
					player.getFoodData().eat(20, 1.0F);
				}
			} else {
				if (flyingPlayer.remove(name)) {
					if (!player.isSpectator() && !player.isCreative()) {
						player.getAbilities().mayfly = false;
						player.getAbilities().flying = false;
					}
				}
				loliPlayer.remove(player);
			}
			if (ConfigLoader.loliPickaxeFindOwner && !player.level().isClientSide && shouldFindOwner(player)) {
				List<ItemEntity> entityItems = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(player.getX() - ConfigLoader.loliPickaxeFindOwnerRange, player.getY() - ConfigLoader.loliPickaxeFindOwnerRange, player.getZ() - ConfigLoader.loliPickaxeFindOwnerRange, player.getX() + ConfigLoader.loliPickaxeFindOwnerRange, player.getY() + ConfigLoader.loliPickaxeFindOwnerRange, player.getZ() + ConfigLoader.loliPickaxeFindOwnerRange));
				for (ItemEntity entityItem : entityItems) {
					ItemStack estack = entityItem.getItem();
					if (!estack.isEmpty() && estack.getItem() instanceof ILoli) {
						ILoli loli = (ILoli) estack.getItem();
						if (loli.hasOwner(estack) && loli.isOwner(estack, player)) {
							entityItem.playerTouch(player);
						}
					}
				}
			}
		} else if (isLoli) {
			entity.removeAllEffects();
		}
	}

	/**
	 * 药水 NBT 用资源路径（{@code "id"}）保存。
	 * <p>
	 * 1.12.2 的数字 id 与 1.20.1 注册表分配的数字 id 完全不对应，按数字 id 查表会解析出错误的药水，
	 * 因此不再兼容数字 id，只支持 {@code "id"} 为资源路径字符串的写法。
	 */
	private static MobEffect readPotion(CompoundTag element) {
		String name = element.getString("id");
		if (name.isEmpty()) {
			return null;
		}
		return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(name));
	}

	/**
	 * 寻主查询的节流控制。
	 *
	 * <p><b>为什么需要节流</b>：寻主逻辑每次都要对 {@code loliPickaxeFindOwnerRange}
	 * （默认 50 格）做一次立方体范围的实体查询。在大型整合包里这项查询每 tick、每玩家执行一次，
	 * 是相当可观的开销（范围实体查询需要遍历区块内的实体列表）。
	 *
	 * <p><b>为什么不改变玩法</b>：寻主的目的是「让掉在地上的、属于本玩家的萝莉镐被吸回」，
	 * 这本身是个持续状态而非瞬时判定。改为每 {@value #FIND_OWNER_INTERVAL} tick 检查一次后，
	 * 掉落物最多晚 0.1~0.15 秒被吸回，玩家几乎无法感知；
	 * 而查询开销降到原来的 1/{@value #FIND_OWNER_INTERVAL}。
	 * 这是「保持结果不变、仅降低检查频率」的优化，不改变任何配置项语义。
	 *
	 * @param player 待检查的玩家
	 * @return true 表示本 tick 应当执行寻主查询
	 */
	private boolean shouldFindOwner(Player player) {
		int interval = FIND_OWNER_INTERVAL;
		if (interval <= 1) {
			return true;
		}
		int tick = player.tickCount;
		return tick % interval == 0;
	}

	/** 寻主查询的执行间隔（tick）。取 3 即每 0.15 秒一次，兼顾响应速度与开销。 */
	private static final int FIND_OWNER_INTERVAL = 3;

	@SubscribeEvent
	public void onEntityItemJoinWorld(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		for (Class<? extends LivingEntity> clazz : antiEntity) {
			if (clazz.isInstance(entity)) {
				event.setCanceled(true);
				return;
			}
		}
		if (entity instanceof ItemEntity) {
			ItemEntity entityItem = (ItemEntity) entity;
			if (!entityItem.getItem().isEmpty() && entityItem.getItem().getItem() instanceof ILoli) {
				entityItem.setInvulnerable(true);
				if (ConfigLoader.loliPickaxeFindOwner) {
					entityItem.setNoPickUpDelay();
				}
			}
		}
	}

	@SubscribeEvent
	public void onEntityItemPickup(EntityItemPickupEvent event) {
		ItemStack stack = event.getItem().getItem();
		if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
			ILoli loli = (ILoli) stack.getItem();
			if (loli.hasOwner(stack) && !loli.isOwner(stack, event.getEntity())) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerOut(PlayerEvent.PlayerLoggedOutEvent event) {
		// 两个集合都必须清理：flyingPlayer 以玩家名为键，若不清理会在玩家
		// 「持有萝莉镐时退出」后永久残留，造成集合无界增长；
		// loliPlayer 则持有 Player 强引用，不清理会阻止玩家对象被 GC。
		loliPlayer.remove(event.getEntity());
		flyingPlayer.remove(event.getEntity().getName().getString());
	}

}
