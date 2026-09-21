package com.anotherstar.common.item;

import java.util.List;
import java.util.function.Consumer;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.common.LoliPickaxe;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 罗莉镐升级材料（附加物），对应原 1.12.2 的同名类。
 * <p>
 * 本物品「一个物品 ID 承载多个等级」：不使用耐久，而是把
 * {@link ItemStack#getDamageValue()} / {@link ItemStack#setDamageValue(int)} 当作 1.12.2 的
 * metadata（子类型）使用，取值 {@code 0 .. subCount - 1}，等级越高代表附加物越强。
 * 1.12.2 通过 {@code setHasSubtypes(subCount != 1)} 声明是否有子类型，
 * 1.20.1 该 API 已移除，因此直接以 {@link #subCount} 是否等于 1 判断。
 * <p>
 * 与 1.12.2 的差异：
 * <ul>
 * <li>{@code getItemStackDisplayName} → {@link #getName(ItemStack)}（改用 {@link Component} 与 i18n 键）；</li>
 * <li>{@code addInformation} → {@link #appendHoverText}；</li>
 * <li>{@code getSubItems} → {@link #fillItemCategory}，由 {@code CreativeTabLoader} 在
 * {@code BuildCreativeModeTabContentsEvent} 中回调（1.12.2 的 {@code setCreativeTab} 已无对应 API）；</li>
 * <li>{@code addPropertyOverride} → {@link #initializeClient} 中的 {@code ItemProperties.register}。</li>
 * </ul>
 */
public class ItemLoliPickaxeMaterial extends Item {

	/** 子类型（等级）总数，合法子类型为 {@code 0 ~ subCount - 1}；等于 1 时表示无子类型。 */
	private final int subCount;
	/** 是否让最后一级（{@code subCount - 1}）使用专属贴图，见 {@link #initializeClient} 注册的 {@code end} 属性。 */
	private final boolean differentEnd;

	/**
	 * @param name         1.12.2 的 unlocalizedName，1.20.1 仅用于语言键，不再参与注册（注册名见 {@code ItemLoader}）
	 * @param subCount     子类型总数，必须 &gt;= 1
	 * @param differentEnd 最后一级子类型是否使用独立贴图
	 */
	public ItemLoliPickaxeMaterial(String name, int subCount, boolean differentEnd) {
		super(new Item.Properties());
		this.subCount = subCount;
		this.differentEnd = differentEnd;
	}

	/**
	 * 1.20.1 已删除 Item#fillItemCategory，改为由 CreativeTabLoader 在 BuildCreativeModeTabContentsEvent 中调用。
	 *
	 * @param tab    触发填充的标签页，仅当为 {@code CreativeTabLoader.loliRecipeTabs}（注册名 {@code loli_recipe}）时才添加内容；不可为 null
	 * @param output 标签页内容收集器，会接收 {@code subCount} 个不同等级的物品堆叠；不可为 null
	 */
	public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
		if (tab == CreativeTabLoader.loliRecipeTabs) {
			for (int i = 0; i < subCount; i++) {
				ItemStack stack = new ItemStack(this);
				stack.setDamageValue(i); // 用 damage 充当等级子类型，逐个等级放入创造栏
				output.accept(stack);
			}
		}
	}

	/**
	 * 返回带等级后缀的显示名，格式由语言键 {@code item.loliMaterialFormat} 决定。
	 *
	 * @param stack 待取名的物品堆叠，不可为 null
	 * @return 有子类型时返回「基础名 + 等级名（最后一级为 {@code item.loliMaterial.end}）」，否则返回原版名称
	 */
	@Override
	public Component getName(ItemStack stack) {
		if (subCount != 1) {
			if (stack.getDamageValue() == subCount - 1) {
				// 最后一级使用「终极/END」字样
				return Component.translatable("item.loliMaterialFormat", Component.translatable(getDescriptionId(stack)), Component.translatable("item.loliMaterial.end"));
			} else {
				return Component.translatable("item.loliMaterialFormat", Component.translatable(getDescriptionId(stack)), Component.translatable("item.loliMaterial." + stack.getDamageValue()));
			}
		} else {
			return super.getName(stack);
		}
	}

	/**
	 * 追加提示：显示「当前等级 → 下一等级 → 最高等级」的合成路线。
	 *
	 * @param stack   待显示提示的物品堆叠，不可为 null
	 * @param level   当前世界，客户端渲染提示时非 null，某些场景（如创造栏搜索）可能为 null
	 * @param tooltip 提示行收集列表，直接就地追加；不可为 null
	 * @param flag    提示标志位（普通/高级提示），本方法未使用；不可为 null
	 */
	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.getDamageValue() < subCount - 1) {
			// 已是倒数第二级时，下一级应显示为「终极」而不是编号
			tooltip.add(Component.translatable("item.loliMaterial.recipe", Component.translatable("item.loliMaterial." + stack.getDamageValue()), stack.getDamageValue() == subCount - 2 ? Component.translatable("item.loliMaterial.end") : Component.translatable("item.loliMaterial." + (stack.getDamageValue() + 1)), Component.translatable("item.loliMaterial." + (subCount - 1))));
		}
	}

	/**
	 * 注册客户端模型属性，对应 1.12.2 的 {@code addPropertyOverride}。
	 *
	 * <p><b>注意</b>：1.20.1 的物品<b>不会自动调用本方法</b> ——
	 * 只有在通过 {@code RegisterClientExtensionsEvent} 注册了
	 * {@link IClientItemExtensions} 时才会被访问，而本项目并未走那条路径。
	 * 因此这里保留本方法作为「该物品需要 end 属性」的自我描述，
	 * <b>真正生效的注册在 {@code ItemModelPropertyLoader}</b>（客户端统一登记）。
	 *
	 * @param consumer 客户端物品扩展收集器，由 Forge 传入，不可为 null
	 */
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		ItemProperties.register(this, new ResourceLocation(LoliPickaxe.MODID, "end"), (stack, level, entity, seed) -> getEndPropertyValue(stack));
	}

	/**
	 * 计算 {@code lolipickaxe:end} 属性的取值，供客户端模型 override 使用。
	 *
	 * <p>取值规则与 1.12.2 一致：仅当本材料具备「终极形态独立贴图」且该堆叠正好是最后一级时返回 1.0。
	 *
	 * @param stack 待判定的物品堆叠；不可为 null
	 * @return 1.0 表示使用终极形态贴图，0.0 表示使用普通贴图
	 */
	public float getEndPropertyValue(ItemStack stack) {
		return differentEnd && stack.getDamageValue() == subCount - 1 ? 1.0F : 0.0F;
	}

	/** @return 本材料是否让最后一级使用独立贴图（决定是否需要注册 {@code end} 属性） */
	public boolean hasDifferentEnd() {
		return differentEnd;
	}

	/** @return 子类型（等级）总数，取值范围 {@code >= 1}；1 表示无子类型 */
	public int getSubCount() {
		return subCount;
	}

}
