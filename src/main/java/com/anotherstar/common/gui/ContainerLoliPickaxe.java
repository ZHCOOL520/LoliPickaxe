package com.anotherstar.common.gui;

import com.anotherstar.common.item.tool.IContainer;
import com.anotherstar.network.LoliSlotChangePacket;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 萝莉镐「储藏室」容器。
 *
 * <p>对应原 1.12.2 的 {@code com.anotherstar.common.gui.ContainerLoliPickaxe}（继承 {@code Container}）。
 * 1.20.1 的关键 API 差异：
 * <ul>
 * <li>{@code IInventory} → {@code Container}；本模组自定义的 {@code ILoliInventory} 在 1.20.1 中直接
 * 继承 {@code Container}，因此这里可以直接把它当作 {@code Container} 使用；</li>
 * <li>{@code Container#addSlotToContainer} → {@code AbstractContainerMenu#addSlot}；</li>
 * <li>{@code IInventory#getField/setField} 被移除，分页索引改用 {@code DataSlot} 同步；</li>
 * <li>{@code onContainerClosed} → {@code removed(Player)}；{@code transferStackInSlot} → {@code quickMoveStack}。</li>
 * </ul>
 *
 * <h2>分页机制（储藏室）</h2>
 * 该容器固定 9×9 = 81 个槽位，但物品实际数据由 {@link InventoryLoliBase} 按「页」组织成
 * {@code List<NonNullList<ItemStack>>}。当前页索引 {@code curPage} 保存在物品 NBT 的
 * {@code Pages.CurPage} 中，并通过一个匿名 {@link DataSlot}（索引 0）在服务端与客户端之间双向同步：
 * 服务端 {@code get()} 读取 {@code loliStorage.getCurrentPage()}，客户端 {@code set(value)} 写回本地
 * {@code loliStorage.setCurrentPage(value)}。翻页时 {@link #updateSlot()} 对 81 个槽位逐个
 * {@code setChanged()}，让客户端按新页重新读取 {@code Container#getItem}（其内部按 {@code curPage} 取页）。
 *
 * <h2>超大堆叠同步</h2>
 * 原版 {@code ContainerSetSlotPacket} 用 byte 传输堆叠数（上限 127），而储藏室允许超过 64 甚至更大的堆叠。
 * 因此 {@link #broadcastChanges()} 在原版同步之后，额外比较 {@link #lastSlots} 与当前槽位，
 * 对发生变化的储藏室槽位补发一个 {@link LoliSlotChangePacket}（int 数量）来修正客户端的显示数量；
 * 玩家背包槽位受原版 64 限制，无需补发。
 *
 * @see MenuLoader#LOLI_PICKAXE_MENU
 * @see InventoryLoliBase
 */
public class ContainerLoliPickaxe extends AbstractContainerMenu {

	/** 容器自身的 MenuType，供开放界面与客户端注册 Screen 使用。 */
	public static final MenuType<ContainerLoliPickaxe> LOLI_PICKAXE_MENU = MenuLoader.LOLI_PICKAXE_MENU;

	/** 萝莉镐的物品容器（实际类型为 {@link InventoryLoliBase} 子类）；物品不含容器时保持为 null。 */
	public ILoliInventory inventory;
	/** {@link #inventory} 的强类型视图，便于访问分页相关方法；非 InventoryLoliBase 时为 null。 */
	private InventoryLoliBase loliStorage;
	/** 打开该容器的玩家（服务端为 ServerPlayer），用于回写 NBT 与补发同步包。 */
	private Player player;
	/** 触发打开容器的萝莉镐物品堆；为空表示构造失败，此时容器不添加任何槽位。 */
	private ItemStack stack;
	/** 萝莉镐所在槽位索引：>=0 为主手快捷栏索引，-1 为副手，-2 为其它情况（如被 GUI 打开时不在手中）。 */
	private int slotIndex;
	/** 上一次同步给客户端的槽位内容，用于补发原版同步包无法承载的超大堆叠数。 */
	private NonNullList<ItemStack> lastSlots;

	/**
	 * 服务端构造：由 {@code NetworkHooks.openScreen} 触发的标准构造路径。
	 *
	 * @param id              容器窗口 id（{@code AbstractContainerMenu.containerId}），由服务端分配，客户端需与之匹配
	 * @param playerInventory 玩家背包，用于定位持有者与添加 27+9 个背包槽位
	 * @param heldStack       触发打开的萝莉镐物品堆；{@link ItemStack#EMPTY} 或非 {@code IContainer} 时不添加任何槽位
	 * @param page            初始页索引（0 起）；由 {@code InventoryLoliBase#setCurrentPage} 做范围收敛
	 */
	public ContainerLoliPickaxe(int id, Inventory playerInventory, ItemStack heldStack, int page) {
		super(LOLI_PICKAXE_MENU, id);
		this.stack = ItemStack.EMPTY;
		Player owner = playerInventory.player;
		if (!heldStack.isEmpty() && heldStack.getItem() instanceof IContainer) {
			// IContainer 是「物品自带容器」的接口：hasInventory 判断该物品实例是否真的带容器数据
			IContainer containerItem = (IContainer) heldStack.getItem();
			if (containerItem.hasInventory(heldStack)) {
				this.stack = heldStack;
				this.inventory = containerItem.getInventory(heldStack);
				this.player = owner;
				if (this.inventory instanceof InventoryLoliBase) {
					this.loliStorage = (InventoryLoliBase) this.inventory;
				}
				if (this.loliStorage != null) {
					// 原 openInventory(player)，从物品 NBT 载入各页内容
					this.loliStorage.startOpen(owner);
					this.loliStorage.setCurrentPage(page);
				}
				if (heldStack == owner.getMainHandItem()) {
					this.slotIndex = playerInventory.selected;
				} else if (heldStack == owner.getOffhandItem()) {
					this.slotIndex = -1;
				} else {
					this.slotIndex = -2;
				}
				// 槽位索引 0..80：储藏室 9×9 网格，槽号 = 行 * 9 + 列；GUI 内坐标按 18px 网格排布，+8 为边框留白
				for (int i = 0; i < 9; i++) {
					for (int j = 0; j < 9; j++) {
						this.addSlot(new LoliSlot(this.inventory, i * 9 + j, j * 18 + 8, i * 18 + 8));
					}
				}
				// 槽位索引 81..107：玩家背包主区 3×9（背包槽 9..35），y 偏移 174 以避开储藏室网格
				for (int i = 0; i < 3; ++i) {
					for (int j = 0; j < 9; ++j) {
						this.addSlot(new Slot(playerInventory, i * 9 + j + 9, j * 18 + 8, i * 18 + 174));
					}
				}
				// 槽位索引 108..116：玩家快捷栏（背包槽 0..8），因此 108 + hotbarIndex 可定位快捷栏槽位
				for (int i = 0; i < 9; ++i) {
					this.addSlot(new Slot(playerInventory, i, i * 18 + 8, 232));
				}
				// DataSlot 0：当前页索引的双向同步通道（服务端 get → 客户端 set），等价原 IInventory#getField(0)/setField(0)
				this.addDataSlot(new DataSlot() {

					@Override
					public int get() {
						return ContainerLoliPickaxe.this.loliStorage == null ? 0 : ContainerLoliPickaxe.this.loliStorage.getCurrentPage();
					}

					@Override
					public void set(int value) {
						if (ContainerLoliPickaxe.this.loliStorage != null) {
							ContainerLoliPickaxe.this.loliStorage.setCurrentPage(value);
						}
					}

				});
			}
		}
		this.lastSlots = NonNullList.withSize(this.slots.size(), ItemStack.EMPTY);
	}

	/**
	 * 供 {@code NetworkHooks.openScreen} 使用：额外数据为 物品 + 页索引。
	 *
	 * <p>1.12.2 的 {@code player.openGui(..., x, y, z)} 把「手持物品 / 手别 / 页索引」编码在
	 * x/y/z 三个 int 中；1.20.1 改为在 {@link FriendlyByteBuf} 里按写入顺序读取 ItemStack 与 VarInt。
	 *
	 * @param id              容器窗口 id，需与服务端分配的一致
	 * @param playerInventory 玩家背包
	 * @param extraData       服务端写入的附加数据；为 null 时按「空物品 + 第 0 页」处理，此时容器无槽位
	 */
	public ContainerLoliPickaxe(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
		this(id, playerInventory, extraData == null ? ItemStack.EMPTY : extraData.readItem(), extraData != null && extraData.isReadable() ? extraData.readVarInt() : 0);
	}

	/** @return 萝莉镐的物品容器；物品不含容器时为 null。 */
	public ILoliInventory getInventory() {
		return inventory;
	}

	/** @return 触发打开容器的萝莉镐物品堆；构造失败时为 {@link ItemStack#EMPTY}。 */
	public ItemStack getStack() {
		return stack;
	}

	/** 当前所在页（0 起），对应原 IInventory#getField(0)。 */
	public int getCurrentPage() {
		return loliStorage == null ? 0 : loliStorage.getCurrentPage();
	}

	/** @return 该萝莉镐允许的最大页数（来自配置/物品，由 {@code ILoliInventory#getMaxPage()} 提供）；容器不可用时为 0。 */
	public int getMaxPage() {
		return inventory == null ? 0 : inventory.getMaxPage();
	}

	/**
	 * 判断容器是否仍然有效：萝莉镐必须仍在原来的手别/快捷栏位上，否则服务端会自动关闭界面。
	 *
	 * @param playerIn 正在查看该容器的玩家
	 * @return true 表示仍可继续使用；萝莉镐被丢弃/移动到手别之外时返回 false
	 */
	@Override
	public boolean stillValid(Player playerIn) {
		if (stack.isEmpty()) {
			return false;
		}
		if (slotIndex == -1) {
			return stack == playerIn.getOffhandItem();
		}
		return slotIndex == playerIn.getInventory().selected && stack == playerIn.getMainHandItem();
	}

	/**
	 * 处理槽位点击。这里只做一层保护：禁止把萝莉镐自身放进自己的快捷栏槽位或通过数字键交换，
	 * 否则物品与其容器会形成自引用。
	 *
	 * @param slotId      被点击的槽位索引，-1 表示点击 GUI 外部（原版语义）
	 * @param dragType    鼠标按键（0 左键 / 1 右键）或热键交换时的快捷栏索引
	 * @param clickTypeIn 点击类型，{@code ClickType.SWAP} 表示数字键交换
	 * @param playerIn    操作玩家
	 */
	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player playerIn) {
		// 不允许把萝莉镐自身塞进自己的快捷栏槽位
		if (slotIndex >= 0 && (slotId == 108 + slotIndex || clickTypeIn == ClickType.SWAP && dragType == slotIndex)) {
			return;
		}
		super.clicked(slotId, dragType, clickTypeIn, playerIn);
		// 【跨容器边界收敛】储藏室允许 20 亿堆叠，但玩家背包槽位没有同样的上限覆写。
		// 原版 doClick 的数字键交换（SWAP）、创造模式中键复制（CLONE）与拖拽（QUICK_CRAFT）
		// 都使用【目标槽位】的 getMaxStackSize，因此会把超大堆叠直接搬进玩家背包，
		// 进而被写入 playerdata，破坏整理类模组/AE2 等对堆叠数 ≤64 的假设。
		// 这里在原版逻辑执行「之后」统一清理：凡落在普通槽位上的超大堆叠，把超出部分退回储藏室。
		// 该处理对储藏室内部的操作完全无影响，超大容量得以完整保留。
		clampOverflowOutOfStorage(playerIn);
	}

	/**
	 * 把因原版点击逻辑而「泄漏」到普通槽位上的超大堆叠收敛回储藏室。
	 *
	 * <p>只处理普通槽位（索引 ≥ {@code inventory.getContainerSize()}）中数量超过该物品原版上限的情况：
	 * 保留原版上限的数量在普通槽位，其余部分尽力塞回储藏室；塞不下的部分丢弃到地面，
	 * 以避免物品凭空消失，也避免异常堆叠数进入 playerdata。
	 *
	 * @param playerIn 操作玩家（仅服务端需要真正处理）
	 */
	private void clampOverflowOutOfStorage(Player playerIn) {
		if (inventory == null || playerIn.level().isClientSide) {
			return;
		}
		int storageSize = inventory.getContainerSize();
		for (int i = storageSize; i < this.slots.size(); i++) {
			Slot slot = this.slots.get(i);
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			int vanillaLimit = stack.getMaxStackSize();
			if (vanillaLimit <= 0 || stack.getCount() <= vanillaLimit) {
				continue;
			}
			int overflow = stack.getCount() - vanillaLimit;
			ItemStack kept = stack.copy();
			kept.setCount(vanillaLimit);
			slot.set(kept);
			// 超出部分优先塞回储藏室，塞不下的丢到玩家脚下（不凭空消失）
			ItemStack toReturn = stack.copy();
			toReturn.setCount(overflow);
			ItemStack remainder = insertIntoStorage(toReturn);
			if (!remainder.isEmpty()) {
				playerIn.drop(remainder, false);
			}
		}
	}

	/**
	 * 把物品尽量放入储藏室各槽位，返回放不下的剩余部分。
	 *
	 * @param stack 待放入的物品堆（不会被修改）
	 * @return 未能放入的剩余部分；全部放入时返回 {@link ItemStack#EMPTY}
	 */
	private ItemStack insertIntoStorage(ItemStack stack) {
		ItemStack remaining = stack.copy();
		int storageSize = inventory.getContainerSize();
		// 先尝试合并到同类型且未满的槽位，再尝试放入空槽位
		for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
			for (int i = 0; i < storageSize && !remaining.isEmpty(); i++) {
				ItemStack existing = inventory.getItem(i);
				if (pass == 0) {
					if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remaining)) {
						continue;
					}
				} else if (!existing.isEmpty()) {
					continue;
				}
				int limit = inventory.getMaxStackSize();
				int space = limit - (pass == 0 ? existing.getCount() : 0);
				if (space <= 0) {
					continue;
				}
				int move = Math.min(space, remaining.getCount());
				if (pass == 0) {
					ItemStack merged = existing.copy();
					merged.setCount(existing.getCount() + move);
					inventory.setItem(i, merged);
				} else {
					ItemStack placed = remaining.copy();
					placed.setCount(move);
					inventory.setItem(i, placed);
				}
				remaining.shrink(move);
			}
		}
		return remaining;
	}

	/**
	 * Shift + 点击的快速搬运。
	 *
	 * <p>搬运规则（与原 1.12.2 基本一致）：
	 * <ul>
	 * <li>槽位索引 &lt; 81（来自储藏室）→ 搬到玩家背包区间 [81, slots.size())；</li>
	 * <li>槽位索引 ≥ 81（来自背包/快捷栏）→ 搬到储藏室区间 [0, 81)。</li>
	 * </ul>
	 * 这正是「双向对搬」的典型写法：{@code targetStart}/{@code targetEnd} 用
	 * {@code inventory.getContainerSize()}（=81）而不是硬编码常量，以便容器尺寸变化时仍然成立。
	 *
	 * @param playerIn 执行操作的玩家
	 * @param index    被 Shift 点击的槽位索引
	 * @return 被搬运走的物品副本（用于拖拽预览）；搬运失败或槽位为空时返回 {@link ItemStack#EMPTY}
	 */
	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack stackResult = ItemStack.EMPTY;
		// 越界保护：整理/转移类模组（Inventory Profiles、AE2 快捷移动等）可能以异常索引调用本方法，
		// 原版直接 slots.get(index) 会抛出 IndexOutOfBoundsException 并导致服务端崩溃。
		if (index < 0 || index >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			stackResult = stack.copy();
			if (index < inventory.getContainerSize()) {
				// 储藏室 → 玩家背包（反向合并到已有同类堆）
				if (!this.moveItemStackTo(stack, inventory.getContainerSize(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, inventory.getContainerSize(), false)) {
				// 玩家背包 → 储藏室（正向填充空槽，避免与原槽位自合并）
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return stackResult;
	}

	/**
	 * 界面关闭回调（1.12.2 的 {@code onContainerClosed}）。把内存中的分页数据回写到物品 NBT。
	 *
	 * @param playerIn 关闭界面的玩家
	 */
	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		if (inventory != null) {
			// 原 closeInventory(player)，回写各页内容
			inventory.stopOpen(playerIn);
		}
	}

	/**
	 * 原版同步包对堆叠数有上限（byte），而萝莉镐储藏室允许极大的堆叠数，
	 * 因此在原版同步之外额外用 {@link LoliSlotChangePacket} 补发完整数量。
	 *
	 * <p>调用时机：服务端每个 tick 都会调用本方法（玩家打开界面期间），
	 * 只在服务端执行、且仅对储藏室区间 [0, 81) 的槽位生效。
	 */
	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (!(player instanceof ServerPlayer) || inventory == null) {
			return;
		}
		ServerPlayer serverPlayer = (ServerPlayer) player;
		// 玩家背包槽位受原版 64 限制，无需补发；只有储藏室槽位可能超过原版同步包的上限。
		// 注意：lastSlots 必须与 slots 等长。构造失败（物品不带容器）时 slots 仍会添加 36 个玩家背包槽位，
		// 而 inventory 为 null（此时上面已提前 return）；但构造成功时 slots.size() = 81 + 36 = 117，
		// 若仅用 inventory.getContainerSize()（81）作上界虽不越界，仍需三者取最小以保证 lastSlots.get(i) 安全。
		int size = Math.min(Math.min(inventory.getContainerSize(), this.slots.size()), this.lastSlots.size());
		for (int i = 0; i < size; ++i) {
			ItemStack slotStack = this.slots.get(i).getItem();
			ItemStack lastStack = this.lastSlots.get(i);
			// 只在内容真正变化时发包，避免每 tick 都产生网络流量
			if (!ItemStack.matches(lastStack, slotStack)) {
				ItemStack sent = slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy();
				this.lastSlots.set(i, sent);
				NetworkHandler.sendToPlayer(new LoliSlotChangePacket(this.containerId, i, sent), serverPlayer);
			}
		}
	}

	/** 上一页（GUI 的 "<" 按钮调用；同时会被客户端本地切换，再发 {@code LoliPickaxeContainerPackte} 通知服务端）。 */
	public void prePage() {
		setPage(getCurrentPage() - 1);
	}

	/** 下一页（GUI 的 ">" 按钮调用）。 */
	public void nextPage() {
		setPage(getCurrentPage() + 1);
	}

	/**
	 * 切换当前页并刷新槽位。
	 *
	 * @param page 目标页索引；超出范围时由 {@code InventoryLoliBase#setCurrentPage} 收敛到 [0, getMaxPage()-1]
	 */
	private void setPage(int page) {
		if (loliStorage == null) {
			return;
		}
		loliStorage.setCurrentPage(page);
		updateSlot();
	}

	/** 标记 81 个储藏室槽位为已变更，促使客户端用新页的数据重绘（{@code Slot#setChanged} 会触发容器同步）。 */
	private void updateSlot() {
		// 上界取「容器声明的槽位数」与「实际已添加的槽位数」的较小者：
		// 构造失败（物品不含容器 → inventory 为 null）或槽位添加异常时，
		// slots 可能比 getContainerSize() 短，直接 get(i) 会抛 IndexOutOfBoundsException。
		// broadcastChanges() 中已采用同样的多重取小防护，此处保持一致。
		int size = Math.min(inventory.getContainerSize(), this.slots.size());
		for (int i = 0; i < size; i++) {
			this.slots.get(i).setChanged();
		}
	}

	/**
	 * 储藏室槽位：取消原版 64 的上限，改由 ILoliInventory#cancelStackLimit / getMaxStackSize 决定，
	 * 这样原版 AbstractContainerMenu 的点击 / 拖拽 / 快速移动逻辑即可直接支持超大堆叠。
	 *
	 * <p>内部类而非静态类：需要读取外层 {@code inventory} 的取消上限配置。
	 */
	public class LoliSlot extends Slot {

		/**
		 * @param container 槽位所属容器（本类中恒为外层 {@code inventory}）
		 * @param index     容器内的槽位索引（0..80）
		 * @param x         GUI 内 x 坐标（像素，相对 GUI 左上角）
		 * @param y         GUI 内 y 坐标（像素，相对 GUI 左上角）
		 */
		public LoliSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		/** @return 容器声明的最大堆叠数（萝莉镐为配置项 {@code loliPickaxeSlotStackLimit}）。 */
		@Override
		public int getMaxStackSize() {
			return container.getMaxStackSize();
		}

		/**
		 * @param stack 待判定上限的物品堆（未使用，仅当未取消上限时透传给原版）
		 * @return 取消上限时返回 {@code inventory.getMaxStackSize()}，否则返回原版的按物品计算结果
		 */
		@Override
		public int getMaxStackSize(ItemStack stack) {
			if (inventory.cancelStackLimit()) {
				return inventory.getMaxStackSize();
			}
			return super.getMaxStackSize(stack);
		}

	}

}
