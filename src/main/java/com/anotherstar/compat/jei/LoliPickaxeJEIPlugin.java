package com.anotherstar.compat.jei;

import java.util.List;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.block.BlockLoader;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.anotherstar.common.item.ItemLoliRecord;
import com.google.common.collect.Lists;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 本模组的 JEI 插件。
 *
 * <h2>为什么需要这个插件</h2>
 *
 * <p>本模组的三个配方全部继承自原版 {@link net.minecraft.world.item.crafting.CustomRecipe}，
 * 其配方 JSON 只有一行 {@code {"type": "..."}} —— 匹配与合成逻辑完全写在 Java 中。
 * JEI 的显示机制依赖配方提供的「原料/产物」数据，而 {@code CustomRecipe}
 * 并不暴露这类结构化数据，因此 <b>JEI 无法自动识别并展示这些配方</b>。
 *
 * <p>这解释了玩家"在 JEI 里查不到本模组合成方式"的现象：
 * 不是配方坏了，而是<b>从未提供过 JEI 所需的展示层</b>。
 *
 * <h2>本插件做了什么</h2>
 *
 * <ul>
 *   <li>注册 4 个配方类别：氪金萝莉 / 小萝莉升级 / 材料叠加 / 材料拆分；</li>
 *   <li>为每个类别构造与真实配方<b>严格一致</b>的展示条目；</li>
 *   <li>把合成台与小萝莉镐登记为催化剂，方便玩家从物品直接跳到配方。</li>
 * </ul>
 *
 * <h2>如何保证不改变原有玩法</h2>
 *
 * <ul>
 *   <li>本插件是<b>只读展示层</b>，不参与任何合成判定；</li>
 *   <li>三个 {@code CustomRecipe} 子类<b>一行未改</b>；</li>
 *   <li>JEI 依赖为 {@code compileOnly}：未安装 JEI 时本类不会被加载，模组照常运行。</li>
 * </ul>
 */
@JeiPlugin
public class LoliPickaxeJEIPlugin implements IModPlugin {

	/** 插件 UID，使用本模组 id 以保证唯一。 */
	private static final ResourceLocation UID = new ResourceLocation(LoliPickaxe.MODID, "jei_plugin");

