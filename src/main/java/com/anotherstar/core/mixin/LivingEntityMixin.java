package com.anotherstar.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.anotherstar.api.ILoliDataHolder;
import com.anotherstar.core.util.EventUtil;

import net.minecraft.world.entity.LivingEntity;

/**
 * 向 {@code LivingEntity} 注入萝莉镐所需的数据字段，并在「确有必要」时改写生命值。
 *
 * <p>对应 1.12.2 中由 ASM 完成的改写：新增 {@code loliDead} / {@code loliCool} /
 * {@code loliDeathTime} 字段，并把原版 {@code getHealth()} / {@code getMaxHealth()}
 * 改名后插入同名方法转发到 {@code EventUtil}。
 *
 * <h2>为什么不再使用 {@code @Shadow} 字段</h2>
 * 原实现需要「原版生命值」和「原版死亡计时」，因此影子了
 * {@code LivingEntity.deathTime} 与 {@code LivingEntity.DATA_HEALTH_ID}。
 * 但 Mixin 注解处理器<b>不会</b>为影子字段生成 refmap 映射（实测 refmap 中只有方法映射），
 * 生产环境（SRG 名称）下会直接抛出
 * {@code @Shadow field deathTime was not located in the target class} 并导致游戏崩溃。
 *
 * <p>因此这里改为「<b>按需覆盖</b>」：{@link EventUtil#overrideHealth(LivingEntity)} 只在真正需要
 * 改写返回值时才返回非 {@code null}，其余情况<b>不取消原版方法</b>：
 * <ul>
 *   <li>不需要读取原版数值 —— 原版方法自己会执行，返回真实生命值；</li>
 *   <li>不注入任何字段 —— 无需 refmap 字段映射；</li>
 *   <li>对普通实体的行为<b>零影响</b>，其它模组对 {@code getHealth} 的注入照常生效（兼容性更好）。</li>
 * </ul>
 *
 * <h2>兼容性</h2>
 * 本类只保留两个 {@code HEAD + cancellable} 注入点，且仅对「持有本人萝莉镐」或
 * 「被标记为已死亡 / 列入灵魂超度名单」的实体生效。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ILoliDataHolder {

	/** 萝莉镐专属：是否已被判定死亡（对应原 loliDead）。 */
	@Unique
	private boolean loli$dead;

	/** 萝莉镐专属：是否进入强制清除流程（对应原 loliCool）。 */
	@Unique
	private boolean loli$cool;

	/** 萝莉镐专属：强制清除计时（对应原 loliDeathTime）。 */
	@Unique
	private int loli$deathTime;

	/** 萝莉镐专属：本 tick 的「受保护（无敌）」缓存标记。 */
	@Unique
	private boolean loli$protected;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isLoliDead() {
		return loli$dead;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLoliDead(boolean dead) {
		this.loli$dead = dead;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isLoliCool() {
		return loli$cool;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLoliCool(boolean cool) {
		this.loli$cool = cool;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getLoliDeathTime() {
		return loli$deathTime;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLoliDeathTime(int time) {
		this.loli$deathTime = time;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isLoliProtected() {
		return loli$protected;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLoliProtected(boolean protect) {
		this.loli$protected = protect;
	}

	/**
	 * 按需改写 {@code getHealth()} 的返回值。
	 *
	 * <p>仅当 {@link EventUtil#overrideHealth(LivingEntity)} 返回非 {@code null}
	 * （受萝莉镐保护 / 被标记死亡 / 灵魂超度名单）时才取消原版方法，
	 * 否则直接放行，原版逻辑与其它模组的注入都不受影响。
	 *
	 * @param cir Mixin 回调，用于写入被替换的返回值
	 */
	@Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
	private void loli$getHealth(CallbackInfoReturnable<Float> cir) {
		Float override = EventUtil.overrideHealth((LivingEntity) (Object) this);
		if (override != null) {
			cir.setReturnValue(override);
		}
	}

	/**
	 * 按需改写 {@code getMaxHealth()} 的返回值。
	 *
	 * <p>语义与 {@link #loli$getHealth(CallbackInfoReturnable)} 完全一致。
	 *
	 * @param cir Mixin 回调，用于写入被替换的返回值
	 */
	@Inject(method = "getMaxHealth", at = @At("HEAD"), cancellable = true)
	private void loli$getMaxHealth(CallbackInfoReturnable<Float> cir) {
		Float override = EventUtil.overrideMaxHealth((LivingEntity) (Object) this);
		if (override != null) {
			cir.setReturnValue(override);
		}
	}

	// 说明（大型整合包兼容性）：
	// 原 1.12.2 版还在 onUpdate() 末尾插入了每 tick 的维护逻辑。
	// 移植版【刻意不再注入 tick()】：tick() 是全生态被注入最多的方法之一
	// （AE2、属性类模组、性能优化模组都会注入甚至 @Overwrite 它），
	// 一旦目标方法被覆盖就会导致 Mixin 应用失败并使游戏无法启动。
	// 该逻辑改由 EventUtil#onLivingTick 监听 Forge 标准事件 LivingTickEvent 实现。

}
