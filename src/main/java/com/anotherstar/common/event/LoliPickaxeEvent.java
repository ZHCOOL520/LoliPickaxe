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

	public static final Set<Class<? extends LivingEntity>> antiEntity = Sets.newHashSet();

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
		boolean isLoli = LoliPickaxeUtil.invHaveLoliPickaxe(entity);
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
			ItemStack stack = LoliPickaxeUtil.getLoliPickaxe(player);
			ILoliPlayerData data = (ILoliPlayerData) player;
			if (data.getHodeLoli() > 0) {
				data.setHodeLoli(data.getHodeLoli() - 1);
			}
			if (isLoli) {
				flyingPlayer.add(name);
				loliPlayer.add(player);
				player.getAbilities().setFlyingSpeed(0.05F);
				player.getAbilities().setWalkingSpeed(0.1F);
				EventUtil.applyReachDistance(player);
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
			if (ConfigLoader.loliPickaxeFindOwner && !player.level().isClientSide) {
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
		loliPlayer.remove(event.getEntity());
	}

}
