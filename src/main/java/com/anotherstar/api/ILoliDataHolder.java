package com.anotherstar.api;

/**
 * 由 Mixin 注入到 {@code LivingEntity} 上的数据接口，
 * 对应原 1.12.2 版本由 ASM 直接加到 EntityLivingBase 上的 loliDead / loliCool / loliDeathTime 字段。
 *
 * <p><b>为什么放在 {@code com.anotherstar.api} 而不是 {@code com.anotherstar.core.mixin}：</b>
 * Mixin 规定「被 mixin 配置拥有的包」中的类<b>不能被普通代码直接引用</b>，
 * 否则启动时抛出
 * {@code IllegalClassLoadError: ... is in a defined mixin package ... and cannot be referenced directly}。
 * 本接口需要被 {@code EventUtil}、{@code LoliPickaxeUtil} 等大量普通类使用，
 * 因此必须与 mixin 类分开放置。
 *
 * <p>读取路径（如 {@code getHealth()}）只使用 {@link #isLoliProtected()} 这个每 tick 缓存标记，
 * 避免在高频方法里反复扫描背包。
 */
public interface ILoliDataHolder {

	/**
	 * 实体是否处于「已被萝莉镐判定死亡」状态。
	 *
	 * @return true 表示该实体被萝莉镐标记为死亡，{@code getHealth()} 将返回 0
	 */
	boolean isLoliDead();

	/**
	 * 设置「已被萝莉镐判定死亡」状态。
	 *
	 * @param dead 是否标记为死亡
	 */
	void setLoliDead(boolean dead);

	/**
	 * 实体是否处于「冷却后强制清除」状态（延迟清除阶段）。
	 *
	 * @return true 表示进入强制清除流程
	 */
	boolean isLoliCool();

	/**
	 * 设置「冷却后强制清除」状态。
	 *
	 * @param cool 是否进入强制清除流程
	 */
	void setLoliCool(boolean cool);

	/**
	 * 获取萝莉镐专属的死亡计时（用于延迟清除，单位为 tick）。
	 *
	 * @return 已累计的死亡计时
	 */
	int getLoliDeathTime();

	/**
	 * 设置萝莉镐专属的死亡计时。
	 *
	 * @param time 死亡计时（tick）
	 */
	void setLoliDeathTime(int time);

	/**
	 * 本 tick 缓存的「是否持有属于自己、且已开启保护效果的萝莉镐」标记。
	 *
	 * @return true 表示本 tick 该实体应被视为受萝莉镐保护（无敌）
	 */
	boolean isLoliProtected();

	/**
	 * 刷新「受萝莉镐保护」缓存标记。
	 *
	 * @param protect 是否受保护
	 */
	void setLoliProtected(boolean protect);

}
