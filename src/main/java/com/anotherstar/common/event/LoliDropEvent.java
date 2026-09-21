package com.anotherstar.common.event;

import com.anotherstar.common.config.ConfigLoader;
import com.anotherstar.common.item.ItemLoader;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LoliDropEvent {

	@SubscribeEvent
	public void onLivingDrop(LivingDropsEvent event) {
		LivingEntity entity = event.getEntity();
		Level level = entity.level();
		if (level.random.nextDouble() < ConfigLoader.loliCardDropProbability) {
			event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(ItemLoader.loliCard())));
		}
		if (level.random.nextDouble() < ConfigLoader.loliCardAlbumDropProbability) {
			event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(ItemLoader.loliCardAlbum())));
		}
		if (!ItemLoader.loliRecords.isEmpty() && entity instanceof Creeper && level.random.nextDouble() < ConfigLoader.loliRecordDropProbability) {
			event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(ItemLoader.loliRecords.get(level.random.nextInt(ItemLoader.loliRecords.size())))));
		}
		if (level.random.nextDouble() < ConfigLoader.entitySoulDropProbability) {
			event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(ItemLoader.entitySoul())));
		}
	}

}
