package dev.auramate.client.mixin;

import dev.auramate.client.Features;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem Tweaks: skip the full-screen floating totem. The method lives in a different class depending on the version. */
@Mixin({GameRenderer.class, InGameHud.class})
public abstract class TotemMixin {
	@Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void auramate$noTotemPop(ItemStack stack, CallbackInfo ci) {
		if (Features.TOTEM_TWEAKS.isEnabled() && stack.isOf(Items.TOTEM_OF_UNDYING)) ci.cancel();
	}
}
