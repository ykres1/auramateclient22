package dev.auramate.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

/** Red flash around the screen edges while you are in the hurt animation. */
public final class DamageTint implements HudElement {
	@Override
	public void render(DrawContext ctx, RenderTickCounter tickCounter) {
		if (!Features.DAMAGE_TINT.isEnabled()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null || p.hurtTime <= 0 || p.maxHurtTime <= 0) return;

		float fade = p.hurtTime / (float) p.maxHurtTime;
		float strength = Features.DAMAGE_TINT.value() / 100f;
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();
		int t = Math.max(3, Math.min(w, h) / 30);
		for (int k = 0; k < 8; k++) {
			int a = (int) (200 * fade * strength * (1 - k / 8f));
			if (a <= 0) break;
			int color = (a << 24) | 0xD01010;
			int o = k * t;
			ctx.fill(0, o, w, o + t, color);                 // top
			ctx.fill(0, h - o - t, w, h - o, color);         // bottom
			ctx.fill(o, o + t, o + t, h - o - t, color);     // left
			ctx.fill(w - o - t, o + t, w - o, h - o - t, color); // right
		}
	}
}
