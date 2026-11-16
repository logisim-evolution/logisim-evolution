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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.std.plexers.PlexersLibrary;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Map;

public class Ttl7447 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7447";

  public static final int PORT_INDEX_B = 0;
  public static final int PORT_INDEX_C = 1;
  public static final int PORT_INDEX_LT = 2;
  public static final int PORT_INDEX_BI_RBO = 3;
  public static final int PORT_INDEX_RBI = 4;
  public static final int PORT_INDEX_D = 5;
  public static final int PORT_INDEX_A = 6;
  public static final int PORT_INDEX_QE = 7;
  public static final int PORT_INDEX_QD = 8;
  public static final int PORT_INDEX_QC = 9;
  public static final int PORT_INDEX_QB = 10;
  public static final int PORT_INDEX_QA = 11;
  public static final int PORT_INDEX_QG = 12;
  public static final int PORT_INDEX_QF = 13;

  public Ttl7447() {
    super(
        _ID,
        (byte) 16,
        new byte[] {9, 10, 11, 12, 13, 14, 15}, // Inputs
        new byte[] {}, // Unused
        new byte[] {4}, // Input/Output
        new String[] {"B", "C", "LT", "BI/RBO", "RBI", "D", "A", "e", "d", "c", "b", "a", "g", "f"},
        new Ttl7447HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y, height);
  }

  /**
   * Logic diagram of the '46A, '47A and 'LS47. Seven independent AND-OR-INVERT gates decode the
   * same BCD rails A, B, C and D into segments f, g, a, b, c, d and e. Each output is active low:
   * the wire stops on the far side of the bubble and runs straight to the pin. LT, RBI and BI/RBO
   * meet a separate blanking gate and do not tie the segment gates together.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final int pinTop = y + PIN_HEIGHT;
    final int pinBot = y + height - PIN_HEIGHT;
    final int gateTop = y + 13;
    final int gateBottom = y + 23;
    final int yA = y + 25;
    final int yB = y + 29;
    final int yC = y + 33;
    final int yD = y + 37;
    final int[] segmentX = {30, 50, 70, 90, 110, 130, 150};
    final String[] segment = {"f", "g", "a", "b", "c", "d", "e"};

    final var previous = g.getFont();
    final var label = new Font(Font.DIALOG, Font.PLAIN, 6);
    g.setFont(label);
    for (var i = 0; i < segmentX.length; i++) {
      final int p = x + segmentX[i];
      g.drawRect(p - 8, gateTop, 16, 10);
      // Dialog 6 is 9 px tall. V_CENTER_OVERALL puts that box on the 16×10 center.
      drawLabel(g, segment[i], p, gateTop + 5);
      drawBubble(g, p, gateTop - 1);
      wire(g, p, pinTop, p, gateTop - 2);
    }

    wire(g, x + 24, yA, x + 144, yA);
    wire(g, x + 10, yB, x + 148, yB);
    wire(g, x + 30, yC, x + 152, yC);
    wire(g, x + 36, yD, x + 156, yD);
    for (final int segmentPin : segmentX) {
      final int p = x + segmentPin;
      wire(g, p - 6, gateBottom, p - 6, segmentPin - 6 == 124 ? y + 46 : yA);
      wire(g, p - 2, gateBottom, p - 2, yB);
      wire(g, p + 2, gateBottom, p + 2, yC);
      wire(g, p + 6, gateBottom, p + 6, yD);
    }

    wire(g, x + 10, pinBot, x + 10, yB);
    wire(g, x + 30, pinBot, x + 30, yC);
    wire(g, x + 110, pinBot, x + 110, yD);
    wire(g, x + 130, pinBot, x + 130, y + 46);
    wire(g, x + 124, y + 46, x + 130, y + 46);

    // Blanking gate between the BI and RBI pins. It does not feed the segment gates.
    g.drawRect(x + 74, y + 39, 10, 10);
    drawLabel(g, "BI", x + 79, y + 44);
    g.setFont(previous);
    drawBubble(g, x + 73, y + 42);
    wire(g, x + 50, pinBot, x + 50, y + 42);
    wire(g, x + 50, y + 42, x + 72, y + 42);
    drawBubble(g, x + 73, y + 46);
    wire(g, x + 70, pinBot, x + 70, y + 46);
    wire(g, x + 70, y + 46, x + 72, y + 46);
    drawBubble(g, x + 85, y + 44);
    wire(g, x + 90, pinBot, x + 90, y + 44);
    wire(g, x + 86, y + 44, x + 90, y + 44);

    for (final int px : new int[] {44, 64, 84, 104, 124}) {
      drawNode(g, x + px, yA);
    }
    for (final int px : new int[] {28, 48, 68, 88, 108, 128}) {
      drawNode(g, x + px, yB);
    }
    for (final int px : new int[] {32, 52, 72, 92, 112, 132}) {
      drawNode(g, x + px, yC);
    }
    for (final int px : new int[] {56, 76, 96, 110, 116, 136}) {
      drawNode(g, x + px, yD);
    }
  }

  /**
   * Draws a label so the center of {@link GraphicsUtil#getTextBounds} is the given point.
   * {@code drawCenteredText} uses half the ascent and leaves the bounds low of that point.
   */
  private static void drawLabel(Graphics g, String text, int centerX, int centerY) {
    GraphicsUtil.drawText(
        g, text, centerX, centerY, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  /** One horizontal or vertical segment. */
  private static void wire(Graphics g, int x1, int y1, int x2, int y2) {
    g.drawLine(x1, y1, x2, y2);
  }

  /** Active-low mark. Even 2×2 oval, center on the wire axis, outside the gate. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction of three or four branches. The oval center is the common point. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.getComponent().setPullPorts(Map.of(PORT_INDEX_BI_RBO, Value.TRUE));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var inputValue = DisplayDecoder.getdecval(state, false, 0, PORT_INDEX_A, PORT_INDEX_B, PORT_INDEX_C, PORT_INDEX_D);
    final var blankZero = state.getPortValue(PORT_INDEX_RBI) == Value.FALSE && inputValue == 0;
    DisplayDecoder.computeDisplayDecoderOutputs(
        state,
        inputValue,
        PORT_INDEX_QA,
        PORT_INDEX_QB,
        PORT_INDEX_QC,
        PORT_INDEX_QD,
        PORT_INDEX_QE,
        PORT_INDEX_QF,
        PORT_INDEX_QG,
        PORT_INDEX_LT,
        PORT_INDEX_BI_RBO,
        PORT_INDEX_RBI);
    final var value = (blankZero && state.getPortValue(PORT_INDEX_LT) != Value.FALSE) ? Value.FALSE : Value.UNKNOWN;
    state.setPort(PORT_INDEX_BI_RBO, value, PlexersLibrary.DELAY);
  }
}
