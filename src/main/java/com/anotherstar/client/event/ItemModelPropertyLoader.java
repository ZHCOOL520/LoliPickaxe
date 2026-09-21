package com.anotherstar.client.event;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.anotherstar.common.item.ItemLoader;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/**
 * 客户端模型属性注册。
 *
 * <h2>为什么需要这个类（关键修复）</h2>
 *
 * <p>原 1.12.2 在 {@code ItemLoader#registerModel} 中用
 * {@code ModelLoader.setCustomMeshDefinition(...)} 为「生物灵魂附加件」注册了
 * {@code end} 属性，模型 JSON 依赖它切换到终极形态贴图
 * （见 {@code models/item/loli_entity_soul_addon.json} 的 {@code overrides}）。
 *
 * <p>移植到 1.20.1 后，该逻辑被写在 {@link ItemLoliPickaxeMaterial#initializeClient} 里。
 * 但 <b>1.20.1 的物品并不会自动调用 {@code initializeClient}</b> ——
 * 它只在走 {@code IClientItemExtensions} 那套注册流程时才会被访问，
 * 而本项目并没有通过 {@code RegisterClientExtensionsEvent} 去触发它。
 *
 * <p>结果：{@code lolipickaxe:end} 这个物品属性<b>从未被注册</b>，
 * 模型 {@code overrides} 里的 predicate 永远取不到值（恒为 0），
 * 于是「生物灵魂附加件」的终极形态贴图<b>永远不会显示</b>。
 *
 * <p>本类用 Forge 1.20.1 的标准方式补齐这件事：在客户端初始化阶段，
 * 对所有「有终极形态独立贴图」的材料统一登记 {@code end} 属性。
 *
 * <h2>为什么不改变玩法</h2>
 *
 * <p>本类只做「把模型属性的取值函数登记进去」，取值逻辑与 1.12.2 完全一致：
 * 当该堆叠是最后一级（{@code damage == subCount - 1}）时返回 1.0，否则返回 0.0。
 * 它不修改任何等级数值、合成配方或物品行为，只影响<b>贴图选择</b>。
 */
@Mod.EventBusSubscriber(modid = LoliPickaxe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemModelPropertyLoader {

	/** 终极形态属性名，与模型 JSON 中的 {@code "lolipickaxe:end"} 对应。 */
	private static final ResourceLocation END_PROPERTY = new ResourceLocation(LoliPickaxe.MODID, "end");

	/**
	 * 在客户端物品注册完成后，为需要切换贴图的材料登记 {@code end} 模型属性。
	 *
	 * <p>选用 {@link RegisterEvent}（物品注册表阶段）是因为：
	 * 此时所有物品已构造完成、可以安全取到实例，且早于模型烘焙，
	 * 保证模型 JSON 解析 {@code overrides} 时该属性已经存在。
	 *
	 * @param event Forge 的注册事件；不可为 null
	 */
	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		if (!event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) {
			return;
		}
		for (Item item : ItemLoader.allMaterials()) {
			if (item instanceof ItemLoliPickaxeMaterial) {
				ItemLoliPickaxeMaterial material = (ItemLoliPickaxeMaterial) item;
				// 只登记真正需要切换贴图的材料，避免给无关物品增加无谓开销
				if (material.hasDifferentEnd()) {
					ItemProperties.register(material, END_PROPERTY,
							(stack, level, entity, seed) -> material.getEndPropertyValue(stack));
				}
			}
		}
	}

}
