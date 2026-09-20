package com.anotherstar.common.item;

import java.util.List;

import com.anotherstar.api.ILoliDataHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemBugEntityClear extends Item {

	public ItemBugEntityClear() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (player.level().isClientSide) {
			player.swing(hand);
			target.setHealth(0.0F);
			target.discard();
			((ILoliDataHolder) target).setLoliDead(true);
			((ILoliDataHolder) target).setLoliCool(true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("§c不要对正常实体使用!"));
	}

}
