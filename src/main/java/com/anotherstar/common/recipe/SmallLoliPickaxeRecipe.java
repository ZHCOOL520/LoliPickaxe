package com.anotherstar.common.recipe;

import java.util.Map;
import java.util.Map.Entry;

import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.anotherstar.common.item.tool.ItemSmallLoliPickaxe;
import com.google.common.collect.Maps;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 原 1.12.2 的普通萝莉升级配方：在普通萝莉上放入比当前等级高一级的材料以提升该属性。
 */
public class SmallLoliPickaxeRecipe extends CustomRecipe {

	public SmallLoliPickaxeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		Map<ItemLoliPickaxeMaterial, String> nbtMap = getNbtMap();
		ItemStack loli = ItemStack.EMPTY;
		Map<ItemLoliPickaxeMaterial, Integer> levels = Maps.newHashMap();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				if (stack.getItem() == ItemLoader.smallLoliPickaxe() && loli.isEmpty()) {
					loli = stack;
				} else if (nbtMap.containsKey(stack.getItem()) && !levels.containsKey(stack.getItem())) {
					levels.put((ItemLoliPickaxeMaterial) stack.getItem(), stack.getDamageValue());
				} else {
					return false;
				}
			}
		}
		if (loli.isEmpty() || levels.isEmpty()) {
			return false;
		}
		CompoundTag nbt = loli.hasTag() ? loli.getTag() : new CompoundTag();
		for (Entry<ItemLoliPickaxeMaterial, Integer> entry : levels.entrySet()) {
			String levelKey = nbtMap.get(entry.getKey());
			int loliLevel = nbt.contains(levelKey) ? nbt.getInt(levelKey) : -1;
			if (loliLevel != entry.getValue() - 1) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
		ItemStack loli = ItemStack.EMPTY;
		NonNullList<ItemStack> addons = NonNullList.create();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				if (stack.getItem() == ItemLoader.smallLoliPickaxe()) {
					loli = stack;
				} else {
					addons.add(stack);
				}
			}
		}
		if (loli.isEmpty()) {
			return ItemStack.EMPTY;
		}
		Map<ItemLoliPickaxeMaterial, String> nbtMap = getNbtMap();
		ItemStack result = loli.copy();
		CompoundTag nbt = result.getOrCreateTag();
		for (ItemStack addon : addons) {
			String levelKey = nbtMap.get(addon.getItem());
			if (levelKey == null) {
				continue;
			}
			int level = nbt.contains(levelKey) ? nbt.getInt(levelKey) : -1;
			nbt.putInt(levelKey, level + 1);
		}
		ItemLoader.smallLoliPickaxe().updateEnchantment(result);
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RecipeLoader.SMALL_LOLI_PICKAXE_SERIALIZER.get();
	}

	/** 原 ItemSmallLoliPickaxe#nbtMap 在 getFull() 中初始化。 */
	private static Map<ItemLoliPickaxeMaterial, String> getNbtMap() {
		if (ItemSmallLoliPickaxe.nbtMap.isEmpty()) {
			ItemSmallLoliPickaxe.getFull();
		}
		return ItemSmallLoliPickaxe.nbtMap;
	}

}
