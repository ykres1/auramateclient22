package dev.auramate.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny properties-file config: feature toggles, lunge delay, panel position. */
public final class Config {
	private static final Properties P = new Properties();
	private static Path file;
	private static boolean loaded;

	private Config() {}

	private static synchronized void load() {
		if (loaded) return;
		loaded = true;
		file = FabricLoader.getInstance().getConfigDir().resolve("auramateclient.properties");
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file)) {
				P.load(r);
			} catch (IOException ignored) {}
		}
	}

	public static synchronized void save() {
		load();
		try (Writer w = Files.newBufferedWriter(file)) {
			P.store(w, "AuraMateClient");
		} catch (IOException ignored) {}
	}

	public static boolean getBool(String key, boolean def) {
		load();
		return Boolean.parseBoolean(P.getProperty(key, Boolean.toString(def)).trim());
	}

	public static int getInt(String key, int def) {
		load();
		try {
			return Integer.parseInt(P.getProperty(key, Integer.toString(def)).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public static void set(String key, Object value) {
		load();
		P.setProperty(key, String.valueOf(value));
	}
}
