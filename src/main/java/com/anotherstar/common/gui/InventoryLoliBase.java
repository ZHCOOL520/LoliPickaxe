package com.anotherstar.common.gui;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class InventoryLoliBase implements ILoliInventory {

	private ItemStack stack;
	private List<NonNullList<ItemStack>> pages;
	private int curPage;

	public InventoryLoliBase(ItemStack stack) {
		this.stack = stack;
		this.pages = Lists.newArrayList();
	}

	/** 原 IInventory#getName，1.20.1 已无对应方法，这里仅用于显示名。 */
	public abstract String getName();

	public Component getDisplayName() {
		return Component.translatable(this.getName());
	}

	@Override
	public int getContainerSize() {
		return 81;
	}

	@Override
	public boolean isEmpty() {
		for (NonNullList<ItemStack> stacks : pages) {
			for (ItemStack stack : stacks) {
				if (!stack.isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		return index >= 0 && index < getContainerSize() ? getPage(curPage).get(index) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack stack = ContainerHelper.removeItem(getPage(curPage), index, count);
		if (!stack.isEmpty()) {
			this.setChanged();
		}
		return stack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack stack = getPage(curPage).get(index);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			getPage(curPage).set(index, ItemStack.EMPTY);
			return stack;
		}
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		getPage(curPage).set(index, stack);
		if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}
		this.setChanged();
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void startOpen(Player player) {
		if (!stack.isEmpty()) {
			pages.clear();
			CompoundTag nbt;
			if (stack.hasTag()) {
				nbt = stack.getTag();
			} else {
				nbt = new CompoundTag();
				stack.setTag(nbt);
			}
			CompoundTag nbtPages;
			if (nbt.contains("Pages")) {
				nbtPages = nbt.getCompound("Pages");
			} else {
				nbtPages = new CompoundTag();
				nbt.put("Pages", nbtPages);
			}
			if (nbtPages.contains("CurPage")) {
				curPage = nbtPages.getInt("CurPage");
			} else {
				curPage = 0;
				nbtPages.putInt("CurPage", 0);
			}
			ListTag pageList;
			if (nbtPages.contains("PageList")) {
				pageList = nbtPages.getList("PageList", 10);
			} else {
				pageList = new ListTag();
				pageList.add(new CompoundTag());
				nbtPages.put("PageList", pageList);
			}
			for (int i = 0; i < pageList.size(); i++) {
				CompoundTag page = pageList.getCompound(i);
				NonNullList<ItemStack> stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
				loadAllItems(page, stacks);
				pages.add(stacks);
			}
		}
	}

	public void loadAllItems(CompoundTag tag, NonNullList<ItemStack> list) {
		ListTag nbttaglist = tag.getList("Items", 10);
		for (int i = 0; i < nbttaglist.size(); ++i) {
			CompoundTag nbttagcompound = nbttaglist.getCompound(i);
			int j = nbttagcompound.getByte("Slot") & 255;
			if (j >= 0 && j < list.size()) {
				ItemStack stack = ItemStack.of(nbttagcompound);
				if (stack.isEmpty()) {
					continue;
				}
				// 原 1.12.2 自定义了 Count 的读写，以便承载超过 byte 上限的堆叠数
				stack.setCount(nbttagcompound.getInt("Count"));
				list.set(j, stack);
			}
		}
	}

	@Override
	public void stopOpen(Player player) {
		if (!stack.isEmpty()) {
			CompoundTag nbt;
			if (stack.hasTag()) {
				nbt = stack.getTag();
			} else {
				nbt = new CompoundTag();
				stack.setTag(nbt);
			}
			CompoundTag nbtPages;
			if (nbt.contains("Pages")) {
				nbtPages = nbt.getCompound("Pages");
			} else {
				nbtPages = new CompoundTag();
				nbt.put("Pages", nbtPages);
			}
			nbtPages.putInt("CurPage", curPage);
			ListTag pageList = new ListTag();
			for (NonNullList<ItemStack> stacks : pages) {
				CompoundTag page = new CompoundTag();
				saveAllItems(page, stacks, false);
				pageList.add(page);
			}
			nbtPages.put("PageList", pageList);
		}
	}

	public CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> list, boolean saveEmpty) {
		ListTag nbttaglist = new ListTag();
		for (int i = 0; i < list.size(); ++i) {
			ItemStack itemstack = list.get(i);
			if (!itemstack.isEmpty()) {
				CompoundTag nbttagcompound = new CompoundTag();
				nbttagcompound.putByte("Slot", (byte) i);
				itemstack.save(nbttagcompound);
				nbttagcompound.remove("Count");
				nbttagcompound.putInt("Count", itemstack.getCount());
				nbttaglist.add(nbttagcompound);
			}
		}
		if (!nbttaglist.isEmpty() || saveEmpty) {
			tag.put("Items", nbttaglist);
		}
		return tag;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return false;
	}

	@Override
	public void clearContent() {
		for (NonNullList<ItemStack> stacks : pages) {
			stacks.clear();
		}
		pages.clear();
	}

	/** 当前页索引，对应原 IInventory 的 getField(0)。 */
	public int getCurrentPage() {
		return curPage;
	}

	/** 设置当前页索引，对应原 IInventory 的 setField(0, value)。 */
	public void setCurrentPage(int value) {
		if (value < 0) {
			value = 0;
		} else if (value >= getMaxPage()) {
			value = getMaxPage() - 1;
		}
		if (value < 0) {
			value = 0;
		}
		if (value >= pages.size()) {
			for (int i = 0; i < value - pages.size() + 1; i++) {
				pages.add(NonNullList.withSize(getContainerSize(), ItemStack.EMPTY));
			}
		}
		curPage = value;
	}

	@Override
	public NonNullList<ItemStack> getPage(int index) {
		if (index >= 0 && index < getMaxPage()) {
			while (index >= pages.size()) {
				pages.add(NonNullList.withSize(getContainerSize(), ItemStack.EMPTY));
			}
			return pages.get(index);
		}
		return NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
	}

}
