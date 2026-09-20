package com.anotherstar.common.item;

import java.util.List;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.client.util.LoliCardUtil;
import com.anotherstar.network.LoliCardPacket;
import com.anotherstar.network.LoliCardPacket.ItemType;
import com.anotherstar.network.NetworkHandler;
import com.google.common.collect.Lists;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemLoliCardAlbum extends Item {

	public ItemLoliCardAlbum() {
		super(new Item.Properties());
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return stack.hasTag() ? 64 : 1;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.anotherstar.client.gui.ClientGuiOpener.openCardAlbum(stack));
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (level.isClientSide && LoliCardUtil.customArtNames != null && LoliCardUtil.customArtNames.length != 0 && (!stack.hasTag() || !stack.getTag().contains("PictureGroup"))) {
			List<String> groups = Lists.newArrayList();
			for (String name : LoliCardUtil.customArtNames) {
				int index = name.indexOf('\'');
				if (index != -1) {
					String group = name.substring(0, index);
					if (!groups.contains(group)) {
						groups.add(group);
					}
				}
			}
			if (!groups.isEmpty()) {
				NetworkHandler.sendToServer(new LoliCardPacket(itemSlot, ItemType.LOLICARDALBUM, groups.get(level.random.nextInt(groups.size()))));
			}
		}
	}

	public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
		if (tab == CreativeTabLoader.loliTabs && LoliCardUtil.customArtNames != null && LoliCardUtil.customArtNames.length != 0) {
			List<String> groups = Lists.newArrayList();
			for (String name : LoliCardUtil.customArtNames) {
				int index = name.indexOf('\'');
				if (index != -1) {
					String group = name.substring(0, index);
					if (!groups.contains(group)) {
						groups.add(group);
						ItemStack stack = new ItemStack(this);
						CompoundTag nbt = new CompoundTag();
						nbt.putString("PictureGroup", group);
						stack.setTag(nbt);
						output.accept(stack);
					}
				}
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			if (nbt.contains("PictureGroup")) {
				tooltip.add(Component.literal(nbt.getString("PictureGroup")));
			}
		}
	}

}
