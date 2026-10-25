package dev.auramate.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Status effect timer and armor HUD. Both can be moved / tweaked from the panel (right-click). */
public final class Hud implements HudElement {
	private static final int UNSET = Integer.MIN_VALUE;
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** True while the panel is open: HUD shows sample content and an outline so it can be edited. */
	public static boolean editing;

	public enum Target {
		EFFECTS("effects"), ARMOR("armor");

		public final String key;
		public int x, y;              // saved position (UNSET = default)
		public int rx, ry, rw, rh;    // bounds drawn last frame (rw == 0: not drawn)

		Target(String key) {
			this.key = key;
			this.x = Config.getInt("hud." + key + ".x", UNSET);
			this.y = Config.getInt("hud." + key + ".y", UNSET);
		}

		public boolean contains(int mx, int my) {
			return rw > 0 && mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
		}

		public void save() {
			Config.set("hud." + key + ".x", x);
			Config.set("hud." + key + ".y", y);
			Config.save();
		}

		public void reset() {
			x = UNSET;
			y = UNSET;
			save();
		}

		public boolean flag(String name, boolean def) {
			return Config.getBool("hud." + key + "." + name, def);
		}

		public void toggle(String name, boolean def) {
			Config.set("hud." + key + "." + name, !flag(name, def));
			Config.save();
		}
	}

	/** The HUD element a feature draws (null for features that are not movable HUD elements). */
	public static Target targetOf(Feature f) {
		if (f == Features.EFFECT_TIMER) return Target.EFFECTS;
		if (f == Features.ARMOR_HUD) return Target.ARMOR;
		return null;
	}

	public static Feature featureOf(Target t) {
		return t == Target.EFFECTS ? Features.EFFECT_TIMER : Features.ARMOR_HUD;
	}

	private record Line(String name, String time, int color, boolean low, int ticks) {}

	@Override
	public void render(DrawContext ctx, RenderTickCounter tickCounter) {
		Target.EFFECTS.rw = 0;
		Target.ARMOR.rw = 0;
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity p = mc.player;
		if (p == null || mc.options.hudHidden) return;
		if (Features.EFFECT_TIMER.isEnabled()) drawEffects(ctx, mc, p);
		if (Features.ARMOR_HUD.isEnabled()) drawArmor(ctx, mc, p);
	}

