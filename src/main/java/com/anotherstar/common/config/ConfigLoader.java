package com.anotherstar.common.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.anotherstar.common.config.annotation.ConfigField;
import com.anotherstar.common.config.annotation.ConfigField.ConfigType;
import com.anotherstar.common.config.annotation.ConfigField.ValurType;
import com.anotherstar.common.item.tool.ILoli;
import com.anotherstar.network.LoliConfigPacket;
import com.anotherstar.network.NetworkHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 注解 + 反射驱动的配置中心。
 *
 * <p><b>职责</b>：本类既是"配置字段的声明处"，也是"配置系统本身"。
 * 每个 {@code public static} 配置字段都带 {@link ConfigField} 注解，静态初始化块反射扫描它们，
 * 建立下面四个索引容器；之后：
 * <ul>
 *   <li>{@link #init()} 依据注解动态构造 {@code ForgeConfigSpec}，由主类
 *       {@code LoliPickaxe} 在构造阶段注册为 {@code lolipickaxe-common.toml}；</li>
 *   <li>{@link #load(boolean)} / {@link #save()} 在"配置文件 &lt;-&gt; 静态字段"之间双向同步；</li>
 *   <li>{@link #sandChange}/{@link #receptionChange} 通过 {@code LoliConfigPacket} 把服务端生效的配置
 *       同步给客户端（客户端只用于显示与本地效果，不落盘）；</li>
 *   <li>{@link #getInt}/{@link #getDouble}/{@link #getBoolean}/{@link #getString} 提供
 *       "物品 NBT 覆盖 &gt; 全局配置"的两级读取，供萝莉镐的每把镐独立配置使用。</li>
 * </ul>
 *
 * <p>对应原 1.12.2 版本中基于 {@code @LoliConfig} 注解 + Configuration 的同类实现。
 */
public class ConfigLoader {

	/** 配置系统日志器。 */
	private static final Logger LOGGER = LogManager.getLogger("LoliPickaxe");

	/** 由 {@link #init()} 构建出来的配置规格，{@link #save()} 通过它把内存值写回 toml 文件。 */
	private static ForgeConfigSpec spec;

	/**
	 * 字段名 -&gt; 该字段在 ForgeConfigSpec 中对应的配置项。
	 *
	 * <p>它是 {@link #load(boolean)} 从文件读值、{@link #save()} 向文件写值的唯一桥梁：
	 * 静态字段只负责在内存中被代码读取，真正持久化的载体是这里的 {@code ConfigValue}。
	 */
	private static final Map<String, ForgeConfigSpec.ConfigValue<?>> configValues = Maps.newHashMap();

	/**
	 * 全部带 {@link ConfigField} 注解的字段名，顺序与 {@code ConfigLoader.class.getFields()} 一致，
	 * 也就是 toml 文件中的条目顺序。它是其余索引与遍历逻辑的"主键列表"。
	 */
	public static final List<String> flags = Lists.newArrayList();

	/** {@link ConfigField#type()} 中包含 COMMAND 的字段名，供 {@code /lolipickaxe config} 命令枚举可改项。 */
	public static final List<String> commandFlags = Lists.newArrayList();

	/** {@link ConfigField#type()} 中包含 GUI 的字段名，供萝莉镐 GUI 枚举可改项（这些项才能写进物品 NBT）。 */
	public static final List<String> guiFlags = Lists.newArrayList();

	/** 字段名 -&gt; 其 {@link ConfigField} 注解实例，避免在热路径上反复反射取注解。 */
	public static final Map<String, ConfigField> flagAnnotations = Maps.newHashMap();

	/** 字段名 -&gt; 对应的 {@link Field} 反射句柄，用于读写静态字段（{@code field.setXxx(null, ...)}）。 */
	public static final Map<String, Field> flagFields = Maps.newHashMap();

	// ------------------------------------------------------------------
	// 配置字段声明区。
	// 下面的每个字段都由 @ConfigField 描述：comment 即中文名称，*DefaultValue 为写进 toml 的默认值，
	// type() 里是否含 CONFIG / COMMAND / GUI 决定它能否落盘、能否用命令改、能否在 GUI 里改；
	// GUI 可改项还会被写入物品 NBT，实现"每把萝莉镐独立配置"。
	// 单位约定：范围/距离为方块，时间为 tick（1 秒 = 20 tick），毫秒类字段在 comment 中特别标注。
	// ------------------------------------------------------------------
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "最大采掘范围", valueType = ValurType.INT, intDefaultValue = 5)
	public static int loliPickaxeMaxRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "强制掉落方块", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeMandatoryDrop;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "显示流体边框", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeStopOnLiquid;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "挖掘距离", valueType = ValurType.DOUBLE, doubleDefaultValue = 0.0, doubleMinValue = 0, doubleMaxValueField = "loliPickaxeBlockReachMaxDistance")
	public static double loliPickaxeBlockReachDistance;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "最大挖掘距离", valueType = ValurType.DOUBLE, doubleDefaultValue = 20.0)
	public static double loliPickaxeBlockReachMaxDistance;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "储藏室最大页数", valueType = ValurType.INT, intDefaultValue = 100)
	public static int loliPickaxeMaxPage;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "储藏室取消物品堆叠限制", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeCancelStackLimit;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "储藏室最大堆叠数", valueType = ValurType.INT, intDefaultValue = 2000000000)
	public static int loliPickaxeSlotStackLimit;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "自动收纳进储藏室", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeAutoAccept;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "反伤", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeThorns;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "潜行右键杀死周围实体", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeKillRangeEntity;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "潜行右键杀死周围实体的范围", valueType = ValurType.INT, intDefaultValue = 50, intMinValue = 0, intMaxValueField = "loliPickaxeMaxKillRange")
	public static int loliPickaxeKillRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "潜行右键杀死周围实体的最大范围", valueType = ValurType.INT, intDefaultValue = 100)
	public static int loliPickaxeMaxKillRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "自动杀死周围实体", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeAutoKillRangeEntity;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "自动杀死周围实体的范围", valueType = ValurType.INT, intDefaultValue = 5, intMinValue = 0, intMaxValueField = "loliPickaxeMaxAutoKillRange")
	public static int loliPickaxeAutoKillRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "自动杀死周围实体的最大范围", valueType = ValurType.INT, intDefaultValue = 10)
	public static int loliPickaxeMaxAutoKillRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "效果持续时间(Tick)", valueType = ValurType.INT, intDefaultValue = 200)
	public static int loliPickaxeDuration;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "丢弃保护时间(ms)", valueType = ValurType.INT, intDefaultValue = 200)
	public static int loliPickaxeDropProtectTime;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "强制清除生物", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeCompulsoryRemove;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "范围攻击对非怪物有效", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeValidToAmityEntity;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "对全部实体有效", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeValidToAllEntity;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "清空玩家背包", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeClearInventory;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "缴械", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeDropItems;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "踢出玩家", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeKickPlayer;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "踢出玩家消息", valueType = ValurType.STRING, stringDefaultValue = "你被氪金萝莉踢出了服务器")
	public static String loliPickaxeKickMessage;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "禁止死亡实体触发实体更新事件", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeForbidOnLivingUpdate;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "伊邪那美(需同时开启踢出玩家)", valueType = ValurType.BOOLEAN, booleanDefaultValue = false, warning = true)
	public static boolean loliPickaxeReincarnation;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "伊邪那美玩家列表", valueType = ValurType.LIST, listDefaultValue = {})
	public static List<String> loliPickaxeReincarnationPlayerList;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "灵魂超度", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeBeyondRedemption;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "灵魂超度玩家列表", valueType = ValurType.LIST, listDefaultValue = {})
	public static List<String> loliPickaxeBeyondRedemptionPlayerList;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "寻找所有者", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeFindOwner;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "寻找所有者范围", valueType = ValurType.INT, intDefaultValue = 50)
	public static int loliPickaxeFindOwnerRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "蓝屏打击", valueType = ValurType.BOOLEAN, booleanDefaultValue = false, warning = true)
	public static boolean loliPickaxeBlueScreenAttack;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "蹦溃打击", valueType = ValurType.BOOLEAN, booleanDefaultValue = false, warning = true)
	public static boolean loliPickaxeExitAttack;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "未响应打击", valueType = ValurType.BOOLEAN, booleanDefaultValue = false, warning = true)
	public static boolean loliPickaxeFailRespondAttack;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "强制死亡延迟特化列表(实体ID:Tick)", valueType = ValurType.MAP, mapDefaultValue = { "ender_dragon:::201" }, mapKeyType = ValurType.STRING, mapValueType = ValurType.INT)
	public static Map<String, Integer> loliPickaxeDelayRemoveList;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "左键范围攻击", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeKillFacing;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "范围攻击范围", valueType = ValurType.INT, intDefaultValue = 50, intMinValue = 0, intMaxValueField = "loliPickaxeMaxKillFacingRange")
	public static int loliPickaxeKillFacingRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "范围攻击最大范围", valueType = ValurType.INT, intDefaultValue = 200)
	public static int loliPickaxeMaxKillFacingRange;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "范围攻击斜率", valueType = ValurType.DOUBLE, doubleDefaultValue = 0.1, doubleMinValue = 0, doubleMaxValueField = "loliPickaxeMaxKillFacingSlope")
	public static double loliPickaxeKillFacingSlope;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "范围攻击最大斜率", valueType = ValurType.DOUBLE, doubleDefaultValue = 1.0)
	public static double loliPickaxeMaxKillFacingSlope;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "视觉迷惑", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliPickaxeInvisible;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "GUI可修改选项", valueType = ValurType.LIST, listType = ValurType.STRING, listDefaultValue = { "loliPickaxeMandatoryDrop", "loliPickaxeStopOnLiquid", "loliPickaxeBlockReachDistance", "loliPickaxeAutoAccept", "loliPickaxeThorns", "loliPickaxeKillRangeEntity", "loliPickaxeKillRange", "loliPickaxeAutoKillRangeEntity", "loliPickaxeAutoKillRange", "loliPickaxeCompulsoryRemove", "loliPickaxeValidToAmityEntity", "loliPickaxeValidToAllEntity", "loliPickaxeClearInventory", "loliPickaxeDropItems", "loliPickaxeKickPlayer", "loliPickaxeKickMessage", "loliPickaxeReincarnation", "loliPickaxeBeyondRedemption", "loliPickaxeBlueScreenAttack", "loliPickaxeExitAttack", "loliPickaxeFailRespondAttack", "loliPickaxeKillFacing", "loliPickaxeKillFacingRange", "loliPickaxeKillFacingSlope", "loliPickaxeInfiniteBattery", "loliPickaxeInvisible", "loliPickaxeShowInvisible" }, warning = true, warningMethod = "guiChangeListWarning")
	public static List<String> loliPickaxeGuiChangeList;
	@ConfigField(type = {}, comment = "额外唱片列表(声音:唱片名:唱片ID)", valueType = ValurType.LIST, listType = ValurType.STRING, listDefaultValue = { "lolirecord:loliRecord:loli_record" })
	public static List<String> loliRecodeNames;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉卡片掉落概率", valueType = ValurType.DOUBLE, doubleDefaultValue = 0.1)
	public static double loliCardDropProbability;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉卡片掉落概率", valueType = ValurType.DOUBLE, doubleDefaultValue = 0.01)
	public static double loliCardAlbumDropProbability;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉唱片掉落概率", valueType = ValurType.DOUBLE, doubleDefaultValue = 0.001)
	public static double loliRecordDropProbability;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "生物灵魂掉落概率", valueType = ValurType.DOUBLE, doubleDefaultValue = 0.01)
	public static double entitySoulDropProbability;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉行走速度", valueType = ValurType.DOUBLE, doubleDefaultValue = 1.0)
	public static double loliSpeed;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉主动攻击", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliAttack;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉瞬移", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliTeleport;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉模型(0:萝莉,1:纳文摩尔,2:纸片人,3:车万女仆)", valueType = ValurType.INT, intDefaultValue = 0)
	public static int loliModelType;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉模型ID", valueType = ValurType.STRING, stringDefaultValue = "touhou_little_maid:remilia_scarlet")
	public static String loliModelId;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉卡片展示框缩放比例", valueType = ValurType.DOUBLE, doubleDefaultValue = 1.0)
	public static double loliCardScale;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉卡片册切换速度", valueType = ValurType.INT, intDefaultValue = 100)
	public static int loliCardAlbumSwitchSpeed;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "萝莉卡片渲染展示框", valueType = ValurType.BOOLEAN, booleanDefaultValue = false)
	public static boolean loliCardRenderFrame;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "附魔最大等级列表", valueType = ValurType.MAP, mapDefaultValue = {}, mapKeyType = ValurType.STRING, mapValueType = ValurType.INT)
	public static Map<String, Integer> loliPickaxeEnchantmentLimit;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "默认附魔最大等级", valueType = ValurType.INT, intDefaultValue = 32)
	public static int loliPickaxeEnchantmentDefaultLimit;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "药水最大等级列表", valueType = ValurType.MAP, mapDefaultValue = {}, mapKeyType = ValurType.STRING, mapValueType = ValurType.INT)
	public static Map<String, Integer> loliPickaxePotionLimit;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "默认药水最大等级", valueType = ValurType.INT, intDefaultValue = 32)
	public static int loliPickaxePotionDefaultLimit;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "启用特效攻击炸弹", valueType = ValurType.BOOLEAN, booleanDefaultValue = false, warning = true)
	public static boolean loliEnableBuffAttackTNT;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "超级电池", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeInfiniteBattery;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "跨世界传送", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeSpaceFolding;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "跨世界传送黑名单", valueType = ValurType.LIST, listType = ValurType.INT, listDefaultValue = {})
	public static List<Integer> loliPickaxeWorldBlacklist;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "传送最远距离", valueType = ValurType.DOUBLE, doubleDefaultValue = 512.0)
	public static double loliPickaxeMaxTeleportDistance;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "萝莉卡片URL", valueType = ValurType.MAP, mapDefaultValue = { "gk_head_portrait.png:::https://www.pixiv.net/artworks/61282195", "小莫女儿:::https://www.pixiv.net/users/5776001" }, mapKeyType = ValurType.STRING, mapValueType = ValurType.STRING)
	public static Map<String, String> loliCardURL;
	@ConfigField(type = { ConfigType.CONFIG }, comment = "创造模式物品栏默认网络卡片", valueType = ValurType.LIST, listType = ValurType.STRING, listDefaultValue = { "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2017/03/18/03/44/39/61965296_p0.png", "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2015/10/23/18/05/06/53170539_p0.jpg", "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2015/09/27/07/15/20/52735806_p0.jpg" })
	public static List<String> loliCardOnlineDefURL;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND, ConfigType.GUI }, comment = "显示隐身生物", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeShowInvisible;
	@ConfigField(type = { ConfigType.CONFIG, ConfigType.COMMAND }, comment = "触发方块破坏事件", valueType = ValurType.BOOLEAN, booleanDefaultValue = true)
	public static boolean loliPickaxeTriggerBreakEvent;

	/**
	 * 反射扫描本类的全部 {@code public static} 字段，把带 {@link ConfigField} 注解的字段登记进
	 * {@link #flags}/{@link #flagAnnotations}/{@link #flagFields}，并按渠道分发到
	 * {@link #commandFlags}/{@link #guiFlags}。
	 *
	 * <p>{@code getFields()} 返回的顺序即字段声明顺序，因此 {@link #flags} 同时也是 toml 里的条目顺序。
	 */
	static {
		try {
			Field[] fields = ConfigLoader.class.getFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(ConfigField.class)) {
					ConfigField annotation = field.getAnnotation(ConfigField.class);
					flags.add(field.getName());
					flagAnnotations.put(field.getName(), annotation);
					flagFields.put(field.getName(), field);
					ConfigType[] types = annotation.type();
					for (ConfigType type : types) {
						switch (type) {
						case COMMAND:
							commandFlags.add(field.getName());
							break;
						case GUI:
							guiFlags.add(field.getName());
							break;
						default:
							break;
						}
					}
				}
			}
		} catch (IllegalArgumentException e) {
			LOGGER.error("Failed to scan config fields", e);
		}
	}

	/**
	 * 依据 {@link ConfigField} 注解动态构建 ForgeConfigSpec，由主类在构造阶段注册。
	 *
	 * <p>副作用：会清空并重新填充 {@link #configValues}，并给 {@link #spec} 赋值。
	 * 需在主类的 {@code registerConfig} 之前调用一次（且仅一次）。
	 *
	 * @return 构建好的配置规格，交由 Forge 解析 {@code lolipickaxe-common.toml}
	 */
	public static ForgeConfigSpec init() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		for (String flag : flags) {
			ConfigField annotation = flagAnnotations.get(flag);
			builder.comment(annotation.comment());
			switch (annotation.valueType()) {
			case INT:
				configValues.put(flag, builder.define(flag, annotation.intDefaultValue()));
				break;
			case DOUBLE:
				configValues.put(flag, builder.define(flag, annotation.doubleDefaultValue()));
				break;
			case BOOLEAN:
				configValues.put(flag, builder.define(flag, annotation.booleanDefaultValue()));
				break;
			case STRING:
				configValues.put(flag, builder.define(flag, annotation.stringDefaultValue()));
				break;
			case LIST:
				// 列表统一按字符串列表落盘（toml 数组），读取时再由 parseTypedList 还原成真实类型
				configValues.put(flag, builder.defineListAllowEmpty(flag, Lists.newArrayList(annotation.listDefaultValue()), o -> o instanceof String));
				break;
			case MAP:
				// 映射同样以字符串列表落盘，每条形如 "键:::值"，避免 toml 无法直接表达异构键值
				configValues.put(flag, builder.defineListAllowEmpty(flag, Lists.newArrayList(annotation.mapDefaultValue()), o -> o instanceof String));
				break;
			default:
				break;
			}
		}
		spec = builder.build();
		return spec;
	}

	/**
	 * 把 {@link #configValues}（即配置文件中的值）刷进对应的静态字段。
	 *
	 * <p>由 {@code CommonProxy#onConfigLoad()} / {@code onConfigReload()} 在 Forge 的
	 * {@code ModConfigEvent.Loading} / {@code Reloading} 时调用。
	 *
	 * <p>副作用：直接改写本类的静态字段，从而影响全服所有已加载逻辑。
	 *
	 * @param reload true 表示这是一次热重载（{@code /reload} 或改文件后重载）。
	 *               此时只同步 {@link ConfigType#CONFIG} 字段，跳过纯 COMMAND / GUI 字段，
	 *               避免把运行时才生效的临时值覆盖掉
	 */
	public static void load(boolean reload) {
		for (String flag : flags) {
			Field field = flagFields.get(flag);
			ConfigField annotation = flagAnnotations.get(flag);
			if (reload) {
				boolean canReload = false;
				for (ConfigType type : annotation.type()) {
					if (type == ConfigType.CONFIG) {
						canReload = true;
						break;
					}
				}
				if (!canReload) {
					continue;
				}
			}
			ForgeConfigSpec.ConfigValue<?> value = configValues.get(flag);
			if (value == null) {
				continue;
			}
			Object loaded;
			try {
				loaded = value.get();
			} catch (Exception e) {
				// 配置尚未被 Forge 加载（例如早期的 tick 就触发了 reload），跳过本次同步即可
				continue;
			}
			try {
				switch (annotation.valueType()) {
				case INT:
					field.setInt(null, ((Number) loaded).intValue());
					break;
				case DOUBLE:
					field.setDouble(null, ((Number) loaded).doubleValue());
					break;
				case BOOLEAN:
					field.setBoolean(null, (Boolean) loaded);
					break;
				case STRING:
					field.set(null, loaded);
					break;
				case LIST:
					field.set(null, parseTypedList(annotation.listType(), (List<?>) loaded));
					break;
				case MAP:
					field.set(null, parseTypedMap(annotation, (List<?>) loaded));
					break;
				default:
					break;
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Failed to load config field {}", flag, e);
			} catch (IllegalAccessException e) {
				LOGGER.error("Failed to load config field {}", flag, e);
			}
		}
	}

	/**
	 * 把配置文件里读到的字符串列表还原成注解声明的真实元素类型。
	 *
	 * @param listType 元素的目标类型（来自 {@link ConfigField#listType()}）
	 * @param raw      从 ForgeConfigSpec 读到的原始列表，元素通常是 String；可为 {@code null}
	 * @return 转换后的可变列表；{@code raw} 为 {@code null} 时返回空列表（不会返回 {@code null}）。
	 *         无法识别的 {@code listType} 会静默跳过对应元素
	 */
	private static List<Object> parseTypedList(ValurType listType, List<?> raw) {
		List<Object> list = Lists.newArrayList();
		if (raw == null) {
			return list;
		}
		for (Object element : raw) {
			String str = String.valueOf(element);
			switch (listType) {
			case INT:
				list.add(Integer.parseInt(str));
				break;
			case DOUBLE:
				list.add(Double.parseDouble(str));
				break;
			case BOOLEAN:
				list.add(Boolean.parseBoolean(str));
				break;
			case STRING:
				list.add(str);
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * 把配置文件里的 {@code "键:::值"} 字符串列表还原成 Map。
	 *
	 * @param annotation 该字段的注解，提供 {@link ConfigField#mapKeyType()} / {@link ConfigField#mapValueType()}
	 * @param raw        原始字符串列表；可为 {@code null}
	 * @return 还原后的可变 Map；{@code raw} 为 {@code null} 时返回空 Map（不会返回 {@code null}）
	 */
	private static Map<Object, Object> parseTypedMap(ConfigField annotation, List<?> raw) {
		Map<Object, Object> map = Maps.newHashMap();
		if (raw == null) {
			return map;
		}
		for (Object element : raw) {
			String str = String.valueOf(element);
			int index = str.lastIndexOf(":::");
			if (index < 0) {
				continue;
			}
			Object key = parseValue(annotation.mapKeyType(), str.substring(0, index));
			Object value = parseValue(annotation.mapValueType(), str.substring(index + 3));
			if (key != null && value != null) {
				map.put(key, value);
			}
		}
		return map;
	}

	/**
	 * 把单个字符串按指定类型解析（{@link #parseTypedMap} 与 {@code receptionChange} 共用）。
	 *
	 * @param type 目标类型
	 * @param str  待解析文本
	 * @return 解析结果；{@code type} 不是标量类型（LIST/MAP/NONE）时返回 {@code null}
	 * @throws NumberFormatException int/double 解析失败时抛出（调用方不捕获，属于配置写错）
	 */
	private static Object parseValue(ValurType type, String str) {
		switch (type) {
		case INT:
			return Integer.parseInt(str);
		case DOUBLE:
			return Double.parseDouble(str);
		case BOOLEAN:
			return Boolean.parseBoolean(str);
		case STRING:
			return str;
		default:
			return null;
		}
	}

	/**
	 * 把静态字段的当前值写回 {@link #configValues} 并落盘到 toml 文件。
	 *
	 * <p>副作用：写文件。仅处理 {@link ConfigType#CONFIG} 字段；运行时由命令/GUI 改过的
	 * COMMAND / GUI 专属字段不会被持久化。
	 *
	 * <p>被 {@link #sandChange} 之外的所有"修改全局配置"入口调用，例如
	 * {@link #addPlayerToReincarnation} / {@link #addPlayerToBeyondRedemption}。
	 */
	public static void save() {
		for (String flag : flags) {
			Field field = flagFields.get(flag);
			ConfigField annotation = flagAnnotations.get(flag);
			boolean canSave = false;
			for (ConfigType type : annotation.type()) {
				if (type == ConfigType.CONFIG) {
					canSave = true;
					break;
				}
			}
			if (!canSave) {
				continue;
			}
			@SuppressWarnings("rawtypes")
			ForgeConfigSpec.ConfigValue value = configValues.get(flag);
			if (value == null) {
				continue;
			}
			try {
				switch (annotation.valueType()) {
				case INT:
					value.set(field.getInt(null));
					break;
				case DOUBLE:
					value.set(field.getDouble(null));
					break;
				case BOOLEAN:
					value.set(field.getBoolean(null));
					break;
				case STRING:
					value.set(field.get(null));
					break;
				case LIST:
					value.set(((List<?>) field.get(null)).stream().map(Object::toString).collect(Collectors.toList()));
					break;
				case MAP:
					value.set(((Map<?, ?>) field.get(null)).entrySet().stream().map(entry -> entry.getKey().toString() + ":::" + entry.getValue().toString()).collect(Collectors.toList()));
					break;
				default:
					break;
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Failed to save config field {}", flag, e);
			} catch (IllegalAccessException e) {
				LOGGER.error("Failed to save config field {}", flag, e);
			} catch (IllegalStateException e) {
				LOGGER.error("Config not loaded yet, skip saving {}", flag);
			}
		}
		try {
			spec.save();
		} catch (IllegalStateException e) {
			LOGGER.error("Config not loaded yet, skip saving file");
		}
	}

	/**
	 * 把玩家加入"伊邪那美"（轮回重生）名单，下次登录时会被清空数据。
	 *
	 * <p>副作用：修改 {@code loliPickaxeReincarnationPlayerList} 并写配置文件。
	 *
	 * @param player 目标玩家
	 */
	public static void addPlayerToReincarnation(Player player) {
		addPlayerToReincarnation(player.getUUID().toString());
	}

	/**
	 * 把玩家 UUID 加入"伊邪那美"名单。
	 *
	 * <p>副作用：修改 {@code loliPickaxeReincarnationPlayerList} 并调用 {@link #save()} 落盘；
	 * 已在名单中时什么都不做（幂等）。
	 *
	 * @param uuid 玩家 UUID 的字符串形式
	 */
	public static void addPlayerToReincarnation(String uuid) {
		if (!loliPickaxeReincarnationPlayerList.contains(uuid)) {
			loliPickaxeReincarnationPlayerList.add(uuid);
			save();
		}
	}

	/**
	 * 把玩家加入"灵魂超度"名单（其生命值会被 {@code EventUtil.getHealth} 恒判为 0）。
	 *
	 * <p>副作用：修改 {@code loliPickaxeBeyondRedemptionPlayerList} 并写配置文件。
	 *
	 * @param player 目标玩家
	 */
	public static void addPlayerToBeyondRedemption(Player player) {
		addPlayerToBeyondRedemption(player.getUUID().toString());
	}

	/**
	 * 把玩家 UUID 加入"灵魂超度"名单。
	 *
	 * <p>副作用：修改 {@code loliPickaxeBeyondRedemptionPlayerList} 并调用 {@link #save()} 落盘；幂等。
	 *
	 * @param uuid 玩家 UUID 的字符串形式
	 */
	public static void addPlayerToBeyondRedemption(String uuid) {
		if (!loliPickaxeBeyondRedemptionPlayerList.contains(uuid)) {
			loliPickaxeBeyondRedemptionPlayerList.add(uuid);
			save();
		}
	}

	/**
	 * 把本类当前全部静态字段打包成 NBT 并下发给客户端（配置同步的"发送端"）。
	 *
	 * <p>LIST 以字符串列表写入，MAP 以 {@code "键:::值"} 字符串列表写入，与 toml 的存储格式保持一致，
	 * 便于 {@link #receptionChange} 直接复用同一套解析逻辑。
	 *
	 * <p>副作用：会发送 {@code LoliConfigPacket} 网络包。
	 *
	 * @param player 目标玩家；为 {@code null} 时广播给所有在线玩家
	 */
	public static void sandChange(ServerPlayer player) {
		CompoundTag data = new CompoundTag();
		for (String flag : flags) {
			Field field = flagFields.get(flag);
			ConfigField annotation = flagAnnotations.get(flag);
			try {
				switch (annotation.valueType()) {
				case INT:
					data.putInt(field.getName(), field.getInt(null));
					break;
				case DOUBLE:
					data.putDouble(field.getName(), field.getDouble(null));
					break;
				case BOOLEAN:
					data.putBoolean(field.getName(), field.getBoolean(null));
					break;
				case STRING:
					data.putString(field.getName(), (String) field.get(null));
					break;
				case LIST: {
					ListTag list = new ListTag();
					for (Object element : (List<?>) field.get(null)) {
						list.add(StringTag.valueOf(element.toString()));
					}
					data.put(field.getName(), list);
					break;
				}
				case MAP: {
					ListTag list = new ListTag();
					for (Map.Entry<?, ?> entry : ((Map<?, ?>) field.get(null)).entrySet()) {
						list.add(StringTag.valueOf(entry.getKey().toString() + ":::" + entry.getValue().toString()));
					}
					data.put(field.getName(), list);
					break;
				}
				default:
					break;
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Failed to read config field {}", flag, e);
			} catch (IllegalAccessException e) {
				LOGGER.error("Failed to read config field {}", flag, e);
			}
		}
		if (player == null) {
			NetworkHandler.sendToAll(new LoliConfigPacket(data));
		} else {
			NetworkHandler.sendToPlayer(new LoliConfigPacket(data), player);
		}
	}

	/**
	 * 接收服务端下发的配置 NBT 并写入本类静态字段（配置同步的"接收端"）。
	 *
	 * <p>与 {@link #load(boolean)} 的区别：数据来源是网络包而非 toml，且这里不会过滤
	 * {@link ConfigType#CONFIG}；客户端据此把本地静态字段对齐到服务端设置，仅用于显示与本地效果。
	 *
	 * <p>副作用：改写本类静态字段；对 {@code warning = true} 的字段还会在客户端聊天栏弹提示。
	 * 仅客户端调用。
	 *
	 * @param data 由 {@link #sandChange} 打包的 NBT，键名为字段名
	 */
	@OnlyIn(Dist.CLIENT)
	public static void receptionChange(CompoundTag data) {
		for (String flag : flags) {
			Field field = flagFields.get(flag);
			ConfigField annotation = flagAnnotations.get(flag);
			try {
				switch (annotation.valueType()) {
				case INT:
					field.setInt(null, data.getInt(field.getName()));
					if (annotation.warning()) {
						showWarning(annotation, Component.translatable("config.worning.int", annotation.comment(), data.getInt(field.getName())));
					}
					break;
				case DOUBLE:
					field.setDouble(null, data.getDouble(field.getName()));
					if (annotation.warning()) {
						showWarning(annotation, Component.translatable("config.worning.double", annotation.comment(), data.getDouble(field.getName())));
					}
					break;
				case BOOLEAN:
					field.setBoolean(null, data.getBoolean(field.getName()));
					if (annotation.warning()) {
						showWarning(annotation, Component.translatable(data.getBoolean(field.getName()) ? "config.worning.boolean.enable" : "config.worning.boolean.disable", annotation.comment()));
					}
					break;
				case STRING:
					field.set(null, data.getString(field.getName()));
					if (annotation.warning()) {
						showWarning(annotation, Component.translatable("config.worning.string", annotation.comment(), data.getString(field.getName())));
					}
					break;
				case LIST: {
					// 8 = Tag.TAG_STRING：列表元素统一按字符串传输，这里再按 listType 还原成真实类型
					ListTag nbtlist = data.getList(field.getName(), 8);
					List<Object> list = Lists.newArrayList();
					for (Tag nbt : nbtlist) {
						String str = nbt.getAsString();
						switch (annotation.listType()) {
						case INT:
							list.add(Integer.parseInt(str));
							break;
						case DOUBLE:
							list.add(Double.parseDouble(str));
							break;
						case BOOLEAN:
							list.add(Boolean.parseBoolean(str));
							break;
						case STRING:
							list.add(str);
							break;
						default:
							break;
						}
					}
					field.set(null, list);
					if (annotation.warning()) {
						if (!invokeWarningMethod(annotation)) {
							printChat(Component.translatable("config.worning.list", annotation.comment()));
							for (Object element : list) {
								printChat(Component.translatable("config.worning.list.element", element.toString()));
							}
						}
					}
					break;
				}
				case MAP: {
					ListTag list = data.getList(field.getName(), 8);
					Map<Object, Object> map = Maps.newHashMap();
					for (Tag nbt : list) {
						String str = nbt.getAsString();
						int index = str.lastIndexOf(":::");
						if (index < 0) {
							continue;
						}
						Object key = parseValue(annotation.mapKeyType(), str.substring(0, index));
						Object value = parseValue(annotation.mapValueType(), str.substring(index + 3));
						if (key != null && value != null) {
							map.put(key, value);
						}
					}
					field.set(null, map);
					if (annotation.warning()) {
						if (!invokeWarningMethod(annotation)) {
							printChat(Component.translatable("config.worning.map", annotation.comment()));
							for (Tag nbt : list) {
								printChat(Component.translatable("config.worning.map.element", nbt.getAsString()));
							}
						}
					}
					break;
				}
				default:
					break;
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Failed to apply config field {}", flag, e);
			} catch (IllegalAccessException e) {
				LOGGER.error("Failed to apply config field {}", flag, e);
			}
		}
	}

	private static void showWarning(ConfigField annotation, Component message) {
		if (!invokeWarningMethod(annotation)) {
			printChat(message);
		}
	}

	private static boolean invokeWarningMethod(ConfigField annotation) {
		if (annotation.warningMethod().isEmpty()) {
			return false;
		}
		try {
			ConfigLoader.class.getMethod(annotation.warningMethod()).invoke(null);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		} catch (SecurityException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static void printChat(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(message, false);
		}
	}

	public static int getInt(ItemStack stack, String flag) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.INT) {
				if (hasStackFlag(stack, flag)) {
					int max;
					if (flags.contains(annotation.intMaxValueField())) {
						max = getInt(stack, annotation.intMaxValueField());
					} else {
						max = annotation.intMaxValue();
					}
					int min;
					if (flags.contains(annotation.intMinValueField())) {
						min = getInt(stack, annotation.intMinValueField());
					} else {
						min = annotation.intMinValue();
					}
					return Mth.clamp(stack.getTag().getCompound(ILoli.CONFIG).getInt(flag), min, max);
				} else {
					try {
						return flagFields.get(flag).getInt(null);
					} catch (IllegalArgumentException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					} catch (IllegalAccessException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					}
				}
			}
		}
		return 0;
	}

	public static double getDouble(ItemStack stack, String flag) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.DOUBLE) {
				if (hasStackFlag(stack, flag)) {
					double max;
					if (flags.contains(annotation.doubleMaxValueField())) {
						max = getDouble(stack, annotation.doubleMaxValueField());
					} else {
						max = annotation.doubleMaxValue();
					}
					double min;
					if (flags.contains(annotation.doubleMinValueField())) {
						min = getDouble(stack, annotation.doubleMinValueField());
					} else {
						min = annotation.doubleMinValue();
					}
					return Mth.clamp(stack.getTag().getCompound(ILoli.CONFIG).getDouble(flag), min, max);
				} else {
					try {
						return flagFields.get(flag).getDouble(null);
					} catch (IllegalArgumentException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					} catch (IllegalAccessException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					}
				}
			}
		}
		return 0;
	}

	public static boolean getBoolean(ItemStack stack, String flag) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.BOOLEAN) {
				if (hasStackFlag(stack, flag)) {
					return stack.getTag().getCompound(ILoli.CONFIG).getBoolean(flag);
				} else {
					try {
						return flagFields.get(flag).getBoolean(null);
					} catch (IllegalArgumentException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					} catch (IllegalAccessException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					}
				}
			}
		}
		return false;
	}

	public static String getString(ItemStack stack, String flag) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.STRING) {
				if (hasStackFlag(stack, flag)) {
					return stack.getTag().getCompound(ILoli.CONFIG).getString(flag);
				} else {
					try {
						return (String) flagFields.get(flag).get(null);
					} catch (IllegalArgumentException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					} catch (IllegalAccessException e) {
						LOGGER.error("Failed to get config field {}", flag, e);
					}
				}
			}
		}
		return null;
	}

	private static boolean hasStackFlag(ItemStack stack, String flag) {
		return guiFlags.contains(flag) && loliPickaxeGuiChangeList.contains(flag) && !stack.isEmpty() && stack.getItem() instanceof ILoli && stack.hasTag() && stack.getTag().contains(ILoli.CONFIG) && stack.getTag().getCompound(ILoli.CONFIG).contains(flag);
	}

	public static CompoundTag getItemConfigs(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ILoli && stack.hasTag() && stack.getTag().contains(ILoli.CONFIG)) {
			return stack.getTag().getCompound(ILoli.CONFIG);
		}
		return null;
	}

	private static CompoundTag getOrCreateStackFlags(ItemStack stack) {
		CompoundTag nbt;
		if (!stack.hasTag()) {
			nbt = new CompoundTag();
			stack.setTag(nbt);
		} else {
			nbt = stack.getTag();
		}
		CompoundTag stackFlags;
		if (nbt.contains(ILoli.CONFIG)) {
			stackFlags = nbt.getCompound(ILoli.CONFIG);
		} else {
			stackFlags = new CompoundTag();
			nbt.put(ILoli.CONFIG, stackFlags);
		}
		return stackFlags;
	}

	public static void setInt(ItemStack stack, String flag, int value) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.INT && guiFlags.contains(flag) && loliPickaxeGuiChangeList.contains(flag) && !stack.isEmpty() && stack.getItem() instanceof ILoli) {
				int max;
				if (flags.contains(annotation.intMaxValueField())) {
					max = getInt(stack, annotation.intMaxValueField());
				} else {
					max = annotation.intMaxValue();
				}
				int min;
				if (flags.contains(annotation.intMinValueField())) {
					min = getInt(stack, annotation.intMinValueField());
				} else {
					min = annotation.intMinValue();
				}
				getOrCreateStackFlags(stack).putInt(flag, Mth.clamp(value, min, max));
			}
		}
	}

	public static void setDouble(ItemStack stack, String flag, double value) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.DOUBLE && guiFlags.contains(flag) && loliPickaxeGuiChangeList.contains(flag) && !stack.isEmpty() && stack.getItem() instanceof ILoli) {
				double max;
				if (flags.contains(annotation.doubleMaxValueField())) {
					max = getDouble(stack, annotation.doubleMaxValueField());
				} else {
					max = annotation.doubleMaxValue();
				}
				double min;
				if (flags.contains(annotation.doubleMinValueField())) {
					min = getDouble(stack, annotation.doubleMinValueField());
				} else {
					min = annotation.doubleMinValue();
				}
				getOrCreateStackFlags(stack).putDouble(flag, Mth.clamp(value, min, max));
			}
		}
	}

	public static void setBoolean(ItemStack stack, String flag, boolean value) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.BOOLEAN && guiFlags.contains(flag) && loliPickaxeGuiChangeList.contains(flag) && !stack.isEmpty() && stack.getItem() instanceof ILoli) {
				getOrCreateStackFlags(stack).putBoolean(flag, value);
			}
		}
	}

	public static void setString(ItemStack stack, String flag, String value) {
		if (flags.contains(flag)) {
			ConfigField annotation = flagAnnotations.get(flag);
			if (annotation.valueType() == ValurType.STRING && guiFlags.contains(flag) && loliPickaxeGuiChangeList.contains(flag) && !stack.isEmpty() && stack.getItem() instanceof ILoli) {
				getOrCreateStackFlags(stack).putString(flag, value);
			}
		}
	}

	public static void setItemConfigs(ItemStack stack, CompoundTag config) {
		if (!stack.isEmpty() && stack.getItem() instanceof ILoli) {
			if (!stack.hasTag()) {
				stack.setTag(new CompoundTag());
			}
			if (config == null) {
				stack.getTag().remove(ILoli.CONFIG);
			} else {
				stack.getTag().put(ILoli.CONFIG, config);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void guiChangeListWarning() {
		boolean title = false;
		for (String element : loliPickaxeGuiChangeList) {
			ConfigField annotation = flagAnnotations.get(element);
			if (annotation != null && annotation.warning()) {
				if (!title) {
					printChat(Component.translatable("config.worning.guiChangeList"));
					title = true;
				}
				printChat(Component.translatable("config.worning.guiChangeList.element", annotation.comment()));
			}
		}
	}

}
