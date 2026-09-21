package com.anotherstar.common.event;

import java.util.Map;
import java.util.Set;

import com.anotherstar.common.item.ItemLoader;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SmallLoliFlyEvent {

	private Set<String> flyingPlayer = Sets.newHashSet();
	private Map<String, Double> dodgeMap = Maps.newHashMap();
	private Map<String, Double> antiInjury = Maps.newHashMap();

	@SubscribeEvent
	public void onPlayerUpdate(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity instanceof Player) {
			Player player = (Player) entity;
			String name = player.getName().getString();
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.getItem() == ItemLoader.smallLoliPickaxe()) {
					if (ItemLoader.smallLoliPickaxe().canFly(stack)) {
						flyingPlayer.add(name);
						player.getAbilities().mayfly = true;
					}
					if (!player.level().isClientSide) {
						switch (ItemLoader.smallLoliPickaxe().buffLevel(stack)) {
						case 3:
							player.getFoodData().eat(20, 1.0F);
						case 2:
							player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 410, 0, false, false));
						case 1:
							player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 410, 0, false, false));
						}
					}
					dodgeMap.put(name, ItemLoader.smallLoliPickaxe().getDodge(stack));
					antiInjury.put(name, ItemLoader.smallLoliPickaxe().getAntiInjury(stack));
					return;
				}
			}
			if (!player.isCreative() && flyingPlayer.contains(name)) {
				flyingPlayer.remove(name);
				player.getAbilities().mayfly = false;
				player.getAbilities().flying = false;
			}
			dodgeMap.remove(name);
			antiInjury.remove(name);
		}
	}

	@SubscribeEvent
	public void onFallDown(LivingHurtEvent event) {
		LivingEntity entity = event.getEntity();
		String name = entity.getName().getString();
		if (!entity.level().isClientSide && entity instanceof Player) {
			if (flyingPlayer.contains(name) && event.getSource().is(DamageTypeTags.IS_FALL)) {
				event.setCanceled(true);
			} else if (dodgeMap.containsKey(name) && entity.level().random.nextDouble() < dodgeMap.get(name)) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onAttack(LivingAttackEvent event) {
		LivingEntity entity = event.getEntity();
		String name = entity.getName().getString();
		if (!entity.level().isClientSide && entity instanceof Player) {
			Player player = (Player) entity;
			if (antiInjury.containsKey(name) && entity.level().random.nextDouble() < antiInjury.get(name)) {
				Entity source = event.getSource().getEntity();
				if (source != null) {
					LivingEntity attacker = null;
					if (source instanceof AbstractArrow) {
						Entity owner = ((AbstractArrow) source).getOwner();
						if (owner instanceof LivingEntity) {
							attacker = (LivingEntity) owner;
						}
					} else if (source instanceof LivingEntity) {
						attacker = (LivingEntity) source;
					}
					if (attacker != null) {
						player.attack(attacker);
						float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
						player.setHealth(Math.min(player.getHealth() + damage, player.getMaxHealth()));
					}
				}
			}
		}
	}

}
