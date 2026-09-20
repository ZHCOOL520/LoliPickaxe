package com.anotherstar.common.item;

import com.anotherstar.common.entity.IEntityLoli;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemLoliDispersal extends Item {

	public ItemLoliDispersal() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (target instanceof IEntityLoli) {
			if (player.level().isClientSide) {
				player.swing(hand);
			} else {
				((IEntityLoli) target).setDispersal(true);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

}
