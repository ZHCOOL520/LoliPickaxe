package com.anotherstar.common.gui;

import com.anotherstar.common.item.tool.IContainer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 萝莉镐「黑名单」容器：9×9 = 81 个「禁用物品」标记槽。
 *
 * <p>对应原 1.12.2 的 {@code com.anotherstar.common.gui.ContainerBlaceListLoliPickaxe}
 * （类名中的 Blace 为原作者的拼写，这里保持不变以维持引用一致）。
 *
 * <h2>与 1.12.2 的差异</h2>
 * <ul>
 * <li>{@code IInventory} → 使用 Forge 的 {@link ItemStackHandler}（不再有独立的 IInventory 实现类）；
 * 槽位包装为 {@link SlotItemHandler}；</li>
 * <li>黑名单数据不按「页」组织（与储藏室不同，本容器没有分页与 DataSlot），
 * 而是直接以 {@code ListTag} 一次性写入物品 NBT 的 {@code Blacklist} 键；</li>
 * <li>1.12.2 使用数字 item id，1.20.1 改为 {@code ResourceLocation}（{@code "Name"} 存
 * {@code namespace:path}，{@code "Damage"} 存 damage 值）。</li>
 * </ul>
 *
 * <h2>NBT 结构（键名 {@code Blacklist}）</h2>
 * {@code Blacklist} 是一个 ListTag，每个元素是一个 CompoundTag：
 * {@code Slot}(int, 槽位索引 0..80) / {@code Name}(String, 物品注册名) / {@code Damage}(int, 损伤值)。
 * 读取在构造时（{@code :66-81}），写回在 {@link #removed(Player)} 中。
 *
 * @see MenuLoader#LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU
 */
public class ContainerBlaceListLoliPickaxe extends AbstractContainerMenu {

	/** 容器自身的 MenuType，供开放界面与客户端注册 Screen 使用。 */
	public static final MenuType<ContainerBlaceListLoliPickaxe> LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU = MenuLoader.LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU;

	/** 81 个黑名单槽位的后端存储（Forge 的 ItemStackHandler，容量固定 81）。 */
	private ItemStackHandler items = new ItemStackHandler(81);
	/** 打开该容器的玩家（服务端为 ServerPlayer），关闭时用于写回 NBT。 */
	private Player player;
	/** 触发打开容器的萝莉镐物品堆；为空表示构造失败。 */
	private ItemStack stack;
	/** 萝莉镐所在槽位索引：>=0 为主手快捷栏索引，-1 为副手，-2 为其它。 */
	private int slotIndex;

	/**
	 * 服务端构造：从萝莉镐 NBT 的 {@code Blacklist} 还原 81 个标记槽。
	 *
	 * @param id              容器窗口 id，需与服务端一致
	 * @param playerInventory 玩家背包
	 * @param heldStack       触发打开的萝莉镐物品堆；空/非 {@code IContainer} 时不添加任何槽位
	 * @param page            页索引，本容器不使用（保留参数以保持与 1.12.2 的 openGui 参数布局一致）
	 */
	public ContainerBlaceListLoliPickaxe(int id, Inventory playerInventory, ItemStack heldStack, int page) {
		super(LOLI_PICKAXE_CONTAINER_BLACKLIST_MENU, id);
		this.stack = ItemStack.EMPTY;
		Player owner = playerInventory.player;
		if (!heldStack.isEmpty() && heldStack.getItem() instanceof IContainer) {
			this.stack = heldStack;
			this.player = owner;
			if (heldStack == owner.getMainHandItem()) {
				this.slotIndex = playerInventory.selected;
			} else if (heldStack == owner.getOffhandItem()) {
				this.slotIndex = -1;
			} else {
				this.slotIndex = -2;
			}
			for (int i = 0; i < 9; i++) {
				for (int j = 0; j < 9; j++) {
					this.addSlot(new SlotItemHandler(this.items, i * 9 + j, j * 18 + 8, i * 18 + 8));
				}
			}
			for (int i = 0; i < 3; ++i) {
				for (int j = 0; j < 9; ++j) {
					this.addSlot(new Slot(playerInventory, i * 9 + j + 9, j * 18 + 8, i * 18 + 174));
				}
			}
			for (int i = 0; i < 9; ++i) {
				this.addSlot(new Slot(playerInventory, i, i * 18 + 8, 232));
			}
			CompoundTag nbt;
			if (heldStack.hasTag()) {
				nbt = heldStack.getTag();
			} else {
				nbt = new CompoundTag();
				heldStack.setTag(nbt);
			}
			if (nbt.contains("Blacklist")) {
				ListTag blackList = nbt.getList("Blacklist", 10);
				if (blackList.size() <= items.getSlots()) {
					for (int i = 0; i < blackList.size(); i++) {
						CompoundTag black = blackList.getCompound(i);
						if (black.contains("Slot") && black.contains("Name") && black.contains("Damage")) {
							// ForgeRegistries.ITEMS#getValue 未命中时返回 null（而非空气），未注册的物品名直接跳过。
							// 物品名来自存档/数据包，构造 ResourceLocation 遇非法字符会抛异常，
							// 这里捕获后跳过该条目，避免一个坏条目导致整个界面打不开。
							Item item;
							try {
								item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(black.getString("Name")));
							} catch (Exception e) {
								continue;
							}
							if (item == null) {
								continue;
							}
							ItemStack blackStack = new ItemStack(item);
							if (item != Items.AIR) {
								blackStack.setDamageValue(black.getInt("Damage"));
							}
							// 槽位索引同样来自存档，越界会抛异常，这里做范围校验
							int slot = black.getInt("Slot");
							if (slot >= 0 && slot < items.getSlots()) {
								items.setStackInSlot(slot, blackStack);
							}
						}
					}
				}
			}
		}
	}

	/** 供 {@code NetworkHooks.openScreen} 使用：额外数据为 物品（黑名单界面不需要页索引）。 */
	public ContainerBlaceListLoliPickaxe(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
		this(id, playerInventory, extraData == null ? ItemStack.EMPTY : extraData.readItem(), 0);
	}

	public ItemStack getStack() {
		return stack;
	}

	@Override
	public boolean stillValid(Player playerIn) {
		if (stack.isEmpty()) {
			return false;
		}
		// 【必须校验物品本体】原实现只比较「当前选中的快捷栏索引」（slotIndex == selected），
		// 完全没有校验那格里的物品是否还是当初打开界面的那把萝莉镐。
		// 后果：玩家打开黑名单界面后切到别的快捷栏格子（放着别的物品），界面仍被视为「有效」，
		// 关闭时会把 81 条黑名单写回 this.stack —— 若该物品已被移走/替换，就造成写入错位。
		// 这里改为与 ContainerLoliPickaxe.stillValid 一致的身份比对，语义更严格且更正确。
		if (slotIndex == -1) {
			return stack == playerIn.getOffhandItem();
		}
		return slotIndex == playerIn.getInventory().selected && stack == playerIn.getMainHandItem();
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player playerIn) {
		if (slotIndex >= 0 && slotId == 108 + slotIndex) {
			return;
		} else if (slotId >= 0 && slotId < items.getSlots()) {
			// 黑名单槽位只接受「单件标记」语义：
			//  - 光标有物品 → 把该物品（数量 1）登记为黑名单项；
			//  - 光标为空   → 清空该槽位（原实现不处理这种情况，
			//                 导致黑名单只能新增、永远无法删除）。
			// 其余点击类型（拖拽/数字键交换等）在此槽位无意义，保持忽略，
			// 以免把整堆物品灌进黑名单存储。
			if (clickTypeIn == ClickType.PICKUP) {
				ItemStack carried = getCarried();
				if (carried.isEmpty()) {
					items.setStackInSlot(slotId, ItemStack.EMPTY);
				} else {
					ItemStack marked = carried.copy();
					marked.setCount(1);
					items.setStackInSlot(slotId, marked);
				}
			}
			return;
		}
		super.clicked(slotId, dragType, clickTypeIn, playerIn);
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		if (!stack.isEmpty()) {
			CompoundTag nbt;
			if (stack.hasTag()) {
				nbt = stack.getTag();
			} else {
				nbt = new CompoundTag();
				stack.setTag(nbt);
			}
			ListTag blackList = new ListTag();
			for (int i = 0; i < items.getSlots(); i++) {
				ItemStack blackStack = items.getStackInSlot(i);
				if (!blackStack.isEmpty()) {
					CompoundTag black = new CompoundTag();
					black.putInt("Slot", i);
					ResourceLocation id = ForgeRegistries.ITEMS.getKey(blackStack.getItem());
					black.putString("Name", id == null ? "minecraft:air" : id.toString());
					black.putInt("Damage", blackStack.getDamageValue());
					blackList.add(black);
				}
			}
			nbt.put("Blacklist", blackList);
		}
	}

}
