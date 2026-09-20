package com.anotherstar.common.entity.ai;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;

public class EntityAILoliAttack extends MeleeAttackGoal {

	/** 原实现攻击间隔为 5 tick。 */
	private int attackTick;

	public EntityAILoliAttack(PathfinderMob creature) {
		super(creature, 1.0D, false);
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.mob.getTarget();
		if (target == null) {
			return false;
		}
		if (LoliPickaxeUtil.invHaveLoliPickaxe(target)) {
			this.mob.setTarget(null);
			return false;
		}
		if (ConfigLoader.loliTeleport) {
			this.mob.ejectPassengers();
			this.mob.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
			return true;
		}
		return super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = this.mob.getTarget();
		if (target == null) {
			return false;
		}
		if (!target.isAlive()) {
			return false;
		}
		if (target instanceof Player && ((Player) target).isSpectator() || LoliPickaxeUtil.invHaveLoliPickaxe(target)) {
			return false;
		}
		return !this.mob.getNavigation().isDone();
	}

	@Override
	public void start() {
		this.attackTick = 0;
		super.start();
	}

	@Override
	public void stop() {
		LivingEntity target = this.mob.getTarget();
		if (target instanceof Player && ((Player) target).isSpectator() || LoliPickaxeUtil.invHaveLoliPickaxe(target)) {
			this.mob.setTarget(null);
		}
		this.mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.attackTick > 0) {
			this.attackTick--;
		}
		super.tick();
	}

	@Override
	protected void checkAndPerformAttack(LivingEntity entity, double distance) {
		double reach = this.getAttackReachSqr(entity);
		if (distance <= reach && this.attackTick <= 0) {
			this.attackTick = 5;
			this.mob.swing(InteractionHand.MAIN_HAND);
			this.mob.doHurtTarget(entity);
		}
	}

}
