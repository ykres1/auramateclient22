package dev.auramate.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Shows the song playing in the Spotify DESKTOP app. No login or API key needed: a background
 * thread asks the OS every few seconds (Windows: the Spotify window title, macOS: AppleScript,
 * Linux: playerctl). It only runs while the feature is on. Display only, no playback controls.
 */
public final class NowPlaying implements HudElement {
	private static volatile String text = "";
	private static volatile boolean playing;
	private static boolean started;

	/** Starts the polling thread once (it idles while the feature is off). */
	public static synchronized void start() {
		if (started) return;
		started = true;
		Thread t = new Thread(() -> {
			while (true) {
				try {
					if (Features.NOW_PLAYING.isEnabled()) poll();
					else {
						text = "";
						playing = false;
					}
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					return;
				} catch (Exception ignored) {
					text = "";
					playing = false;
				}
			}
		}, "AuraMate-NowPlaying");
		t.setDaemon(true);
		t.start();
	}

	private static void poll() throws Exception {
		String os = System.getProperty("os.name", "").toLowerCase();
		String out;
		if (os.contains("win")) {
			out = run("powershell", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command",
					"(Get-Process Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | Select-Object -First 1).MainWindowTitle");
			// Spotify's window title is "Artist - Song" while playing and "Spotify ..." when paused.
			boolean song = out.contains(" - ");
			playing = song;
			text = song ? out : (out.isEmpty() ? "" : "Spotify paused");
		} else if (os.contains("mac")) {
			out = run("osascript", "-e",
					"if application \"Spotify\" is running then tell application \"Spotify\" to if player state is playing then return (artist of current track) & \" - \" & (name of current track)");
			playing = !out.isEmpty();
			text = out;
		} else {
			String status = run("playerctl", "-p", "spotify", "status");
			if (status.equalsIgnoreCase("Playing")) {
				out = run("playerctl", "-p", "spotify", "metadata", "--format", "{{artist}} - {{title}}");
				playing = !out.isEmpty();
				text = out;
			} else {
				playing = false;
				text = status.equalsIgnoreCase("Paused") ? "Spotify paused" : "";
			}
		}
	}

	private static String run(String... cmd) {
		try {
			Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			StringBuilder sb = new StringBuilder();
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = r.readLine()) != null) {
					if (sb.length() > 0) sb.append(' ');
					sb.append(line.trim());
				}
			}
			if (!p.waitFor(3, TimeUnit.SECONDS)) p.destroyForcibly();
			return sb.toString().trim();
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public void render(DrawContext ctx, RenderTickCounter tickCounter) {
		if (!Features.NOW_PLAYING.isEnabled()) return;
		String s = text;
		if (s.isEmpty()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		TextRenderer tr = mc.textRenderer;
		int maxW = ctx.getScaledWindowWidth() / 2;
		String shown = "\u266A " + s;
		if (tr.getWidth(shown) > maxW) shown = tr.trimToWidth(shown, maxW - 8) + "...";
		int w = tr.getWidth(shown) + 10;
		int x = (ctx.getScaledWindowWidth() - w) / 2;
		int y = 4;
		ctx.fill(x, y, x + w, y + 14, 0x88000000);
		ctx.fill(x, y + 13, x + w, y + 14, playing ? 0xFF1DB954 : 0xFF555555);
		ctx.drawTextWithShadow(tr, shown, x + 5, y + 3, playing ? 0xFFFFFFFF : 0xFF9AA5B1);
	}
}
