package com.anotherstar.common.entity.ai;

import com.anotherstar.common.entity.EntityLoli;

import net.minecraft.world.entity.ai.goal.FloatGoal;

public class EntityAILoliSwimming extends FloatGoal {

	private EntityLoli loli;
	private boolean obstructed;

	public EntityAILoliSwimming(EntityLoli loli) {
		super(loli);
		this.loli = loli;
	}

	@Override
	public boolean canContinueToUse() {
		return super.canContinueToUse() && !this.obstructed;
	}

	@Override
	public void tick() {
		if (this.loli.getNavigation().isDone() && this.loli.getTarget() == null) {
			super.tick();
		}
	}

	@Override
	public void start() {
		this.obstructed = false;
	}

}
