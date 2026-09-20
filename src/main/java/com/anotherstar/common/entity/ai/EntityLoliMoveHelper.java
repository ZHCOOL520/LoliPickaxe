package com.anotherstar.common.entity.ai;

import com.anotherstar.common.entity.EntityLoli;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class EntityLoliMoveHelper extends MoveControl {

	private EntityLoli loli;

	public EntityLoliMoveHelper(EntityLoli loli) {
		super(loli);
		this.loli = loli;
	}

	@Override
	public void tick() {
		LivingEntity target = this.loli.getTarget();
		if (target != null && target.isInWater() && this.loli.isInWater()) {
			if (this.operation != MoveControl.Operation.MOVE_TO || this.loli.getNavigation().isDone()) {
				this.loli.setSpeed(0.0F);
				return;
			}
			double dx = this.wantedX - this.loli.getX();
			double dy = this.wantedY - this.loli.getY();
			double dz = this.wantedZ - this.loli.getZ();
			double distance = Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
			dy = dy / distance;
			float yaw = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
			this.loli.setYRot(this.rotlerp(this.loli.getYRot(), yaw, 90.0F));
			this.loli.yBodyRot = this.loli.getYRot();
			float speed = (float) (this.speedModifier * this.loli.getAttributeValue(Attributes.MOVEMENT_SPEED));
			this.loli.setSpeed(this.loli.getSpeed() + (speed - this.loli.getSpeed()) * 0.125F);
			Vec3 motion = this.loli.getDeltaMovement();
			this.loli.setDeltaMovement(motion.x + this.loli.getSpeed() * dx * 0.02D, motion.y + this.loli.getSpeed() * dy * 0.4D, motion.z + this.loli.getSpeed() * dz * 0.02D);
		} else {
			super.tick();
		}
	}

}
