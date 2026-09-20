package com.anotherstar.common.entity;

/**
 * 萝莉实体的“消散”标记接口。
 * <p>
 * 对应原 1.12.2 的同名接口。语义：标记实体本次从世界移除是否为玩家主动“消散”
 * （{@code true} = 允许真正消失，{@code false} = 被视为意外移除，{@code EntityLoli#onRemovedFromWorld}
 * 会在原地补生成一只新萝莉）。
 * <p>
 * {@code LoliPickaxeUtil#invHaveLoliPickaxe} 会把实现本接口的实体一律视为“持有萝莉镐”，
 * 因此萝莉实体免疫萝莉镐的击杀与伤害。
 */
public interface IEntityLoli {

	/**
	 * 查询是否已处于“消散”状态。
	 *
	 * @return {@code true} 表示允许本次移除生效；{@code false} 表示移除后需要补生成新实体
	 */
	boolean isDispersal();

	/**
	 * 设置“消散”状态。
	 *
	 * @param value {@code true} 表示允许真正移除（由消散道具或跨维度传送设置）
	 */
	void setDispersal(boolean value);

}
