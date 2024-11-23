
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.jaams.jaamscore.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.item.Item;

import net.jaams.jaamscore.JaamsCoreMod;

public class JaamsCoreModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, JaamsCoreMod.MODID);
	public static final RegistryObject<Item> CORE_PLAYER_SPAWN_EGG = REGISTRY.register("core_player_spawn_egg", () -> new ForgeSpawnEggItem(JaamsCoreModEntities.CORE_PLAYER, -6710887, -3355444, new Item.Properties()));
	// Start of user code block custom items
	// End of user code block custom items
}
