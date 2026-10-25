package dev.auramate.client;

/** A feature with one saved number (ticks, percent, ...) edited with the panel stepper. */
public class IntSettingFeature extends Feature {
	private final String key, label, unit;
	private final int min, max, step;
	private int value;

	public IntSettingFeature(String id, String name, String description, boolean needsRestart, boolean defaultEnabled,
							 String category, String key, int def, int min, int max, int step, String unit, String label) {
		super(id, name, description, needsRestart, defaultEnabled, category);
		this.key = key;
		this.min = min;
		this.max = max;
		this.step = step;
		this.unit = unit;
		this.label = label;
		this.value = Math.max(min, Math.min(max, Config.getInt(key, def)));
	}

	public int value() {
		return value;
	}

	@Override
	public Setting setting() {
		return new Setting(label, min, max, () -> value, v -> {
			value = Math.max(min, Math.min(max, v));
			Config.set(key, value);
			Config.save();
		}, unit, step);
	}
}
