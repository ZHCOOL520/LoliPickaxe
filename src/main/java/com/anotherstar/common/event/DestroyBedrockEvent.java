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
		boolean autoFurnace = loli.getEnchantmentLevel(EnchantmentLoader.loliAutoFurnace) > 0;
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
					if (dropStacks.isEmpty() && mandatoryDrop && state.getBlock().asItem() != Items.AIR) {
						dropStacks.add(new ItemStack(state.getBlock()));
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
				for (int m = 0; m < inventory.getMaxPage(); m++) {
					NonNullList<ItemStack> stacks = inventory.getPage(m);
					for (int n = 0; n < stacks.size(); n++) {
						ItemStack slotStack = stacks.get(n);
						if (slotStack.isEmpty()) {
							stacks.set(n, dropStack.copy());
							dropStack.setCount(0);
							break;
						} else {
							int maxCount = inventory.cancelStackLimit() ? inventory.getMaxStackSize() : Math.min(inventory.getMaxStackSize(), slotStack.getMaxStackSize());
							int count = Math.min(maxCount - slotStack.getCount(), dropStack.getCount());
							if (count > 0 && ItemStack.isSameItem(slotStack, dropStack) && ItemStack.isSameItemSameTags(slotStack, dropStack)) {
								slotStack.grow(count);
								dropStack.shrink(count);
								if (dropStack.isEmpty()) {
									break;
								}
							}
						}
					}
					if (dropStack.isEmpty()) {
						break;
					}
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
