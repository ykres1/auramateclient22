package dev.auramate.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Small draggable "task manager" panel: one row per feature with an on/off switch.
 * Right-click a row (or an Armor / Effect HUD element) to open its settings window, where you can
 * change its options and move it. Mouse input is polled in render() so it doesn't depend on
 * screen event signatures.
 */
public class PanelScreen extends Screen {
	private static final int W = 240;
	private static final int HEADER = 16;
	private static final int ROW = 20;
	private static final int FOOTER = 14;
	private static final int TABS_H = 14;
	private static final String[] TABS = {"Optimizers", "Visuals", "HUD"};

	private static final int WIN_W = 200;
	private static final int WIN_ROW = 18;

	private static final int BG_RGB = 0x101418;
	private static final int HEAD_RGB = 0x1B2128;
	private static final int BORDER = 0xFF2C3640;
	private static final int BORDER_RGB = 0x2C3640;
	private static final int ROW_HOVER = 0x33FFFFFF;
	private static final int TEXT = 0xFFE6EDF3;
	private static final int MUTED = 0xFF8B98A5;
	private static final int ACCENT = 0xFF3DDC97;
	/** Custom sky blue used for the AuraMate title (change this one value to re-colour it). */
	private static final int SKY = 0xFF56C2FF;
	private static final int OFF = 0xFF3A444F;
	private static final int BTN_HOVER = 0xFF4A5663;
	private static final int CLOSE_HOVER = 0xFFE5534B;

	// mountain + aurora scenes drawn behind the panel and the settings window
	private static final AuroraBackground.Scene PANEL_SCENE = new AuroraBackground.Scene(80);
	private static final AuroraBackground.Scene WIN_SCENE = new AuroraBackground.Scene(66);

	private int px = Config.getInt("panel.x", -1);
	private int py = Config.getInt("panel.y", -1);
	private boolean wasDown;
	private boolean wasRight;
	private boolean dragging;
	private int dragDx, dragDy;
	private int tab = Math.max(0, Math.min(Config.getInt("panel.tab", 0), TABS.length - 1));
	private String flash;
	private long flashUntil;

	// moving a HUD element
	private Hud.Target moving;
	private boolean moveSticky;
	private int moveDx, moveDy;

	// right-click settings window
	private Feature win;
	private List<Row> winRows = new ArrayList<>();
	private int wx = Config.getInt("win.x", -1);
	private int wy = Config.getInt("win.y", -1);
	private boolean winDragging;
	private int winDx, winDy;

	private enum Kind { TEXT, TOGGLE, STEP, BUTTON }

	private static final class Row {
		final Kind kind;
		final String label;
		final BooleanSupplier on;
		final Runnable act;
		final Feature.Setting step;
		final List<String> lines;

		private Row(Kind kind, String label, BooleanSupplier on, Runnable act, Feature.Setting step, List<String> lines) {
			this.kind = kind;
			this.label = label;
			this.on = on;
			this.act = act;
			this.step = step;
			this.lines = lines;
		}

		static Row text(List<String> lines) {
			return new Row(Kind.TEXT, null, null, null, null, lines);
		}

		static Row toggle(String label, BooleanSupplier on, Runnable act) {
			return new Row(Kind.TOGGLE, label, on, act, null, null);
		}

		static Row step(Feature.Setting s) {
			return new Row(Kind.STEP, s.label(), null, null, s, null);
		}

		static Row button(String label, Runnable act) {
			return new Row(Kind.BUTTON, label, null, act, null, null);
		}

		int height() {
			return kind == Kind.TEXT ? lines.size() * 10 + 6 : WIN_ROW;
		}
	}

	public PanelScreen() {
		super(Text.literal("AuraMateClient"));
	}

	@Override
	protected void init() {
		wasDown = leftDown();
		wasRight = rightDown();
		Hud.editing = true;
		if (px < 0 || py < 0) {
			px = (width - W) / 2;
			py = height / 4;
		}
		clamp();
	}

	@Override
	public void removed() {
		Hud.editing = false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// No blur / dim: keep it a small floating window over the game.
	}

	/** Reads the mouse buttons straight from GLFW so it works while a screen is open. */
	private static boolean leftDown() {
		long handle = MinecraftClient.getInstance().getWindow().getHandle();
		return GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}

