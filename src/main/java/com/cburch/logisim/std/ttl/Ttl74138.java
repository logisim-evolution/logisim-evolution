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

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x138 3-line to 8-line decoders/multiplexers
 * Model based on https://www.ti.com/product/SN74LS138 datasheet.
 */
public class Ttl74138 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74138";

  // IC pin indices as specified in the datasheet
  public static final byte A = 1;
  public static final byte B = 2;
  public static final byte C = 3;
  public static final byte nEN2A = 4;
  public static final byte nEN2B = 5;
  public static final byte EN1 = 6;
  public static final byte nY7 = 7;
  public static final byte GND = 8;
  public static final byte nY6 = 9;
  public static final byte nY5 = 10;
  public static final byte nY4 = 11;
  public static final byte nY3 = 12;
  public static final byte nY2 = 13;
  public static final byte nY1 = 14;
  public static final byte nY0 = 15;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  public Ttl74138() {
    super(
        _ID,
        (byte) 16,
        new byte[] {
          nY0, nY1, nY2, nY3, nY4, nY5, nY6, nY7
        },
        new String[] {
          "A", "B", "C", "nG2A Enable (active LOW)", "nG2B Enable (active LOW)", "G1 Enable (active HIGH)", "nY7",
          "nY6", "nY5", "nY4", "nY3", "nY2", "nY1", "nY0"
        },
        null);
  }

  /**
   * One NAND for each active-low output. Rails carry A, B, C and the enable. A bubble on an
   * address input is the bit that is 0 in that output's index, so Y0 decodes 000 and Y7 decodes
   * 111. G2A and G2B (active low) join G1 in one AND; its output is the enable rail. Every block
   * is marked {@code &}.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawDecoder(g, x, y, height);
  }

  private static void drawDecoder(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var gateTop = y + 14;
    final var gateBottom = y + 22;
    final var lowerTop = y + 42;
    final var lowerBottom = y + 48;
    final var rail = new int[] {y + 28, y + 32, y + 36, y + 40};
    // A, B, C, enable, output attachment, pin, left edge, right edge.
    // Y5's four inputs continue down into Y7; every other tap stops on its rail.
    // nY1 is only as wide as the taps around its straight output. That lets the
    // following gates sit further left, so the blocks on pins 11 and 10 do not share an edge.
    final var outputs = new int[][] {
      {20, 24, 34, 38, 30, 30, 18, 40},
      {44, 48, 54, 58, 50, 50, 42, 60},
      {64, 68, 72, 76, 70, 70, 62, 78},
      {84, 88, 92, 96, 90, 90, 82, 98},
      {104, 108, 112, 116, 110, 110, 102, 118},
      {124, 128, 132, 136, 130, 130, 122, 138},
      {144, 148, 156, 152, 150, 150, 142, 158},
    };

    g.drawLine(x + 10, rail[0], x + 144, rail[0]);
    g.drawLine(x + 10, pinBottom, x + 10, rail[0]);
    g.drawLine(x + 24, rail[1], x + 148, rail[1]);
    g.drawLine(x + 30, pinBottom, x + 30, rail[1]);
    g.drawLine(x + 34, rail[2], x + 156, rail[2]);
    g.drawLine(x + 50, pinBottom, x + 50, rail[2]);
    g.drawLine(x + 38, rail[3], x + 152, rail[3]);
    // B and C meet their rails in the middle. A only turns the corner, so it has no dot.
    drawNode(g, x + 30, rail[1]);
    drawNode(g, x + 50, rail[2]);
    // A rail ends on its last tap. B, C and enable start and end on a tap.
    final var railStart = new int[] {10, 24, 34, 38};
    final var railEnd = new int[] {144, 148, 156, 152};

    for (var index = 0; index < outputs.length; index++) {
      final var gate = outputs[index];
      final var left = gate[6];
      final var right = gate[7];
      final var out = gate[4];
      final var pin = gate[5];
      g.drawRect(x + left, gateTop, right - left, gateBottom - gateTop);
      drawAndMark(g, x + left, gateTop, x + right, gateBottom);
      drawBubble(g, x + out, gateTop - 1);
      g.drawLine(x + pin, pinTop, x + pin, gateTop - 2);
      for (var bit = 0; bit < 4; bit++) {
        final var ix = gate[bit];
        final var bubbled = bit < 3 && (index & (1 << bit)) == 0;
        final var from = bubbled ? gateBottom + 2 : gateBottom;
        final var to = index == 5 ? lowerTop : rail[bit];
        g.drawLine(x + ix, from, x + ix, to);
        if (bubbled) {
          drawBubble(g, x + ix, gateBottom + 1);
        }
        if (ix != railStart[bit] && ix != railEnd[bit]) {
          drawNode(g, x + ix, rail[bit]);
        }
      }
    }

    g.drawRect(x + 122, lowerTop, 16, lowerBottom - lowerTop);
    drawAndMark(g, x + 122, lowerTop, x + 138, lowerBottom);
    drawBubble(g, x + 130, lowerBottom + 1);
    g.drawLine(x + 130, lowerBottom + 2, x + 130, pinBottom);

    g.drawRect(x + 68, lowerTop, 44, lowerBottom - lowerTop);
    drawAndMark(g, x + 68, lowerTop, x + 112, lowerBottom);
    g.drawLine(x + 76, rail[3], x + 76, lowerTop);
    drawNode(g, x + 76, rail[3]);
    drawBubble(g, x + 70, lowerBottom + 1);
    drawBubble(g, x + 90, lowerBottom + 1);
    g.drawLine(x + 70, lowerBottom + 2, x + 70, pinBottom);
    g.drawLine(x + 90, lowerBottom + 2, x + 90, pinBottom);
    g.drawLine(x + 110, lowerBottom, x + 110, pinBottom);
  }

  /**
   * IEC AND mark. The glyph hangs below the line box, so the anchor sits one pixel above the
   * block center and the ink clears the lower edge.
   */
  private static void drawAndMark(Graphics g, int left, int top, int right, int bottom) {
    final var previous = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 6));
    GraphicsUtil.drawText(
        g,
        "&",
        (left + right) / 2,
        (top + bottom) / 2 - 1,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_CENTER_OVERALL);
    g.setFont(previous);
  }

  /** Inversion bubble, 2×2, centered on the wire axis. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction dot centered on the common point of the segments. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  // Port consts are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte) ((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  protected void computeState(InstanceState state, byte inEn1, byte inEn2a, byte inEn2b, byte inA, byte inB, byte inC, byte[] outPorts) {
    final var enabled =
        state.getPortValue(mapPort(inEn1)) == Value.TRUE        // Active HIGH
        && state.getPortValue(mapPort(inEn2a)) == Value.FALSE   // Active LOW
        && state.getPortValue(mapPort(inEn2b)) == Value.FALSE;  // Active LOW
    final var A = state.getPortValue(mapPort(inA)) == Value.TRUE ? (byte) 1 : 0;
    final var B = state.getPortValue(mapPort(inB)) == Value.TRUE ? (byte) 2 : 0;
    final var C = state.getPortValue(mapPort(inC)) == Value.TRUE ? (byte) 4 : 0;

    final int[][] outputPortStates = {
      {1, 0, 0, 0, 0, 0, 0, 0},
      {0, 1, 0, 0, 0, 0, 0, 0},
      {0, 0, 1, 0, 0, 0, 0, 0},
      {0, 0, 0, 1, 0, 0, 0, 0},
      {0, 0, 0, 0, 1, 0, 0, 0},
      {0, 0, 0, 0, 0, 1, 0, 0},
      {0, 0, 0, 0, 0, 0, 1, 0},
      {0, 0, 0, 0, 0, 0, 0, 1},
    };

    for (var i = 0; i < 8; i++) { // Active LOW
      final var val = enabled ? (outputPortStates[A + B + C][i] == 0 ? Value.TRUE : Value.FALSE) : Value.TRUE;
      state.setPort(mapPort(outPorts[i]), val, DELAY);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    byte[] out = {nY0, nY1, nY2, nY3, nY4, nY5, nY6, nY7};
    computeState(state, EN1, nEN2A, nEN2B, A, B, C, out);
  }

}
