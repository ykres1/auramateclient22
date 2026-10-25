package dev.auramate.client.mixin;

import dev.auramate.client.Features;
import net.minecraft.client.gui.LogoDrawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the "Minecraft Java Edition" logo so AuraMate's own logo can be drawn instead. */
@Mixin(LogoDrawer.class)
public class LogoDrawerMixin {
	@Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0)
	private void auramate$hideLogo(CallbackInfo ci) {
		if (Features.TITLE.isEnabled()) ci.cancel();
	}
}
