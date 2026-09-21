package com.anotherstar.common.event;

import java.util.List;
import java.util.Optional;

import com.anotherstar.common.gui.ILoliInventory;
import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.tool.ItemSmallLoliPickaxe;
import com.google.common.collect.Lists;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 原 1.12.2 通过 HarvestDropsEvent 修改小萝莉镐的掉落物，1.20.1 已删除该事件
 * （掉落物由战利品表直接生成），这里改为在连锁挖掘期间拦截新生成的掉落物实体，
 * 做与原实现一致的自动熔炼、黑名单过滤和自动收纳处理。
 */
public class SmallLoliBlockDropEvent {

	/** 连锁挖掘期间暂存的玩家与储藏室，用于自动收纳。 */
	private static ServerPlayer collector;
	private static ILoliInventory collectorInventory;

	@SubscribeEvent
	public void onBreak(BlockEvent.BreakEvent event) {
		if (ItemSmallLoliPickaxe.isharvesting || !(event.getPlayer() instanceof ServerPlayer)) {
			return;
		}
		ServerPlayer player = (ServerPlayer) event.getPlayer();
		if (player.getMainHandItem().getItem() != ItemLoader.smallLoliPickaxe()) {
			return;
		}
		collector = player;
	}

	@SubscribeEvent
	public void onItemEntityJoin(EntityJoinLevelEvent event) {
		if (!ItemSmallLoliPickaxe.isharvesting || collector == null || !(event.getEntity() instanceof ItemEntity)) {
			return;
		}
		Level level = event.getEntity().level();
		if (level.isClientSide) {
			return;
		}
		// 原实现在 openInventory/closeInventory 之间读写储藏室，这里对应 startOpen/stopOpen。
		if (collectorInventory == null && ItemSmallLoliPickaxe.inventory != null) {
			collectorInventory = ItemSmallLoliPickaxe.inventory;
			collectorInventory.startOpen(collector);
		}
		ItemEntity entityItem = (ItemEntity) event.getEntity();
		ItemStack stack = entityItem.getItem();
		if (stack.isEmpty()) {
			return;
		}
		List<ItemStack> drops = Lists.newArrayList(stack.copy());
		onDrop(drops, level, 1.0F);
		drops.removeIf(ItemStack::isEmpty);
		if (drops.isEmpty()) {
			event.setCanceled(true);
		} else {
			entityItem.setItem(drops.get(0));
		}
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (collectorInventory != null && !ItemSmallLoliPickaxe.isharvesting) {
			collectorInventory.stopOpen(collector);
			collectorInventory = null;
			collector = null;
		}
	}

	/**
	 * 原掉落物后处理：自动熔炼、黑名单过滤、自动收纳。
	 */
	public static void onDrop(List<ItemStack> drops, Level level, float chance) {
		if (!ItemSmallLoliPickaxe.isharvesting) {
			return;
		}
		NonNullList<ItemStack> furnaceed = NonNullList.create();
		if (ItemSmallLoliPickaxe.autoFurnace) {
			for (ItemStack drop : drops) {
				Optional<SmeltingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drop), level);
				if (recipe.isEmpty()) {
					continue;
				}
				ItemStack result = recipe.get().getResultItem(level.registryAccess());
				if (result.isEmpty()) {
					continue;
				}
				ItemSmallLoliPickaxe.exp += recipe.get().getExperience() * drop.getCount();
				int resultCount = result.getCount() * drop.getCount();
				while (resultCount > 64) {
					furnaceed.add(result.copyWithCount(64));
					resultCount -= 64;
				}
				if (resultCount > 0) {
					furnaceed.add(result.copyWithCount(resultCount));
				}
				drop.setCount(0);
			}
			drops.removeIf(ItemStack::isEmpty);
			drops.addAll(furnaceed);
		}
		if (ItemSmallLoliPickaxe.blacklist != null && !ItemSmallLoliPickaxe.blacklist.isEmpty()) {
			drops.removeIf(stack -> {
				for (ItemStack black : ItemSmallLoliPickaxe.blacklist) {
					if (ItemStack.isSameItem(black, stack)) {
						return true;
					}
				}
				return false;
			});
		}
		if (ItemSmallLoliPickaxe.inventory != null) {
			for (ItemStack drop : drops) {
				if (level.random.nextFloat() > chance) {
					continue;
				}
				// 【关键修复】必须经由 insertItem 写入，而不能直接操作 getPage(...) 返回的列表。
				// getPage 返回的是容器内部的 NonNullList，直接 set/grow 虽然能改到内存，
				// 却不会调用 setChanged()，于是该页不会被视为脏页；stopOpen 时对未变脏的页
				// 会复用 readOpen 阶段读入的原始 NBT 快照，把刚收进来的物品整份覆盖掉，
				// 表现为「收进去了，但打开储藏室根本看不到」。
				// insertItem 内部每一处写入都走 setItemForPage → markDirty，因此能正确落盘。
				ItemStack leftover = ItemSmallLoliPickaxe.inventory.insertItem(drop);
				// 装不下的部分留在 drops 中，照常掉落到地面
				drop.setCount(leftover.isEmpty() ? 0 : leftover.getCount());
			}
			drops.removeIf(ItemStack::isEmpty);
		}
	}

}
