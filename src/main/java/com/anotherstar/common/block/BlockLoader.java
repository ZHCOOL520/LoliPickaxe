package com.anotherstar.common.block;

import java.util.List;

import javax.annotation.Nullable;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockLoader {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LoliPickaxe.MODID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LoliPickaxe.MODID);

	public static final BlockBuffAttackTNT loliBlueScreenTNT = new BlockBuffAttackTNT("LoliBlueScreenTNT", true, false, false);
	public static final BlockItem itemLoliBlueScreenTNT = new LoliBlockItem(loliBlueScreenTNT, null, true);
	public static final BlockBuffAttackTNT loliExitTNT = new BlockBuffAttackTNT("LoliExitTNT", false, true, false);
	public static final BlockItem itemLoliExitTNT = new LoliBlockItem(loliExitTNT, null, true);
	public static final BlockBuffAttackTNT loliFailRespondTNT = new BlockBuffAttackTNT("LoliFailRespondTNT", false, false, true);
	public static final BlockItem itemLoliFailRespondTNT = new LoliBlockItem(loliFailRespondTNT, null, true);
	public static final BlockLoliAltar loliAltar = new BlockLoliAltar();
	public static final BlockItem itemLoliAltar = new LoliBlockItem(loliAltar, "loliAltar.use", false);
	public static final BlockPasswordWorkBench passwordWorkBench = new BlockPasswordWorkBench();
	public static final BlockItem itemPasswordWorkBench = new BlockItem(passwordWorkBench, new Item.Properties());

	static {
		BLOCKS.register("loli_blue_screen_tnt", () -> loliBlueScreenTNT);
		ITEMS.register("loli_blue_screen_tnt", () -> itemLoliBlueScreenTNT);
		BLOCKS.register("loli_exit_tnt", () -> loliExitTNT);
		ITEMS.register("loli_exit_tnt", () -> itemLoliExitTNT);
		BLOCKS.register("loli_fail_respond_tnt", () -> loliFailRespondTNT);
		ITEMS.register("loli_fail_respond_tnt", () -> itemLoliFailRespondTNT);
		BLOCKS.register("loli_altar", () -> loliAltar);
		ITEMS.register("loli_altar", () -> itemLoliAltar);
		BLOCKS.register("password_work_bench", () -> passwordWorkBench);
		ITEMS.register("password_work_bench", () -> itemPasswordWorkBench);
	}

	/**
	 * 1.20.1 中方块物品的提示信息由 BlockItem 提供，这里补上原有提示。
	 */
	public static class LoliBlockItem extends BlockItem {

		@Nullable
		private final String tooltipKey;
		private final boolean buffAttackTNT;

		public LoliBlockItem(Block block, @Nullable String tooltipKey, boolean buffAttackTNT) {
			super(block, new Item.Properties());
			this.tooltipKey = tooltipKey;
			this.buffAttackTNT = buffAttackTNT;
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
			if (this.buffAttackTNT) {
				tooltip.add(Component.translatable(ConfigLoader.loliEnableBuffAttackTNT ? "buffAttackTNT.enable" : "buffAttackTNT.disable"));
			} else if (this.tooltipKey != null) {
				tooltip.add(Component.translatable(this.tooltipKey));
			}
		}

	}

}
