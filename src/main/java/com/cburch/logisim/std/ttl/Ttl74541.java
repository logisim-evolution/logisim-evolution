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

import java.awt.Graphics2D;

/**
 * TTL 74x541: Octal buffer with 3-state outputs
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74f541.pdf">74F541 datasheet</a>.
 */
public class Ttl74541 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74541";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte OE1 = 1;
  public static final byte OE2 = 19;

  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte A3 = 4;
  public static final byte A4 = 5;
  public static final byte A5 = 6;
  public static final byte A6 = 7;
  public static final byte A7 = 8;
  public static final byte A8 = 9;

  // Outputs
  public static final byte Y1 = 18;
  public static final byte Y2 = 17;
  public static final byte Y3 = 16;
  public static final byte Y4 = 15;
  public static final byte Y5 = 14;
  public static final byte Y6 = 13;
  public static final byte Y7 = 12;
  public static final byte Y8 = 11;

  // Power supply
  public static final byte GND = 10;
  public static final byte VCC = 20;

  private static final byte[] INPUTS = new byte[] { A1, A2, A3, A4, A5, A6, A7, A8 };
  private static final byte[] OUTPUTS = new byte[] { Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8 };

  private InstanceState _state;

  public Ttl74541() {
    super(
            _ID,
            (byte) 20,
            OUTPUTS,
            new String[] {
              "nOE1", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8",
              "Y8", "Y7", "Y6", "Y5", "Y4", "Y3", "Y2", "Y1", "nOE2"
            },
            null);
  }

  /** Draw the buffer shape (triangle) plus connectors for all inputs and outputs.
   *
   * @param graphics the graphics context
   * @param x the x coordinate of the tip of the triangle
   * @param y the y coordinate of the tip of the triangle
   * @param top the y coordinate of the top-edge of the device (!)
   * @param bottom the y coordinate of the bottom-edge of the device (!)
   */
  private void drawBuffer(Graphics2D graphics, int x, int y, int top, int bottom) {
    final var g =  (Graphics2D) graphics.create();

    // Input
    g.drawPolyline(
        new int[] { x - 10, x - 10, x, x },
        new int[] { bottom - AbstractTtlGate.PIN_HEIGHT, y + 20, y + 20, y + 10 },
        4);

    // Buffer
    g.drawPolyline(
        new int[] { x, x + 5, x - 5, x },
        new int[] { y, y + 10, y + 10, y },
        4);

    // Output
    g.drawPolyline(
        new int[] { x + 10, x + 10, x, x },
        new int[] { top + AbstractTtlGate.PIN_HEIGHT, y - 5, y - 5, y },
        4);

    // Control
    g.drawPolyline(
        new int[] { x - 10, x - 10, x - 3 },
        new int[] { y + 15, y + 5, y + 5 },
        3);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);

    final var g = (Graphics2D) painter.getGraphics();

    // Draw buffers
    for (var i = 0; i < 8; i++) {
      drawBuffer(g, x + 40 + i * 20, y + 25, y, y + height);
    }

    // OE1
    g.drawPolyline(
        new int[] { x + 10, x + 10, x + 13 },
        new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 43, y + 43 },
        3);
    g.drawOval(x + 13, y + 42, 2, 2);

    // OE2
    g.drawPolyline(
        new int[] { x + 30, x + 30, x + 10, x + 10, x + 13 },
        new int[] { y + AbstractTtlGate.PIN_HEIGHT, y + 20, y + 20, y + 37, y + 37 },
        5);
    g.drawOval(x + 13, y + 36, 2, 2);

    // AND
    g.drawPolyline(
        new int[] { x + 22, x + 15, x + 15, x + 22 },
        new int[] { y + 35, y + 35, y + 45, y + 45 },
        4);
    g.drawArc(x + 17, y + 35, 10, 10, 270, 180);
    g.drawLine(x + 27, y + 40, x + 170, y + 40);
  }

  /** IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  private byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /** Gets the current state of the specified pin
   *
   * @param dsPinNr datasheet pin number
   * @return the current state of the specified pin
   */
  private Value getPort(byte dsPinNr) {
    return _state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  /** Sets the specified pin to the specified value
   *
   * @param dsPinNr datasheet pin number
   * @param v the value for the pin
   */
  private void setPort(byte dsPinNr, Value v) {
    _state.setPort(pinNrToPortNr(dsPinNr), v, DELAY);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;

    var active = (getPort(OE1) == Value.FALSE) && (getPort(OE2) == Value.FALSE);

    for (var i = 0; i < 8; i++) {
      setPort(OUTPUTS[i], active ? getPort(INPUTS[i]) : Value.UNKNOWN);
    }
  }
}
