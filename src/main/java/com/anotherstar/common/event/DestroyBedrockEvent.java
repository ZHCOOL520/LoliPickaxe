package com.anotherstar.common.event;

import java.util.List;
import java.util.Optional;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.enchantment.EnchantmentLoader;
import com.anotherstar.common.gui.ILoliInventory;
import com.anotherstar.common.item.tool.IContainer;
import com.anotherstar.common.item.tool.ILoli;
import com.google.common.collect.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class DestroyBedrockEvent {

	@SubscribeEvent
	public void onPlayerMine(PlayerInteractEvent.LeftClickBlock event) {
		if (!event.getItemStack().isEmpty() && event.getItemStack().getItem() instanceof ILoli && !event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer) {
			breakBlock(event.getItemStack(), event.getPos(), (ServerPlayer) event.getEntity());
		}
	}

	// Block#getCloneItemStack(BlockGetter,BlockPos,BlockState) 已被 Forge 标记为废弃；
	// 其替代品 IForgeBlock#getCloneItemStack(BlockState,HitResult,BlockGetter,BlockPos,Player) 需要额外的
	// HitResult/Player，且默认实现只是转调这个已废弃方法（本工程的 MC/Forge 中无任何实现覆写它）。
	// 为不伪造 HitResult、确保掉落结果与之前完全一致，这里保留原调用并压制警告。
	@SuppressWarnings("deprecation")
	private void breakBlock(ItemStack loli, BlockPos pos, ServerPlayer player) {
		Level level = player.level();
		if (level.isClientSide || player.isCreative() || !(loli.getItem() instanceof ILoli)) {
			return;
		}
		ServerLevel serverLevel = player.serverLevel();
		int range = Mth.clamp(((ILoli) loli.getItem()).getRange(loli), 0, ConfigLoader.loliPickaxeMaxRange);
		boolean mandatoryDrop = ConfigLoader.getBoolean(loli, "loliPickaxeMandatoryDrop");
		// EnchantmentHelper.getItemEnchantmentLevel 已废弃，改用其推荐的 ItemStack#getEnchantmentLevel（等价）。
		boolean silkTouch = loli.getEnchantmentLevel(Enchantments.SILK_TOUCH) > 0;
		boolean autoFurnace = loli.getEnchantmentLevel(EnchantmentLoader.loliAutoFurnace()) > 0;
		boolean auto = ConfigLoader.getBoolean(loli, "loliPickaxeAutoAccept") && ((IContainer) loli.getItem()).hasInventory(loli);
		ILoliInventory inventory = null;
		if (auto) {
			inventory = ((IContainer) loli.getItem()).getInventory(loli);
			inventory.startOpen(player);
		}
		NonNullList<ItemStack> drops = NonNullList.create();
		float exp = 0;
		for (int i = -range; i <= range; i++) {
			for (int j = -range; j <= range; j++) {
				for (int k = -range; k <= range; k++) {
					BlockPos curPos = pos.offset(i, j, k);
					if (ConfigLoader.loliPickaxeTriggerBreakEvent && ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos) == -1) {
						continue;
					}
					BlockState state = level.getBlockState(curPos);
					if (state.isAir()) {
						continue;
					}
					// 1.20.1 的掉落由战利品表决定，直接传入工具即可自动处理时运与精准采集。
					List<ItemStack> dropStacks = Lists.newArrayList(Block.getDrops(state, serverLevel, curPos, null, player, loli));
					if (dropStacks.isEmpty() && silkTouch) {
						// 已废弃调用，保留原因见本方法上方的 @SuppressWarnings 说明。
						ItemStack silkDrop = state.getBlock().getCloneItemStack(serverLevel, curPos, state);
						if (!silkDrop.isEmpty()) {
							dropStacks.add(silkDrop);
						}
					}
					if (autoFurnace) {
						NonNullList<ItemStack> furnaceed = NonNullList.create();
						for (ItemStack stack : dropStacks) {
							Optional<SmeltingRecipe> recipe = serverLevel.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level);
							if (recipe.isEmpty()) {
								continue;
							}
							ItemStack result = recipe.get().getResultItem(level.registryAccess());
							if (result.isEmpty()) {
								continue;
							}
							exp += recipe.get().getExperience() * stack.getCount();
							int resultCount = result.getCount() * stack.getCount();
							while (resultCount > 64) {
								furnaceed.add(result.copyWithCount(64));
								resultCount -= 64;
							}
							if (resultCount > 0) {
								furnaceed.add(result.copyWithCount(resultCount));
							}
						}
						if (!furnaceed.isEmpty()) {
							dropStacks.clear();
							dropStacks.addAll(furnaceed);
						}
					}
					if (mandatoryDrop) {
						// 先剔除空堆：战利品表可能返回「列表非空、但元素全是空堆」，
						// 旧写法只在 dropStacks.isEmpty() 成立时才补掉落，遇到这种情况直接失效，
						// 这正是玩家反馈「强制掉落无法使用」的原因之一。
						dropStacks.removeIf(ItemStack::isEmpty);
						if (dropStacks.isEmpty()) {
							// 用 BlockState 取「选取方块」物品，保留方块状态与 NBT；
							// 旧写法 new ItemStack(state.getBlock()) 会丢失状态信息
							//（例如不同木材种类、带内容的容器方块会掉落错误物品）。
							ItemStack mandatory = state.getBlock().getCloneItemStack(serverLevel, curPos, state);
							if (mandatory.isEmpty() && state.getBlock().asItem() != Items.AIR) {
								mandatory = new ItemStack(state.getBlock().asItem());
							}
							if (!mandatory.isEmpty()) {
								dropStacks.add(mandatory);
							}
						}
					}
					drops.addAll(dropStacks);
					level.removeBlock(curPos, false);
				}
			}
		}
		NonNullList<ItemStack> blacklist = NonNullList.create();
		if (loli.hasTag() && loli.getTag().contains("Blacklist")) {
			ListTag blackList = loli.getTag().getList("Blacklist", 10);
			for (int i = 0; i < blackList.size(); i++) {
				CompoundTag black = blackList.getCompound(i);
				if (black.contains("Name") && black.contains("Damage")) {
					Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(black.getString("Name")));
					if (item != null && item != Items.AIR) {
						ItemStack blackStack = new ItemStack(item);
						blackStack.setDamageValue(black.getInt("Damage"));
						blacklist.add(blackStack);
					}
				}
			}
		}
		if (!blacklist.isEmpty()) {
			drops.removeIf(stack -> {
				for (ItemStack black : blacklist) {
					if (ItemStack.isSameItem(black, stack)) {
						return true;
					}
				}
				return false;
			});
		}
		if (auto) {
			for (ItemStack dropStack : drops) {
				// 【关键修复】必须经由 insertItem 写入，而不是直接操作 getPage(...) 返回的列表。
				// 直接改列表不会调用 setChanged()，该页就不会被标记为脏页；
				// 而 stopOpen() 对未变脏的页会复用读入时的原始 NBT 快照，
				// 于是刚收进储藏室的物品会被旧快照整份覆盖 —— 这正是玩家反馈的
				// 「物品收进背包了但背包里看不到、实际也没有」。
				ItemStack leftover = inventory.insertItem(dropStack);
				// 装不下的部分留在 drops 中，稍后照常掉落到地面
				if (leftover.isEmpty()) {
					dropStack.setCount(0);
				} else {
					dropStack.setCount(leftover.getCount());
				}
			}
		}
		drops.removeIf(ItemStack::isEmpty);
		for (ItemStack dropStack : drops) {
			level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, dropStack));
		}
		if (auto) {
			inventory.stopOpen(player);
		}
		if ((int) exp > 0) {
			level.addFreshEntity(new ExperienceOrb(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, (int) exp));
		}
		SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MODID, "lolisuccess"));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

}
