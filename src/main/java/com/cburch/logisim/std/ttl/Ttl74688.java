/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x688: 8-bit identity comparator.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc688.pdf">TI SN74HC688</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC688.pdf">Nexperia 74HC688</a>
 * data sheets. The device compares words P and Q. A low nE, which TI calls nOE, drives nP=Q low
 * when every bit matches and high otherwise. A high nE forces nP=Q high, which is how a wider
 * comparison is built: the equal output of one device enables the next. There is no greater or
 * less output. The 74HC output is push-pull; this is not the open-collector output of the 74LS688.
 *
 * <p>A bit matches only when both pins read the same solid level. Unknown and error values do not
 * match, and nE is active only when it reads exactly {@link Value#FALSE}. An unconnected device
 * therefore keeps nP=Q high.
 */
public class Ttl74688 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74688";

  public static final int DELAY = 1;

  public static final byte E = 1;
  public static final byte P0 = 2;
  public static final byte Q0 = 3;
  public static final byte P1 = 4;
  public static final byte Q1 = 5;
  public static final byte P2 = 6;
  public static final byte Q2 = 7;
  public static final byte P3 = 8;
  public static final byte Q3 = 9;
  public static final byte P4 = 11;
  public static final byte Q4 = 12;
  public static final byte P5 = 13;
  public static final byte Q5 = 14;
  public static final byte P6 = 15;
  public static final byte Q6 = 16;
  public static final byte P7 = 17;
  public static final byte Q7 = 18;
  public static final byte PQ = 19;

  public static final byte GND = 10;
  public static final byte VCC = 20;

  /** Word P inputs, bit 0 first. */
  private static final byte[] WORD_P = {P0, P1, P2, P3, P4, P5, P6, P7};

  /** Word Q inputs, bit 0 first. */
  private static final byte[] WORD_Q = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};

  private static final byte[] OUTPUTS = {PQ};

  private static final String[] PORT_NAMES = {
    "nE / nOE enable (active low)",
    "P0",
    "Q0",
    "P1",
    "Q1",
    "P2",
    "Q2",
    "P3",
    "Q3",
    "P4",
    "Q4",
    "P5",
    "Q5",
    "P6",
    "Q6",
    "P7",
    "Q7",
    "nP=Q equal (active low)"
  };

  /** Creates a 74688 8-bit identity comparator. */
  public Ttl74688() {
    super(_ID, (byte) 20, OUTPUTS, PORT_NAMES, new Ttl74688HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawComparator(g, x, y, height);
  }

  /**
   * Compact form of the 74HC688 logic diagram. Each bit is an equality cell. The cells share one
   * rail into a gate with active-low enable and an active-low equal output.
   */
  private static void drawComparator(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var rail = y + 30;
    final var gateLeft = x + 14;
    final var gateTop = y + 22;
    final var gateWidth = 12;
    final var gateHeight = 16;
    final var gateRight = gateLeft + gateWidth;
    final var gateBottom = gateTop + gateHeight;
    final var gateMidX = gateLeft + gateWidth / 2;
    final int[] lower = {40, 80, 120, 160};
    final int[] upper = {60, 100, 140, 180};
    for (var center : lower) {
      drawEqualCell(g, x + center, y + 36, true, pinBottom);
      g.drawLine(x + center, y + 36, x + center, rail);
      drawNode(g, x + center, rail);
    }
    for (var stage = 0; stage < upper.length; stage++) {
      final var center = x + upper[stage];
      drawEqualCell(g, center, y + 14, false, pinTop);
      g.drawLine(center, y + 24, center, rail);
      if (stage + 1 < upper.length) {
        drawNode(g, center, rail);
      }
    }
    g.drawLine(gateRight, rail, x + upper[upper.length - 1], rail);
    g.drawRect(gateLeft, gateTop, gateWidth, gateHeight);

    final var enableX = x + 10;
    g.drawLine(enableX, pinBottom, enableX, gateBottom + 4);
    g.drawLine(enableX, gateBottom + 4, gateMidX, gateBottom + 4);
    g.drawOval(gateMidX - 2, gateBottom, 4, 4);

    g.drawOval(gateMidX - 2, gateTop - 4, 4, 4);
    g.drawLine(gateMidX, gateTop - 4, gateMidX, y + 12);
    g.drawLine(gateMidX, y + 12, x + 30, y + 12);
    g.drawLine(x + 30, y + 12, x + 30, pinTop);
  }

  /** One P/Q pair. Inputs come from the pin side; the equal result leaves the inner side. */
  private static void drawEqualCell(
      Graphics g, int center, int top, boolean inputsFromBottom, int pinY) {
    final var left = center - 5;
    final var cell = 10;
    g.drawRect(left, top, cell, cell);
    final var previous = g.getFont();
    g.setFont(previous.deriveFont(Font.PLAIN, 8f));
    GraphicsUtil.drawCenteredText(g, "=", center, top + cell / 2);
    g.setFont(previous);
    final var near = center - 2;
    final var far = center + 2;
    final var lowPin = center - 10;
    final var highPin = center + 10;
    if (inputsFromBottom) {
      final var bottom = top + cell;
      g.drawLine(lowPin, pinY, lowPin, bottom + 4);
      g.drawLine(lowPin, bottom + 4, near, bottom + 4);
      g.drawLine(near, bottom + 4, near, bottom);
      g.drawLine(highPin, pinY, highPin, bottom + 2);
      g.drawLine(highPin, bottom + 2, far, bottom + 2);
      g.drawLine(far, bottom + 2, far, bottom);
    } else {
      g.drawLine(lowPin, pinY, lowPin, top - 2);
      g.drawLine(lowPin, top - 2, near, top - 2);
      g.drawLine(near, top - 2, near, top);
      g.drawLine(highPin, pinY, highPin, top - 4);
      g.drawLine(highPin, top - 4, far, top - 4);
      g.drawLine(far, top - 4, far, top);
    }
  }

  /** Junction dot centered on a wire crossing. Every node is this size. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    setLow(state, PQ, isLow(state, E) && wordsMatch(state));
  }

  private static boolean wordsMatch(InstanceState state) {
    for (var bit = 0; bit < WORD_P.length; bit++) {
      if (!sameKnownLevel(state, WORD_P[bit], WORD_Q[bit])) {
        return false;
      }
    }
    return true;
  }

  /** A pair matches only when both pins carry the same solid high or low level. */
  private static boolean sameKnownLevel(InstanceState state, byte pPin, byte qPin) {
    final var p = state.getPortValue(pinNrToPortNr(pPin));
    final var q = state.getPortValue(pinNrToPortNr(qPin));
    return (p == Value.TRUE || p == Value.FALSE) && p == q;
  }

  private static boolean isLow(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }

  private static void setLow(InstanceState state, byte dsPinNr, boolean low) {
    state.setPort(pinNrToPortNr(dsPinNr), low ? Value.FALSE : Value.TRUE, DELAY);
  }

}
