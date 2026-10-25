package dev.auramate.client;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** One toggleable feature shown as a row in the panel. */
public class Feature {
	public final String id;
	public final String name;
	public final String description;
	/** If true the change only applies after a restart (shown in the panel). */
	public final boolean needsRestart;
	/** Which panel tab the row lives in (Optimizers / Visuals / HUD). */
	public final String category;
	private boolean enabled;

	public Feature(String id, String name, String description, boolean needsRestart, boolean defaultEnabled) {
		this(id, name, description, needsRestart, defaultEnabled, "Visuals");
	}

	public Feature(String id, String name, String description, boolean needsRestart, boolean defaultEnabled, String category) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.needsRestart = needsRestart;
		this.category = category;
		this.enabled = Config.getBool("feature." + id, defaultEnabled);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		if (enabled == value) return;
		enabled = value;
		Config.set("feature." + id, value);
		Config.save();
		onToggle(value);
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	protected void onToggle(boolean value) {}

	/** Optional numeric setting shown as a [-] n [+] stepper. Null for none. */
	public Setting setting() {
		return null;
	}

	public record Setting(String label, int min, int max, IntSupplier get, IntConsumer set, String unit, int step) {
		public Setting(String label, int min, int max, IntSupplier get, IntConsumer set) {
			this(label, min, max, get, set, "t", 1);
		}
	}
}
