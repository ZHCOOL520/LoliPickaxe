package com.anotherstar.client.gui;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.gui.ContainerPasswordWorkbench;
import com.anotherstar.network.NetworkHandler;
import com.anotherstar.network.PasswordUpdataPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIPasswordCrafting extends AbstractContainerScreen<ContainerPasswordWorkbench> {

	private static final ResourceLocation PASSWORD_CRAFTING_TABLE_GUI_TEXTURES = new ResourceLocation(LoliPickaxe.MODID, "textures/gui/container/password_crafting_table.png");

	private EditBox password;

	public GUIPasswordCrafting(ContainerPasswordWorkbench menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageHeight = 196;
	}

	@Override
	protected void init() {
		super.init();
		this.password = new EditBox(this.font, this.leftPos + 29, this.topPos + 18, 75, 16, Component.translatable("gui.password"));
		this.password.setTextColor(16777215);
		this.password.setFocused(true);
		this.addRenderableWidget(this.password);
		this.setInitialFocus(this.password);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> NetworkHandler.sendToServer(new PasswordUpdataPacket(password.getValue()))).bounds(this.leftPos + 114, this.topPos + 16, 30, 20).build());
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.password"), 28, 6, 4210752, false);
		guiGraphics.drawString(this.font, Component.translatable("container.crafting"), 28, 36, 4210752, false);
		guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(PASSWORD_CRAFTING_TABLE_GUI_TEXTURES, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
	}

}
