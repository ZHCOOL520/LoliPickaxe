package com.anotherstar.common.recipe;

import java.util.Map.Entry;

import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.anotherstar.common.item.tool.ItemLoliPickaxe;
import com.anotherstar.common.item.tool.ItemSmallLoliPickaxe;

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
 * 原 1.12.2 的萝莉镐配方：满级普通萝莉 + 满级生物灵魂。
 */
public class LoliPickaxeRecipe extends CustomRecipe {

	public LoliPickaxeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		ItemStack loli = ItemStack.EMPTY;
		ItemStack soul = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				if (stack.getItem() == ItemLoader.smallLoliPickaxe() && loli.isEmpty()) {
					loli = stack;
				} else if (stack.getItem() == ItemLoader.entitySoul() && soul.isEmpty()) {
					soul = stack;
				} else {
					return false;
				}
			}
		}
		if (loli.isEmpty() || !loli.hasTag() || soul.isEmpty() || soul.getDamageValue() != ItemLoader.entitySoul().getSubCount() - 1) {
			return false;
		}
		CompoundTag nbt = loli.getTag();
		for (Entry<ItemLoliPickaxeMaterial, String> entry : ItemSmallLoliPickaxe.nbtMap.entrySet()) {
			if (!nbt.contains(entry.getValue()) || nbt.getInt(entry.getValue()) != entry.getKey().getSubCount() - 1) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
		ItemStack loli = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.getItem() == ItemLoader.smallLoliPickaxe()) {
				loli = stack;
				break;
			}
		}
		ItemStack result = ItemLoliPickaxe.getDef().copy();
		if (loli.hasTag()) {
			CompoundTag tag = result.getTag();
			if (tag == null) {
				tag = new CompoundTag();
				result.setTag(tag);
			}
			if (loli.getTag().contains("Pages")) {
				tag.put("Pages", loli.getTag().getCompound("Pages"));
			}
			if (loli.getTag().contains("Blacklist")) {
				tag.put("Blacklist", loli.getTag().getCompound("Blacklist"));
			}
		}
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 1;
	}

	@Override
	public String getGroup() {
		return "loli_pickaxe";
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RecipeLoader.LOLI_PICKAXE_SERIALIZER.get();
	}

}
