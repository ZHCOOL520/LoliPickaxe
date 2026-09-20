package com.anotherstar.common.block;

import com.anotherstar.common.gui.ContainerPasswordWorkbench;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class BlockPasswordWorkBench extends Block {

	public BlockPasswordWorkBench() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F));
	}

	// 重写目标方法本身已废弃（1.20.1 的 BlockBehaviour#use 标注为 @Deprecated），
	// 但这是父类要求必须实现的方法，且 1.20.1 没有可替代的方块交互覆写点，故压制警告。
	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide && player instanceof ServerPlayer) {
			MenuProvider provider = new SimpleMenuProvider((id, inventory, playerIn) -> new ContainerPasswordWorkbench(id, inventory, pos), Component.translatable("container.password_work_bench"));
			NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

}
