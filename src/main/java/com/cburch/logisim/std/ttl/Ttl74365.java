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
 * TTL 74x365: hex non-inverting buffer/line driver with three-state outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT365.pdf">Nexperia
 * 74HC365/74HCT365 datasheet</a>. {@code nOE1} and {@code nOE2} are active low and common to all
 * six buffers. While both are low, each output follows its data input. A high level on either
 * enable releases every output. The 74x367 is a different device: its two enables split the six
 * buffers into groups of four and two. High-impedance is represented as {@link Value#UNKNOWN}.
 * Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on either enable releases every output. While the device is
 * enabled, an unknown or error data input is copied to that channel only.
 */
public class Ttl74365 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74365";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte OE1 = 1;
  public static final byte A1 = 2;
  public static final byte Y1 = 3;
  public static final byte A2 = 4;
  public static final byte Y2 = 5;
  public static final byte A3 = 6;
  public static final byte Y3 = 7;
  public static final byte GND = 8;
  public static final byte Y4 = 9;
  public static final byte A4 = 10;
  public static final byte Y5 = 11;
  public static final byte A5 = 12;
  public static final byte Y6 = 13;
  public static final byte A6 = 14;
  public static final byte OE2 = 15;
  public static final byte VCC = 16;

  /** Data inputs in channel order. */
  private static final byte[] INPUTS = {A1, A2, A3, A4, A5, A6};

  /** Data outputs in channel order. */
  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4, Y5, Y6};

  private static final String[] PORT_NAMES = {
    "nOE1 Output enable 1 (active low)",
    "1A",
    "1Y",
    "2A",
    "2Y",
    "3A",
    "3Y",
    "4Y",
    "4A",
    "5Y",
    "5A",
    "6Y",
    "6A",
    "nOE2 Output enable 2 (active low)"
  };

  /** Creates a 74365 hex buffer/line driver with three-state outputs. */
  public Ttl74365() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74365HdlGenerator());
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
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 30));
    g.drawLine((x + 30), (y + 30), (x + 34), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 36), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 34), (y + 36));
    g.drawLine((x + 48), (y + 30), (x + 50), (y + 30));
    g.drawLine((x + 50), (y + 30), (x + 50), (y + height - 7));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 30));
    g.drawLine((x + 70), (y + 30), (x + 74), (y + 30));
    g.drawLine((x + 74), (y + 24), (x + 86), (y + 30));
    g.drawLine((x + 74), (y + 36), (x + 86), (y + 30));
    g.drawLine((x + 74), (y + 24), (x + 74), (y + 36));
    g.drawLine((x + 88), (y + 30), (x + 90), (y + 30));
    g.drawLine((x + 90), (y + 30), (x + 90), (y + height - 7));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + 30));
    g.drawLine((x + 110), (y + 30), (x + 114), (y + 30));
    g.drawLine((x + 114), (y + 24), (x + 126), (y + 30));
    g.drawLine((x + 114), (y + 36), (x + 126), (y + 30));
    g.drawLine((x + 114), (y + 24), (x + 114), (y + 36));
    g.drawLine((x + 128), (y + 30), (x + 130), (y + 30));
    g.drawLine((x + 130), (y + 30), (x + 130), (y + height - 7));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 30));
    g.drawLine((x + 130), (y + 30), (x + 134), (y + 30));
    g.drawLine((x + 134), (y + 24), (x + 146), (y + 30));
    g.drawLine((x + 134), (y + 36), (x + 146), (y + 30));
    g.drawLine((x + 134), (y + 24), (x + 134), (y + 36));
    g.drawLine((x + 148), (y + 30), (x + 150), (y + 30));
    g.drawLine((x + 150), (y + 30), (x + 150), (y + 7));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 30));
    g.drawLine((x + 90), (y + 30), (x + 94), (y + 30));
    g.drawLine((x + 94), (y + 24), (x + 106), (y + 30));
    g.drawLine((x + 94), (y + 36), (x + 106), (y + 30));
    g.drawLine((x + 94), (y + 24), (x + 94), (y + 36));
    g.drawLine((x + 108), (y + 30), (x + 110), (y + 30));
    g.drawLine((x + 110), (y + 30), (x + 110), (y + 7));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 30));
    g.drawLine((x + 50), (y + 30), (x + 54), (y + 30));
    g.drawLine((x + 54), (y + 24), (x + 66), (y + 30));
    g.drawLine((x + 54), (y + 36), (x + 66), (y + 30));
    g.drawLine((x + 54), (y + 24), (x + 54), (y + 36));
    g.drawLine((x + 68), (y + 30), (x + 70), (y + 30));
    g.drawLine((x + 70), (y + 30), (x + 70), (y + 7));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 22));
    structureBubble(g, (x + 10), (y + height - 7));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 22));
    structureBubble(g, (x + 30), (y + 7));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable1 = state.getPortValue(pinNrToPortNr(OE1));
    final var outputEnable2 = state.getPortValue(pinNrToPortNr(OE2));
    final var enabled = outputEnable1 == Value.FALSE && outputEnable2 == Value.FALSE;
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value =
          enabled ? state.getPortValue(pinNrToPortNr(INPUTS[index])) : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
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
