package net.jaams.jaamscore.configuration;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class JaamsCoreCommonConfiguration {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TASKFIREWORKS;
	public static final ForgeConfigSpec.ConfigValue<Double> TASKFIREWORKSAMOUNT;
	public static final ForgeConfigSpec.ConfigValue<Double> TASKFIREWORKSFLIGHT;
	public static final ForgeConfigSpec.ConfigValue<Double> TASKFIREWORKSTYPE;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TASKFIREWORKSFLICKER;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TASKFIREWORKSTRAIL;
	public static final ForgeConfigSpec.ConfigValue<Boolean> GOALFIREWORKS;
	public static final ForgeConfigSpec.ConfigValue<Double> GOALFIREWORKSAMOUNT;
	public static final ForgeConfigSpec.ConfigValue<Double> GOALFIREWORKSFLIGHT;
	public static final ForgeConfigSpec.ConfigValue<Double> GOALFIREWORKSTYPE;
	public static final ForgeConfigSpec.ConfigValue<Boolean> GOALFIREWORKSFLICKER;
	public static final ForgeConfigSpec.ConfigValue<Boolean> GOALFIREWORKSTRAIL;
	public static final ForgeConfigSpec.ConfigValue<Boolean> CHALLENGEFIREWORKS;
	public static final ForgeConfigSpec.ConfigValue<Double> CHALLENGEFIREWORKSAMOUNT;
	public static final ForgeConfigSpec.ConfigValue<Double> CHALLENGEFIREWORKSFLIGHT;
	public static final ForgeConfigSpec.ConfigValue<Double> CHALLENGEFIREWORKSTYPE;
	public static final ForgeConfigSpec.ConfigValue<Boolean> CHALLENGEFIREWORKSFLICKER;
	public static final ForgeConfigSpec.ConfigValue<Boolean> CHALLENGEFIREWORKSTRAIL;
	// Mechanics
	public static final ForgeConfigSpec.ConfigValue<Boolean> TWOHANDED;
	// Other
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BACKBLACKLIST;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BACKWHITELIST;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TWOHANDEDWHITELIST;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TWOHANDEDBLACKLIST;
	// Back Item Mechanic
	public static final ForgeConfigSpec.ConfigValue<Boolean> BACKITEMMECHANIC;
	public static final ForgeConfigSpec.ConfigValue<Boolean> BACKITEMSHIELD;
	public static final ForgeConfigSpec.ConfigValue<Boolean> BACKITEMTOTEM;
	public static final ForgeConfigSpec.ConfigValue<Boolean> BACKITEMEXPLOSIVES;
	// Entity Behavior customization
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AVOIDENTITIES;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTACKENTITIES;
	static {
		BUILDER.push("Dramatic Advancements");
		// Task Fireworks
		BUILDER.push("Advancements Type Task");
		TASKFIREWORKS = BUILDER.comment("Enables or disables fireworks for task completions.").define("Enable Fireworks", true);
		TASKFIREWORKSAMOUNT = BUILDER.comment("Sets the amount of fireworks for tasks.").defineInRange("Fireworks Amount", 1.0, 1.0, 10.0); // Min 1, Max 10
		TASKFIREWORKSFLIGHT = BUILDER.comment("Sets the flight duration of fireworks for tasks.").defineInRange("Fireworks Flight", -0.5, -1.0, 3.0); // Min 0, Max 3 (flight duration)
		TASKFIREWORKSTYPE = BUILDER.comment("Sets the type of fireworks for tasks.").defineInRange("Fireworks Type", 6.0, 0.0, 4.0); // Only valid types are 0-4
		TASKFIREWORKSFLICKER = BUILDER.comment("Enables or disables the flicker effect for task fireworks.").define("Fireworks Flicker", true);
		TASKFIREWORKSTRAIL = BUILDER.comment("Enables or disables the trail effect for task fireworks.").define("Fireworks Trail", true);
		BUILDER.pop();
		// Goal Fireworks
		BUILDER.push("Advancements Type Goal");
		GOALFIREWORKS = BUILDER.comment("Enables or disables fireworks for goal completions.").define("Enable Fireworks", true);
		GOALFIREWORKSAMOUNT = BUILDER.comment("Sets the amount of fireworks for goals.").defineInRange("Fireworks Amount", 1.0, 1.0, 10.0); // Min 1, Max 10
		GOALFIREWORKSFLIGHT = BUILDER.comment("Sets the flight duration of fireworks for goals.").defineInRange("Fireworks Flight", -0.5, -1.0, 3.0); // Min 0, Max 3
		GOALFIREWORKSTYPE = BUILDER.comment("Sets the type of fireworks for goals.").defineInRange("Fireworks Type", 4.0, 0.0, 4.0); // Only valid types are 0-4
		GOALFIREWORKSFLICKER = BUILDER.comment("Enables or disables the flicker effect for goal fireworks.").define("Fireworks Flicker", true);
		GOALFIREWORKSTRAIL = BUILDER.comment("Enables or disables the trail effect for goal fireworks.").define("Fireworks Trail", true);
		BUILDER.pop();
		// Challenge Fireworks
		BUILDER.push("Advancements Type Challenge");
		CHALLENGEFIREWORKS = BUILDER.comment("Enables or disables fireworks for challenge completions.").define("Enable Fireworks", true);
		CHALLENGEFIREWORKSAMOUNT = BUILDER.comment("Sets the amount of fireworks for challenges.").defineInRange("Fireworks Amount", 1.0, 1.0, 10.0); // Min 1, Max 10
		CHALLENGEFIREWORKSFLIGHT = BUILDER.comment("Sets the flight duration of fireworks for challenges.").defineInRange("Fireworks Flight", -0.5, -1.0, 3.0); // Min 0, Max 3
		CHALLENGEFIREWORKSTYPE = BUILDER.comment("Sets the type of fireworks for challenges.").defineInRange("Fireworks Type", 3.0, 0.0, 4.0); // Only valid types are 0-4
		CHALLENGEFIREWORKSFLICKER = BUILDER.comment("Enables or disables the flicker effect for challenge fireworks.").define("Fireworks Flicker", true);
		CHALLENGEFIREWORKSTRAIL = BUILDER.comment("Enables or disables the trail effect for challenge fireworks.").define("Fireworks Trail", true);
		BUILDER.pop();
		BUILDER.pop();
		// Entity Behavior Customization
		BUILDER.push("Core Player Handler");
		AVOIDENTITIES = BUILDER.comment("Entities  that core player avoid.").defineList("Avoid Entities", List.of("minecraft:creeper", "minecraft:warden"), entry -> entry instanceof String);
		ATTACKENTITIES = BUILDER.comment("Entities that core player should attack.").defineList("Attack Entities", List.of("minecraft:zombie"), entry -> entry instanceof String);
		BUILDER.pop();
		// Mod Mechanics
		BUILDER.push("Mod Mechanics");
		BUILDER.push("Two Handed");
		BUILDER.push("Two Handed Compat");
		TWOHANDEDWHITELIST = BUILDER.comment("Whitelist for two-handed weapons").defineList("Two Handed Whitelist", List.of(), entry -> true);
		TWOHANDEDBLACKLIST = BUILDER.comment("Blacklist for two-handed weapons").defineList("Two Handed Blacklist", List.of(), entry -> true);
		BUILDER.pop();
		TWOHANDED = BUILDER.comment("Enable or disable two-handed item mechanic").define("Two Handed", true);
		BUILDER.pop();
		BUILDER.push("Back Item");
		BUILDER.push("Back Item Mechanic Compat");
		BACKBLACKLIST = BUILDER.comment("Blacklist for back items").defineList("Back Item Blacklist", List.of("jaams_weaponry:gauntlet"), entry -> true);
		BACKWHITELIST = BUILDER.comment("Whitelist for back items").defineList("Back Item Whitelist", List.of("minecraft:fishing_rod", "minecraft:totem_of_undying", "jaams_weaponry:dynamite"), entry -> true);
		BUILDER.pop();
		BUILDER.push("Back Item Handler");
		BACKITEMMECHANIC = BUILDER.comment("Enable or disable if the back item mechanic should be active").define("Back Item Mechanic", true);
		BACKITEMSHIELD = BUILDER.comment("Enable or disable if shields on the back can block attacks").define("Shield Back Item", true);
		BACKITEMTOTEM = BUILDER.comment("Enable or disable if totems of undying on the back activate upon death").define("Totem Back Item ", true);
		BACKITEMEXPLOSIVES = BUILDER.comment("Enable or disable if explosives on the back can be activated").define("Explosives Back Item ", true);
		BUILDER.pop();
		BUILDER.pop();
		// Build the configuration
		SPEC = BUILDER.build();
	}
}
