package com.anotherstar.common.recipe;

import java.util.Map;

import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.google.common.collect.Maps;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 原 1.12.2 的叠加配方：9 个同级材料合成 1 个更高级材料，或 1 个材料拆成 9 个低一级材料。
 */
public class SuperpositionRecipe extends CustomRecipe {

	/** 可叠加的材料物品 -> 最高等级(damage)。 */
	private static final Map<Item, Integer> superpositionAble = Maps.newHashMap();

	static {
		registItem(ItemLoader.coalAddon);
		registItem(ItemLoader.ironAddon);
		registItem(ItemLoader.goldAddon);
		registItem(ItemLoader.redstoneAddon);
		registItem(ItemLoader.lapisAddon);
		registItem(ItemLoader.diamondAddon);
		registItem(ItemLoader.emeraldAddon);
		registItem(ItemLoader.obsidianAddon);
		registItem(ItemLoader.glowAddon);
		registItem(ItemLoader.quartzAddon);
		registItem(ItemLoader.netherStarAddon);
		registItem(ItemLoader.entitySoul);
	}

	public SuperpositionRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	public static void registItem(ItemLoliPickaxeMaterial item) {
		registItem(item, item.getSubCount() - 1);
	}

	public static void registItem(Item item, int maxDamage) {
		if (maxDamage > 0) {
			superpositionAble.put(item, maxDamage);
		}
	}

	public static Map<Item, Integer> getSuperpositionAble() {
		return superpositionAble;
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		Item item = null;
		int damage = -1;
		int count = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				if (!superpositionAble.containsKey(stack.getItem())) {
					return false;
				}
				count++;
				if (item == null) {
					item = stack.getItem();
				} else if (stack.getItem() != item) {
					return false;
				}
				if (damage == -1) {
					damage = stack.getDamageValue();
				} else if (stack.getDamageValue() != damage) {
					return false;
				}
			}
		}
		if (item == null) {
			return false;
		}
		if (count == 9) {
			return damage < superpositionAble.get(item);
		} else if (count == 1) {
			return damage > 0;
		}
		return false;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
		Item item = null;
		int damage = -1;
		int count = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				count++;
				item = stack.getItem();
				damage = stack.getDamageValue();
			}
		}
		if (item == null) {
			return ItemStack.EMPTY;
		}
		if (count == 9) {
			if (damage >= superpositionAble.getOrDefault(item, 0)) {
				return ItemStack.EMPTY;
			}
			ItemStack result = new ItemStack(item, 1);
			result.setDamageValue(damage + 1);
			return result;
		} else if (count == 1) {
			if (damage <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack result = new ItemStack(item, 9);
			result.setDamageValue(damage - 1);
			return result;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width == 3 && height == 3 || width == 1 && height == 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RecipeLoader.SUPERPOSITION_SERIALIZER.get();
	}

}
