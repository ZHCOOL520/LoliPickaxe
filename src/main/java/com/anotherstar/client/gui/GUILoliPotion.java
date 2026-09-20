package com.anotherstar.client.gui;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.anotherstar.client.gui.assembly.GUILoliList;
import com.anotherstar.network.LoliPotionPacket;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class GUILoliPotion extends Screen {

	private ItemStack stack;
	private Button done;
	private Button add;
	private Button remove;
	private EditBox level;
	private GUILoliList potionList;
	private GUILoliList selectPotionList;
	private ResourceLocation[] potions;
	private List<LoliEntry> selectPotions;

	public GUILoliPotion(ItemStack stack) {
		super(Component.empty());
		this.stack = stack.copy();
	}

	@Override
	protected void init() {
		this.done = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
			Map<MobEffect, Integer> potionMap = Maps.newLinkedHashMap();
			if (!selectPotions.isEmpty()) {
				for (LoliEntry entry : selectPotions) {
					MobEffect potion = ForgeRegistries.MOB_EFFECTS.getValue(entry.potion);
					if (potion != null) {
						potionMap.put(potion, entry.level);
					}
				}
			}
			setPotions(potionMap, stack);
			CompoundTag send = new CompoundTag();
			if (stack.hasTag() && stack.getTag().contains("LoliPotion")) {
				ListTag list = stack.getTag().getList("LoliPotion", 10);
				send.put("LoliPotion", list);
			}
			NetworkHandler.sendToServer(new LoliPotionPacket(send));
			onClose();
		}).bounds(this.width / 2 - 160, this.height / 2 + 95, 320, 20).build());
		this.add = this.addRenderableWidget(Button.builder(Component.translatable("gui.loliAdd"), button -> {
			int numberLevel;
			try {
				numberLevel = Integer.parseInt(level.getValue());
			} catch (Exception e) {
				numberLevel = 0;
			}
			if (potionList.selected >= 0 && potionList.selected < potions.length) {
				selectPotions.add(new LoliEntry(potions[potionList.selected], numberLevel));
				selectPotionList.add();
				remove.active = true;
			}
		}).bounds(this.width / 2 + 60, this.height / 2 - 95, 100, 20).build());
		this.remove = this.addRenderableWidget(Button.builder(Component.translatable("gui.loliRemove"), button -> {
			if (selectPotionList.selected >= 0 && selectPotionList.selected < selectPotions.size()) {
				selectPotions.remove(selectPotionList.selected);
				if (selectPotionList.selected >= selectPotions.size()) {
					selectPotionList.selected = selectPotions.size() - 1;
				}
				if (selectPotions.size() == 0) {
					remove.active = false;
				}
				selectPotionList.remove();
			}
		}).bounds(this.width / 2 + 60, this.height / 2 - 65, 100, 20).build());
		this.level = new EditBox(this.font, this.width / 2 + 60, this.height / 2 - 35, 100, 20, Component.empty());
		this.level.setValue("0");
		this.addRenderableWidget(this.level);
		this.potions = ForgeRegistries.MOB_EFFECTS.getKeys().toArray(new ResourceLocation[0]);
		this.potionList = new GUILoliList(this.width / 2 - 160, this.height / 2 - 115, 100, 200, potions.length, 75, 15) {

			@Override
			public void moveElement(int from, int to) {
			}

			@Override
			public String getElementName(int index) {
				MobEffect potion = ForgeRegistries.MOB_EFFECTS.getValue(potions[index]);
				return potion == null ? "" : Component.translatable(potion.getDescriptionId()).getString();
			}

			@Override
			public int getElementColor(int index) {
				return 16777215;
			}

		};
		this.potionList.selected = 0;
		this.selectPotions = Lists.newArrayList();
		Map<MobEffect, Integer> potionMap = getPotions(stack);
		for (Entry<MobEffect, Integer> entry : potionMap.entrySet()) {
			ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(entry.getKey());
			if (id != null) {
				selectPotions.add(new LoliEntry(id, entry.getValue()));
			}
		}
		if (selectPotions.isEmpty()) {
			remove.active = false;
		}
		this.selectPotionList = new GUILoliList(this.width / 2 - 50, this.height / 2 - 115, 100, 200, selectPotions.size(), 75, 15) {

			@Override
			public void moveElement(int from, int to) {
				selectPotions.add(to, selectPotions.remove(from));
			}

			@Override
			public String getElementName(int index) {
				MobEffect potion = ForgeRegistries.MOB_EFFECTS.getValue(selectPotions.get(index).potion);
				int level = selectPotions.get(index).level;
				if (potion == null) {
					return "";
				}
				String name = Component.translatable(potion.getDescriptionId()).getString();
				name += " " + LoliRomeDigitalUtil.intToRoman(level + 1);
				return name;
			}

			@Override
			public int getElementColor(int index) {
				return 16777215;
			}

		};
		this.selectPotionList.selected = 0;
	}

	private Map<MobEffect, Integer> getPotions(ItemStack stack) {
		Map<MobEffect, Integer> map = Maps.newLinkedHashMap();
		ListTag list = stack.hasTag() ? stack.getTag().getList("LoliPotion", 10) : new ListTag();
		for (int i = 0; i < list.size(); ++i) {
			CompoundTag element = list.getCompound(i);
			MobEffect potion = MobEffect.byId(element.getShort("id"));
			int level = element.getByte("lvl");
			if (potion != null) {
				map.put(potion, level);
			}
		}
		return map;
	}

	private void setPotions(Map<MobEffect, Integer> potionMap, ItemStack stack) {
		ListTag list = new ListTag();
		for (Entry<MobEffect, Integer> entry : potionMap.entrySet()) {
			MobEffect potion = entry.getKey();
			if (potion != null) {
				int i = entry.getValue();
				CompoundTag element = new CompoundTag();
				element.putShort("id", (short) MobEffect.getId(potion));
				element.putByte("lvl", (byte) i);
				list.add(element);
			}
		}
		if (list.isEmpty()) {
			if (stack.hasTag()) {
				stack.getTag().remove("LoliPotion");
			}
		} else {
			stack.getOrCreateTag().put("LoliPotion", list);
		}
	}

	@Override
	public void tick() {
		this.level.tick();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseClicked(mouseX, mouseY, button);
		potionList.mouseClick((int) mouseX, (int) mouseY);
		selectPotionList.mouseClick((int) mouseX, (int) mouseY);
		return handled;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		potionList.mouseScrolled(mouseX, mouseY, delta);
		selectPotionList.mouseScrolled(mouseX, mouseY, delta);
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawString(this.font, Component.translatable("gui.loliPotion"), this.width / 2 + 60, this.height / 2 - 110, 16777215, false);
		potionList.draw(guiGraphics, mouseX, mouseY);
		potionList.update(mouseX, mouseY);
		selectPotionList.draw(guiGraphics, mouseX, mouseY);
		selectPotionList.update(mouseX, mouseY);
	}

	private class LoliEntry {

		public ResourceLocation potion;
		public int level;

		public LoliEntry(ResourceLocation potion, int level) {
			this.potion = potion;
			this.level = level;
		}

	}

}
