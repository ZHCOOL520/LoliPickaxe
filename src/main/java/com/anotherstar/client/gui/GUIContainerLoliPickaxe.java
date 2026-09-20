package com.anotherstar.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.anotherstar.common.LoliPickaxe;
import com.anotherstar.common.gui.ContainerLoliPickaxe;
import com.anotherstar.common.gui.InventoryLoliBase;
import com.anotherstar.network.LoliPickaxeContainerPackte;
import com.anotherstar.network.NetworkHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUIContainerLoliPickaxe extends AbstractContainerScreen<ContainerLoliPickaxe> {

	private static final ResourceLocation LOLI_PICKAXE_CONTAINER_GUI_TEXTURE = new ResourceLocation(LoliPickaxe.MODID, "textures/gui/container/loli_pickaxe_container.png");

	private final ContainerLoliPickaxe loliContainer;

	public GUIContainerLoliPickaxe(ContainerLoliPickaxe menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.loliContainer = menu;
		this.imageWidth = 240;
		this.imageHeight = 256;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			loliContainer.prePage();
			NetworkHandler.sendToServer(new LoliPickaxeContainerPackte(false));
		}).bounds(this.leftPos + 173, this.topPos + 22, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			loliContainer.nextPage();
			NetworkHandler.sendToServer(new LoliPickaxeContainerPackte(true));
		}).bounds(this.leftPos + 213, this.topPos + 22, 20, 20).build());
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(LOLI_PICKAXE_CONTAINER_GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		Component name = loliContainer.inventory instanceof InventoryLoliBase ? ((InventoryLoliBase) loliContainer.inventory).getDisplayName() : Component.empty();
		guiGraphics.drawString(this.font, name, 173, 8, 4210752, false);
		String page = String.valueOf(loliContainer.getCurrentPage() + 1);
		guiGraphics.drawString(this.font, page, 203 - this.font.width(page) / 2, 27, 4210752, false);
	}

	@Override
	protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
			ItemStack stack = this.hoveredSlot.getItem();
			List<Component> tip = new ArrayList<>(getTooltipFromContainerItem(stack));
			if (stack.getCount() > 1000) {
				tip.add(Component.translatable("container.loliPickaxe.stackCount", stack.getCount()));
			}
			guiGraphics.renderTooltip(this.font, tip, stack.getTooltipImage(), stack, mouseX, mouseY);
			return;
		}
		super.renderTooltip(guiGraphics, mouseX, mouseY);
	}

}
