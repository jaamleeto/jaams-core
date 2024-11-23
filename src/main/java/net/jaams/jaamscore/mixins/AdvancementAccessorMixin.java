package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.advancements.Advancement;

@Mixin(AdvancementToast.class)
public interface AdvancementAccessorMixin {
	@Accessor("advancement")
	Advancement getAdvancement();
}
