package com.anotherstar.common.entity;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.entity.ai.EntityAILoliAttack;
import com.anotherstar.common.entity.ai.EntityAILoliNearestAttackableEntity;
import com.anotherstar.common.entity.ai.EntityAILoliNearestAttackablePlayer;
import com.anotherstar.common.entity.ai.EntityAILoliSwimming;
import com.anotherstar.common.entity.ai.EntityLoliMoveHelper;
import com.anotherstar.util.LoliPickaxeUtil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.ITeleporter;

/**
 * 萝莉实体。
 * <p>
 * 对应原 1.12.2 的 {@code com.anotherstar.common.entity.EntityLoli}（继承 {@code EntityCreature}）。
 * 1.20.1 的关键 API 差异：
 * <ul>
 *   <li>{@code EntityCreature} → {@link PathfinderMob}；</li>
 *   <li>{@code moveHelper} 字段 → 通过 {@code setMoveControl} / 构造期赋值 {@code this.moveControl} 设置；</li>
 *   <li>{@code setPathPriority} → {@link #setPathfindingMalus}；</li>
 *   <li>{@code tasks.addTask / targetTasks.addTask} → {@code goalSelector.addGoal / targetSelector.addGoal}
 *       （{@code EntityAIBase} → {@code Goal}）；</li>
 *   <li>{@code onEntityUpdate} → {@link #tick}；{@code attackEntityAsMob} → {@link #doHurtTarget}；</li>
 *   <li>模型类型由客户端渲染器读取的 {@code ConfigLoader.loliModelType} 决定（0=普通、1=蕾米、2=纸片），
 *       服务端只负责同步实体本身，不感知模型；</li>
 *   <li>原版 IceAndFire 石化属性兼容（{@code getCapability}）在 1.20.1 已移除。</li>
 * </ul>
 */
public class EntityLoli extends PathfinderMob implements IEntityLoli {

	/**
	 * 是否已“消散”。
	 * <p>
	 * 语义：为 {@code false} 时，实体被移出世界会自动在原地生成一只新的同类萝莉（见
	 * {@link #onRemovedFromWorld()}），即“杀不死”；为 {@code true} 时才允许真正消失。
	 * 默认 {@code false}；跨维度传送（{@link #changeDimension}）会先置为 {@code true}，
	 * 避免换维度时被判定为意外移除而复制实体。玩家使用消散道具
	 * （{@code ItemLoliDispersal}）会让 {@link #setDispersal(boolean)} 置 {@code true}。
	 */
	private boolean dispersal;
	/** 是否正处于跨维度传送中；默认 {@code false}（原 1.12.2 遗留的公开标记位）。 */
	public boolean dimChangeing;

	/**
	 * 主构造器（由实体类型工厂调用）。
	 *
	 * @param type  实体类型，通常为 {@link EntityLoader#LOLI_TYPE}
	 * @param level 实体所在世界（服务端或客户端）
	 */
	public EntityLoli(EntityType<? extends EntityLoli> type, Level level) {
		super(type, level);
		// 使用自定义移动控制器，以便在水中追击水中目标时走“游泳”逻辑而不是普通寻路。
		this.moveControl = new EntityLoliMoveHelper(this);
		// 水中寻路代价设为 0，让萝莉不会因为“水里不好走”而绕开水域。
		this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
		dispersal = false;
		dimChangeing = false;
	}

	/**
	 * 便捷构造器，固定使用 {@link EntityLoader#LOLI_TYPE}。
	 *
	 * @param level 实体所在世界
	 */
	public EntityLoli(Level level) {
		this(EntityLoader.LOLI_TYPE, level);
	}

