package com.anotherstar.client.gui;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.anotherstar.client.gui.assembly.GUILoliList;
import com.anotherstar.network.LoliEnchantmentPacket;
import com.anotherstar.network.NetworkHandler;
import com.anotherstar.util.LoliRomeDigitalUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class GUILoliEnchantment extends Screen {

	/** 1.20.1 附魔 NBT 键（1.12.2 为 "ench"）。 */
	private static final String ENCHANTMENT_TAG = "Enchantments";

	private ItemStack stack;
	private Button done;
	private Button add;
	private Button remove;
	private EditBox level;
	private GUILoliList enchantmentList;
	private GUILoliList selectEnchantmentList;
	private ResourceLocation[] enchantments;
	private List<LoliEntry> selectEnchantments;

	public GUILoliEnchantment(ItemStack stack) {
		super(Component.empty());
		this.stack = stack.copy();
	}

	@Override
	protected void init() {
		this.done = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
			Map<Enchantment, Integer> enchMap = Maps.newLinkedHashMap();
			if (!selectEnchantments.isEmpty()) {
				for (LoliEntry entry : selectEnchantments) {
					Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(entry.enchantment);
					if (ench != null) {
						enchMap.put(ench, entry.level);
					}
				}
			}
			EnchantmentHelper.setEnchantments(enchMap, stack);
			CompoundTag send = new CompoundTag();
			if (stack.hasTag() && stack.getTag().contains(ENCHANTMENT_TAG)) {
				ListTag list = stack.getTag().getList(ENCHANTMENT_TAG, 10);
				send.put(ENCHANTMENT_TAG, list);
			}
			NetworkHandler.sendToServer(new LoliEnchantmentPacket(send));
			onClose();
		}).bounds(this.width / 2 - 160, this.height / 2 + 95, 320, 20).build());
		this.add = this.addRenderableWidget(Button.builder(Component.translatable("gui.loliAdd"), button -> {
			int numberLevel;
			try {
				numberLevel = Integer.parseInt(level.getValue());
			} catch (Exception e) {
				numberLevel = 1;
			}
			if (enchantmentList.selected >= 0 && enchantmentList.selected < enchantments.length) {
				selectEnchantments.add(new LoliEntry(enchantments[enchantmentList.selected], numberLevel));
				selectEnchantmentList.add();
				remove.active = true;
			}
		}).bounds(this.width / 2 + 60, this.height / 2 - 95, 100, 20).build());
		this.remove = this.addRenderableWidget(Button.builder(Component.translatable("gui.loliRemove"), button -> {
			if (selectEnchantmentList.selected >= 0 && selectEnchantmentList.selected < selectEnchantments.size()) {
				selectEnchantments.remove(selectEnchantmentList.selected);
				if (selectEnchantmentList.selected >= selectEnchantments.size()) {
					selectEnchantmentList.selected = selectEnchantments.size() - 1;
				}
				if (selectEnchantments.size() == 0) {
					remove.active = false;
				}
				selectEnchantmentList.remove();
			}
		}).bounds(this.width / 2 + 60, this.height / 2 - 65, 100, 20).build());
		this.level = new EditBox(this.font, this.width / 2 + 60, this.height / 2 - 35, 100, 20, Component.empty());
		this.level.setValue("1");
		this.addRenderableWidget(this.level);
		this.enchantments = ForgeRegistries.ENCHANTMENTS.getKeys().toArray(new ResourceLocation[0]);
		this.enchantmentList = new GUILoliList(this.width / 2 - 160, this.height / 2 - 115, 100, 200, enchantments.length, 75, 15) {

			@Override
			public void moveElement(int from, int to) {
			}

			@Override
			public String getElementName(int index) {
				Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(enchantments[index]);
				return ench == null ? "" : Component.translatable(ench.getDescriptionId()).getString();
			}

			@Override
			public int getElementColor(int index) {
				return 16777215;
			}

		};
		this.enchantmentList.selected = 0;
		this.selectEnchantments = Lists.newArrayList();
		Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
		for (Entry<Enchantment, Integer> entry : enchMap.entrySet()) {
			ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
			if (id != null) {
				selectEnchantments.add(new LoliEntry(id, entry.getValue()));
			}
		}
		if (selectEnchantments.isEmpty()) {
			remove.active = false;
		}
		this.selectEnchantmentList = new GUILoliList(this.width / 2 - 50, this.height / 2 - 115, 100, 200, selectEnchantments.size(), 75, 15) {

			@Override
			public void moveElement(int from, int to) {
				selectEnchantments.add(to, selectEnchantments.remove(from));
			}

			@Override
			public String getElementName(int index) {
				Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(selectEnchantments.get(index).enchantment);
				int level = selectEnchantments.get(index).level;
				if (ench == null) {
					return "";
				}
				String name = Component.translatable(ench.getDescriptionId()).getString();
				if (level != 1 || ench.getMaxLevel() != 1) {
					name += " " + LoliRomeDigitalUtil.intToRoman(level);
				}
				return name;
			}

			@Override
			public int getElementColor(int index) {
				// 原实现用 TextFormatting.RED 前缀标记诅咒附魔，这里改为直接返回红色
				Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(selectEnchantments.get(index).enchantment);
				return ench != null && ench.isCurse() ? 16733525 : 16777215;
			}

		};
		this.selectEnchantmentList.selected = 0;
	}

	@Override
	public void tick() {
		this.level.tick();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseClicked(mouseX, mouseY, button);
		enchantmentList.mouseClick((int) mouseX, (int) mouseY);
		selectEnchantmentList.mouseClick((int) mouseX, (int) mouseY);
		return handled;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		enchantmentList.mouseScrolled(mouseX, mouseY, delta);
		selectEnchantmentList.mouseScrolled(mouseX, mouseY, delta);
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawString(this.font, Component.translatable("gui.loliEnch"), this.width / 2 + 60, this.height / 2 - 110, 16777215, false);
		enchantmentList.draw(guiGraphics, mouseX, mouseY);
		enchantmentList.update(mouseX, mouseY);
		selectEnchantmentList.draw(guiGraphics, mouseX, mouseY);
		selectEnchantmentList.update(mouseX, mouseY);
	}

	private class LoliEntry {

		public ResourceLocation enchantment;
		public int level;

		public LoliEntry(ResourceLocation enchantment, int level) {
			this.enchantment = enchantment;
			this.level = level;
		}

	}

}
