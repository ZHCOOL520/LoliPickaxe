package com.anotherstar.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anotherstar.core.util.EventUtil;

import net.minecraft.world.entity.player.Inventory;

/**
 * 持有萝莉镐的玩家背包不可被清空 / 缴械。
 * 原实现由 ASM 替换 InventoryPlayer#dropAllItems/clearMatchingItems/clear。
 *
 * <p><b>兼容性说明</b>：{@code dropAll} / {@code clearContent} 可能被整理类、AE2 等
 * 涉及背包操作的模组改写，因此使用 {@code require = 0} 做优雅降级 ——
 * 匹配失败时只会在日志中告警并跳过「背包保护」，不会让整合包崩溃。
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

	@Inject(method = "dropAll", at = @At("HEAD"), cancellable = true, require = 0)
	private void loli$dropAll(CallbackInfo ci) {
		if (EventUtil.isProtectedInventory((Inventory) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "clearContent", at = @At("HEAD"), cancellable = true, require = 0)
	private void loli$clearContent(CallbackInfo ci) {
		if (EventUtil.isProtectedInventory((Inventory) (Object) this)) {
			ci.cancel();
		}
	}

}
