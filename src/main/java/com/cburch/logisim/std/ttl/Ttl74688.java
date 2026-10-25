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
import java.awt.Graphics;
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
   * Eight separate equality results enter one gate, together with active-low nE. The gate output
   * is the active-low P=Q pin. The results are not tied together.
   */
  private static void drawComparator(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var gateLeft = x + 16;
    final var gateTop = y + 16;
    final var gateWidth = 16;
    final var gateHeight = 28;
    final var gateRight = gateLeft + gateWidth;
    final var gateBottom = gateTop + gateHeight;
    // Rightmost cell uses the lane nearest its row, so most runs do not have to cross.
    final int[] upperCenters = {60, 100, 140, 180};
    final int[] upperLanes = {29, 26, 23, 20};
    final int[] lowerCenters = {40, 80, 120, 160};
    final int[] lowerLanes = {31, 34, 37, 40};
    for (var i = 0; i < upperCenters.length; i++) {
      final var center = x + upperCenters[i];
      drawEqualCell(g, center, y + 10, false, pinTop);
      final var lane = y + upperLanes[i];
      g.drawLine(center, y + 18, center, lane);
      g.drawLine(gateRight, lane, center, lane);
    }
    for (var i = 0; i < lowerCenters.length; i++) {
      final var center = x + lowerCenters[i];
      drawEqualCell(g, center, y + 42, true, pinBottom);
      final var lane = y + lowerLanes[i];
      g.drawLine(center, y + 42, center, lane);
      g.drawLine(gateRight, lane, center, lane);
    }
    g.drawRect(gateLeft, gateTop, gateWidth, gateHeight);
    final var enableX = x + 10;
    final var enableIn = gateLeft + 8;
    g.drawOval(enableX - 2, pinBottom - 4, 4, 4);
    g.drawLine(enableX, pinBottom - 4, enableX, gateBottom + 2);
    g.drawLine(enableX, gateBottom + 2, enableIn, gateBottom + 2);
    g.drawLine(enableIn, gateBottom + 2, enableIn, gateBottom);
    final var outputX = x + 30;
    g.drawLine(outputX, gateTop, outputX, pinTop + 4);
    g.drawOval(outputX - 2, pinTop, 4, 4);
  }

  /** One P/Q pair. Both leads are the same length and the equals mark is centered in the cell. */
  private static void drawEqualCell(
      Graphics g, int center, int top, boolean inputsFromBottom, int pinY) {
    final var size = 8;
    g.drawRect(center - size / 2, top, size, size);
    g.drawLine(center - 2, top + 3, center + 2, top + 3);
    g.drawLine(center - 2, top + 5, center + 2, top + 5);
    final var edge = inputsFromBottom ? top + size : top;
    final var lane = inputsFromBottom ? edge + 2 : edge - 2;
    for (var side : new int[] {-1, 1}) {
      final var pin = center + side * 10;
      final var entry = center + side * 2;
      g.drawLine(pin, pinY, pin, lane);
      g.drawLine(pin, lane, entry, lane);
      g.drawLine(entry, lane, entry, edge);
    }
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
