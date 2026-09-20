package com.anotherstar.common.item;

import java.util.List;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.common.config.ConfigLoader;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemLoliCardOnline extends Item {

	public ItemLoliCardOnline() {
		super(new Item.Properties());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
				if (player.isShiftKeyDown()) {
					com.anotherstar.client.gui.ClientGuiOpener.openCardOnlineConfig(stack);
				} else {
					com.anotherstar.client.gui.ClientGuiOpener.openCardOnline(stack);
				}
			});
		}
		return InteractionResultHolder.success(stack);
	}

	public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
		if (tab == CreativeTabLoader.loliTabs) {
			output.accept(new ItemStack(this));
			List<String> urls = ConfigLoader.loliCardOnlineDefURL;
			if (urls != null) {
				for (String url : urls) {
					ItemStack stack = new ItemStack(this);
					CompoundTag nbt = new CompoundTag();
					nbt.putString("ImageUrl", url);
					stack.setTag(nbt);
					output.accept(stack);
				}
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			if (nbt.contains("ImageUrl")) {
				tooltip.add(Component.literal(nbt.getString("ImageUrl")));
			}
		}
	}

}
