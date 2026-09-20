package com.anotherstar.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.anotherstar.api.ILoliPlayerData;

import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public abstract class PlayerMixin implements ILoliPlayerData {

	@Unique
	private int loli$hodeLoli;

	@Override
	public int getHodeLoli() {
		return loli$hodeLoli;
	}

	@Override
	public void setHodeLoli(int hodeLoli) {
		this.loli$hodeLoli = hodeLoli;
	}

}
