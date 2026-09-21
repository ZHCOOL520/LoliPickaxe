package com.anotherstar.common.item.tool;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.enchantment.EnchantmentLoader;
import com.anotherstar.common.entity.IEntityLoli;
import com.anotherstar.common.gui.ILoliInventory;
import com.anotherstar.common.gui.InventoryLoliPickaxe;
import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.util.LoliPickaxeUtil;
import com.google.common.collect.Maps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ItemLoliPickaxe extends PickaxeItem implements ILoli {

	public static final Tier LOLI = new LoliTier();

	private static ItemStack def = null;

	private static void init() {
		def = new ItemStack(ItemLoader.loliPickaxe());
		Map<Enchantment, Integer> enchMap = Maps.newHashMap();
		enchMap.put(Enchantments.BLOCK_FORTUNE, 32);
		enchMap.put(EnchantmentLoader.loliAutoFurnace(), 1);
		EnchantmentHelper.setEnchantments(enchMap, def);
		ListTag list = new ListTag();
		CompoundTag element = new CompoundTag();
		element.putShort("id", (short) 16);
		element.putByte("lvl", (byte) 0);
		list.add(element);
		element = new CompoundTag();
		element.putShort("id", (short) 13);
		element.putByte("lvl", (byte) 0);
		list.add(element);
		def.getOrCreateTag().put("LoliPotion", list);
	}

	public static ItemStack getDef() {
		if (def == null) {
			init();
		}
		return def;
	}

	public ItemLoliPickaxe() {
		super(LOLI, 0, 0.0F, new Item.Properties().stacksTo(1));
	}

	@Override
	public int getDamage(ItemStack stack) {
		return 0;
	}

	@Override
	public void setDamage(ItemStack stack, int damage) {
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return 0.0F;
	}

	// 重写目标方法本身已废弃（Forge 建议改用下面已覆写的 isCorrectToolForDrops(ItemStack,BlockState)）。
	// 但直接删掉本覆写会回落到 DiggerItem 自带的挖掘等级判定（LoliTier 等级 32）而改变行为，故压制警告。
	@Override
	@SuppressWarnings("deprecation")
	public boolean isCorrectToolForDrops(BlockState state) {
		return false;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return false;
	}

	@Override
	public int getEntityLifespan(ItemStack itemStack, Level level) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isRepairable(ItemStack stack) {
		return false;
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		return leftClickEntity(player, entity);
	}

	public boolean leftClickEntity(LivingEntity loli, Entity entity) {
		if (!entity.level().isClientSide && (loli instanceof Player || loli instanceof IEntityLoli)) {
			ItemStack stack = loli.getMainHandItem();
			boolean success = false;
			if (entity instanceof Player) {
				LoliPickaxeUtil.killPlayer((Player) entity, loli);
				success = true;
			} else if (entity instanceof LivingEntity) {
				LoliPickaxeUtil.killEntityLiving((LivingEntity) entity, loli);
				success = true;
			} else if (ConfigLoader.getBoolean(stack, "loliPickaxeValidToAllEntity")) {
				LoliPickaxeUtil.killEntity(entity);
				success = true;
			}
			if (ConfigLoader.getBoolean(stack, "loliPickaxeKillFacing")) {
				LoliPickaxeUtil.killFacing(loli);
				success = true;
			}
			if (success) {
				playSuccessSound(loli);
			}
			return success;
		}
		return false;
	}

	private static void playSuccessSound(LivingEntity entity) {
		SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MODID, "lolisuccess"));
		Player player = entity instanceof Player ? (Player) entity : null;
		entity.level().playSound(player, entity.blockPosition(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide) {
			if (player.isShiftKeyDown()) {
				if (ConfigLoader.getBoolean(stack, "loliPickaxeKillRangeEntity")) {
					int range = ConfigLoader.getInt(stack, "loliPickaxeKillRange");
					int count = LoliPickaxeUtil.killRangeEntity(level, player, range);
					player.displayClientMessage(Component.translatable("loliPickaxe.killrangeentity", range * 2, count), false);
					playSuccessSound(player);
				}
			} else {
				CompoundTag nbt = stack.getTag();
				if (nbt == null) {
					nbt = new CompoundTag();
					nbt.putInt("range", 0);
					stack.setTag(nbt);
				} else if (nbt.contains("range")) {
					nbt.putInt("range", nbt.getInt("range") >= ConfigLoader.loliPickaxeMaxRange ? 0 : nbt.getInt("range") + 1);
				} else {
					nbt.putInt("range", 1);
				}
				player.displayClientMessage(Component.translatable("loliPickaxe.range", 1 + 2 * nbt.getInt("range")), false);
				playSuccessSound(player);
			}
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("已在GitHub上开源"));
		tooltip.add(Component.translatable("loliPickaxe.curRange", 1 + 2 * getRange(stack)));
		if (ConfigLoader.getBoolean(stack, "loliPickaxeMandatoryDrop")) {
			tooltip.add(Component.translatable("loliPickaxe.mandatoryDrop"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeStopOnLiquid")) {
			tooltip.add(Component.translatable("loliPickaxe.stopOnLiquid"));
		}
		double distance = ConfigLoader.getDouble(stack, "loliPickaxeBlockReachDistance");
		if (distance > 0) {
			tooltip.add(Component.translatable("loliPickaxe.blockReachDistance", distance));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeAutoAccept")) {
			tooltip.add(Component.translatable("loliPickaxe.autoAccept"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeThorns")) {
			tooltip.add(Component.translatable("loliPickaxe.thorns"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeKillRangeEntity")) {
			tooltip.add(Component.translatable("loliPickaxe.killRange", 2 * ConfigLoader.getInt(stack, "loliPickaxeKillRange")));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeAutoKillRangeEntity")) {
			tooltip.add(Component.translatable("loliPickaxe.autoKillRange", 2 * ConfigLoader.getInt(stack, "loliPickaxeAutoKillRange")));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeCompulsoryRemove")) {
			tooltip.add(Component.translatable("loliPickaxe.compulsoryRemove"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeValidToAmityEntity")) {
			tooltip.add(Component.translatable("loliPickaxe.validToAmityEntity"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeValidToAllEntity")) {
			tooltip.add(Component.translatable("loliPickaxe.validToAllEntity"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeClearInventory")) {
			tooltip.add(Component.translatable("loliPickaxe.clearInventory"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeDropItems")) {
			tooltip.add(Component.translatable("loliPickaxe.dropItems"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeKickPlayer")) {
			tooltip.add(Component.translatable("loliPickaxe.kickPlayer"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeReincarnation")) {
			tooltip.add(Component.translatable("loliPickaxe.reincarnation"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeBeyondRedemption")) {
			tooltip.add(Component.translatable("loliPickaxe.beyondRedemption"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeBlueScreenAttack")) {
			tooltip.add(Component.translatable("loliPickaxe.blueScreenAttack"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeExitAttack")) {
			tooltip.add(Component.translatable("loliPickaxe.exitAttack"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeFailRespondAttack")) {
			tooltip.add(Component.translatable("loliPickaxe.failRespondAttack"));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeKillFacing")) {
			tooltip.add(Component.translatable("loliPickaxe.killFacing", ConfigLoader.getInt(stack, "loliPickaxeKillFacingRange"), ConfigLoader.getDouble(stack, "loliPickaxeKillFacingSlope")));
		}
		if (ConfigLoader.getBoolean(stack, "loliPickaxeInfiniteBattery")) {
			tooltip.add(Component.translatable("loliPickaxe.infiniteBattery"));
		}
	}

	@Override
	public boolean onDroppedByPlayer(ItemStack stack, Player player) {
		int time = ConfigLoader.loliPickaxeDropProtectTime;
		if (time <= 0) {
			return true;
		}
		CompoundTag nbt = stack.getTag();
		if (nbt == null) {
			nbt = new CompoundTag();
			nbt.putLong("preDropTime", System.currentTimeMillis());
			stack.setTag(nbt);
			return false;
		} else {
			if (nbt.contains("preDropTime")) {
				long preDropTime = nbt.getLong("preDropTime");
				long curDropTime = System.currentTimeMillis();
				nbt.putLong("preDropTime", curDropTime);
				return curDropTime - preDropTime < time;
			} else {
				nbt.putLong("preDropTime", System.currentTimeMillis());
				return false;
			}
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (!level.isClientSide && entity instanceof Player) {
			if (!hasOwner(stack)) {
				setOwner(stack, (Player) entity);
			}
		}
	}

	public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
		if (tab == CreativeTabLoader.loliTabs) {
			output.accept(getDef());
		}
	}

	@Override
	public boolean hasOwner(ItemStack stack) {
		return stack.hasTag() && (stack.getTag().contains("Owner") || stack.getTag().contains("OwnerUUID"));
	}

	@Override
	public boolean isOwner(ItemStack stack, Player player) {
		// getTag() 在物品无 NBT 时为 null；原实现直接解引用会抛 NPE。
		// 调用链 checkOwner → isOwner 会在「物品尚未被 inventoryTick 写入 Owner」的瞬间命中，
		// 属于高频路径，因此这里必须做空值保护。
		CompoundTag nbt = stack.getTag();
		if (nbt == null) {
			return false;
		}
		return nbt.getString("Owner").equals(player.getName().getString()) || nbt.getString("OwnerUUID").equals(player.getUUID().toString());
	}

	public void setOwner(ItemStack stack, Player player) {
		stack.getOrCreateTag().putString("Owner", player.getName().getString());
		stack.getOrCreateTag().putString("OwnerUUID", player.getUUID().toString());
	}

	@Override
	public int getRange(ItemStack stack) {
		int range = 1;
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains("range")) {
			range = nbt.getInt("range");
		}
		return range;
	}

	@Override
	public boolean hasInventory(ItemStack stack) {
		return true;
	}

	/**
	 * 每个萝莉镐物品堆对应<b>唯一</b>的容器实例缓存。
	 *
	 * <p><b>为什么必须缓存</b>：{@link InventoryLoliBase} 是「{@code startOpen} 读入全部页 →
	 * 内存中修改 → {@code stopOpen} 写回全部页」的模型。若每次 {@link #getInventory(ItemStack)}
	 * 都返回新实例，就会出现多个实例各自持有同一物品 NBT 的一份内存副本：
	 * 自动收纳（掉落物收集）与已打开的分页储藏室界面并发时，
	 * <b>后调用 {@code stopOpen} 的那个实例会把另一个实例的改动整份覆盖掉</b>，造成物品凭空消失。
	 *
	 * <p><b>为什么这样改不改变逻辑</b>：容器对外的读写语义、页的组织方式、落盘格式全部不变，
	 * 只是把「同一物品的多个内存视图」收敛为「同一物品只有一个权威视图」。
	 * 单线程串行场景（只有 GUI、或只有自动收纳）的表现与改动前完全一致。
	 *
	 * <p><b>为什么用弱引用 + 身份比较</b>：
	 * <ul>
	 *   <li>键必须是「物品堆对象的身份」而非内容相等 —— {@code ItemStack} 未重写
	 *       {@code equals/hashCode}，默认即身份语义，符合我们的需要；</li>
	 *   <li>用 {@link WeakHashMap} 保存：物品堆一旦不再被任何地方引用（被销毁/丢弃/替换），
	 *       其缓存条目会被 GC 自动回收，避免长时间运行下缓存无界增长；</li>
	 *   <li>值持有该物品堆的引用，因此额外用 {@link java.lang.ref.WeakReference} 包装值，
	 *       避免「值 → 物品堆 → 键」形成强引用环导致 WeakHashMap 永不回收。</li>
	 * </ul>
	 */
	private static final Map<ItemStack, java.lang.ref.WeakReference<InventoryLoliPickaxe>> INVENTORY_CACHE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	@Override
	public ILoliInventory getInventory(ItemStack stack) {
		if (stack.isEmpty()) {
			return new InventoryLoliPickaxe(stack);
		}
		synchronized (INVENTORY_CACHE) {
			java.lang.ref.WeakReference<InventoryLoliPickaxe> ref = INVENTORY_CACHE.get(stack);
			InventoryLoliPickaxe cached = ref == null ? null : ref.get();
			if (cached == null) {
				cached = new InventoryLoliPickaxe(stack);
				INVENTORY_CACHE.put(stack, new java.lang.ref.WeakReference<>(cached));
			}
			return cached;
		}
	}

	/**
	 * 清理某个萝莉镐物品堆的容器缓存。
	 *
	 * <p>物品被销毁、消耗或玩家退出时可调用，立即释放对应缓存（弱引用本身也会自动回收，
	 * 这里是「确定性释放」的可选加速）。不影响任何容器数据本身。
	 *
	 * @param stack 目标物品堆
	 */
	public static void clearInventoryCache(ItemStack stack) {
		synchronized (INVENTORY_CACHE) {
			INVENTORY_CACHE.remove(stack);
		}
	}

	/**
	 * 清空全部容器缓存（例如服务器停止、存档切换时）。
	 */
	public static void clearAllInventoryCaches() {
		synchronized (INVENTORY_CACHE) {
			INVENTORY_CACHE.clear();
		}
	}

}
