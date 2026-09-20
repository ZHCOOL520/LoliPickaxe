package com.anotherstar.common.entity;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 实体注册器。
 * <p>
 * 对应原 1.12.2 的 {@code com.anotherstar.common.entity.EntityLoader}
 * （在那里它用 {@code EntityEntryBuilder} 在 {@code RegistryEvent.Register<EntityEntry>} 中注册实体，
 * 并用 {@code RenderingRegistry} 注册客户端渲染器）。
 * <p>
 * 1.20.1 的关键 API 差异：
 * <ul>
 *   <li>实体注册改为延迟注册（{@link DeferredRegister}），注册目标为 {@code ForgeRegistries.ENTITY_TYPES}；</li>
 *   <li>实体属性不再在 {@code applyEntityAttributes} 里设置，而是通过 {@code EntityAttributeCreationEvent}
 *       一次性提供 {@link net.minecraft.world.entity.ai.attributes.AttributeSupplier}；</li>
 *   <li>客户端渲染器已拆分到 client 包，不再由本类负责。</li>
 * </ul>
 */
public class EntityLoader {

	/** 实体类型延迟注册器，注册目标为 {@code ForgeRegistries.ENTITY_TYPES}，命名空间 {@code lolipickaxe}。 */
	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LoliPickaxe.MODID);

	/**
	 * 萝莉实体类型，注册名 {@code lolipickaxe:loli}（对应原 1.12.2 的实体 id {@code lolipickaxe:loli}，网络 id 219）。
	 * <p>
	 * {@code MobCategory.CREATURE} 表示归类为被动生物（影响生成上限/刷怪分类）；
	 * 碰撞箱 0.6F(宽) x 1.5F(高) 与原 1.12.2 的 {@code setSize(0.6F, 1.5F)} 一致；
	 * 客户端追踪范围 80 格、每 3 tick 同步一次，对应原版的 {@code tracker(80, 3, false)}。
	 */
	public static final EntityType<EntityLoli> LOLI_TYPE = EntityType.Builder
			.<EntityLoli>of((type, level) -> new EntityLoli(type, level), MobCategory.CREATURE)
			.sized(0.6F, 1.5F)
			.clientTrackingRange(80)
			.updateInterval(3)
			.build(LoliPickaxe.MODID + ":loli");

	/**
	 * 萝莉增益攻击 TNT 实体类型，注册名 {@code lolipickaxe:loli_buff_attack_tnt}
	 * （对应原 1.12.2 的实体 id {@code lolipickaxe:loli_buff_attack_tnt}，网络 id 220）。
	 * <p>
	 * {@code MobCategory.MISC} 表示杂项实体（不参与生物生成上限计算）；
	 * 碰撞箱 0.98F x 0.98F 与原版 TNT 一致；追踪范围 80 格、每 3 tick 同步一次。
	 */
	public static final EntityType<EntityLoliBuffAttackTNT> LOLI_BUFF_ATTACK_TNT_TYPE = EntityType.Builder
			.<EntityLoliBuffAttackTNT>of((type, level) -> new EntityLoliBuffAttackTNT(level), MobCategory.MISC)
			.sized(0.98F, 0.98F)
			.clientTrackingRange(80)
			.updateInterval(3)
			.build(LoliPickaxe.MODID + ":loli_buff_attack_tnt");

	static {
		// 显式写出注册名，保证与 1.12.2 的资源路径一致（lolipickaxe:loli / lolipickaxe:loli_buff_attack_tnt）。
		ENTITY_TYPES.register("loli", () -> LOLI_TYPE);
		ENTITY_TYPES.register("loli_buff_attack_tnt", () -> LOLI_BUFF_ATTACK_TNT_TYPE);
		// 实体属性的注册事件只能从 mod 事件总线拿到，这里在类加载（CommonProxy.preInit）时自行挂上。
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(EntityLoader::onEntityAttributeCreation);
	}

	/**
	 * 为实体类型登记属性表；不登记属性的实体在生成时会抛异常。
	 * <p>
	 * 对应原 1.12.2 的 {@code EntityLoli#applyEntityAttributes}。TNT 实体继承自
	 * {@link net.minecraft.world.entity.item.PrimedTnt}，不需要属性表，故不在此登记。
	 *
	 * @param event Forge 的实体属性创建事件（mod 总线，不可取消）
	 */
	private static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
		event.put(LOLI_TYPE, EntityLoli.createAttributes().build());
	}

}
