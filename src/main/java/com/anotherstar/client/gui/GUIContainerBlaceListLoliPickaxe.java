package com.anotherstar.client.gui;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.gui.ContainerBlaceListLoliPickaxe;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIContainerBlaceListLoliPickaxe extends AbstractContainerScreen<ContainerBlaceListLoliPickaxe> {

	private static final ResourceLocation LOLI_PICKAXE_CONTAINER_BLACELIST_GUI_TEXTURE = new ResourceLocation(LoliPickaxe.MODID, "textures/gui/container/loli_pickaxe_container_blacklist.png");

	public GUIContainerBlaceListLoliPickaxe(ContainerBlaceListLoliPickaxe menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageHeight = 256;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(LOLI_PICKAXE_CONTAINER_BLACELIST_GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
	}

}
