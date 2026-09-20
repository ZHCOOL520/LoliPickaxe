package com.anotherstar.common.item;

import java.util.function.Supplier;

import com.anotherstar.common.LoliPickaxe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;

public class ItemLoliRecord extends RecordItem {

	/** 唱片时长未知，给一个足够长的默认值，避免点唱机立刻弹出唱片。 */
	private static final int RECORD_LENGTH_TICKS = 20 * 60 * 3;

	public ItemLoliRecord(String name, String record) {
		super(0, soundSupplier(record), new Item.Properties().stacksTo(1), RECORD_LENGTH_TICKS);
	}

	private static Supplier<SoundEvent> soundSupplier(String record) {
		SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(LoliPickaxe.MODID, record));
		return () -> sound;
	}

}
