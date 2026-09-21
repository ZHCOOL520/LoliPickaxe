package com.anotherstar.common.gui;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface ILoliInventory extends Container {

	int getMaxPage();

	NonNullList<ItemStack> getPage(int index);

	boolean cancelStackLimit();

	/**
	 * 把一整个物品堆并入储藏室，返回<b>未能放入</b>的剩余物品。
	 *
	 * <p><b>为什么需要这个方法（关键修复）</b>：
	 * {@link #getPage(int)} 返回的是容器<b>内部</b>的 {@code NonNullList}。
	 * 调用方直接对它 {@code set}/{@code grow} 虽然能改到内存里的数据，
	 * 但<b>不会经过 {@link Container#setChanged()}</b>，因此该页不会被标记为「脏」。
	 * 而落盘时 {@code stopOpen} 对「未变脏的页」会直接复用读入时的原始 NBT 快照，
	 * 于是这些改动会被旧快照整份覆盖 —— 表现为「物品进了内存但存档与界面里都没有」。
	 *
	 * <p>自动收纳（连锁挖矿的掉落收集）正是走的这条路径，所以必须改为经由本方法写入，
	 * 由内部调用 {@link Container#setChanged()} 正确标记脏页。
	 *
	 * @param stack 待放入的物品堆（不会被修改）
	 * @return 未能放入的剩余物品；全部放入时返回 {@link ItemStack#EMPTY}
	 */
	ItemStack insertItem(ItemStack stack);

}
