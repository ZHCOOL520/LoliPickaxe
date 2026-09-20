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
		def = new ItemStack(ItemLoader.loliPickaxe);
		Map<Enchantment, Integer> enchMap = Maps.newHashMap();
		enchMap.put(Enchantments.BLOCK_FORTUNE, 32);
		enchMap.put(EnchantmentLoader.loliAutoFurnace, 1);
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
		return stack.getTag().getString("Owner").equals(player.getName().getString()) || stack.getTag().getString("OwnerUUID").equals(player.getUUID().toString());
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

	@Override
	public ILoliInventory getInventory(ItemStack stack) {
		return new InventoryLoliPickaxe(stack);
	}

}
