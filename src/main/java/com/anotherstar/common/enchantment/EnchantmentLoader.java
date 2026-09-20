package com.anotherstar.common.enchantment;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class EnchantmentLoader {

	public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, LoliPickaxe.MODID);

	public static final EnchantmentAutoFurnace loliAutoFurnace = new EnchantmentAutoFurnace();

	static {
		ENCHANTMENTS.register("loli_auto_furnace", () -> loliAutoFurnace);
	}

}
