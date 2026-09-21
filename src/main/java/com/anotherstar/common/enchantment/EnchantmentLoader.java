package com.anotherstar.common.enchantment;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class EnchantmentLoader {

	public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, LoliPickaxe.MODID);

	/*
	 * 说明：Enchantment 的构造器内部并不调用 createIntrusiveHolder
	 * （已核对 Enchantment.java，其构造器只做字段赋值），因此它不是本模组启动崩溃的成因。
	 * 但为了与 ItemLoader / BlockLoader / EntityLoader 保持一致的「绝不在 <clinit> 中构造注册对象」约定，
	 * 这里同样改为懒加载形式 —— 这不改变任何行为，只是消除同类隐患。
	 */
	private static EnchantmentAutoFurnace loliAutoFurnaceInstance;

	/** @return 自动熔炼附魔（注册名 {@code loli_auto_furnace}，全局单例） */
	public static EnchantmentAutoFurnace loliAutoFurnace() {
		if (loliAutoFurnaceInstance == null) {
			loliAutoFurnaceInstance = new EnchantmentAutoFurnace();
		}
		return loliAutoFurnaceInstance;
	}

	static {
		ENCHANTMENTS.register("loli_auto_furnace", EnchantmentLoader::loliAutoFurnace);
	}

}
