package com.anotherstar.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.anotherstar.core.util.EventUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 持有萝莉镐时可以看到隐身生物。
 * 原实现由 ASM 替换 RenderLivingBase 中对 isInvisibleToPlayer 的调用。
 *
 * <p><b>兼容性说明</b>：{@code Entity#isInvisibleTo} 是渲染相关的高频方法，
 * 光影/渲染优化类模组也可能注入它，因此这里使用 {@code require = 0}：
 * 若目标方法在其它模组介入后无法匹配，仅打印一条警告并跳过本功能，
 * <b>不会</b>导致整合包启动失败。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

	@Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true, require = 0)
	private void loli$isInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (player != null && EventUtil.showInvisible(player)) {
			cir.setReturnValue(false);
		}
	}

}
