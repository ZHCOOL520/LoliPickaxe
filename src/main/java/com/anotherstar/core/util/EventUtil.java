package com.anotherstar.core.util;

import javax.annotation.Nullable;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.api.ILoliDataHolder;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 萝莉镐核心逻辑（「无敌」、反伤、强制清除、绝地免疫等）的统一入口。
 *
 * <p><b>移植说明</b>：1.12.2 版本通过 ASM 把本类的方法直接塞进原版方法体内
 * （{@code getHealth} / {@code getMaxHealth} / {@code onUpdate} / {@code ForgeHooks.onLivingDeath} 等）。
 * 1.20.1 不再支持这种改写，因此改为：
 * <ul>
 *   <li>与原版方法体等价的逻辑 → 由 {@code com.anotherstar.core.mixin} 下的 Mixin 转发到本类；</li>
 *   <li>“事件语义”的逻辑 → 直接监听 Forge 的标准事件（{@link LivingAttackEvent}、{@link LivingDeathEvent}、{@link PlayerEvent}）。</li>
 * </ul>
 *
 * <p><b>「无敌」实现要点</b>：
 * <ol>
 *   <li>{@link #onUpdate(LivingEntity)} 每 tick 刷新一次「受保护」缓存，并把生命值/上限钉死在 20；</li>
 *   <li>{@link #onLivingAttack(LivingAttackEvent)} 直接取消所有指向受保护实体的伤害（含坠落、火焰、虚空、
 *       {@code /kill} 等），从根本上保证不会被任意来源打死；</li>
 *   <li>{@link #onLivingDeath(LivingDeathEvent)} 作为兜底：即使有模组绕过伤害事件直接调用 {@code die()}，
 *       也会把实体救回来；</li>
 *   <li>{@link #onPlayerClone(PlayerEvent.Clone)} 在重生/换维度时清空萝莉状态，避免「死亡标记」被带进新实体导致永久假死。</li>
 * </ol>
 *
 * <p><b>与其他模组的兼容性</b>：
 * <ul>
 *   <li>全部使用 Forge 标准事件，且注册优先级为 {@link EventPriority#LOWEST}，
 *       保证其它模组的事件处理器仍能先收到并处理事件（我们只做最终否决）；</li>
 *   <li>只对「真正持有本人萝莉镐」的实体生效，不影响其它模组的实体与玩法；</li>
 *   <li>{@link #getHealth(LivingEntity)} / {@link #getMaxHealth(LivingEntity)} 等只读路径不再产生副作用
 *       （不扫描背包、不写属性），避免高频调用时与其它模组互相干扰；</li>
 *   <li>属性写入从「只读方法」移到每 tick 一次的 {@link #onUpdate(LivingEntity)} 中，避免属性重算风暴与客户端不同步。</li>
 * </ul>
 */
public class EventUtil {

	/** 萝莉镐自定义伤害类型（数据包：{@code data/lolipickaxe/damage_type/loli.json}）。 */
	public static final ResourceKey<DamageType> LOLI_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(LoliPickaxe.MODID, "loli"));

	/** 「无敌」判定时使用的固定生命值上限。 */
	private static final float LOLI_HEALTH = 20.0F;

	/**
	 * 构造萝莉镐专属伤害来源。
	 *
	 * <p>1.12.2 使用 {@code new DamageSource("loli")} 这种字符串构造；1.20.1 的伤害类型必须来自
	 * {@code damage_type} 注册表，因此这里通过已注册的自定义伤害类型 {@link #LOLI_DAMAGE} 构造。
	 *
	 * @param level  伤害发生的世界，用于取得注册表访问入口
	 * @param source 造成伤害的实体，可为 {@code null}
	 * @return 带有消息 id {@code loli} 的伤害来源
	 */
	public static DamageSource loliDamage(Level level, @Nullable Entity source) {
		Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LOLI_DAMAGE);
		if (source == null) {
			return new DamageSource(holder);
		}
		return new DamageSource(holder, source, source);
	}

	/**
	 * 判断伤害是否来自萝莉镐（用于区分「强制击杀」与普通伤害）。
	 *
	 * @param source 待判断的伤害来源，可为 {@code null}
	 * @return true 表示该伤害由萝莉镐造成
	 */
	public static boolean isLoliDamage(DamageSource source) {
		return source != null && "loli".equals(source.getMsgId());
	}

	/**
	 * 判断某个玩家背包是否处于「受萝莉镐保护」状态，用于 Mixin 拦截背包清空/缴械。
	 *
	 * @param inventory 玩家背包
	 * @return true 表示该背包不可被清空
	 */
	public static boolean isProtectedInventory(Inventory inventory) {
		return inventory.player != null && LoliPickaxeUtil.invHaveLoliPickaxe(inventory.player);
	}

	/**
	 * 判断某个玩家是否受萝莉镐保护，用于 Mixin 拦截被踢出。
	 *
	 * @param player 玩家
	 * @return true 表示该玩家不可被踢出
	 */
	public static boolean isProtectedPlayer(Player player) {
		return LoliPickaxeUtil.invHaveLoliPickaxe(player);
	}

	/**
	 * 判断持有萝莉镐的玩家是否应当看见隐身生物（{@code loliPickaxeShowInvisible}）。
	 *
	 * @param player 玩家
	 * @return true 表示应当把目标实体显示为可见
	 */
	public static boolean showInvisible(Player player) {
		ItemStack loli = LoliPickaxeUtil.getLoliPickaxe(player);
		return !loli.isEmpty() && ConfigLoader.getBoolean(loli, "loliPickaxeShowInvisible");
	}

	// ------------------------------------------------------------------
	// 「无敌」核心：伤害拦截
	// ------------------------------------------------------------------

	/**
	 * 无敌核心拦截：取消所有指向「受萝莉镐保护实体」的伤害。
	 *
	 * <p>1.20.1 中所有伤害（近战、弹射物、爆炸、火焰、坠落、溺水、虚空、{@code /kill} 等）
	 * 最终都会经过 {@link LivingAttackEvent}，因此在这里取消即可获得完整免疫，
	 * 比 1.12.2 依靠「改 {@code getHealth} 返回值」的做法更彻底、也更稳定。
	 *
	 * <p>反伤：当本次伤害「足以致死」（数值 ≥ 20）时，按原版语义击杀攻击者。
	 * 若攻击者也受萝莉镐保护则不再互相触发，避免递归。
	 *
	 * @param event 攻击事件
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingAttack(LivingAttackEvent event) {
		LivingEntity entity = event.getEntity();
		if (!((ILoliDataHolder) entity).isLoliProtected()) {
			return;
		}
		boolean lethal = event.getAmount() >= LOLI_HEALTH;
		event.setCanceled(true);
		if (lethal && !isLoliDamage(event.getSource())) {
			applyThorns(entity, event.getSource());
		}
	}

	/**
	 * 反伤处理：击杀对本实体造成致死伤害的攻击者。
	 *
	 * @param entity 受保护的实体（萝莉镐持有者）
	 * @param source 伤害来源
	 */
	private static void applyThorns(LivingEntity entity, DamageSource source) {
		if (!ConfigLoader.getBoolean(LoliPickaxeUtil.getLoliPickaxe(entity), "loliPickaxeThorns")) {
			return;
		}
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity)) {
			return;
		}
		LivingEntity living = (LivingEntity) attacker;
		// 自身 & 同样受保护者不反伤：防止两个萝莉镐持有者互相递归斩杀
		if (living == entity || LoliPickaxeUtil.invHaveLoliPickaxe(living)) {
			return;
		}
		if (living instanceof Player) {
			LoliPickaxeUtil.killPlayer((Player) living, entity);
		} else {
			LoliPickaxeUtil.killEntityLiving(living, entity);
		}
	}

	/**
	 * 无线无敌兜底：即使有模组绕过伤害事件直接让实体 {@code die()}，也把萝莉镐持有者救回来。
	 *
	 * <p>对应原 1.12.2 中替换 {@code ForgeHooks.onLivingDeath} 的逻辑；
	 * 其中「萝莉镐造成的伤害不允许被复活」这一条通过 {@code setCanceled(false)} 强制保留。
	 *
	 * @param event 死亡事件
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (isLoliDamage(event.getSource())) {
			// 萝莉镐的击杀属于「强制清除」，不允许被其它模组的免死逻辑拦下
			event.setCanceled(false);
			return;
		}
		if (!((ILoliDataHolder) entity).isLoliProtected()) {
			return;
		}
		ILoliDataHolder holder = (ILoliDataHolder) entity;
		entity.setHealth(entity.getMaxHealth());
		holder.setLoliDead(false);
		holder.setLoliDeathTime(0);
		event.setCanceled(true);
	}

	/**
	 * 重生 / 换维度时清空萝莉状态。
	 *
	 * <p>对应 1.12.2 中由 ASM 改写的 {@code PlayerList#recreatePlayerEntity}：
	 * 若不清理，{@code loliDead}/{@code loliCool} 会残留，导致玩家重生后进入「永久假死」。
	 *
	 * @param event 玩家克隆事件（死亡重生、换维度时触发）
	 */
	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		ILoliDataHolder holder = (ILoliDataHolder) event.getEntity();
		holder.setLoliDead(false);
		holder.setLoliCool(false);
		holder.setLoliDeathTime(0);
		holder.setLoliProtected(false);
	}

	/**
	 * 「伊邪那美」：名单内的玩家登录时清空数据，实现轮回重生。
	 *
	 * <p>对应 1.12.2 中由 ASM 改写的 {@code SaveHandler#readPlayerData} 返回 {@code null}；
	 * 1.20.1 里玩家数据在登录前就已加载，因此改为在登录事件里主动清空，效果等价。
	 *
	 * @param event 玩家登录事件
	 */
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer)) {
			return;
		}
		ServerPlayer player = (ServerPlayer) event.getEntity();
		String uuid = player.getUUID().toString();
		if (ConfigLoader.loliPickaxeReincarnationPlayerList == null || !ConfigLoader.loliPickaxeReincarnationPlayerList.contains(uuid)) {
			return;
		}
		player.getInventory().clearContent();
		player.getEnderChestInventory().clearContent();
		player.removeAllEffects();
		player.setExperienceLevels(0);
		player.setExperiencePoints(0);
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);
		player.setHealth(player.getMaxHealth());
		ConfigLoader.loliPickaxeReincarnationPlayerList.remove(uuid);
		ConfigLoader.save();
	}

	// ------------------------------------------------------------------
	// 每 tick 维护（由 LivingTickEvent 驱动，不再注入原版 tick()）
	// ------------------------------------------------------------------

	/**
	 * 每 tick 维护的入口，监听 Forge 标准事件 {@link LivingEvent.LivingTickEvent}。
	 *
	 * <p><b>为什么不注入 {@code LivingEntity#tick()}：</b>
	 * {@code tick()} 是全生态被 Mixin 注入最多的方法之一（AE2、属性类模组、性能优化模组等），
	 * 并且存在被 {@code @Overwrite} 的可能 —— 一旦目标方法被覆盖，本模组的注入会应用失败，
	 * 直接导致游戏启动崩溃。改用 Forge 事件后：
	 * <ul>
	 *   <li>零注入点冲突，不会与任何模组抢占同一个 injection point；</li>
	 *   <li>不会被其它模组的方法改写影响执行时机。</li>
	 * </ul>
	 * 唯一差异是执行时机由「tick 末尾」变为「tick 开始」，对生命值/飞行等维护逻辑无实质影响。
	 *
	 * @param event 生物 tick 事件
	 */
	@SubscribeEvent
	public static void onLivingTick(LivingEvent.LivingTickEvent event) {
		onUpdate(event.getEntity());
	}

	/**
	 * 每 tick 维护：刷新「无敌」缓存、钉住生命值、强制清除、允许飞行与调整挖掘距离。
	 *
	 * <p>对应原 1.12.2 中 ASM 在 {@code EntityLivingBase#onUpdate} 末尾插入的 {@code EventUtil.onUpdate(this)}。
	 *
	 * @param entity 被 tick 的实体
	 */
	public static void onUpdate(LivingEntity entity) {
		ILoliDataHolder holder = (ILoliDataHolder) entity;
		boolean isLoli = LoliPickaxeUtil.invHaveLoliPickaxe(entity);
		// 每 tick 只在这里判断一次，getHealth() 等高频方法只读该缓存
		holder.setLoliProtected(isLoli);

		if (!isLoli && holder.isLoliCool()) {
			// 延迟清除到期：直接移除实体
			entity.setHealth(0.0F);
			entity.discard();
			return;
		}
		if (ConfigLoader.loliPickaxeForbidOnLivingUpdate && !isLoli && (holder.isLoliDead() || entity.isDeadOrDying() || entity.getHealth() == 0.0F)) {
			int time = holder.getLoliDeathTime() + 1;
			holder.setLoliDeathTime(time);
			if (time >= 20) {
				entity.discard();
			}
			return;
		}
		if (isLoli) {
			// 无敌：把生命上限与当前生命钉在 20（SynchedEntityData 对相同值不会重复同步）
			if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
				entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(LOLI_HEALTH);
			}
			entity.setHealth(LOLI_HEALTH);
			holder.setLoliDead(false);
			holder.setLoliCool(false);
			if (entity instanceof Player) {
				Player player = (Player) entity;
				player.getAbilities().mayfly = true;
				if (player instanceof ServerPlayer) {
					((ServerPlayer) player).onUpdateAbilities();
				}
				applyReachDistance(player);
			}
		}
	}

	/**
	 * 把配置中的挖掘距离同步到 Forge 的 {@code BLOCK_REACH} 属性。
	 *
	 * <p>对应原 1.12.2 中由 ASM 替换 {@code Entity#rayTrace} 的做法；
	 * 改用标准属性后，不会干扰其它模组对射线追踪的改动。
	 *
	 * @param player 玩家
	 */
	public static void applyReachDistance(Player player) {
		if (player.getAttribute(ForgeMod.BLOCK_REACH.get()) == null) {
			return;
		}
		ItemStack loli = LoliPickaxeUtil.getLoliPickaxe(player);
		double distance = loli.isEmpty() ? 0.0 : ConfigLoader.getDouble(loli, "loliPickaxeBlockReachDistance");
		double target = distance > 0 ? distance : player.getAttribute(ForgeMod.BLOCK_REACH.get()).getAttribute().getDefaultValue();
		if (player.getAttribute(ForgeMod.BLOCK_REACH.get()).getBaseValue() != target) {
			player.getAttribute(ForgeMod.BLOCK_REACH.get()).setBaseValue(target);
		}
	}

	// ------------------------------------------------------------------
	// 生命值读写（由 LivingEntityMixin 转发）
	// ------------------------------------------------------------------

	/**
	 * 空值安全的「灵魂超度名单」判定。
	 *
	 * <p>配置在 {@code ModConfigEvent.Loading} 时才被填充，而实体 Tick/生命值读取可能更早发生，
	 * 因此这里必须容忍 {@code null}，避免早期空指针导致崩溃。
	 *
	 * @param uuid 玩家 UUID 字符串
	 * @return true 表示该玩家位于「灵魂超度」名单中
	 */
	private static boolean beyondRedemption(String uuid) {
		return ConfigLoader.loliPickaxeBeyondRedemptionPlayerList != null && ConfigLoader.loliPickaxeBeyondRedemptionPlayerList.contains(uuid);
	}

	/**
	 * 生命值覆盖判定（由 {@code LivingEntityMixin} 在 {@code getHealth()} 的 HEAD 处调用）。
	 *
	 * <p><b>设计要点</b>：只在<b>确有必要</b>时才返回非 {@code null}，
	 * 返回 {@code null} 时 Mixin <b>不会取消原版方法</b>，于是：
	 * <ul>
	 *   <li>普通实体完全走原版实现，本模组零影响；</li>
	 *   <li>无需读取「原版生命值」，因此不需要 {@code @Shadow} 注入
	 *       {@code LivingEntity.DATA_HEALTH_ID} —— Mixin 注解处理器不会为影子字段生成
	 *       refmap 映射，生产环境会因 "No refMap loaded" 直接崩溃，这里从设计上规避；</li>
	 *   <li>其它模组注入 {@code getHealth()} 的逻辑在普通实体上照常生效。</li>
	 * </ul>
	 *
	 * @param entity 实体
	 * @return 需要覆盖的生命值；无需覆盖时返回 {@code null}
	 */
	public static Float overrideHealth(LivingEntity entity) {
		ILoliDataHolder holder = (ILoliDataHolder) entity;
		if (holder.isLoliProtected()) {
			return LOLI_HEALTH;
		}
		if (holder.isLoliDead()) {
			return 0.0F;
		}
		// 「灵魂超度」名单只可能包含玩家 UUID，因此仅玩家才需要查询，
		// 避免对每个生物都执行一次 UUID→String 分配（大型整合包实体数量巨大，热路径分配会显著增加 GC 压力）。
		if (entity instanceof Player && beyondRedemption(entity.getUUID().toString())) {
			return 0.0F;
		}
		return null;
	}

	/**
	 * 生命上限覆盖判定（由 {@code LivingEntityMixin} 在 {@code getMaxHealth()} 的 HEAD 处调用）。
	 *
	 * <p>语义与 {@link #overrideHealth(LivingEntity)} 完全一致：只在必要时返回非 {@code null}。
	 * 另外，属性基值的写入统一放在 {@link #onUpdate(LivingEntity)}（每 tick 一次），
	 * 避免在只读路径上频繁触发属性重算。
	 *
	 * @param entity 实体
	 * @return 需要覆盖的生命上限；无需覆盖时返回 {@code null}
	 */
	public static Float overrideMaxHealth(LivingEntity entity) {
		ILoliDataHolder holder = (ILoliDataHolder) entity;
		if (holder.isLoliProtected()) {
			return LOLI_HEALTH;
		}
		if (holder.isLoliDead()) {
			return 0.0F;
		}
		if (entity instanceof Player && beyondRedemption(entity.getUUID().toString())) {
			return 0.0F;
		}
		return null;
	}

}
