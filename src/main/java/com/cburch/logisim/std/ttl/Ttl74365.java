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

  /**
   * Six non-inverting three-state buffers. Both active-low enables enter one AND gate on its left
   * face; the gate output is the shared active-high enable and stops on each buffer slant.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var railY = y + height / 2;
    final var topMid = pinTop + 11;
    final var bottomMid = pinBottom - 11;
    final var gateWidth = 12;
    final var gateHeight = 16;
    final var gateRight = x + 26;
    final var inputFace = gateRight - gateWidth;
    final var upperInput = railY - 2;
    final var lowerInput = railY + 2;
    Drawgates.paintAnd(g, gateRight, railY, gateWidth, gateHeight, false);
    drawBubble(g, inputFace - 1, upperInput);
    drawBubble(g, inputFace - 1, lowerInput);

    final var oe2Elbow = railY - gateHeight / 2 - 4;
    g.drawLine(x + 30, pinTop, x + 30, oe2Elbow);
    g.drawLine(x + 30, oe2Elbow, x + 10, oe2Elbow);
    g.drawLine(x + 10, oe2Elbow, x + 10, upperInput);
    g.drawLine(x + 10, upperInput, inputFace - 2, upperInput);
    g.drawLine(x + 10, pinBottom, x + 10, lowerInput);
    g.drawLine(x + 10, lowerInput, inputFace - 2, lowerInput);

    drawBuffer(g, x + 30, x + 50, pinBottom, bottomMid, railY);
    drawBuffer(g, x + 70, x + 90, pinBottom, bottomMid, railY);
    drawBuffer(g, x + 110, x + 130, pinBottom, bottomMid, railY);
    drawBuffer(g, x + 50, x + 70, pinTop, topMid, railY);
    drawBuffer(g, x + 90, x + 110, pinTop, topMid, railY);
    drawBuffer(g, x + 130, x + 150, pinTop, topMid, railY);
    g.drawLine(gateRight, railY, x + 140, railY);
    drawNode(g, x + 40, railY);
    drawNode(g, x + 60, railY);
    drawNode(g, x + 80, railY);
    drawNode(g, x + 100, railY);
    drawNode(g, x + 120, railY);
  }

  /**
   * One buffer, four pixels left of its output pin. The enable tap is six pixels in from the base.
   * The output runs straight from the tip to the pin column, then turns into the pin.
   */
  private static void drawBuffer(
      Graphics g, int inputX, int outputX, int pinY, int midY, int railY) {
    final var tipX = outputX - 4;
    final var baseX = tipX - 12;
    final var towardRail = Integer.signum(railY - midY);
    g.drawPolygon(
        new int[] {baseX, tipX, baseX}, new int[] {midY - 4, midY, midY + 4}, 3);
    g.drawLine(inputX, pinY, inputX, midY);
    g.drawLine(inputX, midY, baseX, midY);
    g.drawLine(tipX, midY, outputX, midY);
    g.drawLine(outputX, midY, outputX, pinY);
    g.drawLine(baseX + 6, railY, baseX + 6, midY + 2 * towardRail);
  }

  /** Inversion bubble, 2×2, centered on the wire axis and touching the gate face. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction dot centered on the common point of the meeting wires. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
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

}