	private static void drawEffects(DrawContext ctx, MinecraftClient mc, PlayerEntity p) {
		Target t = Target.EFFECTS;
		boolean blink = t.flag("blink", true);
		boolean level = t.flag("level", true);
		TextRenderer tr = mc.textRenderer;

		List<StatusEffectInstance> list = new ArrayList<>(p.getStatusEffects());
		if (Features.FULLBRIGHT.isEnabled()) list.removeIf(e -> e.getEffectType() == StatusEffects.NIGHT_VISION);
		list.sort(Comparator.comparingInt((StatusEffectInstance e) -> e.isInfinite() ? Integer.MAX_VALUE : e.getDuration()));
		List<Line> lines = new ArrayList<>();
		for (StatusEffectInstance e : list) {
			String name = e.getEffectType().value().getName().getString();
			if (level && e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
			String time = e.isInfinite() ? "**:**" : format(e.getDuration());
			int color = 0xFF000000 | e.getEffectType().value().getColor();
			lines.add(new Line(name, time, color, !e.isInfinite() && e.getDuration() < 200, e.getDuration()));
		}
		if (lines.isEmpty() && editing) {
			lines.add(new Line("Speed II", "1:30", 0xFF7CAFC6, false, 1800));
			lines.add(new Line("Strength", "0:08", 0xFF932423, true, 160));
		}
		if (lines.isEmpty()) return;

		int w = 0;
		for (Line l : lines) w = Math.max(w, tr.getWidth(l.name()) + 6 + tr.getWidth(l.time()));
		int h = lines.size() * 11;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		int x = place(t.x, 4, w, sw);
		int y = place(t.y, 4, h, sh);

		int ly = y;
		for (Line l : lines) {
			ctx.drawTextWithShadow(tr, l.name(), x, ly, l.color());
			boolean show = !(blink && l.low()) || (l.ticks() / 10) % 2 == 0;
			if (show) {
				ctx.drawTextWithShadow(tr, l.time(), x + tr.getWidth(l.name()) + 6, ly,
						l.low() ? 0xFFFF5555 : 0xFFFFFFFF);
			}
			ly += 11;
		}
		finish(ctx, t, x, y, w, h);
	}

	private static void drawArmor(DrawContext ctx, MinecraftClient mc, PlayerEntity p) {
		Target t = Target.ARMOR;
		boolean horizontal = t.flag("horizontal", false);
		boolean percent = t.flag("percent", true);
		TextRenderer tr = mc.textRenderer;

		List<ItemStack> stacks = new ArrayList<>();
		List<Integer> pcts = new ArrayList<>();
		for (EquipmentSlot s : ARMOR_SLOTS) {
			ItemStack st = p.getEquippedStack(s);
			if (st.isEmpty()) continue;
			stacks.add(st);
			pcts.add(pct(st));
		}
		if (stacks.isEmpty() && editing) {
			stacks.add(new ItemStack(Items.DIAMOND_HELMET));
			stacks.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
			stacks.add(new ItemStack(Items.DIAMOND_LEGGINGS));
			stacks.add(new ItemStack(Items.DIAMOND_BOOTS));
			pcts.add(95);
			pcts.add(70);
			pcts.add(40);
			pcts.add(12);
		}
		int n = stacks.size();
		if (n == 0) return;

		int w, h;
		if (horizontal) {
			w = n * 26;
			h = percent ? 28 : 16;
		} else {
			w = percent ? 46 : 16;
			h = n * 20;
		}
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();
		int x = place(t.x, sw - w - 4, w, sw);
		int y = place(t.y, sh / 2 - h / 2, h, sh);

		for (int i = 0; i < n; i++) {
			ItemStack st = stacks.get(i);
			int pc = pcts.get(i);
			String txt = pc + "%";
			if (horizontal) {
				int cx = x + i * 26;
				ctx.drawItem(st, cx + 5, y);
				if (percent && pc >= 0) ctx.drawTextWithShadow(tr, txt, cx + (26 - tr.getWidth(txt)) / 2, y + 18, durabilityColor(pc));
			} else {
				int cy = y + i * 20;
				ctx.drawItem(st, x, cy);
				if (percent && pc >= 0) ctx.drawTextWithShadow(tr, txt, x + 19, cy + 4, durabilityColor(pc));
			}
		}
		finish(ctx, t, x, y, w, h);
	}

	/** Records the drawn bounds and, while editing, outlines them. */
	private static void finish(DrawContext ctx, Target t, int x, int y, int w, int h) {
		t.rx = x;
		t.ry = y;
		t.rw = w;
		t.rh = h;
		if (!editing) return;
		int c = 0xAA3DDC97;
		ctx.fill(x - 2, y - 2, x + w + 2, y - 1, c);
		ctx.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, c);
		ctx.fill(x - 2, y - 2, x - 1, y + h + 2, c);
		ctx.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, c);
	}

	private static int place(int saved, int def, int size, int max) {
		int v = saved == UNSET ? def : saved;
		return Math.max(0, Math.min(v, Math.max(0, max - size)));
	}

	/** Durability left in percent, or -1 for items that can't break. */
	private static int pct(ItemStack st) {
		if (!st.isDamageable()) return -1;
		int max = st.getMaxDamage();
		int left = max - st.getDamage();
		return Math.max(0, Math.min(100, Math.round(left * 100f / max)));
	}

	private static int durabilityColor(int pct) {
		if (pct > 60) return 0xFF55FF55;
		if (pct > 30) return 0xFFFFFF55;
		if (pct > 15) return 0xFFFFAA00;
		return 0xFFFF5555;
	}

	private static String format(int ticks) {
		int total = ticks / 20;
		return String.format("%d:%02d", total / 60, total % 60);
	}

	private static String roman(int n) {
		String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		return n < r.length ? r[n] : Integer.toString(n);
	}
}
