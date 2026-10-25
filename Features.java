package dev.auramate.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Features {
	private static final List<Feature> ALL = new ArrayList<>();

	// ---- Optimizers ----
	public static final LungeSwapFeature LUNGE_SWAP = register(new LungeSwapFeature());
	public static final Feature FAST_SHIELD = register(new Feature(
			"fastshield", "Fast Shield", "Shields start blocking after 3 ticks instead of 5.", true, true, "Optimizers"));
	public static final IntSettingFeature WIND_CHARGE_OPT = register(new IntSettingFeature(
			"windchargeopt", "Wind Charge Opt", "Wind charge cooldown in ticks (vanilla is 10). Starts at 5.",
			true, true, "Optimizers", "windcharge.ticks", 5, 0, 20, 1, "t", "ticks"));
	public static final Feature COBWEB_OPT = register(new Feature(
			"cobwebopt", "Cobweb Optimizer", "Hold right-click with cobwebs to place one every tick instead of every 4.",
			false, true, "Optimizers"));
	public static final ElytraSwapFeature ELYTRA_SWAP = register(new ElytraSwapFeature());
	public static final Feature CRYSTAL_OPT = register(new Feature(
			"crystalopt", "Crystal Optimizer", "Hitting an end crystal removes it on your screen right away instead of waiting for the server.",
			false, true, "Optimizers"));
	public static final Feature ANCHOR_OPT = register(new Feature(
			"anchoropt", "Anchor Optimizer", "Exploding a charged respawn anchor removes it on your screen right away so you can place the next one.",
			false, true, "Optimizers"));
	public static final Feature ANCHOR_FAST = register(new Feature(
			"anchorfast", "Anchor Fast Place", "Hold right-click with respawn anchors to place one every tick instead of every 4.",
			false, true, "Optimizers"));

	// ---- Visuals ----
	public static final Feature FULLBRIGHT = register(new Feature(
			"fullbright", "Fullbright", "Client-side night vision so everything is fully lit.", false, false, "Visuals"));
	public static final IntSettingFeature DAMAGE_TINT = register(new IntSettingFeature(
			"damagetint", "Damage Tint", "Red screen-edge flash when you take damage. Stepper = strength.",
			false, true, "Visuals", "damagetint.strength", 60, 10, 100, 10, "%", "strength"));
	public static final Feature TOTEM_TWEAKS = register(new Feature(
			"totemtweaks", "Totem Tweaks", "Hides the big totem pop animation (sound and particles stay).",
			false, true, "Visuals"));
	public static final Feature HEARTS = register(new Feature(
			"iseeyourhearts", "See Hearts", "Shows other players' hearts and saturation in their nametag.", false, true, "Visuals"));
	public static final Feature TITLE = register(new Feature(
			"title", "Title Logo", "Replaces the Minecraft logo on the title screen with AuraMate.", false, true, "Visuals"));
	public static final Feature MOUNTAIN_BG = register(new Feature(
			"mountainbg", "Aurora Mountain", "Menu background (title, singleplayer, multiplayer, options): a snow-covered mountain under a moving aurora.", false, true, "Visuals"));
	public static final IntSettingFeature PANEL_OPACITY = register(new IntSettingFeature(
			"panelopacity", "See-through Panel", "How see-through this panel is. Stepper = opacity (off = solid).",
			false, true, "Visuals", "panel.opacity", 55, 15, 100, 5, "%", "opacity"));

	// ---- HUD ----
	public static final Feature EFFECT_TIMER = register(new Feature(
			"effecttimer", "Effect Timer", "Shows active status effects with a countdown (top-left).", false, true, "HUD"));
	public static final Feature ARMOR_HUD = register(new Feature(
			"armorhud", "Armor HUD", "Shows your armor pieces with durability % (right side).", false, true, "HUD"));
	public static final Feature NOW_PLAYING = register(new Feature(
			"nowplaying", "Spotify Now Playing", "Shows the song playing in the Spotify desktop app (top center).", false, false, "HUD"));

	private Features() {}

	public static <T extends Feature> T register(T feature) {
		ALL.add(feature);
		return feature;
	}

	public static List<Feature> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static List<Feature> inCategory(String category) {
		List<Feature> l = new ArrayList<>();
		for (Feature f : ALL) if (f.category.equals(category)) l.add(f);
		return l;
	}
}
