package com.anotherstar.common.event;

import java.util.Queue;

import com.google.common.collect.Queues;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LoliTickEvent {

	private static final Queue<Runnable> tickStartTasks = Queues.newArrayDeque();
	private static final Queue<Runnable> tickEndTasks = Queues.newArrayDeque();

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == Phase.START) {
			synchronized (tickStartTasks) {
				while (!tickStartTasks.isEmpty()) {
					tickStartTasks.poll().run();
				}
			}
		} else {
			synchronized (tickEndTasks) {
				while (!tickEndTasks.isEmpty()) {
					tickEndTasks.poll().run();
				}
			}
		}
	}

	public static void addTask(Runnable task, Phase phase) {
		if (phase == Phase.START) {
			synchronized (tickStartTasks) {
				tickStartTasks.add(task);
			}
		} else {
			synchronized (tickEndTasks) {
				tickEndTasks.add(task);
			}
		}
	}

	public static class TickStartTask implements Runnable {

		private int tick;
		private Runnable task;

		public TickStartTask(int tick, Runnable task) {
			this.tick = tick;
			this.task = task;
		}

		@Override
		public void run() {
			if (--tick > 0) {
				addTask(new TickEndTask(this), Phase.END);
			} else {
				task.run();
			}
		}

	}

	public static class TickEndTask implements Runnable {

		private Runnable nextTask;

		public TickEndTask(Runnable nextTask) {
			this.nextTask = nextTask;
		}

		@Override
		public void run() {
			addTask(nextTask, Phase.START);
		}

	}

}
