package com.anotherstar.common.item.tool;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.gui.ILoliInventory;
import com.anotherstar.common.gui.InventorySmallLoliPickaxe;
import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemSmallLoliPickaxe extends Item implements IContainer {

	public static Map<ItemLoliPickaxeMaterial, String> nbtMap = Maps.newHashMap();
	public static boolean isharvesting = false;
	public static boolean autoFurnace = false;
	public static int fortuneLevel = 0;
	public static float exp = 0;
	public static ILoliInventory inventory = null;
	public static NonNullList<ItemStack> blacklist = null;
	private static ItemStack full = null;

	private static void init() {
		nbtMap.put(ItemLoader.coalAddon(), "LoliDodge");
		nbtMap.put(ItemLoader.ironAddon(), "LoliDiggingSpeed");
		nbtMap.put(ItemLoader.goldAddon(), "LoliAttackDamage");
		nbtMap.put(ItemLoader.redstoneAddon(), "LoliAttackSpeed");
		nbtMap.put(ItemLoader.lapisAddon(), "LoliFortuneLevel");
		nbtMap.put(ItemLoader.diamondAddon(), "LoliDiggingLevel");
		nbtMap.put(ItemLoader.emeraldAddon(), "LoliDiggingRange");
		nbtMap.put(ItemLoader.obsidianAddon(), "LoliAntiInjury");
		nbtMap.put(ItemLoader.glowAddon(), "LoliBuff");
		nbtMap.put(ItemLoader.quartzAddon(), "LoliHitRange");
		nbtMap.put(ItemLoader.netherStarAddon(), "LoliBackpackPage");
		nbtMap.put(ItemLoader.autoFurnaceAddon(), "LoliAutoFurnace");
		nbtMap.put(ItemLoader.flyAddon(), "LoliFly");
		full = new ItemStack(ItemLoader.smallLoliPickaxe());
		CompoundTag nbt = new CompoundTag();
		for (Entry<ItemLoliPickaxeMaterial, String> entry : ItemSmallLoliPickaxe.nbtMap.entrySet()) {
			nbt.putInt(entry.getValue(), entry.getKey().getSubCount() - 1);
		}
		full.setTag(nbt);
		ItemLoader.smallLoliPickaxe().updateEnchantment(full);
	}

	public static ItemStack getFull() {
		if (full == null) {
			init();
		}
		return full;
	}

	public ItemSmallLoliPickaxe() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (isCorrectToolForDrops(stack, state) && stack.hasTag() && stack.getTag().contains("LoliDiggingSpeed")) {
			return getTransformValue("LoliDiggingSpeed", stack.getTag().getInt("LoliDiggingSpeed"));
		}
		return 1;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		if (stack.hasTag() && stack.getTag().contains("LoliDiggingLevel")) {
			return isCorrectTierForDrops(getHarvestLevel(stack), state);
		}
		return false;
	}

	/** 1.20.1 没有 Block#getHarvestLevel，这里直接把原采集等级语义映射到原版工具等级标签上。 */
	private static boolean isCorrectTierForDrops(int harvestLevel, BlockState state) {
		if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
			return harvestLevel >= 3;
		} else if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
			return harvestLevel >= 2;
		} else if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
			return harvestLevel >= 1;
		}
		return true;
	}

	public int getHarvestLevel(ItemStack stack) {
		if (stack.hasTag() && stack.getTag().contains("LoliDiggingLevel")) {
			return getTransformValue("LoliDiggingLevel", stack.getTag().getInt("LoliDiggingLevel"));
		}
		return -1;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
		Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
		if (slot == EquipmentSlot.MAINHAND) {
			double damage = 0;
			double speed = 0;
			if (stack.hasTag()) {
				if (stack.getTag().contains("LoliAttackDamage")) {
					damage = getDoubleTransformValue("LoliAttackDamage", stack.getTag().getInt("LoliAttackDamage"));
				}
				if (stack.getTag().contains("LoliAttackSpeed")) {
					speed = getTransformValue("LoliAttackSpeed", stack.getTag().getInt("LoliAttackSpeed"));
				}
			}
			multimap.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID, "Tool modifier", damage, AttributeModifier.Operation.ADDITION));
			multimap.put(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_UUID, "Tool modifier", speed, AttributeModifier.Operation.ADDITION));
		}
		return multimap;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entityIn, int itemSlot, boolean isSelected) {
		updateEnchantment(stack);
	}

	public void updateEnchantment(ItemStack stack) {
		if (stack.hasTag()) {
			if (stack.getTag().contains("LoliFortuneLevel")) {
				int level = getTransformValue("LoliFortuneLevel", stack.getTag().getInt("LoliFortuneLevel"));
				Map<Enchantment, Integer> enchMap = Maps.newHashMap();
				enchMap.put(Enchantments.BLOCK_FORTUNE, level);
				enchMap.put(Enchantments.MOB_LOOTING, level);
				EnchantmentHelper.setEnchantments(enchMap, stack);
			}
		} else {
			stack.setTag(new CompoundTag());
		}
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
		if (!level.isClientSide && entity instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) entity;
			int range = getRange(stack);
			isharvesting = true;
			autoFurnace = stack.hasTag() && stack.getTag().contains("LoliAutoFurnace") ? getTransformValue("LoliAutoFurnace", stack.getTag().getInt("LoliAutoFurnace")) == 0 : false;
			fortuneLevel = stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
			exp = 0;
			blacklist = NonNullList.create();
			if (stack.hasTag() && stack.getTag().contains("Blacklist")) {
				ListTag blackList = stack.getTag().getList("Blacklist", 10);
				for (int i = 0; i < blackList.size(); i++) {
					ItemStack blackStack = getBlackStack(blackList.getCompound(i));
					if (!blackStack.isEmpty()) {
						blacklist.add(blackStack);
					}
				}
			}
			if (hasInventory(stack)) {
				inventory = getInventory(stack);
			}
			int bonusLevel = stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
			for (int i = -range; i <= range; i++) {
				for (int j = -range; j <= range; j++) {
					for (int k = -range; k <= range; k++) {
						BlockPos curPos = pos.offset(i, j, k);
						BlockState curState = level.getBlockState(curPos);
						if (curState.isAir()) {
							continue;
						}
						// 对应原 1.12.2 的 Block#canCollideCheck(state, false)：无碰撞箱的方块（花草等）不参与连锁
						if (curState.getCollisionShape(level, curPos).isEmpty()) {
							continue;
						}
						if (!isCorrectToolForDrops(stack, curState)) {
							continue;
						}
						if (ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, curPos) == -1) {
							continue;
						}
						BlockEntity tile = level.getBlockEntity(curPos);
						ItemStack tool = stack.copy();
						boolean canHarvest = curState.canHarvestBlock(level, curPos, player);
						if (curState.onDestroyedByPlayer(level, curPos, player, canHarvest, level.getFluidState(curPos))) {
							curState.getBlock().destroy(level, curPos, curState);
						}
						if (canHarvest) {
							curState.getBlock().playerDestroy(level, player, curPos, curState, tile, tool);
							int dropExp = curState.getBlock().getExpDrop(curState, level, level.random, curPos, bonusLevel, 0);
							if (dropExp > 0) {
								curState.getBlock().popExperience((ServerLevel) level, curPos, dropExp);
							}
						}
					}
				}
			}
			if (inventory != null) {
				inventory = null;
			}
			blacklist = null;
			if ((int) exp > 0) {
				level.addFreshEntity(new ExperienceOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (int) exp));
			}
			fortuneLevel = 0;
			autoFurnace = false;
			isharvesting = false;
		}
		return false;
	}

	private static ItemStack getBlackStack(CompoundTag black) {
		if (black.contains("Name")) {
			// ForgeRegistries.ITEMS#getValue 未命中时返回 null（而非空气），未注册的物品名按空气处理。
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(black.getString("Name")));
			if (item == null) {
				return ItemStack.EMPTY;
			}
			ItemStack blackStack = new ItemStack(item);
			if (black.contains("Damage")) {
				blackStack.setDamageValue(black.getInt("Damage"));
			}
			return blackStack;
		}
		return ItemStack.of(black);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			if (player.isShiftKeyDown()) {
				if (nbt.contains("LoliHitRange")) {
					if (!level.isClientSide) {
						int range = getTransformValue("LoliHitRange", nbt.getInt("LoliHitRange")) / 2;
						List<Entity> list = level.getEntities(player, player.getBoundingBox().inflate(range + 0.7, range + 0.1, range + 0.7), entity -> !(entity instanceof Player || entity instanceof ArmorStand || entity instanceof AmbientCreature || (entity instanceof PathfinderMob && !(entity instanceof Monster))));
						for (Entity entity : list) {
							player.attack(entity);
						}
						playSuccessSound(player);
					}
					player.resetAttackStrengthTicker();
					return InteractionResultHolder.success(stack);
				}
			} else {
				if (!level.isClientSide) {
					if (nbt.contains("LoliDiggingRange")) {
						int maxRange = (getTransformValue("LoliDiggingRange", stack.getTag().getInt("LoliDiggingRange")) - 1) / 2 + 1;
						if (nbt.contains("LoliCurrentDiggingRange")) {
							nbt.putInt("LoliCurrentDiggingRange", (nbt.getInt("LoliCurrentDiggingRange") + 1) % maxRange);
						} else {
							nbt.putInt("LoliCurrentDiggingRange", 1);
						}
						player.displayClientMessage(Component.translatable("loliPickaxe.range", 1 + 2 * nbt.getInt("LoliCurrentDiggingRange")), false);
						playSuccessSound(player);
						return InteractionResultHolder.success(stack);
					}
				}
			}
		}
		return InteractionResultHolder.pass(stack);
	}

	private static void playSuccessSound(LivingEntity entity) {
		SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MODID, "lolisuccess"));
		Player player = entity instanceof Player ? (Player) entity : null;
		entity.level().playSound(player, entity.blockPosition(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	public int getRange(ItemStack stack) {
		if (stack.hasTag() && stack.getTag().contains("LoliCurrentDiggingRange")) {
			return stack.getTag().getInt("LoliCurrentDiggingRange");
		}
		return 0;
	}

	// Item#getEnchantmentValue() 在 1.20.1 已被 Forge 废弃（javadoc：Use ItemStack sensitive version），
	// 故改为覆写非废弃的 ItemStack 敏感版本；两者在本类中都返回 0（Item 默认值同为 0），行为不变。
	@Override
	public int getEnchantmentValue(ItemStack stack) {
		return 0;
	}

	@Override
	public boolean isRepairable(ItemStack stack) {
		return false;
	}

	public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
		if (tab == CreativeTabLoader.loliTabs) {
			output.accept(new ItemStack(this));
			output.accept(getFull());
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			for (String levelKey : nbtMap.values()) {
				if (nbt.contains(levelKey)) {
					int lvl = nbt.getInt(levelKey);
					int value = getTransformValue(levelKey, lvl);
					if (value == Integer.MIN_VALUE) {
						tooltip.add(Component.translatable("smallLoliPickaxe." + levelKey, getDoubleTransformValue(levelKey, lvl)));
					} else {
						tooltip.add(Component.translatable("smallLoliPickaxe." + levelKey, value));
					}
				}
			}
		}
	}

	public int getTransformValue(String key, int level) {
		switch (key) {
		case "LoliDiggingSpeed":
			return 4 << level;
		case "LoliDiggingLevel":
			switch (level) {
			case 0:
				return 1;
			case 1:
				return 3;
			case 2:
				return 7;
			case 3:
				return 13;
			case 4:
				return 21;
			case 5:
				return 32;
			default:
				return -1;
			}
		case "LoliDiggingRange":
			return level * 2 + 3;
		case "LoliAttackSpeed":
			return 2 << level;
		case "LoliFortuneLevel":
			return 1 << level;
		case "LoliBackpackPage":
			return 2 << level;
		case "LoliBuff":
			return level + 1;
		case "LoliHitRange":
			return level * 10 + 6;
		case "LoliAutoFurnace":
			return level;
		case "LoliFly":
			return level;
		default:
			return Integer.MIN_VALUE;
		}
	}

	public double getDoubleTransformValue(String key, int level) {
		switch (key) {
		case "LoliDodge":
			return (level + 1) / 10.0;
		case "LoliAttackDamage":
			return 4 + Math.pow(2, Math.pow(2, level));
		case "LoliAntiInjury":
			return (level + 1) / 10.0;
		default:
			return Integer.MIN_VALUE;
		}
	}

	@Override
	public boolean hasInventory(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("LoliBackpackPage");
	}

	/**
	 * 每个小型萝莉镐物品堆对应唯一的容器实例缓存，理由与
	 * {@link ItemLoliPickaxe#getInventory(ItemStack)} 完全一致：
	 * 避免同一物品出现多个内存视图，导致 {@code stopOpen} 互相整份覆盖而丢物品。
	 * 使用弱引用以免长时间运行时缓存无界增长。
	 */
	private static final Map<ItemStack, java.lang.ref.WeakReference<InventorySmallLoliPickaxe>> INVENTORY_CACHE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	@Override
	public ILoliInventory getInventory(ItemStack stack) {
		if (stack.isEmpty()) {
			return new InventorySmallLoliPickaxe(stack);
		}
		synchronized (INVENTORY_CACHE) {
			java.lang.ref.WeakReference<InventorySmallLoliPickaxe> ref = INVENTORY_CACHE.get(stack);
			InventorySmallLoliPickaxe cached = ref == null ? null : ref.get();
			if (cached == null) {
				cached = new InventorySmallLoliPickaxe(stack);
				INVENTORY_CACHE.put(stack, new java.lang.ref.WeakReference<>(cached));
			}
			return cached;
		}
	}

	public int getMaxPage(ItemStack stack) {
		return getTransformValue("LoliBackpackPage", stack.getTag().getInt("LoliBackpackPage"));
	}

	public boolean canFly(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("LoliFly") && getTransformValue("LoliFly", stack.getTag().getInt("LoliFly")) == 0;
	}

	public double getDodge(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("LoliDodge") ? getDoubleTransformValue("LoliDodge", stack.getTag().getInt("LoliDodge")) : 0;
	}

	public double getAntiInjury(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("LoliAntiInjury") ? getDoubleTransformValue("LoliAntiInjury", stack.getTag().getInt("LoliAntiInjury")) : 0;
	}

	public int buffLevel(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("LoliBuff") ? getTransformValue("LoliBuff", stack.getTag().getInt("LoliBuff")) : -1;
	}

}
