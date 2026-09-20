package com.anotherstar.common.item;

import java.util.List;

import com.anotherstar.client.creative.CreativeTabLoader;
import com.anotherstar.client.util.LoliCardUtil;
import com.anotherstar.network.LoliCardPacket;
import com.anotherstar.network.LoliCardPacket.ItemType;
import com.anotherstar.network.NetworkHandler;
import com.google.common.collect.Lists;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 罗莉立绘卡片，对应原 1.12.2 的同名类。
 * <p>
 * 一张卡片只承载一幅立绘，立绘名保存在 NBT 键 {@code picture} 中（值为资源包 {@code assets/&lt;ns&gt;/lolicards/}
 * 下的图片名）。右键打开卡片界面；界面的实际执掌者是客户端 {@code com.anotherstar.client.gui.ClientGuiOpener}。
 * <p>
 * 与 1.12.2 的差异：
 * <ul>
 * <li>{@code player.openGui(LoliGUIHandler.GUI_LOLI_CARD,...)} → {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}
 * 调用 {@code ClientGuiOpener.openCard(stack)}（1.20.1 无 GUI Handler，且必须避免在服务端加载客户端类）；</li>
 * <li>{@code getItemStackLimit} → {@link #getMaxStackSize(ItemStack)}；</li>
 * <li>{@code onUpdate} → {@link #inventoryTick}；</li>
 * <li>{@code getSubItems} → {@link #fillItemCategory}，由 {@code CreativeTabLoader} 回调。</li>
 * </ul>
 * 注意：{@link #getMaxStackSize} 依赖 NBT，同一物品「已绑定立绘/未绑定立绘」两种堆叠不能合并。
 */
public class ItemLoliCard extends Item {

	public ItemLoliCard() {
		super(new Item.Properties());
	}

	/**
	 * @param stack 物品堆叠，不可为 null
	 * @return 已绑定立绘（含 NBT）时 64，未绑定时 1——未绑定的空卡需要保持独立，避免自动分配立绘时相互覆盖
	 */
	@Override
	public int getMaxStackSize(ItemStack stack) {
		return stack.hasTag() ? 64 : 1;
	}

	/**
	 * 右键使用：仅在客户端打开卡片界面（服务端不做事，也返回成功以阻止后续交互）。
	 *
	 * @param level  当前世界，用于判断逻辑侧；不可为 null
	 * @param player 使用者，不可为 null
	 * @param hand   使用的那只手，不可为 null
	 * @return 恒为 {@link InteractionResultHolder#success(ItemStack)}（成功且不消耗物品）
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.anotherstar.client.gui.ClientGuiOpener.openCard(stack));
		}
		return InteractionResultHolder.success(stack);
	}

	/**
	 * 背包内每 tick 调用：客户端发现「未绑定立绘」的卡时，随机挑一个立绘名并发包给服务端，由服务端写入 NBT（有副作用：发网络包 / 修改 NBT）。
	 *
	 * @param stack      物品堆叠，不可为 null
	 * @param level      当前世界，只有客户端（{@code level.isClientSide}）才触发，避免服务端重复分配；不可为 null
	 * @param entity     持有者实体，本方法未使用；不可为 null
	 * @param itemSlot   该物品所在的背包槽位下标，会随包发送给服务端以便定位物品；取值范围 {@code 0..背包大小-1}
	 * @param isSelected 是否为主手选中槽位，本方法未使用
	 */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (level.isClientSide && LoliCardUtil.customArtNames != null && LoliCardUtil.customArtNames.length != 0 && (!stack.hasTag() || !stack.getTag().contains("picture"))) {
			List<String> accessName = Lists.newArrayList();
			for (String name : LoliCardUtil.customArtNames) {
				if (!name.contains("''")) { // 名字里带 '' 的是卡册分组名，不属于单张卡片
					accessName.add(name);
				}
			}
			if (!accessName.isEmpty()) {
				NetworkHandler.sendToServer(new LoliCardPacket(itemSlot, ItemType.LOLICARD, accessName.get(level.random.nextInt(accessName.size()))));
			}
		}
	}

	/**
	 * 向创造栏填充「每种立绘一张卡」，对应 1.12.2 的 {@code getSubItems}；由 {@code CreativeTabLoader} 在构建标签页时调用。
	 *
	 * @param tab    目标标签页，仅当为 {@code CreativeTabLoader.loliTabs}（注册名 {@code loli}）且已扫描到立绘资源时才填充；不可为 null
	 * @param output 标签页内容收集器；不可为 null
	 */
	public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
		if (tab == CreativeTabLoader.loliTabs && LoliCardUtil.customArtNames != null && LoliCardUtil.customArtNames.length != 0) {
			for (String name : LoliCardUtil.customArtNames) {
				if (!name.contains("''")) {
					ItemStack stack = new ItemStack(this);
					CompoundTag nbt = new CompoundTag();
					nbt.putString("picture", name); // NBT 键 picture：该卡绑定的立绘名
					stack.setTag(nbt);
					output.accept(stack);
				}
			}
		}
	}

	/**
	 * 提示行显示卡片绑定的立绘名。
	 *
	 * @param stack   物品堆叠，不可为 null
	 * @param level   当前世界，未使用，可能为 null
	 * @param tooltip 提示行收集列表，就地追加；不可为 null
	 * @param flag    提示标志位，未使用；不可为 null
	 */
	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			if (nbt.contains("picture")) {
				tooltip.add(Component.literal(nbt.getString("picture")));
			}
		}
	}

}
