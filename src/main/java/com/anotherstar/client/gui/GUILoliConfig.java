package com.anotherstar.client.gui;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.config.annotation.ConfigField;
import com.anotherstar.common.config.annotation.ConfigField.ValurType;
import com.anotherstar.network.LoliItemConfigPacket;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GUILoliConfig extends Screen {

	private static final ResourceLocation LOLI_PICKAXE_CONFIG_GUI_TEXTURE = new ResourceLocation(LoliPickaxe.MODID, "textures/gui/loli_pickaxe_config.png");

	private ItemStack stack;
	private Button done;
	private Button pre;
	private Button next;
	private Button booleanValue;
	private EditBox otherValue;
	private int curPage;

	public GUILoliConfig(ItemStack stack) {
		super(Component.empty());
		this.stack = stack.copy();
		curPage = 0;
	}

	@Override
	protected void init() {
		this.done = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
			if (stack.hasTag()) {
				NetworkHandler.sendToServer(new LoliItemConfigPacket(ConfigLoader.getItemConfigs(stack)));
			}
			onClose();
		}).bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
		this.pre = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			if (--curPage < 0) {
				curPage = ConfigLoader.loliPickaxeGuiChangeList.size() - 1;
			}
			changePage();
		}).bounds(this.width / 2 - 100, this.height / 2 - 40, 20, 20).build());
		this.next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			if (++curPage >= ConfigLoader.loliPickaxeGuiChangeList.size()) {
				curPage = 0;
			}
			changePage();
		}).bounds(this.width / 2 + 80, this.height / 2 - 40, 20, 20).build());
		this.booleanValue = this.addRenderableWidget(Button.builder(Component.literal("false"), button -> {
			button.setMessage(Component.literal(button.getMessage().getString().equals("false") ? "true" : "false"));
			if (curPage >= 0 && curPage < ConfigLoader.loliPickaxeGuiChangeList.size()) {
				String flag = ConfigLoader.loliPickaxeGuiChangeList.get(curPage);
				ConfigLoader.setBoolean(stack, flag, button.getMessage().getString().equals("true"));
			}
		}).bounds(this.width / 2 - 40, this.height / 2 - 10, 80, 20).build());
		this.otherValue = new EditBox(this.font, this.width / 2 - 80, this.height / 2 - 10, 160, 20, Component.empty());
		this.otherValue.setMaxLength(100);
		this.addRenderableWidget(this.otherValue);
		changePage();
	}

	@Override
	public void tick() {
		this.otherValue.tick();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseClicked(mouseX, mouseY, button);
		applyOtherValue();
		return handled;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// 原实现不绘制默认背景，让世界画面透出
		guiGraphics.blit(LOLI_PICKAXE_CONFIG_GUI_TEXTURE, (this.width - 220) / 2, (this.height - 140) / 2, 0, 0, 220, 120);
		if (curPage >= 0 && curPage < ConfigLoader.loliPickaxeGuiChangeList.size()) {
			String flag = ConfigLoader.loliPickaxeGuiChangeList.get(curPage);
			ConfigField annotations = ConfigLoader.flagAnnotations.get(flag);
			if (annotations != null) {
				guiGraphics.drawCenteredString(this.font, Component.literal(annotations.comment()), this.width / 2, this.height / 2 - 60, 16777215);
			}
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void applyOtherValue() {
		if (this.otherValue.isFocused() || curPage < 0 || curPage >= ConfigLoader.loliPickaxeGuiChangeList.size()) {
			return;
		}
		String flag = ConfigLoader.loliPickaxeGuiChangeList.get(curPage);
		ConfigField annotations = ConfigLoader.flagAnnotations.get(flag);
		if (annotations == null) {
			return;
		}
		try {
			switch (annotations.valueType()) {
			case INT:
				ConfigLoader.setInt(stack, flag, Integer.parseInt(otherValue.getValue()));
				break;
			case DOUBLE:
				ConfigLoader.setDouble(stack, flag, Double.parseDouble(otherValue.getValue()));
				break;
			case STRING:
				ConfigLoader.setString(stack, flag, otherValue.getValue());
				break;
			default:
				break;
			}
		} catch (NumberFormatException e) {
		}
	}

	private void changePage() {
		if (ConfigLoader.loliPickaxeGuiChangeList.isEmpty()) {
			return;
		}
		if (curPage < 0 || curPage >= ConfigLoader.loliPickaxeGuiChangeList.size()) {
			return;
		}
		String flag = ConfigLoader.loliPickaxeGuiChangeList.get(curPage);
		while (!ConfigLoader.flagAnnotations.containsKey(flag) && ConfigLoader.loliPickaxeGuiChangeList.size() > 1) {
			ConfigLoader.loliPickaxeGuiChangeList.remove(curPage);
			if (curPage >= ConfigLoader.loliPickaxeGuiChangeList.size()) {
				curPage = ConfigLoader.loliPickaxeGuiChangeList.size() - 1;
			}
			flag = ConfigLoader.loliPickaxeGuiChangeList.get(curPage);
		}
		ConfigField annotations = ConfigLoader.flagAnnotations.get(flag);
		if (annotations == null) {
			return;
		}
		if (annotations.valueType() == ValurType.BOOLEAN) {
			booleanValue.active = true;
			booleanValue.visible = true;
			otherValue.setEditable(false);
			otherValue.setVisible(false);
		} else {
			booleanValue.active = false;
			booleanValue.visible = false;
			otherValue.setEditable(true);
			otherValue.setVisible(true);
		}
		switch (annotations.valueType()) {
		case INT:
			otherValue.setValue(String.valueOf(ConfigLoader.getInt(stack, flag)));
			break;
		case DOUBLE:
			otherValue.setValue(String.valueOf(ConfigLoader.getDouble(stack, flag)));
			break;
		case STRING:
			otherValue.setValue(String.valueOf(ConfigLoader.getString(stack, flag)));
			break;
		case BOOLEAN:
			booleanValue.setMessage(Component.literal(ConfigLoader.getBoolean(stack, flag) ? "true" : "false"));
			break;
		default:
			break;
		}
	}

}
