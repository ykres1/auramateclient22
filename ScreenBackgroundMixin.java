package dev.auramate.client.mixin;

import dev.auramate.client.AuroraBackground;
import dev.auramate.client.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the menu panorama with the snowy mountain + aurora on the title screen and on every other
 * menu shown outside a world (Singleplayer, Multiplayer, Options, ...). The panorama is drawn through
 * different methods depending on the version, so every candidate is hooked (missing ones are skipped).
 */
@Mixin({Screen.class, TitleScreen.class})
public abstract class ScreenBackgroundMixin {
	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void auramate$background(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (!Features.MOUNTAIN_BG.isEnabled() || MinecraftClient.getInstance().world != null) return;
		AuroraBackground.draw(context, self.width, self.height);
		// other menus get a light dim so their text stays readable; the title screen stays bright
		if (!(self instanceof TitleScreen)) context.fill(0, 0, self.width, self.height, 0x66000000);
		ci.cancel();
	}

	@Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void auramate$panorama(DrawContext context, float deltaTicks, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (!Features.MOUNTAIN_BG.isEnabled() || MinecraftClient.getInstance().world != null) return;
		AuroraBackground.draw(context, self.width, self.height);
		ci.cancel();
	}
}
