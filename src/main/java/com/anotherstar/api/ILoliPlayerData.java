package com.anotherstar.api;

/**
 * 由 Mixin 注入到 {@code Player} 上的数据接口，
 * 对应原 1.12.2 版本由 ASM 直接加到 EntityPlayer 上的 hodeLoli 字段。
 *
 * <p>与 {@link ILoliDataHolder} 同理，必须放在 mixin 包之外才能被普通代码引用，
 * 否则会触发 {@code IllegalClassLoadError}。
 */
public interface ILoliPlayerData {

	/**
	 * 获取「持有萝莉镐」的剩余计时。
	 *
	 * <p>该值由 {@code LoliPickaxeUtil#checkOwner} 在玩家持有本人萝莉镐时刷新为配置时长，
	 * 用于在萝莉镐短暂离开背包（如被放下、换手）时仍保持保护效果。
	 *
	 * @return 剩余计时（tick），0 表示未持有
	 */
	int getHodeLoli();

	/**
	 * 设置「持有萝莉镐」的剩余计时。
	 *
	 * @param hodeLoli 剩余计时（tick）
	 */
	void setHodeLoli(int hodeLoli);

}
