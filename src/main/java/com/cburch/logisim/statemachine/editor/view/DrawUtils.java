package com.cburch.logisim.statemachine.editor.view;

import java.awt.Graphics;

public class DrawUtils {
	public static void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h, boolean capOnly) {
		int dx = x2 - x1, dy = y2 - y1;
		double D = Math.sqrt(dx * dx + dy * dy);
		double xm = D - d, xn = xm, ym = h, yn = -h, x;
		double sin = dy / D, cos = dx / D;

		x = xm * cos - ym * sin + x1;
		ym = xm * sin + ym * cos + y1;
		xm = x;

		x = xn * cos - yn * sin + x1;
		yn = xn * sin + yn * cos + y1;
		xn = x;

		int[] xpoints = { x2, (int) xm, (int) xn };
		int[] ypoints = { y2, (int) ym, (int) yn };

		if(!capOnly) g.drawLine(x1, y1,  x2, y2);
		g.fillPolygon(xpoints, ypoints, 3);
	}

}