	/**
	 * 构建萝莉的属性表。
	 * <p>
	 * 对应原 1.12.2 的 {@code applyEntityAttributes}：
	 * 最大生命 20（10 颗心）、跟随/索敌距离 64 格、
	 * 移动速度取配置 {@code ConfigLoader.loliSpeed}、
	 * 游泳速度取移动速度的 15 倍（{@code ForgeMod.SWIM_SPEED} 是 1.20.1 新增的游泳速度属性）。
	 *
	 * @return 已填好数值的属性构造器（由 {@code EntityAttributeCreationEvent} 调用 {@code build()}）
	 */
	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.FOLLOW_RANGE, 64.0D)
				.add(Attributes.MOVEMENT_SPEED, ConfigLoader.loliSpeed)
				.add(ForgeMod.SWIM_SPEED.get(), ConfigLoader.loliSpeed * 15);
	}

	/**
	 * 注册 AI 目标与寻路目标。
	 * <p>
	 * 对应原 1.12.2 的 {@code initEntityAI}。数值为优先级（越小越先执行）：
	 * <ul>
	 *   <li>goal 0：{@link EntityAILoliAttack}，负责移动接近 + 攻击（开启瞬移时直接贴脸）；</li>
	 *   <li>goal 4：闲逛（避开水面）；goal 5：看向 16 格内玩家；</li>
	 *   <li>goal 6：{@link EntityAILoliSwimming}，浮出水面（仅在无目标/寻路结束时生效）；</li>
	 *   <li>goal 7：随机张望；</li>
	 *   <li>target 2：玩家索敌与通用生物索敌，两者同优先级，都会尝试设置攻击目标。</li>
	 * </ul>
	 */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new EntityAILoliAttack(this));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D, 4.0F));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(6, new EntityAILoliSwimming(this));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(2, new EntityAILoliNearestAttackablePlayer(this));
		this.targetSelector.addGoal(2, new EntityAILoliNearestAttackableEntity(this));
	}

	/**
	 * 每 tick 刷新一次速度属性。
	 * <p>
	 * 对应原 1.12.2 的 {@code onEntityUpdate}：允许通过 {@code /loli loliSpeed ...} 热改配置后立即生效，
	 * 无需重新生成实体。游泳速度固定为移动速度的 15 倍。
	 */
	@Override
	public void tick() {
		AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(ConfigLoader.loliSpeed);
		}
		AttributeInstance swimSpeed = this.getAttribute(ForgeMod.SWIM_SPEED.get());
		if (swimSpeed != null) {
			swimSpeed.setBaseValue(ConfigLoader.loliSpeed * 15);
		}
		super.tick();
	}

	/**
	 * 原 1.12.2 由 ItemLoliPickaxe#leftClickEntity 处理，这里直接使用等价逻辑以免依赖物品包内部实现。
	 *
	 * @param entity 被攻击的目标实体（调用方保证非空，且与本实体相邻/在攻击距离内）
	 * @return 客户端恒返回 {@code false}（不产生攻击判定）；服务端返回 {@code true} 表示攻击已生效
	 */
	@Override
	public boolean doHurtTarget(Entity entity) {
		// 击杀逻辑只在服务端执行，避免客户端重复触发。
		if (this.level().isClientSide) {
			return false;
		}
		LoliPickaxeUtil.kill(entity, this);
		// 配置开启“杀伤朝向”时，连带击杀攻击方向上的实体。
		if (ConfigLoader.getBoolean(ItemStack.EMPTY, "loliPickaxeKillFacing")) {
			LoliPickaxeUtil.killFacing(this);
		}
		return true;
	}

	/**
	 * 移动逻辑。
	 * <p>
	 * 对应原 1.12.2 的 {@code travel(float, float, float)}：在水中且目标也在水中时改为“游泳追击”
	 * （以 0.01 的移动系数叠加位移，每 tick 速度衰减 0.9），否则走原版移动。
	 *
	 * @param input 本地坐标系下的移动输入向量（前/上/侧向），不可为 null
	 */
	@Override
	public void travel(Vec3 input) {
		if (!this.level().isClientSide && this.isInWater() && this.getTarget() != null && this.getTarget().isInWater()) {
			this.moveRelative(0.01F, input);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
		} else {
			super.travel(input);
		}
	}

	/**
	 * 对应原 1.12.2 的 {@code canDespawn}：恒返回 {@code false}，萝莉永不因远离玩家而自然消失。
	 *
	 * @param distance 与最近玩家的距离（格）
	 * @return 恒为 {@code false}
	 */
	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	/**
	 * 击杀萝莉掉落的经验值。
	 *
	 * @return 恒为 {@link Integer#MAX_VALUE}，对应原 1.12.2 的 {@code getExperiencePoints}
	 */
	@Override
	public int getExperienceReward() {
		return Integer.MAX_VALUE;
	}

	/**
	 * 是否可被拴绳牵引，恒为 {@code false}（对应原 1.12.2 的 {@code canBeLeashedTo}）。
	 *
	 * @param player 尝试拴绳的玩家
	 * @return 恒为 {@code false}
	 */
	@Override
	public boolean canBeLeashed(Player player) {
		return false;
	}

	/**
	 * 液体中的上浮逻辑（1.20.1 合并了原版的 {@code handleJumpWater} 与 {@code handleJumpLava}）。
	 *
	 * @param fluid 当前所处流体标签（水或岩浆），仅用于分发，本实现不区分
	 */
	// 重写目标方法本身已废弃（Forge 建议改用 jumpInFluid(FluidType)）。
	// 但 1.20.1 引擎的流体跳跃走的是 jumpInFluid 分支，本方法已不再被调用；
	// 若改为覆写 jumpInFluid 会真正启用这段上浮逻辑，改变运行时行为，故保留原覆写并压制警告。
	@Override
	@SuppressWarnings("deprecation")
	protected void jumpInLiquid(TagKey<Fluid> fluid) {
		// 每 tick 给 0.04 的上浮速度，与原版 handleJumpWater/handleJumpLava 一致。
		this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.04D, 0.0D));
	}

	/**
	 * 掉出世界底部时（对应原 1.12.2 的 {@code outOfWorld}）的处理：
	 * 先踢下乘客，再把实体拉回 Y=256，避免被原版“虚空伤害/删除”逻辑移除。
	 */
	@Override
	protected void onBelowWorld() {
		this.ejectPassengers();
		this.moveTo(this.getX(), 256.0D, this.getZ(), this.getYRot(), this.getXRot());
	}

	/**
	 * 跨维度传送。
	 * <p>
	 * 先置 {@link #dispersal} 为 {@code true}，使传送导致的“移出世界”不会被
	 * {@link #onRemovedFromWorld()} 误判为需要复制实体。
	 *
	 * @param level      目标维度（服务端世界）
	 * @param teleporter 传送器实现
	 * @return 新维度中的实体实例（可能为 {@code null}，表示未成功传送）
	 */
	@Override
	public Entity changeDimension(ServerLevel level, ITeleporter teleporter) {
		dispersal = true;
		return super.changeDimension(level, teleporter);
	}

	/**
	 * 实体被移出世界时的回调。
	 * <p>
	 * 对应原 1.12.2 的同名方法：若不属于“消散”（{@code dispersal == false}）且处于服务端，
	 * 则在原坐标生成一只新的萝莉，实现“无法被移除”。新实体创建后会置 {@code dispersal = true}
	 * 防止递归复制。
	 */
	@Override
	public void onRemovedFromWorld() {
		if (!dispersal && !this.level().isClientSide) {
			EntityLoli loli = new EntityLoli(EntityLoader.LOLI_TYPE, this.level());
			loli.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
			this.level().addFreshEntity(loli);
			dispersal = true;
		}
		super.onRemovedFromWorld();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isDispersal() {
		return dispersal;
	}

	/** {@inheritDoc} */
	@Override
	public void setDispersal(boolean value) {
		dispersal = value;
	}

}
