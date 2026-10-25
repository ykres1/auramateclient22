package dev.auramate.client;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/**
 * Snow-covered mountain range under a moving aurora, drawn with plain rectangles and gradients
 * (no textures). Used as the menu background and as the backdrop of the AuraMate panel.
 * Each {@link Scene} keeps its own terrain cache and can draw into any rectangle of the screen.
 */
public final class AuroraBackground {
	// stars
	private static final int STARS = 100;
	private static final float[] SX = new float[STARS], SY = new float[STARS], SP = new float[STARS];

	static {
		Random r = new Random(7);
		for (int i = 0; i < STARS; i++) {
			SX[i] = r.nextFloat();
			SY[i] = r.nextFloat();
			SP[i] = r.nextFloat();
		}
	}

	private static final int[] AURORA = {0x3DFF9A, 0x2FD6E0, 0x9C6BFF};

	private static final int SNOW_LIT_TOP = 0xFFF2FBF8, SNOW_LIT_BOT = 0xFFD2EBE8;
	private static final int SNOW_SHADE_TOP = 0xFFA3C0D8, SNOW_SHADE_BOT = 0xFF7893B4;
	private static final int ROCK_LIT_TOP = 0xFF64788F, ROCK_LIT_BOT = 0xFF3E4E66;
	private static final int ROCK_SHADE_TOP = 0xFF34435A, ROCK_SHADE_BOT = 0xFF1E293B;
	private static final int TREE = 0xFF0C2A28, TREE_SNOW = 0xFFDDEBF6, TRUNK = 0xFF2A1C14;

	private static final double[][] TREES = {
			{0.035, 1.00}, {0.080, 0.70}, {0.120, 1.20}, {0.170, 0.80},
			{0.790, 0.90}, {0.850, 1.25}, {0.905, 0.75}, {0.955, 1.05}};

	private AuroraBackground() {}

	/** Scene used for the full-screen menu background. */
	private static final Scene MENU = new Scene(240);

	/** Draws the current frame full-screen. Always draws (the sky is opaque, so a double draw is harmless). */
	public static void draw(DrawContext ctx, int w, int h) {
		MENU.draw(ctx, 0, 0, w, h);
	}

	public static void draw(DrawContext ctx, int w, int h, double t) {
		MENU.draw(ctx, 0, 0, w, h, t);
	}

	/** A scene that can be drawn into any rectangle (x, y, w, h) of the screen. */
	public static final class Scene {
		private final int maxCols;
		private int cw = -1, ch = -1, cols, base;
		private int[] xs, far, range, snow, fore;
		private double[] jitter;
		private int ox, oy;

		/** @param maxColumns terrain resolution cap: smaller = fewer draw calls (use ~w/3 for small panels). */
		public Scene(int maxColumns) {
			this.maxCols = Math.max(8, maxColumns);
		}

		public void draw(DrawContext ctx, int x, int y, int w, int h) {
			draw(ctx, x, y, w, h, (System.currentTimeMillis() % 10_000_000L) / 1000.0);
		}

		public void draw(DrawContext ctx, int x, int y, int w, int h, double t) {
			if (w < 8 || h < 8) return;
			ox = x;
			oy = y;
			if (w != cw || h != ch) build(w, h);
			sky(ctx, w, h);
			stars(ctx, w, h, t);
			aurora(ctx, w, h, t);
			mountains(ctx, w, h);
			// mist where the mountains meet the snow
			grad(ctx, 0, base - (int) (h * 0.06), w, base + (int) (h * 0.05), 0x00BFE0F5, 0x66BFE0F5);
			foreground(ctx, w, h);
		}

		private void box(DrawContext ctx, int x0, int y0, int x1, int y1, int color) {
			ctx.fill(ox + x0, oy + y0, ox + x1, oy + y1, color);
		}

		private void grad(DrawContext ctx, int x0, int y0, int x1, int y1, int top, int bottom) {
			ctx.fillGradient(ox + x0, oy + y0, ox + x1, oy + y1, top, bottom);
		}

