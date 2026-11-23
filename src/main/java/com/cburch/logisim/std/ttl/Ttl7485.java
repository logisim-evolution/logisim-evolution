/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class Ttl7485 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7485";

  public Ttl7485() {
    super(
        _ID,
        (byte) 16,
        new byte[] {5, 6, 7},
        new String[] {
          "B3",
          "A<B",
          "A=B",
          "A>B",
          "A>B",
          "A=B",
          "A<B",
          "B0",
          "A0",
          "B1",
          "A1",
          "A2",
          "B2",
          "A3"
        },
        new Ttl7485HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawComparator(g, x, y, height);
  }

  /**
   * Four identical bit cells compare A with B. Each cell takes A above B on the left and drives one
   * shared result bus from the right. The output blocks take that bus and a cascade input on the
   * left and drive the magnitude output on the right.
   */
  private static void drawComparator(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var cellTop = y + 14;
    final var cellBot = y + 22;
    final var outTop = y + 30;
    final var outBot = y + 38;
    final var c3L = x + 36;
    final var c3R = x + 42;
    final var c2L = x + 76;
    final var c2R = x + 82;
    final var c1L = x + 98;
    final var c1R = x + 104;
    final var c0L = x + 136;
    final var c0R = x + 142;
    final var ltL = x + 36;
    final var ltR = x + 44;
    final var eqL = x + 56;
    final var eqR = x + 64;
    final var gtL = x + 76;
    final var gtR = x + 84;
    final var yA = y + 16;
    final var yBit = y + 18;
    final var yB = y + 20;
    final var yBus = y + 26;
    final var yTap = y + 32;
    final var yMag = y + 34;
    final var yCas = y + 36;
    final var yUnder = y + 38;
    final var yLess = y + 42;
    final var yEqual = y + 46;

    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 5));
    drawBlock(g, c3L, cellTop, c3R, cellBot, "3");
    drawBlock(g, c2L, cellTop, c2R, cellBot, "2");
    drawBlock(g, c1L, cellTop, c1R, cellBot, "1");
    drawBlock(g, c0L, cellTop, c0R, cellBot, "0");
    drawBlock(g, ltL, outTop, ltR, outBot, "<");
    drawBlock(g, eqL, outTop, eqR, outBot, "=");
    drawBlock(g, gtL, outTop, gtR, outBot, ">");
    g.setFont(previousFont);

    // One result bus. Each branch is a single polyline, so the thick corner is filled
    // and the run still ends on the outermost taps.
    final var graphics = (Graphics2D) g;
    final var previousStroke = graphics.getStroke();
    graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g.drawPolyline(
        new int[] {ltL, x + 34, x + 34, x + 144, x + 144, c0R},
        new int[] {yTap, yTap, yBus, yBus, yBit, yBit},
        6);
    g.drawPolyline(new int[] {c3R, x + 44, x + 44}, new int[] {yBit, yBit, yBus}, 3);
    g.drawPolyline(new int[] {c2R, x + 84, x + 84}, new int[] {yBit, yBit, yBus}, 3);
    g.drawPolyline(new int[] {c1R, x + 106, x + 106}, new int[] {yBit, yBit, yBus}, 3);
    g.drawPolyline(new int[] {x + 54, x + 54, eqL}, new int[] {yBus, yTap, yTap}, 3);
    g.drawPolyline(new int[] {x + 74, x + 74, gtL}, new int[] {yBus, yTap, yTap}, 3);
    graphics.setStroke(previousStroke);

    // A is the upper left contact and B the lower one. When B's pin is to the right of the cell,
    // that wire drops down the pin and rises on the left.
    g.drawLine(pinX(x, 15), pinTop, pinX(x, 15), yA);
    g.drawLine(pinX(x, 15), yA, c3L, yA);
    g.drawLine(pinX(x, 1), pinBottom, pinX(x, 1), yB);
    g.drawLine(pinX(x, 1), yB, c3L, yB);
    g.drawLine(pinX(x, 13), pinTop, pinX(x, 13), yA);
    g.drawLine(pinX(x, 13), yA, c2L, yA);
    g.drawLine(pinX(x, 14), pinTop, pinX(x, 14), yB);
    g.drawLine(pinX(x, 14), yB, c2L, yB);
    g.drawLine(pinX(x, 12), pinTop, pinX(x, 12), yA);
    g.drawLine(pinX(x, 12), yA, c1L, yA);
    g.drawLine(pinX(x, 11), pinTop, pinX(x, 11), yUnder);
    g.drawLine(pinX(x, 11), yUnder, x + 94, yUnder);
    g.drawLine(x + 94, yUnder, x + 94, yB);
    g.drawLine(x + 94, yB, c1L, yB);
    g.drawLine(pinX(x, 10), pinTop, pinX(x, 10), yA);
    g.drawLine(pinX(x, 10), yA, c0L, yA);
    g.drawLine(pinX(x, 9), pinTop, pinX(x, 9), yUnder);
    g.drawLine(pinX(x, 9), yUnder, x + 134, yUnder);
    g.drawLine(x + 134, yUnder, x + 134, yB);
    g.drawLine(x + 134, yB, c0L, yB);

    // Cascades use the lower left contact. Each magnitude output leaves on the right.
    g.drawLine(pinX(x, 2), pinBottom, pinX(x, 2), yCas);
    g.drawLine(pinX(x, 2), yCas, ltL, yCas);
    g.drawLine(ltR, yMag, x + 46, yMag);
    g.drawLine(x + 46, yMag, x + 46, yLess);
    g.drawLine(x + 46, yLess, pinX(x, 7), yLess);
    g.drawLine(pinX(x, 7), yLess, pinX(x, 7), pinBottom);
    g.drawLine(pinX(x, 3), pinBottom, pinX(x, 3), yCas);
    g.drawLine(pinX(x, 3), yCas, eqL, yCas);
    g.drawLine(eqR, yMag, x + 66, yMag);
    g.drawLine(x + 66, yMag, x + 66, yEqual);
    g.drawLine(x + 66, yEqual, pinX(x, 6), yEqual);
    g.drawLine(pinX(x, 6), yEqual, pinX(x, 6), pinBottom);
    g.drawLine(pinX(x, 4), pinBottom, pinX(x, 4), yCas);
    g.drawLine(pinX(x, 4), yCas, gtL, yCas);
    g.drawLine(gtR, yMag, pinX(x, 5), yMag);
    g.drawLine(pinX(x, 5), yMag, pinX(x, 5), pinBottom);
  }

  /** Label bounds are centered on the block: even text height meets both edges equally. */
  private static void drawBlock(Graphics g, int left, int top, int right, int bottom, String name) {
    g.drawRect(left, top, right - left, bottom - top);
    GraphicsUtil.drawText(
        g,
        name,
        (left + right) / 2,
        top + (bottom - top) / 2,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_CENTER_OVERALL);
  }

  private static int pinX(int origin, int pin) {
    return pin <= 8 ? origin + (pin - 1) * 20 + 10 : origin + (16 - pin) * 20 + 10;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var A0 = state.getPortValue(8) == Value.TRUE ? (byte) 1 : 0;
    final var A1 = state.getPortValue(10) == Value.TRUE ? (byte) 2 : 0;
    final var A2 = state.getPortValue(11) == Value.TRUE ? (byte) 4 : 0;
    final var A3 = state.getPortValue(13) == Value.TRUE ? (byte) 8 : 0;
    final var B0 = state.getPortValue(7) == Value.TRUE ? (byte) 1 : 0;
    final var B1 = state.getPortValue(9) == Value.TRUE ? (byte) 2 : 0;
    final var B2 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
    final var B3 = state.getPortValue(0) == Value.TRUE ? (byte) 8 : 0;
    final var A = (byte) (A3 + A2 + A1 + A0);
    final var B = (byte) (B3 + B2 + B1 + B0);
    if (A > B) {
      state.setPort(4, Value.TRUE, 1);
      state.setPort(5, Value.FALSE, 1);
      state.setPort(6, Value.FALSE, 1);
    } else if (A < B) {
      state.setPort(4, Value.FALSE, 1);
      state.setPort(5, Value.FALSE, 1);
      state.setPort(6, Value.TRUE, 1);
    } else {
      if (state.getPortValue(2) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.TRUE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else if (state.getPortValue(1) == Value.TRUE && state.getPortValue(3) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else if (state.getPortValue(1) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.TRUE, 1);
      } else if (state.getPortValue(3) == Value.TRUE) {
        state.setPort(4, Value.TRUE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else {
        state.setPort(4, Value.TRUE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.TRUE, 1);
      }
    }
  }
}
