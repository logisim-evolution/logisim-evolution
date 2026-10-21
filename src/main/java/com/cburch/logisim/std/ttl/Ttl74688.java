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
    g.drawRect((x + 35), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 40), (y + 28));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 40), (y + height - 13));
    g.drawLine((x + 40), (y + height - 13), (x + 40), (y + 34));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 40), (y + height - 13));
    g.drawLine((x + 40), (y + height - 13), (x + 40), (y + 22));
    g.drawRect((x + 75), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 80), (y + 28));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 80), (y + height - 13));
    g.drawLine((x + 80), (y + height - 13), (x + 80), (y + 34));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 80), (y + height - 13));
    g.drawLine((x + 80), (y + height - 13), (x + 80), (y + 22));
    g.drawRect((x + 115), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 120), (y + 28));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 120), (y + height - 13));
    g.drawLine((x + 120), (y + height - 13), (x + 120), (y + 34));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 13));
    g.drawLine((x + 130), (y + height - 13), (x + 120), (y + height - 13));
    g.drawLine((x + 120), (y + height - 13), (x + 120), (y + 22));
    g.drawRect((x + 155), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 160), (y + 28));
    g.drawLine((x + 150), (y + height - 7), (x + 150), (y + height - 13));
    g.drawLine((x + 150), (y + height - 13), (x + 160), (y + height - 13));
    g.drawLine((x + 160), (y + height - 13), (x + 160), (y + 34));
    g.drawLine((x + 170), (y + height - 7), (x + 170), (y + height - 13));
    g.drawLine((x + 170), (y + height - 13), (x + 160), (y + height - 13));
    g.drawLine((x + 160), (y + height - 13), (x + 160), (y + 22));
    g.drawRect((x + 175), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 180), (y + 28));
    g.drawLine((x + 190), (y + 7), (x + 190), (y + 13));
    g.drawLine((x + 190), (y + 13), (x + 180), (y + 13));
    g.drawLine((x + 180), (y + 13), (x + 180), (y + 34));
    g.drawLine((x + 170), (y + 7), (x + 170), (y + 13));
    g.drawLine((x + 170), (y + 13), (x + 180), (y + 13));
    g.drawLine((x + 180), (y + 13), (x + 180), (y + 22));
    g.drawRect((x + 135), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 140), (y + 28));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 13));
    g.drawLine((x + 150), (y + 13), (x + 140), (y + 13));
    g.drawLine((x + 140), (y + 13), (x + 140), (y + 34));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 140), (y + 13));
    g.drawLine((x + 140), (y + 13), (x + 140), (y + 22));
    g.drawRect((x + 95), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 100), (y + 28));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 100), (y + 13));
    g.drawLine((x + 100), (y + 13), (x + 100), (y + 34));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 100), (y + 13));
    g.drawLine((x + 100), (y + 13), (x + 100), (y + 22));
    g.drawRect((x + 55), (y + 22), 10, 12);
    GraphicsUtil.drawCenteredText(g, "=", (x + 60), (y + 28));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 60), (y + 13));
    g.drawLine((x + 60), (y + 13), (x + 60), (y + 34));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 60), (y + 13));
    g.drawLine((x + 60), (y + 13), (x + 60), (y + 22));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 40));
    structureBubble(g, (x + 30), (y + 7));
    GraphicsUtil.drawCenteredText(g, "=", (x + 30), (y + 40));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 16));
    structureBubble(g, (x + 10), (y + height - 7));
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

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
