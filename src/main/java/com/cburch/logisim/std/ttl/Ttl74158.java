/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021.
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.util.GraphicsUtil.H_CENTER;
import static com.cburch.logisim.util.GraphicsUtil.V_CENTER_OVERALL;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x158: quadruple 2-line to 1-line data selector, inverted outputs.
 *
 * <p>Model based on https://www.ti.com/product/SN74LS158 datasheet. Four sections share select and
 * an active-low strobe. Each section outputs the inverse of A when select is low and the inverse
 * of B when select is high. A high strobe forces every output high.
 */
public class Ttl74158 extends Ttl74157 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74158";

  private static final int BLOCK_WIDTH = 12;
  private static final int BLOCK_HEIGHT = 16;

  // 74x158 is 74x157 with inverted outputs.
  public Ttl74158() {
    super(_ID, true);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 4));
    // Turned so the long sides face the pins: inputs on the left, inverted Y on the right.
    // Upper row is sections 4 and 3; lower row is sections 1 and 2. The left edge carries S and G.
    drawBlock(g, x + 74, y + 11);
    drawBlock(g, x + 134, y + 11);
    drawBlock(g, x + 54, y + 33);
    drawBlock(g, x + 114, y + 33);
    g.setFont(previousFont);

    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    drawUpperData(g, x, y, pinTop);
    drawLowerData(g, x, y, pinBottom);
    drawStrobe(g, x, y, pinTop);
    drawSelect(g, x, y, pinBottom);
    drawOutputs(g, x, y, pinTop, pinBottom);
    drawBubbles(g, x, y);
    drawJoints(g, x, y);
  }

  /** Centered MUX label. Font 4 is 9×8 and sits inside the 12×16 block. */
  private static void drawBlock(Graphics g, int left, int top) {
    g.drawRect(left, top, BLOCK_WIDTH, BLOCK_HEIGHT);
    GraphicsUtil.drawText(
        g, "MUX", left + BLOCK_WIDTH / 2, top + BLOCK_HEIGHT / 2, H_CENTER, V_CENTER_OVERALL);
  }

  /** A and B of the upper sections. B, the nearer pin, stops above A's lead. */
  private static void drawUpperData(Graphics g, int x, int y, int pinTop) {
    wire(g, x + 70, pinTop, x + 70, y + 13, x + 74, y + 13);
    wire(g, x + 50, pinTop, x + 50, y + 17, x + 74, y + 17);
    wire(g, x + 130, pinTop, x + 130, y + 13, x + 134, y + 13);
    wire(g, x + 110, pinTop, x + 110, y + 17, x + 134, y + 17);
  }

  /** A and B of the lower sections. B stops below A's lead. */
  private static void drawLowerData(Graphics g, int x, int y, int pinBottom) {
    wire(g, x + 50, pinBottom, x + 50, y + 47, x + 54, y + 47);
    wire(g, x + 30, pinBottom, x + 30, y + 43, x + 54, y + 43);
    wire(g, x + 110, pinBottom, x + 110, y + 47, x + 114, y + 47);
    wire(g, x + 90, pinBottom, x + 90, y + 43, x + 114, y + 43);
  }

  /**
   * Active-low strobe from pin 15. The rail under the upper row reaches the far side of each
   * input bubble; it does not run through a section.
   */
  private static void drawStrobe(Graphics g, int x, int y, int pinTop) {
    wire(g, x + 30, pinTop, x + 30, y + 21, x + 72, y + 21);
    wire(g, x + 64, y + 21, x + 64, y + 29);
    // The rail stops on section 3's tap. Past x + 124 it ran under pin 9 and joined nothing.
    wire(g, x + 48, y + 29, x + 124, y + 29);
    wire(g, x + 124, y + 29, x + 124, y + 21, x + 132, y + 21);
    wire(g, x + 48, y + 29, x + 48, y + 35, x + 52, y + 35);
    wire(g, x + 104, y + 29, x + 104, y + 35, x + 112, y + 35);
  }

  /** Select from pin 1. The lower rail joins the upper contacts beside section 4, not through it. */
  private static void drawSelect(Graphics g, int x, int y, int pinBottom) {
    wire(g, x + 10, pinBottom, x + 10, y + 25, x + 74, y + 25);
    wire(g, x + 70, y + 25, x + 70, y + 33, x + 100, y + 33);
    wire(g, x + 100, y + 33, x + 100, y + 25, x + 134, y + 25);
    wire(g, x + 10, y + 39, x + 54, y + 39);
    wire(g, x + 100, y + 33, x + 100, y + 39, x + 114, y + 39);
  }

  /** Inverted outputs. The last segment into each bubble runs along its horizontal axis. */
  private static void drawOutputs(Graphics g, int x, int y, int pinTop, int pinBottom) {
    wire(g, x + 90, pinTop, x + 90, y + 19, x + 88, y + 19);
    wire(g, x + 150, pinTop, x + 150, y + 19, x + 148, y + 19);
    wire(g, x + 70, pinBottom, x + 70, y + 41, x + 68, y + 41);
    wire(g, x + 130, pinBottom, x + 130, y + 41, x + 128, y + 41);
  }

  private static void drawBubbles(Graphics g, int x, int y) {
    bubble(g, x + 73, y + 21);
    bubble(g, x + 133, y + 21);
    bubble(g, x + 53, y + 35);
    bubble(g, x + 113, y + 35);
    bubble(g, x + 87, y + 19);
    bubble(g, x + 147, y + 19);
    bubble(g, x + 67, y + 41);
    bubble(g, x + 127, y + 41);
  }

  /** Branches only. A bend of one wire is not a joint. */
  private static void drawJoints(Graphics g, int x, int y) {
    dot(g, x + 64, y + 21);
    dot(g, x + 64, y + 29);
    dot(g, x + 104, y + 29);
    dot(g, x + 10, y + 39);
    dot(g, x + 70, y + 25);
    dot(g, x + 100, y + 33);
  }

  /** Orthogonal wire through the given points. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Inversion mark. Even 2×2 oval, center on the wire axis. */
  private static void bubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Branch of three or more segments. The oval center is the common point. */
  private static void dot(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }
}
