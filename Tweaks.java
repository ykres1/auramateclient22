package dev.auramate.client;

import dev.auramate.client.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;

/** Small per-tick tweaks: Cobweb Optimizer, Anchor Fast Place and Fullbright. */
public final class Tweaks {
	private static boolean fullbrightApplied;

	private Tweaks() {}

	/** Holding right-click with cobwebs: clear the use cooldown so one is placed every tick. */
	public static void cobweb(MinecraftClient mc) {
		if (!Features.COBWEB_OPT.isEnabled() || mc.player == null || mc.currentScreen != null) return;
		if (!mc.options.useKey.isPressed()) return;
		if (!mc.player.getMainHandStack().isOf(Items.COBWEB) && !mc.player.getOffHandStack().isOf(Items.COBWEB)) return;
		((MinecraftClientAccessor) mc).auramate$setItemUseCooldown(0);
	}

	/** Holding right-click with respawn anchors: clear the use cooldown so one is placed every tick. */
	public static void anchorFast(MinecraftClient mc) {
		if (!Features.ANCHOR_FAST.isEnabled() || mc.player == null || mc.currentScreen != null) return;
		if (!mc.options.useKey.isPressed()) return;
		if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR) && !mc.player.getOffHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
		((MinecraftClientAccessor) mc).auramate$setItemUseCooldown(0);
	}

	/** Client-side night vision (no particles, no icon). Removed again when turned off. */
	public static void fullbright(MinecraftClient mc) {
		if (mc.player == null) {
			fullbrightApplied = false;
			return;
		}
		if (Features.FULLBRIGHT.isEnabled()) {
			StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
			if (cur == null || cur.getDuration() < 400) {
				mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
			}
			fullbrightApplied = true;
		} else if (fullbrightApplied) {
			mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
			fullbrightApplied = false;
		}
	}
}
