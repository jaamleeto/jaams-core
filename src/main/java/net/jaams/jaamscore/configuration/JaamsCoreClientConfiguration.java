package net.jaams.jaamscore.configuration;

import net.minecraftforge.common.ForgeConfigSpec;

public class JaamsCoreClientConfiguration {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;
	// Toast Handler settings
	public static final ForgeConfigSpec.ConfigValue<Boolean> TOASTTASK;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TOASTGOAL;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TOASTCHALLENGE;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TOASTRECIPE;
	public static final ForgeConfigSpec.ConfigValue<Boolean> TOASTOTHER;
	// Back Item Layer Configuration
	public static final ForgeConfigSpec.ConfigValue<Boolean> RENDERFIXED;
	public static final ForgeConfigSpec.ConfigValue<Boolean> BACKMESSAGES;
	// Transformation Helper Configuration
	public static final ForgeConfigSpec.ConfigValue<Double> TRANSLATEX;
	public static final ForgeConfigSpec.ConfigValue<Double> TRANSLATEY;
	public static final ForgeConfigSpec.ConfigValue<Double> TRANSLATEZ;
	public static final ForgeConfigSpec.ConfigValue<Double> ROTATIONX;
	public static final ForgeConfigSpec.ConfigValue<Double> ROTATIONY;
	public static final ForgeConfigSpec.ConfigValue<Double> ROTATIONZ;
	public static final ForgeConfigSpec.ConfigValue<Double> ROTATIONXN;
	public static final ForgeConfigSpec.ConfigValue<Double> ROTATIONYN;
	public static final ForgeConfigSpec.ConfigValue<Double> ROTATIONZN;
	public static final ForgeConfigSpec.ConfigValue<Double> SCALE;
	static {
		// Toast Handler
		BUILDER.push("Toast Handler");
		TOASTTASK = BUILDER.comment("Enables or disables toast notifications for tasks.").define("Enable Task Toasts", true);
		TOASTGOAL = BUILDER.comment("Enables or disables toast notifications for goals.").define("Enable Goal Toasts", true);
		TOASTCHALLENGE = BUILDER.comment("Enables or disables toast notifications for challenges.").define("Enable Challenge Toasts", true);
		TOASTRECIPE = BUILDER.comment("Enables or disables toast notifications for recipes.").define("Enable Recipe Toasts", true);
		TOASTOTHER = BUILDER.comment("Enables or disables toast notifications for other events.").define("Enable Other Toasts", true);
		BUILDER.pop();
		// Back Item Layer Configuration
		BUILDER.push("Back Item Layer");
		RENDERFIXED = BUILDER.comment("Fix render items on the back").define("Render Fixed", false);
		BACKMESSAGES = BUILDER.comment("Enable back item messages").define("Back Item Messages", true);
		BUILDER.pop();
		// Transformation Helper Configuration
		BUILDER.push("Transform Helper");
		TRANSLATEX = BUILDER.comment("Translation on X axis").define("Translate X", 0.5);
		TRANSLATEY = BUILDER.comment("Translation on Y axis").define("Translate Y", 0.5);
		TRANSLATEZ = BUILDER.comment("Translation on Z axis").define("Translate Z", 0.5);
		ROTATIONX = BUILDER.comment("Rotation on X axis").define("Rotation X", 0.0);
		ROTATIONY = BUILDER.comment("Rotation on Y axis").define("Rotation Y", 0.0);
		ROTATIONZ = BUILDER.comment("Rotation on Z axis").define("Rotation Z", 0.0);
		ROTATIONXN = BUILDER.comment("Negative rotation on X axis").define("Rotation NX", 0.0);
		ROTATIONYN = BUILDER.comment("Negative rotation on Y axis").define("Rotation NY", 0.0);
		ROTATIONZN = BUILDER.comment("Negative rotation on Z axis").define("Rotation NZ", 0.0);
		SCALE = BUILDER.comment("Scaling factor").define("Scale", 1.0);
		BUILDER.pop();
		// Build the configuration
		SPEC = BUILDER.build();
	}
}
