package net.jaams.jaamscore.manager;

import org.checkerframework.checker.units.qual.min;

import net.minecraft.server.dedicated.Settings;

import java.util.List;

public class CustomSpawnManager {
	public String entity;
	public Settings settings;
	public Conditions conditions;

	public static class Settings {
		public double chance;
		public SpawnGroup spawnGroup;
	}

	public static class SpawnGroup {
		public int min;
		public int max;
	}

	public static class Conditions {
		public List<String> biomes;
		public List<String> structures;
		public List<String> on_blocks;
		public Integer min_height;
		public Integer max_height;
		public String time_of_day;
		public String weather;
	}
}
