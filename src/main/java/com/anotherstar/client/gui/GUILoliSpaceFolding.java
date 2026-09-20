package com.anotherstar.client.gui;

import java.util.List;
import java.util.Map;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.network.LoliSpaceFoldingPacket;
import com.anotherstar.network.NetworkHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GUILoliSpaceFolding extends Screen {

	private static final ResourceLocation LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE = new ResourceLocation(LoliPickaxe.MODID, "textures/gui/loli_pickaxe_space_folding.png");

	private final Player player;
	private final Map<Button, ResourceKey<Level>> worldButtons = Maps.newHashMap();
	private ResourceKey<Level> selectedWorld;
	private Button selectedWorldButton;
	private Button absoluteButton;
	private Button relativeButton;
	private boolean absolute;
	private EditBox xField;
	private EditBox yField;
	private EditBox zField;
	private int count;
	private int row;
	private int col;
	private int dx;
	private int dy;

	public GUILoliSpaceFolding(Player player) {
		super(Component.empty());
		this.player = player;
		this.absolute = true;
	}

	@Override
	protected void init() {
		List<ResourceKey<Level>> ids = Lists.newArrayList();
		ClientPacketListener connection = this.minecraft.getConnection();
		if (connection != null) {
			ids.addAll(connection.levels());
		}
		ResourceKey<Level> current = this.minecraft.level == null ? null : this.minecraft.level.dimension();
		if (current != null && !ids.contains(current)) {
			ids.add(current);
		}
		count = ids.size();
		row = (count - 1) / 4 + 1;
		col = count > 4 ? 4 : count;
		dx = 5 - col * 35;
		dy = -25 - row * 15;
		int index = 0;
		for (ResourceKey<Level> id : ids) {
			Button button = this.addRenderableWidget(Button.builder(Component.literal(id.location().toString()), this::selectWorld).bounds(this.width / 2 + dx + (index % col) * 70, this.height / 2 + dy + index / 4 * 30, 60, 20).build());
			worldButtons.put(button, id);
			if (id.equals(current)) {
				button.active = false;
				selectedWorldButton = button;
				selectedWorld = id;
			}
			index++;
		}
		row += 2;
		col = 4;
		dx = -135;
		this.xField = new EditBox(this.font, this.width / 2 + dx, this.height / 2 + dy + row * 30 - 60, 60, 20, Component.empty());
		this.xField.setMaxLength(100);
		this.xField.setValue(String.valueOf(player.getX()));
		this.addRenderableWidget(this.xField);
		this.yField = new EditBox(this.font, this.width / 2 + dx + 70, this.height / 2 + dy + row * 30 - 60, 60, 20, Component.empty());
		this.yField.setMaxLength(100);
		this.yField.setValue(String.valueOf(player.getY()));
		this.addRenderableWidget(this.yField);
		this.zField = new EditBox(this.font, this.width / 2 + dx + 140, this.height / 2 + dy + row * 30 - 60, 60, 20, Component.empty());
		this.zField.setMaxLength(100);
		this.zField.setValue(String.valueOf(player.getZ()));
		this.addRenderableWidget(this.zField);
		this.setInitialFocus(this.xField);
		this.absoluteButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.loliAbsolute"), button -> selectLocation(true)).bounds(this.width / 2 + dx, this.height / 2 + dy + row * 30 - 30, 60, 20).build());
		this.absoluteButton.active = false;
		this.relativeButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.loliRelative"), button -> selectLocation(false)).bounds(this.width / 2 + dx + 70, this.height / 2 + dy + row * 30 - 30, 60, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> confirm()).bounds(this.width / 2 + dx + 210, this.height / 2 + dy + row * 30 - 60, 60, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(this.width / 2 + dx + 140, this.height / 2 + dy + row * 30 - 30, 130, 20).build());
	}

	private void selectWorld(Button button) {
		ResourceKey<Level> id = worldButtons.get(button);
		if (id == null) {
			return;
		}
		if (selectedWorldButton != null) {
			selectedWorldButton.active = true;
		}
		button.active = false;
		selectedWorldButton = button;
		selectedWorld = id;
	}

	private void selectLocation(boolean toAbsolute) {
		if (absolute == toAbsolute) {
			return;
		}
		try {
			double x = Double.parseDouble(xField.getValue());
			double y = Double.parseDouble(yField.getValue());
			double z = Double.parseDouble(zField.getValue());
			if (toAbsolute) {
				xField.setValue(String.valueOf(x + player.getX()));
				yField.setValue(String.valueOf(y + player.getY()));
				zField.setValue(String.valueOf(z + player.getZ()));
			} else {
				xField.setValue(String.valueOf(x - player.getX()));
				yField.setValue(String.valueOf(y - player.getY()));
				zField.setValue(String.valueOf(z - player.getZ()));
			}
		} catch (NumberFormatException e) {
			xField.setValue("0.0");
			yField.setValue("0.0");
			zField.setValue("0.0");
		}
		absolute = toAbsolute;
		absoluteButton.active = !toAbsolute;
		relativeButton.active = toAbsolute;
	}

	private void confirm() {
		if (selectedWorld != null) {
			try {
				double x = Double.parseDouble(xField.getValue());
				double y = Double.parseDouble(yField.getValue());
				double z = Double.parseDouble(zField.getValue());
				if (absolute) {
					x -= player.getX();
					y -= player.getY();
					z -= player.getZ();
				}
				NetworkHandler.sendToServer(new LoliSpaceFoldingPacket(selectedWorld, x, y, z));
			} catch (NumberFormatException e) {
			}
		}
		onClose();
	}

	@Override
	public void tick() {
		this.xField.tick();
		this.yField.tick();
		this.zField.tick();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx - 10, this.height / 2 + dy - 10, 0, 0, 10, 10);
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * col - 10, this.height / 2 + dy - 10, 80, 0, 10, 10);
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx - 10, this.height / 2 + dy + 30 * row - 10, 0, 40, 10, 10);
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * col - 10, this.height / 2 + dy + 30 * row - 10, 80, 40, 10, 10);
		for (int i = 0; i < col - 1; i++) {
			guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * i, this.height / 2 + dy - 10, 10, 0, 70, 10);
			guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * i, this.height / 2 + dy + 30 * row - 10, 10, 40, 70, 10);
		}
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * col - 70, this.height / 2 + dy - 10, 10, 0, 60, 10);
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * col - 70, this.height / 2 + dy + 30 * row - 10, 10, 40, 60, 10);
		for (int i = 0; i < row - 1; i++) {
			guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx - 10, this.height / 2 + dy + 30 * i, 0, 10, 10, 30);
			guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * col - 10, this.height / 2 + dy + 30 * i, 80, 10, 10, 30);
		}
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx - 10, this.height / 2 + dy + 30 * row - 30, 0, 10, 10, 20);
		guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * col - 10, this.height / 2 + dy + 30 * row - 30, 80, 10, 10, 20);
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				guiGraphics.blit(LOLI_PICKAXE_SPACE_FOLDING_GUI_TEXTURE, this.width / 2 + dx + 70 * j, this.height / 2 + dy + 30 * i, 10, 10, j == col - 1 ? 60 : 70, i == row - 1 ? 20 : 30);
			}
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

}