	public static final RecipeType<LoliJeiRecipes.Display> LOLI_PICKAXE_TYPE = RecipeType.create(LoliPickaxe.MODID, "loli_pickaxe", LoliJeiRecipes.Display.class);
	public static final RecipeType<LoliJeiRecipes.Display> SMALL_LOLI_PICKAXE_TYPE = RecipeType.create(LoliPickaxe.MODID, "small_loli_pickaxe", LoliJeiRecipes.Display.class);
	public static final RecipeType<LoliJeiRecipes.Display> SUPERPOSITION_TYPE = RecipeType.create(LoliPickaxe.MODID, "superposition", LoliJeiRecipes.Display.class);
	public static final RecipeType<LoliJeiRecipes.Display> SPLIT_TYPE = RecipeType.create(LoliPickaxe.MODID, "split", LoliJeiRecipes.Display.class);
	public static final RecipeType<LoliJeiRecipes.Drop> MOB_DROP_TYPE = RecipeType.create(LoliPickaxe.MODID, "mob_drop", LoliJeiRecipes.Drop.class);
	/** 「萝莉祭坛摆放方式」类别：展示 63×63 的建造图案。 */
	public static final RecipeType<LoliJeiRecipes.AltarLayout> ALTAR_TYPE = RecipeType.create(LoliPickaxe.MODID, "altar", LoliJeiRecipes.AltarLayout.class);

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new LoliPickaxeCategory(helper, LOLI_PICKAXE_TYPE), new SmallLoliPickaxeCategory(helper, SMALL_LOLI_PICKAXE_TYPE), new SuperpositionCategory(helper, SUPERPOSITION_TYPE), new LoliSplitCategory(helper, SPLIT_TYPE), new LoliDropCategory(helper, MOB_DROP_TYPE), new LoliAltarCategory(helper, ALTAR_TYPE));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(LOLI_PICKAXE_TYPE, buildPickaxeRecipes());
		registration.addRecipes(SMALL_LOLI_PICKAXE_TYPE, buildUpgradeRecipes());
		registration.addRecipes(SUPERPOSITION_TYPE, buildSuperpositionRecipes());
		registration.addRecipes(SPLIT_TYPE, buildSplitRecipes());
		registration.addRecipes(MOB_DROP_TYPE, buildDropRecipes());
		// 祭坛摆法：固定一条，图案由 LoliAltarPattern 提供
		registration.addRecipes(ALTAR_TYPE, Lists.newArrayList(new LoliJeiRecipes.AltarLayout()));
		// 「基本物品怎么获取」的说明页：这些物品不是合成的，而是概率掉落，
		// 光靠配方列表玩家看不到来源，因此逐项补信息页。
		addInfoPages(registration);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		// 合成台是三个配方共用的合成载体
		ItemStack craftingTable = new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE);
		registration.addRecipeCatalyst(craftingTable, LOLI_PICKAXE_TYPE);
		registration.addRecipeCatalyst(craftingTable, SMALL_LOLI_PICKAXE_TYPE);
		registration.addRecipeCatalyst(craftingTable, SUPERPOSITION_TYPE);
		registration.addRecipeCatalyst(craftingTable, SPLIT_TYPE);
		// 小萝莉镐本身也可作为入口，方便玩家从物品跳转到升级配方
		registration.addRecipeCatalyst(new ItemStack(ItemLoader.smallLoliPickaxe()), SMALL_LOLI_PICKAXE_TYPE);
		// 祭坛方块作为入口：点击祭坛方块即可查看怎么摆
		registration.addRecipeCatalyst(new ItemStack(BlockLoader.itemLoliAltar()), ALTAR_TYPE);
	}

	/**
	 * 构造「氪金萝莉」展示条目。
	 *
	 * <p>与 {@code LoliPickaxeRecipe#matches} 严格对应：
	 * 需要「全部属性满级的小萝莉」+「满级的生物灵魂」。
	 *
	 * @return 展示条目列表（此配方固定一条）
	 */
	private List<LoliJeiRecipes.Display> buildPickaxeRecipes() {
		ItemLoliPickaxeMaterial soul = ItemLoader.entitySoul();
		List<ItemStack> inputs = Lists.newArrayList();
		inputs.add(LoliJeiStacks.fullLevelSmallLoli());
		// 灵魂附加件必须满级，对应 matches() 中 damage == subCount-1 的判定
		inputs.add(LoliJeiStacks.material(soul, soul.getSubCount() - 1));
		return Lists.newArrayList(new LoliJeiRecipes.Display(inputs, new ItemStack(ItemLoader.loliPickaxe())));
	}

	/**
	 * 构造「小萝莉属性升级」展示条目。
	 *
	 * <p>与 {@code SmallLoliPickaxeRecipe#matches} 对应：
	 * 镐子当前等级必须是「材料等级 - 1」，升级后 +1。
	 *
	 * <p>为避免条目数量爆炸（14 种材料 × 各自多个等级），
	 * 每种材料只展示<b>三个代表档位</b>：起步(1级)、中档、满级。
	 * 这样既说明了规律，也不会让 JEI 列表过长而卡顿。
	 *
	 * @return 展示条目列表
	 */
	private List<LoliJeiRecipes.Display> buildUpgradeRecipes() {
		List<LoliJeiRecipes.Display> recipes = Lists.newArrayList();
		for (ItemLoliPickaxeMaterial material : LoliJeiStacks.nbtMap().keySet()) {
			int max = material.getSubCount() - 1;
			if (max < 1) {
				// 单级材料（自动熔炼/飞行）没有升级空间，跳过
				continue;
			}
			for (int level : representativeLevels(max)) {
				List<ItemStack> inputs = Lists.newArrayList();
				inputs.add(LoliJeiStacks.smallLoliWithMaterialLevel(material, level));
				inputs.add(LoliJeiStacks.material(material, level));
				recipes.add(new LoliJeiRecipes.Display(inputs, LoliJeiStacks.smallLoliAfterUpgrade(material, level)));
			}
		}
		return recipes;
	}

	/**
	 * 计算应展示的代表性等级档位。
	 *
	 * @param max 该材料的最高等级
	 * @return 不超过 3 个档位的升序列表，全部落在 {@code [1, max]}
	 */
	private static List<Integer> representativeLevels(int max) {
		List<Integer> levels = Lists.newArrayList();
		levels.add(1);
		if (max >= 2 && max != 1) {
			levels.add(Math.max(1, max / 2));
		}
		if (max >= 2) {
			levels.add(max);
		}
		return levels.stream().distinct().sorted().toList();
	}

	/**
	 * 构造「材料叠加」展示条目：9 个 N 级材料 → 1 个 N+1 级材料。
	 *
	 * @return 展示条目列表
	 */
	private List<LoliJeiRecipes.Display> buildSuperpositionRecipes() {
		List<LoliJeiRecipes.Display> recipes = Lists.newArrayList();
		for (ItemLoliPickaxeMaterial material : LoliJeiStacks.superpositionMaterials()) {
			int max = material.getSubCount() - 1;
			for (int level = 0; level < max; level++) {
				List<ItemStack> inputs = Lists.newArrayList();
				for (int i = 0; i < 9; i++) {
					inputs.add(LoliJeiStacks.material(material, level));
				}
				recipes.add(new LoliJeiRecipes.Display(inputs, LoliJeiStacks.material(material, level + 1)));
			}
		}
		return recipes;
	}

	/**
	 * 构造「材料拆分」展示条目：1 个 N+1 级材料 → 9 个 N 级材料。
	 *
	 * @return 展示条目列表
	 */
	private List<LoliJeiRecipes.Display> buildSplitRecipes() {
		List<LoliJeiRecipes.Display> recipes = Lists.newArrayList();
		for (ItemLoliPickaxeMaterial material : LoliJeiStacks.superpositionMaterials()) {
			int max = material.getSubCount() - 1;
			for (int level = max; level > 0; level--) {
				List<ItemStack> inputs = Lists.newArrayList();
				inputs.add(LoliJeiStacks.material(material, level));
				ItemStack output = LoliJeiStacks.material(material, level - 1);
				output.setCount(9);
				recipes.add(new LoliJeiRecipes.Display(inputs, output));
			}
		}
		return recipes;
	}

	/**
	 * 构造「生物掉落」展示条目。
	 *
	 * <p>与 {@code com.anotherstar.common.event.LoliDropEvent#onLivingDrop} 一一对应：
	 * <ul>
	 *   <li>萝莉卡片、卡片册、生物灵魂：任意生物死亡时按各自概率掉落；</li>
	 *   <li>萝莉唱片：<b>仅苦力怕</b>死亡时按概率掉落。</li>
	 * </ul>
	 *
	 * <p>概率在构造时从 {@link ConfigLoader} 读取，因此管理员改配置并重启后，
	 * JEI 会显示新的数值，不会与真实掉率脱节。
	 *
	 * @return 掉落展示条目列表
	 */
	private List<LoliJeiRecipes.Drop> buildDropRecipes() {
		List<LoliJeiRecipes.Drop> drops = Lists.newArrayList();
		// 以「铁剑」代表「击杀任意生物」，比用某个具体生物的刷怪蛋更准确
		ItemStack anyMob = new ItemStack(Items.IRON_SWORD);
		ItemStack creeper = new ItemStack(Items.CREEPER_SPAWN_EGG);
		drops.add(new LoliJeiRecipes.Drop(anyMob, new ItemStack(ItemLoader.loliCard()), ConfigLoader.loliCardDropProbability));
		drops.add(new LoliJeiRecipes.Drop(anyMob, new ItemStack(ItemLoader.loliCardAlbum()), ConfigLoader.loliCardAlbumDropProbability));
		if (!ItemLoader.loliRecords.isEmpty()) {
			drops.add(new LoliJeiRecipes.Drop(creeper, new ItemStack(ItemLoader.loliRecords.get(0)), ConfigLoader.loliRecordDropProbability));
		}
		drops.add(new LoliJeiRecipes.Drop(anyMob, new ItemStack(ItemLoader.entitySoul()), ConfigLoader.entitySoulDropProbability));
		return drops;
	}

	/**
	 * 为「不是合成得到」的物品补充 JEI 信息页。
	 *
	 * <p>这些物品（生物灵魂、卡片、卡片册、唱片）都靠生物概率掉落，
	 * 玩家在 JEI 里只能看到它们存在，却看不到来源，这正是「最基本物品不知道怎么获取」的原因。
	 * 信息页会显示从配置实时读取的掉落概率。
	 *
	 * @param registration 配方注册器；不可为 null
	 */
	private void addInfoPages(IRecipeRegistration registration) {
		registration.addItemStackInfo(new ItemStack(ItemLoader.loliPickaxe()), Component.translatable("jei.lolipickaxe.info.pickaxe"));
		registration.addItemStackInfo(new ItemStack(ItemLoader.smallLoliPickaxe()), Component.translatable("jei.lolipickaxe.info.small_pickaxe"));
		// 生物灵魂共 7 个等级，每个等级都要挂上说明，否则玩家点到非 0 级时看不到任何信息
		registration.addItemStackInfo(materialStacks(ItemLoader.entitySoul()), Component.translatable("jei.lolipickaxe.info.soul", LoliDropCategory.formatPercent(ConfigLoader.entitySoulDropProbability)));
		registration.addItemStackInfo(new ItemStack(ItemLoader.loliCard()), Component.translatable("jei.lolipickaxe.info.card", LoliDropCategory.formatPercent(ConfigLoader.loliCardDropProbability)));
		registration.addItemStackInfo(new ItemStack(ItemLoader.loliCardAlbum()), Component.translatable("jei.lolipickaxe.info.card_album", LoliDropCategory.formatPercent(ConfigLoader.loliCardAlbumDropProbability)));
		if (!ItemLoader.loliRecords.isEmpty()) {
			List<ItemStack> records = Lists.newArrayList();
			for (ItemLoliRecord record : ItemLoader.loliRecords) {
				records.add(new ItemStack(record));
			}
			registration.addItemStackInfo(records, Component.translatable("jei.lolipickaxe.info.record", LoliDropCategory.formatPercent(ConfigLoader.loliRecordDropProbability)));
		}
		// 三色炸弹：说明可用 TNT 在工作台合成，避免玩家以为它们无处可得
		registration.addItemStackInfo(Lists.newArrayList(new ItemStack(BlockLoader.itemLoliBlueScreenTNT()), new ItemStack(BlockLoader.itemLoliExitTNT()), new ItemStack(BlockLoader.itemLoliFailRespondTNT())), Component.translatable("jei.lolipickaxe.info.tnt"));
		// 密码工作台：说明"除了摆材料还要输密码"，否则玩家会以为它坏了
		registration.addItemStackInfo(new ItemStack(BlockLoader.itemPasswordWorkBench()), Component.translatable("jei.lolipickaxe.info.password_workbench"));
		// 祭坛方块：指向 JEI 中的「萝莉祭坛摆放方式」类别
		registration.addItemStackInfo(new ItemStack(BlockLoader.itemLoliAltar()), Component.translatable("jei.lolipickaxe.info.altar_block"));
	}

	/**
	 * 把某个材料物品的<b>全部等级</b>展开成堆叠列表。
	 *
	 * <p>JEI 的信息页按物品堆匹配，若只登记 0 级，玩家点到其它等级的物品就没有说明。
	 *
	 * @param material 材料物品；不可为 null
	 * @return 覆盖 {@code 0..subCount-1} 全部等级的堆叠列表
	 */
	private static List<ItemStack> materialStacks(ItemLoliPickaxeMaterial material) {
		List<ItemStack> stacks = Lists.newArrayList();
		int subCount = Math.max(1, material.getSubCount());
		for (int i = 0; i < subCount; i++) {
			ItemStack stack = new ItemStack(material);
			stack.setDamageValue(i);
			stacks.add(stack);
		}
		return stacks;
	}

}
