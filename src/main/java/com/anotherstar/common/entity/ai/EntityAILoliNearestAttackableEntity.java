package com.anotherstar.common.entity.ai;

import java.util.Comparator;
import java.util.List;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.entity.EntityLoli;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class EntityAILoliNearestAttackableEntity extends NearestAttackableTargetGoal<LivingEntity> {

	public EntityAILoliNearestAttackableEntity(EntityLoli creature) {
		super(creature, LivingEntity.class, false);
	}

	@Override
	public boolean canUse() {
		if (!ConfigLoader.loliAttack) {
			return false;
		}
		List<LivingEntity> list = this.mob.level().getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), entity -> entity != this.mob && this.canAttack(entity, this.targetConditions));
		list.removeIf(entity -> entity instanceof EntityLoli);
		if (list.isEmpty()) {
			return false;
		}
		list.sort(Comparator.comparingDouble(this.mob::distanceToSqr));
		this.setTarget(list.get(0));
		return true;
	}

}