	private static boolean rightDown() {
		long handle = MinecraftClient.getInstance().getWindow().getHandle();
		return GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
	}

	private int panelHeight() {
		return HEADER + TABS_H + Features.inCategory(TABS[tab]).size() * ROW + FOOTER;
	}

	/** Panel background alpha (0-255): the See-through Panel feature controls it, off = solid. */
	private static int panelAlpha() {
		if (!Features.PANEL_OPACITY.isEnabled()) return 255;
		return Features.PANEL_OPACITY.value() * 255 / 100;
	}

	/** Dark tint over the aurora so text stays readable; the See-through Panel setting lets more of the aurora show. */
	private static int scrim() {
		return (120 + panelAlpha() * 3 / 10) << 24 | BG_RGB;
	}

	private void clamp() {
		px = Math.max(0, Math.min(px, width - W));
		py = Math.max(0, Math.min(py, height - panelHeight()));
	}

	private int winHeight() {
		int h = HEADER + 2 + 4;
		for (Row r : winRows) h += r.height();
		return h;
	}

	private void clampWin() {
		wx = Math.max(0, Math.min(wx, width - WIN_W));
		wy = Math.max(0, Math.min(wy, height - winHeight()));
	}

	// ------------------------------------------------------------------ settings window

	private void openWindow(Feature f) {
		win = f;
		winRows = buildRows(f);
		if (wx < 0 || wy < 0) {
			wx = px + W + 6;
			if (wx + WIN_W > width) wx = px - WIN_W - 6;
			wy = py;
		}
		clampWin();
	}

	private List<Row> buildRows(Feature f) {
		List<Row> rows = new ArrayList<>();
		rows.add(Row.text(wrap(f.description + (f.needsRestart ? " (restart to apply)" : ""), WIN_W - 12)));
		rows.add(Row.toggle("Enabled", f::isEnabled, f::toggle));

		Feature.Setting s = f.setting();
		if (s != null) rows.add(Row.step(s));

		if (f == Features.LUNGE_SWAP) {
			rows.add(Row.text(wrap("Key: G (change it in Options > Controls). Needs a wind charge and a Lunge spear in your hotbar.", WIN_W - 12)));
		} else if (f == Features.ELYTRA_SWAP) {
			rows.add(Row.text(wrap("Key: H (change it in Options > Controls).", WIN_W - 12)));
		}

		Hud.Target t = Hud.targetOf(f);
		if (t != null) {
			if (t == Hud.Target.ARMOR) {
				rows.add(Row.toggle("Horizontal layout", () -> t.flag("horizontal", false), () -> t.toggle("horizontal", false)));
				rows.add(Row.toggle("Show durability %", () -> t.flag("percent", true), () -> t.toggle("percent", true)));
			} else {
				rows.add(Row.toggle("Blink when low", () -> t.flag("blink", true), () -> t.toggle("blink", true)));
				rows.add(Row.toggle("Show level", () -> t.flag("level", true), () -> t.toggle("level", true)));
			}
			rows.add(Row.button("Move on screen", () -> startMove(t)));
			rows.add(Row.button("Reset position", t::reset));
		}
		return rows;
	}

	private void startMove(Hud.Target t) {
		if (t.rw <= 0) {
			showFlash("Turn it on first, then you can move it");
			return;
		}
		moving = t;
		moveSticky = true;
		moveDx = t.rw / 2;
		moveDy = t.rh / 2;
	}

	private void showFlash(String s) {
		flash = s;
		flashUntil = System.currentTimeMillis() + 2000;
	}

