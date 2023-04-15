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

import java.awt.BasicStroke;
import java.awt.Graphics2D;

/**
 * TTL 74x151: 8-line to 1-line data selector
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls151.pdf">74LS151 datasheet</a>.
 */
public class Ttl74151 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74151";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte A = 11;
  public static final byte B = 10;
  public static final byte C = 9;

  public static final byte G = 7;

  public static final byte D0 = 4;
  public static final byte D1 = 3;
  public static final byte D2 = 2;
  public static final byte D3 = 1;
  public static final byte D4 = 15;
  public static final byte D5 = 14;
  public static final byte D6 = 13;
  public static final byte D7 = 12;

  // Outputs
  public static final byte Y = 5;
  public static final byte W = 6;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] INPUTS = new byte[] { D0, D1, D2, D3, D4, D5, D6, D7 };

  private InstanceState _state;

  public Ttl74151() {
    super(
            _ID,
            (byte) 16,
            new byte[] {
              Y, W
            },
            new String[] {
              "D3", "D2", "D1", "D0", "Y", "W", "nG",
              "C", "B", "A", "D7", "D6", "D5", "D4"
            },
            null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);

    final var g = (Graphics2D) painter.getGraphics();

    // D0..D1
    for (var i = 0; i <= 1; i++) {
      // D0
      g.drawPolyline(
          new int[] { x + 70 - i * 20, x + 70 - i * 20, x + 34 - i * 2, x + 34 - i * 2, x + 45 + i * 10, x + 45 + i * 10 },
          new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 47 + i * 3, y + 47 + i * 3, y + 27 - i * 2, y + 27 - i * 2, y + 29 },
          6);
    }

    // D2..D3
    for (var i = 2; i <= 3; i++) {
      // D2
      g.drawPolyline(
          new int[] { x + 70 - i * 20, x + 70 - i * 20, x + 45 + i * 10, x + 45 + i * 10 },
          new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 27 - i * 2, y + 27 - i * 2, y + 29 },
          4);
    }

    // D4..D7
    for (var i = 4; i <= 7; i++) {
      g.drawPolyline(
          new int[] { x - 50 + i * 20, x - 50 + i * 20, x + 45 + i * 10, x + 45 + i * 10 },
          new int[] { y + AbstractTtlGate.PIN_HEIGHT, y + 27 - i * 2, y + 27 - i * 2, y + 29 },
          4);
    }

    // Y
    g.drawPolyline(
        new int[] { x + 90, x + 90, x + 75, x + 75 },
        new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 50, y + 50, y + 44 },
        4);

    // W
    g.drawPolyline(
        new int[] { x + 110, x + 110, x + 85, x + 85 },
        new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 47, y + 47, y + 46 },
        4);
    g.drawOval(x + 84, y + 44, 2, 2);

    // Mux
    g.drawPolygon(
        new int[] { x + 35, x + 125, x + 120, x + 40 },
        new int[] { y + 29, y + 29, y + 44, y + 44 },
        4);

    for (var i = 0; i <= 7; i++) {
      g.drawString(Integer.toString(i), x + 43 + i * 10, y + 34);
    }

    g.drawString("S", x + 119, y + 36);
    g.drawString("E", x + 116, y + 43);

    // Enable
    g.drawPolyline(
        new int[] { x + 130, x + 130, x + 123 },
        new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 41, y + 41 },
        3);
    g.drawOval(x + 121, y + 40, 2, 2);

    // Select A
    g.drawPolyline(
        new int[] { x + 110, x + 110, x + 112 },
        new int[] { y + AbstractTtlGate.PIN_HEIGHT, y + 9, y + 11 },
        3);

    // Select B
    g.drawPolyline(
        new int[] { x + 130, x + 130, x + 132 },
        new int[] { y + AbstractTtlGate.PIN_HEIGHT, y + 9, y + 11 },
        3);

    // Select C
    g.drawPolyline(
        new int[] { x + 150, x + 150, x + 148 },
        new int[] { y + AbstractTtlGate.PIN_HEIGHT, y + 9, y + 11 },
        3);

    // Select bus
    g.setStroke(new BasicStroke(2));
    g.drawLine(x + 112, y + 11, x + 148, y + 11);
    g.drawPolyline(
        new int[] { x + 134, x + 134, x + 124 },
        new int[] { y + 11, y + 34, y + 34 },
        3);
  }

  /** IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  protected byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /** Gets the current state of the specified pin
   *
   * @param dsPinNr datasheet pin number
   * @return true if the specified pin has a logic high level
   */
  private boolean getPort(byte dsPinNr) {
    return _state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.TRUE;
  }

  /** Sets the specified pin to the specified level
   *
   * @param dsPinNr datasheet pin number
   * @param b the logic level for the pin
   */
  private void setPort(byte dsPinNr, boolean b) {
    _state.setPort(pinNrToPortNr(dsPinNr), b ? Value.TRUE : Value.FALSE, DELAY);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;

    var inputs = new boolean[INPUTS.length];

    for (var i = 0; i < inputs.length; i++) {
      inputs[i] = getPort(INPUTS[i]);
    }

    final int select = (getPort(C) ? 4 : 0) + (getPort(B) ? 2 : 0) + (getPort(A) ? 1 : 0);

    setPort(Y, !getPort(G) &&  inputs[select]);
    setPort(W,  getPort(G) || !inputs[select]);
  }
}
