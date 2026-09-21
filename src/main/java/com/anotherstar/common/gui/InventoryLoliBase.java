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

	/**
	 * 脏页标记：{@code dirtyPages.get(i) == true} 表示第 i 页自上次读入/落盘后被修改过。
	 *
	 * <p><b>为什么需要它</b>：{@link #stopOpen(Player)} 原本无条件重建<b>整份</b> {@code PageList}
	 * （最多 100 页 × 81 槽）。在大型整合包里，物品 NBT 会随每次开关界面反复序列化，
	 * 既浪费 CPU 又撑大物品数据（存在触及网络包体积上限的风险）。
	 *
	 * <p><b>为什么这样改不改变逻辑</b>：落盘的 NBT <b>结构、键名、数值完全不变</b>，
	 * 只是「没被动过的页」沿用其原始 NBT 内容而不再重新序列化。
	 * 对玩家而言，页内容、翻页、重启后数据三者都与改动前完全一致。
	 */
	private final List<Boolean> dirtyPages = Lists.newArrayList();

	/**
	 * 原始页 NBT 缓存：与 {@link #pages} 同索引，保存 {@link #startOpen(Player)} 时读入的原始页标签。
	 *
	 * <p>未变脏的页在 {@link #stopOpen(Player)} 时直接复用这里的对象，避免重新序列化；
	 * 这样既保持落盘内容等价，又省去重复构建 {@code Items} 列表的开销。
	 */
	private final List<CompoundTag> rawPages = Lists.newArrayList();

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
		if (index < 0 || index >= getContainerSize()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = ContainerHelper.removeItem(getPage(curPage), index, count);
		if (!stack.isEmpty()) {
			this.setChanged();
		}
		return stack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		// 越界保护：整理/自动化类模组可能以异常索引直接调用 Container API（不经过原版点击校验），
		// 未保护时 NonNullList.get/set 会抛 IndexOutOfBoundsException 并中断服务端 tick。
		if (index < 0 || index >= getContainerSize()) {
			return ItemStack.EMPTY;
		}
		NonNullList<ItemStack> page = getPage(curPage);
		ItemStack stack = page.get(index);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			page.set(index, ItemStack.EMPTY);
			this.setChanged();
			return stack;
		}
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		// 越界保护，理由同 removeItemNoUpdate
		if (index < 0 || index >= getContainerSize()) {
			return;
		}
		NonNullList<ItemStack> page = getPage(curPage);
		// 注意：必须先收敛数量再写入，且要作用于「真正被保存的堆」，
		// 原实现先 set 再修改传入的 stack，导致存档里仍是超大数量、而调用方拿到被改过的堆（数据不一致）。
		ItemStack stored = stack;
		if (!stored.isEmpty() && stored.getCount() > this.getMaxStackSize()) {
			stored = stored.copy();
			stored.setCount(this.getMaxStackSize());
		}
		page.set(index, stored);
		this.setChanged();
	}

	@Override
	public void setChanged() {
		// 【必须真正生效】原实现是空方法，导致「非 GUI 路径的写入」只要没走到 stopOpen 就永久丢失。
		// 这里标记当前页为脏，使其在 stopOpen/落盘时被重写。
		// 语义上与「任何写操作都要能被持久化」一致，不改变玩家可感知的行为。
		markDirty(curPage);
	}

	/**
	 * 把指定页标记为「已修改」，供落盘时决定是否需要重新序列化。
	 *
	 * @param page 页索引；越界时忽略
	 */
	protected void markDirty(int page) {
		if (page < 0) {
			return;
		}
		while (dirtyPages.size() <= page) {
			dirtyPages.add(Boolean.TRUE);
		}
		dirtyPages.set(page, Boolean.TRUE);
	}

	/**
	 * 判断某页是否已修改。
	 *
	 * @param page 页索引
	 * @return true 表示该页自读入后被修改过
	 */
	protected boolean isDirty(int page) {
		return page >= 0 && page < dirtyPages.size() && Boolean.TRUE.equals(dirtyPages.get(page));
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void startOpen(Player player) {
		if (!stack.isEmpty()) {
			pages.clear();
			rawPages.clear();
			dirtyPages.clear();
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
				// 记录原始页标签并按「未改动」登记：
				// 未变脏的页在 stopOpen 时原样复用，避免重复序列化（内容与重新序列化完全等价）。
				rawPages.add(page.copy());
				dirtyPages.add(Boolean.FALSE);
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
				int count = nbttagcompound.getInt("Count");
				// 越界/损坏 NBT 保护：手改或被破坏的存档可能带来负数或超大数量，
				// 一旦流入容器会污染客户端同步与其它模组对堆叠数的假设，故收敛到 [1, 上限]。
				int limit = this.getMaxStackSize();
				if (count < 1) {
					count = 1;
				} else if (limit > 0 && count > limit) {
					count = limit;
				}
				stack.setCount(count);
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
			for (int i = 0; i < pages.size(); i++) {
				// 未修改过的页直接复用读入时的原始标签：落盘内容与「重新序列化」完全等价
				// （因为该页的内存状态就是从这份标签读出来的，期间没有任何写入），
				// 但省去了重新构建 Items 列表的开销。这是纯粹的「少做无用功」，
				// 不改变任何可观测行为。
				if (!isDirty(i) && i < rawPages.size()) {
					pageList.add(rawPages.get(i).copy());
					continue;
				}
				CompoundTag page = new CompoundTag();
				saveAllItems(page, pages.get(i), false);
				pageList.add(page);
			}
			nbtPages.put("PageList", pageList);
			// 落盘后全部页回到「干净」状态，与读入时一致
			for (int i = 0; i < dirtyPages.size(); i++) {
				dirtyPages.set(i, Boolean.FALSE);
			}
			for (int i = 0; i < pages.size(); i++) {
				if (i < rawPages.size()) {
					rawPages.set(i, snapshotPage(pages.get(i)));
				} else {
					rawPages.add(snapshotPage(pages.get(i)));
				}
			}
		}
	}

	/**
	 * 把某一页的当前内容序列化为标签，用于刷新 {@link #rawPages} 缓存。
	 *
	 * @param page 页内容
	 * @return 该页的 NBT 快照
	 */
	private CompoundTag snapshotPage(NonNullList<ItemStack> page) {
		CompoundTag tag = new CompoundTag();
		saveAllItems(tag, page, false);
		return tag;
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

	/**
	 * {@inheritDoc}
	 *
	 * <p>与直接操作 {@link #getPage(int)} 返回的列表相比，本方法的唯一区别是
	 * <b>每一处写入都经过 {@link #setItem(int, ItemStack)} 并因此调用 {@link #setChanged()}</b>，
	 * 从而把对应页标记为脏页、保证 {@code stopOpen} 时真正落盘。
	 * 堆叠判定（{@code isSameItemSameTags}）、上限取值（{@code cancelStackLimit}）、
	 * 以及「先填已有堆、再占空槽」的填充顺序都与原先的自动收纳逻辑保持一致。
	 */
	@Override
	public ItemStack insertItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		// 复制一份用于计算剩余量；不改动调用方传入的物品堆
		ItemStack remaining = stack.copy();
		int slotCount = this.getContainerSize();
		int maxStackSize = this.getMaxStackSize();
		boolean unlimited = this.cancelStackLimit();

		for (int page = 0; page < this.getMaxPage() && !remaining.isEmpty(); page++) {
			NonNullList<ItemStack> stacks = this.getPage(page);
			// 第一遍：并入同类型的已有堆
			for (int slot = 0; slot < slotCount && !remaining.isEmpty(); slot++) {
				ItemStack existing = stacks.get(slot);
				if (existing.isEmpty()) {
					continue;
				}
				if (!ItemStack.isSameItem(existing, remaining) || !ItemStack.isSameItemSameTags(existing, remaining)) {
					continue;
				}
				int perSlotMax = unlimited ? maxStackSize : Math.min(maxStackSize, existing.getMaxStackSize());
				int room = perSlotMax - existing.getCount();
				if (room <= 0) {
					continue;
				}
				int moved = Math.min(room, remaining.getCount());
				if (moved <= 0) {
					continue;
				}
				// 复制后写入，避免把容器内部的堆共享给调用方
				ItemStack merged = existing.copy();
				merged.setCount(existing.getCount() + moved);
				this.setItemForPage(page, slot, merged);
				remaining.shrink(moved);
			}
			// 第二遍：占用空槽
			for (int slot = 0; slot < slotCount && !remaining.isEmpty(); slot++) {
				if (!stacks.get(slot).isEmpty()) {
					continue;
				}
				int perSlotMax = unlimited ? maxStackSize : Math.min(maxStackSize, remaining.getMaxStackSize());
				int moved = Math.min(perSlotMax, remaining.getCount());
				if (moved <= 0) {
					continue;
				}
				ItemStack placed = remaining.copy();
				placed.setCount(moved);
				this.setItemForPage(page, slot, placed);
				remaining.shrink(moved);
			}
		}
		return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
	}

	/**
	 * 把物品写入指定页的指定槽位，并正确标记该页为脏页。
	 *
	 * <p>{@link #setItem(int, ItemStack)} 只能作用于<b>当前页</b>，而自动收纳需要跨页填充，
	 * 因此这里直接按页写入；除「目标页由参数指定」外，数量收敛规则与 {@code setItem} 完全一致。
	 *
	 * @param page  目标页索引
	 * @param index 页内槽位索引
	 * @param stack 要写入的物品堆
	 */
	private void setItemForPage(int page, int index, ItemStack stack) {
		NonNullList<ItemStack> stacks = this.getPage(page);
		if (index < 0 || index >= stacks.size()) {
			return;
		}
		ItemStack stored = stack;
		if (!stored.isEmpty() && stored.getCount() > this.getMaxStackSize()) {
			stored = stored.copy();
			stored.setCount(this.getMaxStackSize());
		}
		stacks.set(index, stored);
		// 【关键】必须标记脏页，否则 stopOpen 会用读入时的旧快照覆盖掉本次写入
		this.markDirty(page);
	}

	@Override
	public void clearContent() {
		for (NonNullList<ItemStack> stacks : pages) {
			stacks.clear();
		}
		pages.clear();
		// 同步清空页跟踪状态，避免 rawPages/dirtyPages 与实际页数脱节
		rawPages.clear();
		dirtyPages.clear();
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
				// 新建的页必须登记为脏页，否则它不会被写入存档（新页通常为空，
				// 但页的存在本身需要落到 PageList 中，翻页位置才稳定）。
				rawPages.add(new CompoundTag());
				dirtyPages.add(Boolean.TRUE);
			}
		}
		curPage = value;
	}

	@Override
	public NonNullList<ItemStack> getPage(int index) {
		if (index >= 0 && index < getMaxPage()) {
			while (index >= pages.size()) {
				pages.add(NonNullList.withSize(getContainerSize(), ItemStack.EMPTY));
				rawPages.add(new CompoundTag());
				dirtyPages.add(Boolean.TRUE);
			}
			return pages.get(index);
		}
		// 【不要返回临时列表】原实现此处返回一个新的 NonNullList，任何写入都会落到这个
		// 立即被回收的临时对象上并静默丢失（同时 setChanged 又是空实现，永远不会落盘），
		// 属于「看起来成功、实际丢物品」的隐蔽数据丢失。
		// 越界页索引统一收敛到当前页，保证读写始终作用于真实存储。
		return getPage(curPage);
	}

}
