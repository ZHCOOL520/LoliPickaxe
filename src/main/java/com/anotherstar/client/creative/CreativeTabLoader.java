package com.anotherstar.client.creative;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.block.BlockLoader;
import com.anotherstar.common.item.ItemLoader;
import com.anotherstar.common.item.ItemLoliPickaxeMaterial;
import com.anotherstar.common.item.ItemLoliRecord;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

/**
 * 1.20.1 创造模式物品栏。1.12.2 的 CreativeTabs 被替换为数据驱动的 CreativeModeTab，
 * 因此标签页在类加载阶段（即 CommonProxy.preInit 调用 TABS.register 之前）急切构造好，
 * 内容则通过 mod 事件总线的 BuildCreativeModeTabContentsEvent 填充。
 */
public class CreativeTabLoader {

	public static CreativeModeTab loliTabs;

	public static CreativeModeTab loliRecipeTabs;

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LoliPickaxe.MODID);

	static {
		loliTabs = CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.loli"))
				.icon(() -> new ItemStack(ItemLoader.loliPickaxe()))
				.build();
		loliRecipeTabs = CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.loliRecipe"))
				.icon(() -> {
					ItemStack stack = new ItemStack(ItemLoader.entitySoul());
					stack.setDamageValue(ItemLoader.entitySoul().getSubCount() - 1);
					return stack;
				})
				.build();
		TABS.register("loli", () -> loliTabs);
		TABS.register("loli_recipe", () -> loliRecipeTabs);
		// 与 EntityLoader 一致：注册项需要在类加载时就挂到 mod 事件总线上。
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(CreativeTabLoader::onBuildContents);
	}

	/**
	 * 标签页本身已在静态初始化块中构造并注册，此方法仅保留 1.12.2 的调用入口。
	 */
	public static void init() {
	}

	private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTab() == loliTabs) {
			fillLoliTabs(event);
		} else if (event.getTab() == loliRecipeTabs) {
			fillLoliRecipeTabs(event);
		}
	}

	private static void fillLoliTabs(CreativeModeTab.Output output) {
		ItemLoader.loliPickaxe().fillItemCategory(loliTabs, output);
		ItemLoader.smallLoliPickaxe().fillItemCategory(loliTabs, output);
		output.accept(new ItemStack(ItemLoader.loliDispersal()));
		output.accept(new ItemStack(ItemLoader.bugEntityClear()));
		ItemLoader.loliCard().fillItemCategory(loliTabs, output);
		ItemLoader.loliCardAlbum().fillItemCategory(loliTabs, output);
		ItemLoader.loliCardOnline().fillItemCategory(loliTabs, output);
		for (ItemLoliRecord record : ItemLoader.loliRecords) {
			output.accept(new ItemStack(record));
		}
		// 萝莉刷怪蛋：对应 1.12.2 的 EntityRegistry.registerEgg。
		// 移植时遗漏了这一步，导致萝莉实体完全没有生成方式。放在唱片之后，保持既有顺序不变。
		output.accept(new ItemStack(ItemLoader.loliSpawnEgg()));
		// 方块物品：三色 TNT 此前不在任何物品栏，玩家只能靠记配方合成才能拿到。
		// 现统一移入本物品栏，位置排在工具与卡片之后。
		output.accept(new ItemStack(BlockLoader.itemLoliBlueScreenTNT()));
		output.accept(new ItemStack(BlockLoader.itemLoliExitTNT()));
		output.accept(new ItemStack(BlockLoader.itemLoliFailRespondTNT()));
		output.accept(new ItemStack(BlockLoader.itemLoliAltar()));
		output.accept(new ItemStack(BlockLoader.itemPasswordWorkBench()));
	}

	private static void fillLoliRecipeTabs(CreativeModeTab.Output output) {
		ItemLoliPickaxeMaterial[] materials = { ItemLoader.coalAddon(), ItemLoader.ironAddon(), ItemLoader.goldAddon(), ItemLoader.redstoneAddon(), ItemLoader.lapisAddon(), ItemLoader.diamondAddon(), ItemLoader.emeraldAddon(), ItemLoader.obsidianAddon(), ItemLoader.glowAddon(), ItemLoader.quartzAddon(), ItemLoader.netherStarAddon(), ItemLoader.autoFurnaceAddon(), ItemLoader.flyAddon(), ItemLoader.entitySoul() };
		for (ItemLoliPickaxeMaterial material : materials) {
			material.fillItemCategory(loliRecipeTabs, output);
		}
	}

}
