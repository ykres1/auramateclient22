package dev.auramate.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Hearts {
	private Hearts() {}

	public static boolean isEnabled() {
		return Features.HEARTS.isEnabled();
	}

	public static Text decorate(PlayerEntity player, Text base) {
		MutableText out = Text.empty().append(base);
		out.append(Text.literal(" ♥ " + fmt(player.getHealth())).formatted(Formatting.RED));
		Float sat = saturation(player);
		if (sat != null) {
			out.append(Text.literal(" Sat " + fmt(sat)).formatted(Formatting.GOLD));
		}
		return out;
	}

	/** Own saturation is always known; others only on an integrated server. */
	private static Float saturation(PlayerEntity player) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (player == mc.player) return player.getHungerManager().getSaturationLevel();
		if (mc.getServer() != null) {
			ServerPlayerEntity sp = mc.getServer().getPlayerManager().getPlayer(player.getUuid());
			if (sp != null) return sp.getHungerManager().getSaturationLevel();
		}
		return null;
	}

	private static String fmt(float v) {
		return String.format("%.1f", v);
	}
}