	private List<String> wrap(String text, int maxW) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = cur.length() == 0 ? word : cur + " " + word;
			if (cur.length() > 0 && textRenderer.getWidth(test) > maxW) {
				out.add(cur.toString());
				cur = new StringBuilder(word);
			} else {
				cur = new StringBuilder(test);
			}
		}
		if (cur.length() > 0) out.add(cur.toString());
		return out;
	}

	private boolean inWindow(int mx, int my) {
		return win != null && in(mx, my, wx, wy, WIN_W, winHeight());
	}

	private void windowPress(int mx, int my) {
		if (in(mx, my, wx + WIN_W - 14, wy + 2, 12, 12)) {
			win = null;
			return;
		}
		if (in(mx, my, wx, wy, WIN_W, HEADER)) {
			winDragging = true;
			winDx = mx - wx;
			winDy = my - wy;
			return;
		}
		int ry = wy + HEADER + 2;
		for (Row r : winRows) {
			int rh = r.height();
			if (my >= ry && my < ry + rh) {
				switch (r.kind) {
					case TOGGLE, BUTTON -> r.act.run();
					case STEP -> {
						int sx = wx + WIN_W - 58;
						int cur = r.step.get().getAsInt();
						if (in(mx, my, sx, ry + 4, 12, 12)) {
							r.step.set().accept(Math.max(r.step.min(), cur - r.step.step()));
						} else if (in(mx, my, sx + 38, ry + 4, 12, 12)) {
							r.step.set().accept(Math.min(r.step.max(), cur + r.step.step()));
						}
					}
					default -> { }
				}
				return;
			}
			ry += rh;
		}
	}

	private void drawWindow(DrawContext ctx, int mx, int my) {
		int h = winHeight();
		ctx.fill(wx - 1, wy - 1, wx + WIN_W + 1, wy + h + 1, 0xFF000000 | BORDER_RGB);
		WIN_SCENE.draw(ctx, wx, wy, WIN_W, h);
		ctx.fill(wx, wy, wx + WIN_W, wy + h, (190 << 24) | BG_RGB);
		ctx.fill(wx, wy, wx + WIN_W, wy + HEADER, 0xFF000000 | HEAD_RGB);
		ctx.drawTextWithShadow(textRenderer, win.name, wx + 6, wy + 4, SKY);
		if (in(mx, my, wx + WIN_W - 14, wy + 2, 12, 12)) ctx.fill(wx + WIN_W - 14, wy + 2, wx + WIN_W - 2, wy + 14, CLOSE_HOVER);
		ctx.drawTextWithShadow(textRenderer, "x", wx + WIN_W - 10, wy + 4, TEXT);

		int ry = wy + HEADER + 2;
		for (Row r : winRows) {
			int rh = r.height();
			switch (r.kind) {
				case TEXT -> {
					for (int i = 0; i < r.lines.size(); i++) {
						ctx.drawTextWithShadow(textRenderer, r.lines.get(i), wx + 6, ry + 3 + i * 10, MUTED);
					}
				}
				case TOGGLE -> {
					if (in(mx, my, wx, ry, WIN_W, rh)) ctx.fill(wx, ry, wx + WIN_W, ry + rh, ROW_HOVER);
					ctx.drawTextWithShadow(textRenderer, r.label, wx + 8, ry + 5, TEXT);
					drawSwitch(ctx, wx + WIN_W - 32, ry + 4, r.on.getAsBoolean());
				}
				case STEP -> {
					String l = r.label.isEmpty() ? r.label : Character.toUpperCase(r.label.charAt(0)) + r.label.substring(1);
					ctx.drawTextWithShadow(textRenderer, l, wx + 8, ry + 5, TEXT);
					drawStepper(ctx, mx, my, wx + WIN_W - 58, ry, r.step);
				}
				case BUTTON -> {
					boolean hov = in(mx, my, wx + 6, ry + 1, WIN_W - 12, rh - 2);
					ctx.fill(wx + 6, ry + 1, wx + WIN_W - 6, ry + rh - 1, hov ? BTN_HOVER : OFF);
					ctx.drawTextWithShadow(textRenderer, r.label, wx + (WIN_W - textRenderer.getWidth(r.label)) / 2, ry + 5, TEXT);
				}
			}
			ry += rh;
		}
	}

	// ------------------------------------------------------------------ main loop

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		boolean down = leftDown();
		boolean rdown = rightDown();
		List<Feature> feats = Features.inCategory(TABS[tab]);
		winRows = win != null ? buildRows(win) : new ArrayList<>();

		// ---- input (edge-detected polling) ----
		if (moving != null) {
			Hud.Target t = moving;
			t.x = Math.max(0, Math.min(mx - moveDx, Math.max(0, width - t.rw)));
			t.y = Math.max(0, Math.min(my - moveDy, Math.max(0, height - t.rh)));
			boolean drop = moveSticky ? (down && !wasDown) : !down;
			if (drop) {
				t.save();
				moving = null;
			}
		} else if (down && !wasDown) {
			onPress(mx, my, feats);
		} else if (down && dragging) {
			px = mx - dragDx;
			py = my - dragDy;
			clamp();
		} else if (down && winDragging) {
			wx = mx - winDx;
			wy = my - winDy;
			clampWin();
		} else if (!down && dragging) {
			dragging = false;
			Config.set("panel.x", px);
			Config.set("panel.y", py);
			Config.save();
		} else if (!down && winDragging) {
			winDragging = false;
			Config.set("win.x", wx);
			Config.set("win.y", wy);
			Config.save();
		}
		if (rdown && !wasRight && moving == null) onRightPress(mx, my, feats);
		wasDown = down;
		wasRight = rdown;
		winRows = win != null ? buildRows(win) : new ArrayList<>();

		// ---- draw ----
		int h = panelHeight();
		int a = panelAlpha();
		int top = py + HEADER + TABS_H;
		ctx.fill(px - 1, py - 1, px + W + 1, py + h + 1, (Math.min(255, a + 70) << 24) | BORDER_RGB);
		PANEL_SCENE.draw(ctx, px, py, W, h);
		ctx.fill(px, py, px + W, py + h, scrim());
		ctx.fill(px, py, px + W, py + HEADER, (Math.min(255, a + 40) << 24) | HEAD_RGB);
		ctx.drawTextWithShadow(textRenderer, "AuraMate", px + 6, py + 4, SKY);
		int active = (int) Features.all().stream().filter(Feature::isEnabled).count();
		String count = active + "/" + Features.all().size();
		ctx.drawTextWithShadow(textRenderer, count, px + W - 18 - textRenderer.getWidth(count), py + 4, MUTED);

		if (in(mx, my, px + W - 14, py + 2, 12, 12)) ctx.fill(px + W - 14, py + 2, px + W - 2, py + 14, CLOSE_HOVER);
		ctx.drawTextWithShadow(textRenderer, "x", px + W - 10, py + 4, TEXT);

		// tabs
		int tw = W / TABS.length;
		for (int i = 0; i < TABS.length; i++) {
			int tx = px + i * tw;
			boolean sel = i == tab;
			boolean hov = in(mx, my, tx, py + HEADER, tw, TABS_H);
			if (sel || hov) ctx.fill(tx, py + HEADER, tx + tw, py + HEADER + TABS_H, sel ? 0x33FFFFFF : 0x1AFFFFFF);
			if (sel) ctx.fill(tx + 3, py + HEADER + TABS_H - 2, tx + tw - 3, py + HEADER + TABS_H, ACCENT);
			String tn = TABS[i];
			ctx.drawTextWithShadow(textRenderer, tn, tx + (tw - textRenderer.getWidth(tn)) / 2, py + HEADER + 3, sel ? TEXT : MUTED);
		}

		String hint = "Right-click a row (or a HUD element) for settings";
		boolean blocked = inWindow(mx, my);
		for (int i = 0; i < feats.size(); i++) {
			Feature f = feats.get(i);
			int ry = top + i * ROW;
			if (!blocked && in(mx, my, px, ry, W, ROW)) {
				ctx.fill(px, ry, px + W, ry + ROW, ROW_HOVER);
				hint = f.description + (f.needsRestart ? " (restart to apply)" : "");
			}
			ctx.fill(px + 6, ry + 8, px + 10, ry + 12, f.isEnabled() ? ACCENT : OFF);
			ctx.drawTextWithShadow(textRenderer, f.name, px + 15, ry + 6, f.isEnabled() ? TEXT : MUTED);

			Feature.Setting s = f.setting();
			if (s != null) drawStepper(ctx, mx, my, stepperX(), ry, s);
			drawSwitch(ctx, px + W - 30, ry + 5, f.isEnabled());
		}

		if (flash != null && System.currentTimeMillis() < flashUntil) hint = flash;

		int fy = py + h - FOOTER;
		ctx.fill(px, fy, px + W, fy + 1, BORDER);
		String shown = textRenderer.getWidth(hint) > W - 8 ? textRenderer.trimToWidth(hint, W - 14) + "..." : hint;
		ctx.drawTextWithShadow(textRenderer, shown, px + 4, fy + 4, MUTED);

		if (win != null) drawWindow(ctx, mx, my);
		if (moving != null) ctx.drawTextWithShadow(textRenderer, "Click to place", mx + 10, my + 10, ACCENT);
	}

	private boolean inPanel(int mx, int my) {
		return in(mx, my, px, py, W, panelHeight());
	}

	private int stepperX() {
		return px + W - 30 - 14 - 26 - 14;
	}

	private void drawSwitch(DrawContext ctx, int sx, int sy, boolean on) {
		ctx.fill(sx, sy, sx + 24, sy + 10, on ? ACCENT : OFF);
		int kx = on ? sx + 14 : sx + 1;
		ctx.fill(kx, sy + 1, kx + 9, sy + 9, 0xFFFFFFFF);
	}

	private void drawStepper(DrawContext ctx, int mx, int my, int sx, int ry, Feature.Setting s) {
		boolean hm = in(mx, my, sx, ry + 4, 12, 12);
		boolean hp = in(mx, my, sx + 38, ry + 4, 12, 12);
		ctx.fill(sx, ry + 4, sx + 12, ry + 16, hm ? BTN_HOVER : OFF);
		ctx.fill(sx + 38, ry + 4, sx + 50, ry + 16, hp ? BTN_HOVER : OFF);
		ctx.drawTextWithShadow(textRenderer, "-", sx + 4, ry + 6, TEXT);
		ctx.drawTextWithShadow(textRenderer, "+", sx + 42, ry + 6, TEXT);
		String v = s.get().getAsInt() + s.unit();
		ctx.drawTextWithShadow(textRenderer, v, sx + 12 + (26 - textRenderer.getWidth(v)) / 2, ry + 6, TEXT);
	}

	// ------------------------------------------------------------------ clicks

	private void onPress(int mx, int my, List<Feature> feats) {
		if (inWindow(mx, my)) {
			windowPress(mx, my);
			return;
		}
		if (!inPanel(mx, my)) {
			// drag a HUD element straight from the screen
			for (Hud.Target t : Hud.Target.values()) {
				if (t.contains(mx, my)) {
					moving = t;
					moveSticky = false;
					moveDx = mx - t.rx;
					moveDy = my - t.ry;
					return;
				}
			}
			return;
		}
		if (in(mx, my, px + W - 14, py + 2, 12, 12)) {
			close();
			return;
		}
		if (in(mx, my, px, py, W, HEADER)) {
			dragging = true;
			dragDx = mx - px;
			dragDy = my - py;
			return;
		}
		int top = py + HEADER + TABS_H;
		if (in(mx, my, px, py + HEADER, W, TABS_H)) {
			int ti = (mx - px) / (W / TABS.length);
			if (ti >= 0 && ti < TABS.length && ti != tab) {
				tab = ti;
				Config.set("panel.tab", tab);
				Config.save();
				clamp();
			}
			return;
		}
		for (int i = 0; i < feats.size(); i++) {
			Feature f = feats.get(i);
			int ry = top + i * ROW;
			if (!in(mx, my, px, ry, W, ROW)) continue;
			Feature.Setting s = f.setting();
			if (s != null) {
				int sx = stepperX();
				int cur = s.get().getAsInt();
				if (in(mx, my, sx, ry + 4, 12, 12)) {
					s.set().accept(Math.max(s.min(), cur - s.step()));
					return;
				}
				if (in(mx, my, sx + 38, ry + 4, 12, 12)) {
					s.set().accept(Math.min(s.max(), cur + s.step()));
					return;
				}
			}
			f.toggle();
			return;
		}
	}

	private void onRightPress(int mx, int my, List<Feature> feats) {
		if (inWindow(mx, my)) return;
		if (inPanel(mx, my)) {
			int top = py + HEADER + TABS_H;
			for (int i = 0; i < feats.size(); i++) {
				if (in(mx, my, px, top + i * ROW, W, ROW)) {
					openWindow(feats.get(i));
					return;
				}
			}
			return;
		}
		for (Hud.Target t : Hud.Target.values()) {
			if (t.contains(mx, my)) {
				openWindow(Hud.featureOf(t));
				return;
			}
		}
	}

	private static boolean in(int mx, int my, int rx, int ry, int rw, int rh) {
		return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
	}
}
