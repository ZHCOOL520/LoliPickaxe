package com.anotherstar.common.block;

import com.anotherstar.common.entity.EntityLoliBuffAttackTNT;
import com.anotherstar.network.LoliDeadPacket;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class BlockBuffAttackTNT extends TntBlock {

	private boolean blueScreen;
	private boolean exit;
	private boolean failRespond;

	public BlockBuffAttackTNT(String name, boolean blueScreen, boolean exit, boolean failRespond) {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).instabreak());
		this.blueScreen = blueScreen;
		this.exit = exit;
		this.failRespond = failRespond;
	}

	/**
	 * 对应原 1.12.2 的 onBlockDestroyedByExplosion。
	 */
	@Override
	public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
		if (!level.isClientSide) {
			EntityLoliBuffAttackTNT entitytntprimed = new EntityLoliBuffAttackTNT(level, (double) ((float) pos.getX() + 0.5F), (double) pos.getY(), (double) ((float) pos.getZ() + 0.5F), explosion.getIndirectSourceEntity(), this);
			entitytntprimed.setFuse(level.random.nextInt(entitytntprimed.getFuse() / 4) + entitytntprimed.getFuse() / 8);
			level.addFreshEntity(entitytntprimed);
		}
	}

	/**
	 * 对应原 1.12.2 的 explode。
	 */
	@Override
	public void onCaughtFire(BlockState state, Level level, BlockPos pos, Direction direction, LivingEntity igniter) {
		if (!level.isClientSide) {
			EntityLoliBuffAttackTNT entitytntprimed = new EntityLoliBuffAttackTNT(level, (double) ((float) pos.getX() + 0.5F), (double) pos.getY(), (double) ((float) pos.getZ() + 0.5F), igniter, this);
			level.addFreshEntity(entitytntprimed);
			level.playSound(null, entitytntprimed.getX(), entitytntprimed.getY(), entitytntprimed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	public void buffAttack(ServerPlayer player) {
		NetworkHandler.sendToPlayer(new LoliDeadPacket(false, blueScreen, exit, failRespond), player);
	}

}
