package com.anotherstar.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anotherstar.core.util.EventUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * 持有萝莉镐的玩家不可被踢出。
 * 原实现由 ASM 替换 NetHandlerPlayServer#disconnect。
 *
 * <p><b>兼容性说明</b>：{@code disconnect} 常被网络/反作弊类模组注入，
 * 因此使用 {@code require = 0}；匹配失败时仅告警并跳过「禁止踢出」功能，
 * 不会导致整合包无法启动。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

	@Inject(method = "disconnect", at = @At("HEAD"), cancellable = true, require = 0)
	private void loli$disconnect(Component message, CallbackInfo ci) {
		ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
		if (self.player != null && EventUtil.isProtectedPlayer(self.player)) {
			ci.cancel();
		}
	}

}