		// ------------------------------------------------------------------ terrain

		private void build(int w, int h) {
			cw = w;
			ch = h;
			cols = Math.max(1, Math.min(w, maxCols));
			xs = new int[cols + 1];
			for (int i = 0; i <= cols; i++) xs[i] = (int) ((long) i * w / cols);
			base = (int) (h * 0.80);
			far = new int[cols];
			range = new int[cols];
			snow = new int[cols];
			fore = new int[cols];
			jitter = new double[cols];
			Random r = new Random(11);
			for (int i = 0; i < cols; i++) {
				double u = (i + 0.5) / cols;
				jitter[i] = r.nextDouble() * 2 - 1;

				double f = 0.17 + 0.05 * Math.sin(u * 9 + 1) + 0.035 * Math.sin(u * 23 + 4) + 0.02 * Math.sin(u * 51);
				far[i] = base - (int) (h * f);

				double hh = Math.max(peak(u, 0.60, 0.42, 0.60), Math.max(peak(u, 0.14, 0.27, 0.40), peak(u, 0.97, 0.22, 0.30)));
				if (hh > 0) hh *= 1 + 0.04 * Math.sin(u * 70 + 2) + 0.025 * Math.sin(u * 131 + 5) + 0.012 * Math.sin(u * 257 + 1);
				range[i] = base - (int) (h * hh);

				double frac = 0.62 + 0.18 * Math.sin(u * 37 + 0.7) + 0.12 * Math.sin(u * 83 + 2.2);
				frac = Math.max(0.35, Math.min(0.95, frac));
				snow[i] = range[i] + (int) ((base - range[i]) * frac);

				fore[i] = base + (int) (h * (0.035 + 0.025 * Math.sin(u * 4 + 1) + 0.012 * Math.sin(u * 11 + 2)));
			}
		}

		private static double peak(double u, double cx, double halfW, double height) {
			double d = Math.abs(u - cx) / halfW;
			if (d >= 1) return 0;
			return height * Math.pow(1 - d, 1.25);
		}

		// ------------------------------------------------------------------ layers

		private void sky(DrawContext ctx, int w, int h) {
			int mid = (int) (h * 0.55);
			grad(ctx, 0, 0, w, mid, 0xFF050A1C, 0xFF0C1D38);
			grad(ctx, 0, mid, w, h, 0xFF0C1D38, 0xFF1B4A5E);
		}

		private void stars(DrawContext ctx, int w, int h, double t) {
			for (int i = 0; i < STARS; i++) {
				int x = (int) (SX[i] * w);
				int y = (int) (SY[i] * h * 0.62);
				double tw = 0.5 + 0.5 * Math.sin(t * (0.8 + SP[i] * 1.5) + SP[i] * 40);
				int a = 70 + (int) (150 * tw);
				box(ctx, x, y, x + 1, y + 1, (a << 24) | 0xFFFFFF);
			}
		}

		private void aurora(DrawContext ctx, int w, int h, double t) {
			for (int layer = 0; layer < AURORA.length; layer++) {
				int rgb = AURORA[layer];
				double strength = layer == 2 ? 0.55 : 1.0;
				for (int i = 0; i < cols; i++) {
					int x0 = xs[i], x1 = xs[i + 1];
					if (x1 <= x0) continue;
					double u = (i + 0.5) / cols;
					double wave = Math.sin(u * 6.0 + t * (0.25 + layer * 0.07) + layer * 1.7) * 0.5
							+ Math.sin(u * 13.0 - t * 0.31 + layer) * 0.25;
					double bottomY = h * (0.34 + layer * 0.05) + wave * h * 0.07;
					double ray = 0.55 + 0.45 * Math.sin(u * 90.0 + Math.sin(u * 7 + t * 0.5) * 4.0);
					double fade = 0.5 + 0.5 * Math.sin(u * 3.2 + t * 0.18 + layer * 2.1);
					double height = h * (0.20 + 0.12 * fade + 0.04 * layer);
					int a = (int) (150 * strength * ray * (0.30 + 0.70 * fade));
					if (a <= 2) continue;
					int bottom = (int) bottomY;
					int top = (int) (bottomY - height);
					if (top < 0) top = 0;
					if (bottom <= top) continue;
					grad(ctx, x0, top, x1, bottom, rgb, (a << 24) | rgb);
					int glow = Math.max(2, h / 40);
					grad(ctx, x0, bottom, x1, bottom + glow, ((a / 2) << 24) | rgb, rgb);
				}
			}
		}

