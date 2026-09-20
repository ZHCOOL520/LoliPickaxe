package com.anotherstar.common.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置字段标记注解。
 *
 * <p>本模组没有手写 {@code ForgeConfigSpec.Builder} 调用，而是给 {@link com.anotherstar.common.config.ConfigLoader}
 * 中的每一个 {@code public static} 配置字段打上本注解，再由 {@code ConfigLoader} 在静态初始化块里
 * 反射扫描（{@code Class#getFields()} + {@code isAnnotationPresent}）自动生成配置项与索引容器。
 *
 * <p>注解属性被复用于三处：
 * <ol>
 *   <li>构建 {@code ForgeConfigSpec}（默认值 / 注释 / 顺序）；</li>
 *   <li>{@code ConfigLoader.load()} 与 {@code save() } 时把配置文件的值与静态字段互相同步；</li>
 *   <li>{@code ConfigLoader.getXxx(ItemStack, String)} / {@code setXxx(...)} 时提供取值范围（用于把玩家在 GUI
 *       里改的值夹到合法区间）。</li>
 * </ol>
 *
 * @see com.anotherstar.common.config.ConfigLoader
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigField {

	/**
	 * 该配置允许出现的"渠道"，可多选。
	 *
	 * <p>用途：静态扫描时据此把字段名分发到 {@code commandFlags}（{@link #COMMAND}）与 {@code guiFlags}（{@link #GUI}）；
	 * {@link #CONFIG} 则用于判定该字段是否可写入配置文件、是否允许热重载（见 {@code ConfigLoader#load(boolean)}）。
	 *
	 * @return 渠道数组；空数组表示该字段只在内存中生效，不落盘、不可用命令/GUI 修改
	 */
	ConfigType[] type();

	/**
	 * 写入配置文件的注释（同时作为 GUI / 警告文本里对该选项的中文说明）。
	 *
	 * @return 中文说明文本
	 */
	String comment();

	/**
	 * 字段的数值形态，决定 {@code ForgeConfigSpec} 用哪种 define 重载、以及同步时调用哪个 setter/getter。
	 *
	 * @return {@link ValurType} 之一；{@code LIST} 与 {@code MAP} 都以字符串列表形式落盘
	 */
	ValurType valueType();

	/** @return {@link ValurType#INT} 时的配置默认值 */
	int intDefaultValue() default 0;

	/** @return {@link ValurType#INT} 时的最小值（仅用于 GUI/{@code ItemStack} 写入时的钳位） */
	int intMinValue() default 0;

	/** @return {@link ValurType#INT} 时的最大值；为 0 且未指定 {@link #intMaxValueField()} 时表示不限制上界 */
	int intMaxValue() default 0;

	/** @return 另一个 INT 字段名，用它的运行期取值作为本字段的最小值（如"最大范围"字段）；为空表示用 {@link #intMinValue()} */
	String intMinValueField() default "";

	/** @return 另一个 INT 字段名，用它的运行期取值作为本字段的最大值；为空表示用 {@link #intMaxValue()} */
	String intMaxValueField() default "";

	/** @return {@link ValurType#DOUBLE} 时的配置默认值 */
	double doubleDefaultValue() default 0.0;

	/** @return {@link ValurType#DOUBLE} 时的最小值（仅用于 GUI/{@code ItemStack} 写入时的钳位） */
	double doubleMinValue() default 0;

	/** @return {@link ValurType#DOUBLE} 时的最大值；配合 {@link #doubleMaxValueField()} 使用 */
	double doubleMaxValue() default 0;

	/** @return 另一个 DOUBLE 字段名，用它的运行期取值作为本字段的最小值；为空表示用 {@link #doubleMinValue()} */
	String doubleMinValueField() default "";

	/** @return 另一个 DOUBLE 字段名，用它的运行期取值作为本字段的最大值；为空表示用 {@link #doubleMaxValue()} */
	String doubleMaxValueField() default "";

	/** @return {@link ValurType#BOOLEAN} 时的配置默认值 */
	boolean booleanDefaultValue() default false;

	/** @return {@link ValurType#STRING} 时的配置默认值 */
	String stringDefaultValue() default "";

	/** @return {@link ValurType#LIST} 时的默认元素（以字符串形式保存，读取时按 {@link #listType()} 还原） */
	String[] listDefaultValue() default {};

	/** @return {@link ValurType#LIST} 时元素的真实类型，用于把配置文件里的字符串还原成 int/double/boolean/String */
	ValurType listType() default ValurType.STRING;

	/**
	 * @return {@link ValurType#MAP} 时的默认条目，每条形如 {@code "键:::值"}
	 *         （分隔符 {@code :::} 见 {@code ConfigLoader#parseTypedMap}）
	 */
	String[] mapDefaultValue() default {};

	/** @return {@link ValurType#MAP} 时键的真实类型 */
	ValurType mapKeyType() default ValurType.STRING;

	/** @return {@link ValurType#MAP} 时值的真实类型 */
	ValurType mapValueType() default ValurType.INT;

	/**
	 * 是否为"危险选项"。
	 *
	 * <p>客户端通过 {@code LoliConfigPacket} 收到服务端配置后，会对 {@code warning = true} 的字段向玩家
	 * 弹出提示气泡（例如"蓝屏打击""伊邪那美"等会伤害客户端的选项）。
	 *
	 * @return true 表示需要在客户端提示玩家
	 */
	boolean warning() default false;

	/**
	 * 自定义警告处理方法的名称（无参数、无返回值的 {@code static} 方法，定义在 {@code ConfigLoader} 中）。
	 *
	 * <p>存在时优先反射调用它来展示警告（例如 GUI 可改列表需要逐项列出），调用失败或为空则退回默认聊天栏提示。
	 *
	 * @return 方法名；为空表示使用默认提示
	 */
	String warningMethod() default "";

	/**
	 * 配置字段的生效渠道。
	 */
	enum ConfigType {
		/** 占位值，未使用。 */
		NONE,
		/** 写入配置文件、可被 {@code ConfigLoader.load}/{@code save} 同步（也据此决定是否允许热重载）。 */
		CONFIG,
		/** 可通过 {@code /lolipickaxe config} 命令修改。 */
		COMMAND,
		/** 可在萝莉镐 GUI 中修改（并且会被写入物品 NBT，从而支持"每把镐独立配置"）。 */
		GUI
	}

	/**
	 * 配置字段的数值形态。
	 */
	enum ValurType {
		/** 整型。 */
		INT,
		/** 双精度浮点。 */
		DOUBLE,
		/** 布尔。 */
		BOOLEAN,
		/** 字符串。 */
		STRING,
		/** 列表；落盘为字符串列表，读取时按 {@code listType()} 还原。 */
		LIST,
		/** 映射；落盘为 {@code "键:::值"} 字符串列表，读取时按 {@code mapKeyType()}/{@code mapValueType()} 还原。 */
		MAP
	}

}
