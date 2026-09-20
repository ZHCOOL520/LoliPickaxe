package com.anotherstar.common.entity.ai;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.entity.EntityLoli;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class EntityAILoliNearestAttackablePlayer extends NearestAttackableTargetGoal<Player> {

	public EntityAILoliNearestAttackablePlayer(EntityLoli creature) {
		super(creature, Player.class, false);
	}

	@Override
	public boolean canUse() {
		if (!ConfigLoader.loliAttack) {
			return false;
		}
		// 原实现直接拿平方距离与跟随距离比较，这里保持一致。
		double range = this.getFollowDistance();
		Player target = null;
		double min = Double.MAX_VALUE;
		for (Player player : this.mob.level().players()) {
			if (!player.isSpectator() && !LoliPickaxeUtil.invHaveLoliPickaxe(player)) {
				double distance = player.distanceToSqr(this.mob);
				if (distance < min && distance < range) {
					min = distance;
					target = player;
				}
			}
		}
		if (target == null) {
			return false;
		}
		this.setTarget(target);
		return true;
	}

}