		private void mountains(DrawContext ctx, int w, int h) {
			for (int i = 0; i < cols; i++) {
				int x0 = xs[i], x1 = xs[i + 1];
				if (x1 <= x0) continue;
				int top = range[i];
				if (top >= base - 1) continue;
				int a = range[Math.max(0, i - 1)];
				int b = range[Math.min(cols - 1, i + 1)];
				boolean lit = b <= a; // left flank (rising to the right) catches the moonlight
				double k = 1 + 0.03 * jitter[i];
				int sn = Math.max(top + 1, snow[i]);
				grad(ctx, x0, top, x1, sn,
						scale(lit ? SNOW_LIT_TOP : SNOW_SHADE_TOP, k), scale(lit ? SNOW_LIT_BOT : SNOW_SHADE_BOT, k));
				grad(ctx, x0, sn, x1, h,
						scale(lit ? ROCK_LIT_TOP : ROCK_SHADE_TOP, k), scale(lit ? ROCK_LIT_BOT : ROCK_SHADE_BOT, k));
				if (lit) box(ctx, x0, top, x1, top + 1, 0xFFFFFFFF);
			}
		}

		private void foreground(DrawContext ctx, int w, int h) {
			for (int i = 0; i < cols; i++) {
				int x0 = xs[i], x1 = xs[i + 1];
				if (x1 <= x0) continue;
				grad(ctx, x0, fore[i], x1, h, 0xFFE3EEF8, 0xFFA9BED9);
				box(ctx, x0, fore[i], x1, fore[i] + 2, 0xFFF6FBFF);
			}
			for (double[] tr : TREES) {
				int col = Math.max(0, Math.min(cols - 1, (int) (tr[0] * cols)));
				int cx = (xs[col] + xs[col + 1]) / 2;
				int th = (int) (h * 0.11 * tr[1]);
				tree(ctx, cx, fore[col] + 3, th);
			}
		}

		private void tree(DrawContext ctx, int cx, int baseY, int th) {
			if (th < 8) return;
			int tiers = 4;
			int tierH = Math.max(3, (int) (th / 3.25));
			int step = Math.max(2, (int) (tierH * 0.75));
			int trunk = Math.max(2, th / 9);
			box(ctx, cx - 1, baseY - trunk, cx + 1, baseY, TRUNK);
			int top = baseY - trunk - (step * (tiers - 1) + tierH);
			double widest = th * 0.31;
			for (int k = 0; k < tiers; k++) {
				int y0 = top + k * step;
				double bh = widest * (0.35 + 0.65 * (k + 1) / tiers);
				for (int dy = 0; dy < tierH; dy++) {
					int half = Math.max(1, (int) (bh * (0.30 + 0.70 * dy / (double) tierH)));
					box(ctx, cx - half, y0 + dy, cx + half, y0 + dy + 1, dy == 0 ? TREE_SNOW : TREE);
				}
			}
		}
	}

	private static int scale(int argb, double k) {
		int r = clamp(((argb >> 16) & 0xFF) * k);
		int g = clamp(((argb >> 8) & 0xFF) * k);
		int b = clamp((argb & 0xFF) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int clamp(double v) {
		return Math.max(0, Math.min(255, (int) v));
	}
}
