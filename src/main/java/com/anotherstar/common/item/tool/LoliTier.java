package com.anotherstar.common.item.tool;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 对应原 1.12.2 的 EnumHelper.addToolMaterial("LOLI", 32, 0, 0, 0, 0)：
 * 挖掘等级 32、耐久 0、挖掘速度 0、攻击伤害 0、附魔等级 0。
 */
public class LoliTier implements Tier {

	@Override
	public int getUses() {
		return 0;
	}

	@Override
	public float getSpeed() {
		return 0.0F;
	}

	@Override
	public float getAttackDamageBonus() {
		return 0.0F;
	}

	// 重写目标方法本身已废弃（Forge 建议改用 TierSortingRegistry/getTag 表达等级关系），
	// 但这是 Tier 接口要求必须实现的方法，且没有能表达“等级 32”的替代写法，故压制警告。
	@Override
	@SuppressWarnings("deprecation")
	public int getLevel() {
		return 32;
	}

	@Override
	public int getEnchantmentValue() {
		return 0;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.EMPTY;
	}

}
