package com.anotherstar.compat.jei;

import java.util.List;

import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.anotherstar.common.item.tool.ItemSmallLoliPickaxe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraft.resources.ResourceLocation;

/**
 * JEI 展示数据的构造工具。
 *
 * <h2>为什么需要独立的构造逻辑</h2>
 *
 * <p>本模组的三个配方都是 {@code CustomRecipe}：JSON 里只有 {@code type}，
 * 真正的匹配与合成逻辑写在 Java 的 {@code matches()} / {@code assemble()} 中。
 * JEI 无法静态解析这类配方，只能由插件<b>显式构造</b>用于展示的物品堆叠。
 *
 * <h2>最重要的约束：展示必须与真实配方一致</h2>
 *
 * <p>这里构造的每一个 {@link ItemStack}，其等级/damage 都必须与
 * {@code matches()} 中的判定条件严格对应。否则玩家会"照着 JEI 做却合不出来"，
 * 那比不显示更糟。因此本类的每个方法都标注了它对应的判定来源。
 *
 * <p>本类<b>不参与任何实际合成</b>，只用于 JEI 渲染。
 */
public final class LoliJeiStacks {

	private LoliJeiStacks() {
	}

	/**
	 * 构造指定等级的材料堆叠。
	 *
	 * @param material 材料物品；不可为 null
	 * @param level    等级（写入 damage 值），会被夹到 {@code [0, subCount-1]}
	 * @return 对应等级的物品堆叠
	 */
	public static ItemStack material(ItemLoliPickaxeMaterial material, int level) {
		ItemStack stack = new ItemStack(material);
		int clamped = Math.max(0, Math.min(level, material.getSubCount() - 1));
		stack.setDamageValue(clamped);
		return stack;
	}

	/**
	 * 构造「全部属性满级」的小萝莉，对应 {@code LoliPickaxeRecipe#matches} 的要求：
	 * 遍历 {@link ItemSmallLoliPickaxe#nbtMap}，每个键的值都必须等于该材料的 {@code subCount - 1}。
	 *
	 * @return 所有属性满级的小萝莉堆叠
	 */
	public static ItemStack fullLevelSmallLoli() {
		ItemStack stack = new ItemStack(ItemLoader.smallLoliPickaxe());
		CompoundTag nbt = new CompoundTag();
		for (java.util.Map.Entry<ItemLoliPickaxeMaterial, String> entry : nbtMap().entrySet()) {
			nbt.putInt(entry.getValue(), entry.getKey().getSubCount() - 1);
		}
		stack.setTag(nbt);
		return stack;
	}

	/**
	 * 构造指定单项属性等级的小萝莉。
	 *
	 * <p>对应 {@code SmallLoliPickaxeRecipe#matches} 中
	 * 「镐子当前等级 == 材料等级 - 1」这一条件：
	 * 因此传入的是材料的等级，镐子该属性写入 {@code level - 1}。
	 *
	 * @param material 待升级的材料
	 * @param level    材料等级（镐子将处于 {@code level - 1} 级）
	 * @return 升级前的小萝莉堆叠
	 */
	public static ItemStack smallLoliWithMaterialLevel(ItemLoliPickaxeMaterial material, int level) {
		ItemStack stack = new ItemStack(ItemLoader.smallLoliPickaxe());
		CompoundTag nbt = new CompoundTag();
		String key = nbtMap().get(material);
		if (key != null) {
			// 配方要求：镐子当前等级 = 材料等级 - 1
			nbt.putInt(key, Math.max(0, level - 1));
		}
		stack.setTag(nbt);
		return stack;
	}

	/**
	 * 构造「该材料升级完成后」的小萝莉，用于 JEI 的产物槽展示。
	 *
	 * <p>对应 {@code SmallLoliPickaxeRecipe#assemble}：把镐子上该属性 +1。
	 *
	 * @param material 已使用的材料
	 * @param level    材料等级
	 * @return 升级后的小萝莉堆叠
	 */
	public static ItemStack smallLoliAfterUpgrade(ItemLoliPickaxeMaterial material, int level) {
		ItemStack stack = new ItemStack(ItemLoader.smallLoliPickaxe());
		CompoundTag nbt = new CompoundTag();
		String key = nbtMap().get(material);
		if (key != null) {
			nbt.putInt(key, Math.max(0, Math.min(level, material.getSubCount() - 1)));
		}
		stack.setTag(nbt);
		return stack;
	}

	/**
	 * 取得材料 → NBT 键的映射。
	 *
	 * <p>该映射由 {@link ItemSmallLoliPickaxe#getFull()} 惰性初始化，
	 * 因此这里做一次空检查触发初始化，避免 JEI 在早期阶段拿到空表。
	 *
	 * @return 材料到 NBT 键的映射；永不返回 null
	 */
	public static java.util.Map<ItemLoliPickaxeMaterial, String> nbtMap() {
		if (ItemSmallLoliPickaxe.nbtMap.isEmpty()) {
			ItemSmallLoliPickaxe.getFull();
		}
		return ItemSmallLoliPickaxe.nbtMap;
	}

	/**
	 * 返回所有「可叠加/可升级」的材料物品列表（不含单级的自动熔炼与飞行附加物）。
	 *
	 * <p>与 {@code SuperpositionRecipe} 静态块中登记的材料集合保持一致。
	 *
	 * @return 材料列表
	 */
	public static List<ItemLoliPickaxeMaterial> superpositionMaterials() {
		return List.of(ItemLoader.coalAddon(), ItemLoader.ironAddon(), ItemLoader.goldAddon(), ItemLoader.redstoneAddon(), ItemLoader.lapisAddon(), ItemLoader.diamondAddon(), ItemLoader.emeraldAddon(), ItemLoader.obsidianAddon(), ItemLoader.glowAddon(), ItemLoader.quartzAddon(), ItemLoader.netherStarAddon(), ItemLoader.entitySoul());
	}

	/**
	 * 构建插件用的 UID 辅助方法。
	 *
	 * @param path 路径段
	 * @return 本模组命名空间下的资源路径
	 */
	public static ResourceLocation id(String path) {
		return new ResourceLocation(com.anotherstar.common.LoliPickaxe.MODID, path);
	}

}
