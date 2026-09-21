package com.anotherstar.common.entity;

import com.anotherstar.common.block.BlockBuffAttackTNT;
import com.anotherstar.common.block.BlockLoader;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.network.LoliDeadPacket;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 萝莉增益攻击 TNT 实体（被点燃的 {@code BlockBuffAttackTNT} 生成的引信实体）。
 * <p>
 * 对应原 1.12.2 的同名实体类（原版继承 {@code EntityTNTPrimed}，1.20.1 为
 * {@link PrimedTnt}）。与原版 TNT 的区别：爆炸/引信结束时不是造成爆炸伤害，
 * 而是对周围 5 格内的服务端玩家下发 {@code LoliDeadPacket}，触发“蓝屏 / 退出 / 无响应”
 * 三种“增益攻击”。方块类型通过同步数据 {@link #BLOCK_DATA} 而不是 NBT 同步到客户端，
 * 以便客户端使用正确的外观渲染。
 */
public class EntityLoliBuffAttackTNT extends PrimedTnt {

	/** 同步数据：产生该 TNT 的方块注册名（如 {@code lolipickaxe:loli_blue_screen_tnt}）；客户端据此选择渲染外观，默认空串。 */
	private static final EntityDataAccessor<String> BLOCK_DATA = SynchedEntityData.defineId(EntityLoliBuffAttackTNT.class, EntityDataSerializers.STRING);

	/**
	 * 仅带世界的构造器（实体类型工厂及反序列化使用）。
	 *
	 * @param levelIn 实体所在世界
	 */
	public EntityLoliBuffAttackTNT(Level levelIn) {
		super(EntityLoader.LOLI_BUFF_ATTACK_TNT_TYPE(), levelIn);
	}

	/**
	 * 生成一只已记录来源方块的 TNT。
	 *
	 * @param levelIn 实体所在世界
	 * @param x       生成坐标 X（格）
	 * @param y       生成坐标 Y（格）
	 * @param z       生成坐标 Z（格）
	 * @param igniter 点燃者（可为 {@code null}，例如被爆炸波及生成）
	 * @param block   来源方块，决定消散时执行哪种增益攻击
	 */
	public EntityLoliBuffAttackTNT(Level levelIn, double x, double y, double z, LivingEntity igniter, BlockBuffAttackTNT block) {
		super(levelIn, x, y, z, igniter);
		setBlock(block);
	}

	/** 注册同步数据；必须在构造函数之外、由 {@code SynchedEntityData} 调用。 */
	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(BLOCK_DATA, "");
	}

	/**
	 * 从同步数据解析来源方块。
	 *
	 * @return 同步数据中的方块实例；若为空串或解析结果不是 {@link BlockBuffAttackTNT}，
	 *         回退为 {@link BlockLoader#loliBlueScreenTNT}（客户端缺省外观）
	 */
	private BlockBuffAttackTNT getBlock() {
		String name = this.entityData.get(BLOCK_DATA);
		if (!name.isEmpty()) {
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
			if (block instanceof BlockBuffAttackTNT) {
				return (BlockBuffAttackTNT) block;
			}
		}
		return BlockLoader.loliBlueScreenTNT();
	}

	/**
	 * 把来源方块写入同步数据。
	 *
	 * @param block 来源方块（非空）
	 */
	private void setBlock(BlockBuffAttackTNT block) {
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
		this.entityData.set(BLOCK_DATA, id == null ? "" : id.toString());
	}

	/**
	 * 每 tick 处理引信。
	 * <p>
	 * 借助 {@code isRemoved()} 在 {@code super.tick()} 前后的差异判断“本 tick 引信刚好结束”，
	 * 然后用 AABB 预筛 + 精确距离二次判断，对 5 格内的服务端玩家执行增益攻击。
	 * 配置 {@code loliEnableBuffAttackTNT} 关闭时不生效（此时等同于普通 TNT 消失）。
	 */
	@Override
	public void tick() {
		boolean removed = this.isRemoved();
		super.tick();
		if (!removed && this.isRemoved() && !this.level().isClientSide && ConfigLoader.loliEnableBuffAttackTNT) {
			// 先用 5 格包围盒粗筛，再按实际距离 < 5 精确判定，避免角落实体被误伤。
			AABB area = new AABB(this.getX() - 5.0D, this.getY() - 5.0D, this.getZ() - 5.0D, this.getX() + 5.0D, this.getY() + 5.0D, this.getZ() + 5.0D);
			for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, area)) {
				if (this.distanceTo(player) < 5.0F) {
					getBlock().buffAttack(player);
				}
			}
		}
	}

	/**
	 * 读取存档中的来源方块。
	 *
	 * @param compound 实体 NBT；键 {@code "block"} 保存方块注册名，缺失或非法时回退为默认方块
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		String name = compound.getString("block");
		if (!name.isEmpty()) {
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
			if (block instanceof BlockBuffAttackTNT) {
				setBlock((BlockBuffAttackTNT) block);
			}
		}
	}

	/**
	 * 写入来源方块到存档。
	 *
	 * @param compound 实体 NBT；写入键 {@code "block"}（方块注册名字符串，取不到时写空串）
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(getBlock());
		compound.putString("block", id == null ? "" : id.toString());
	}

	/**
	 * 供渲染使用的默认方块状态（对应原 1.12.2 中渲染器从实体获取方块状态的逻辑）。
	 *
	 * @return 来源方块的默认状态
	 */
	public BlockState getDefaultState() {
		return getBlock().defaultBlockState();
	}

}
