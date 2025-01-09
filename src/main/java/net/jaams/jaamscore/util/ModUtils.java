package net.jaams.jaamscore.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;

public class ModUtils {
	public static boolean isEntityInBattleMode(Entity entity) {
		if (entity == null) {
			return false;
		}
		CompoundTag entityData = new CompoundTag();
		entity.saveWithoutId(entityData);
		CompoundTag forgeCaps = entityData.getCompound("ForgeCaps");
		if (forgeCaps.contains("epicfight:skill_cap")) {
			CompoundTag skillCap = forgeCaps.getCompound("epicfight:skill_cap");
			String playerMode = skillCap.getString("playerMode");
			return "BATTLE".equals(playerMode);
		}
		return false;
	}
}
