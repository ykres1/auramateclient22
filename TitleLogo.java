package dev.auramate.client;

import net.minecraft.client.gui.DrawContext;

/** Blocky "AURAMATE" logo drawn with plain rectangles (no textures needed). */
public final class TitleLogo {
	private static final int CELL = 4;
	private static final int GAP = 1; // cells between letters

	private static final String[] A = {"01110", "10001", "10001", "11111", "10001", "10001", "10001"};
	private static final String[] U = {"10001", "10001", "10001", "10001", "10001", "10001", "01110"};
	private static final String[] R = {"11110", "10001", "10001", "11110", "10100", "10010", "10001"};
	private static final String[] M = {"10001", "11011", "10101", "10101", "10001", "10001", "10001"};
	private static final String[] T = {"11111", "00100", "00100", "00100", "00100", "00100", "00100"};
	private static final String[] E = {"11111", "10000", "10000", "11110", "10000", "10000", "11111"};
	private static final String[][] WORD = {A, U, R, A, M, A, T, E};

	private TitleLogo() {}

	public static void draw(DrawContext ctx, int screenWidth) {
		if (!Features.TITLE.isEnabled()) return;
		int letterW = 5 * CELL;
		int total = WORD.length * letterW + (WORD.length - 1) * GAP * CELL;
		int x0 = (screenWidth - total) / 2;
		int y0 = 30;
		// shadow pass, then colour pass
		for (int pass = 0; pass < 2; pass++) {
			int off = pass == 0 ? 2 : 0;
			for (int li = 0; li < WORD.length; li++) {
				int lx = x0 + li * (letterW + GAP * CELL);
				String[] rows = WORD[li];
				for (int r = 0; r < rows.length; r++) {
					int color = pass == 0 ? 0xFF0B2A1E : rowColor(r, rows.length);
					for (int c = 0; c < 5; c++) {
						if (rows[r].charAt(c) != '1') continue;
						int px = lx + c * CELL + off;
						int py = y0 + r * CELL + off;
						ctx.fill(px, py, px + CELL, py + CELL, color);
					}
				}
			}
		}
	}

	/** Light mint at the top fading to the accent green at the bottom. */
	private static int rowColor(int row, int rows) {
		float t = row / (float) (rows - 1);
		int r = (int) (0xB8 + (0x3D - 0xB8) * t);
		int g = (int) (0xF5 + (0xDC - 0xF5) * t);
		int b = (int) (0xDA + (0x97 - 0xDA) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
}
