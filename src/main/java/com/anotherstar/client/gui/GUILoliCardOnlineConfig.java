package com.anotherstar.client.gui;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.network.LoliCardOnlinePacket;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GUILoliCardOnlineConfig extends Screen {

	private static final ResourceLocation LOLI_CARD_ONLINE_CONFIG_GUI_TEXTURE = new ResourceLocation(LoliPickaxe.MODID, "textures/gui/loli_card_online_config.png");

	private String url;
	private EditBox urlField;
	private Button done;

	public GUILoliCardOnlineConfig(String url) {
		super(Component.empty());
		this.url = url;
	}

	@Override
	protected void init() {
		this.urlField = new EditBox(this.font, this.width / 2 - 80, this.height / 2 - 20, 160, 20, Component.translatable("gui.loliCardOnline"));
		this.urlField.setMaxLength(500);
		if (this.url != null) {
			this.urlField.setValue(url);
		}
		this.urlField.setFocused(true);
		this.urlField.moveCursorToEnd();
		this.addRenderableWidget(this.urlField);
		this.setInitialFocus(this.urlField);
		this.done = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
			NetworkHandler.sendToServer(new LoliCardOnlinePacket(urlField.getValue()));
			onClose();
		}).bounds(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());
	}

	@Override
	public void tick() {
		this.urlField.tick();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.blit(LOLI_CARD_ONLINE_CONFIG_GUI_TEXTURE, (this.width - 220) / 2, (this.height - 100) / 2, 0, 0, 220, 90);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, Component.translatable("gui.loliCardOnline"), this.width / 2, this.height / 2 - 40, 16777215);
	}

}
