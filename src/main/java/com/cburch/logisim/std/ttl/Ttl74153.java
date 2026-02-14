/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 * TTL 74x153: Dual 4-line to 1-line data selector
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls153.pdf">74LS153 datasheet</a>.
 */
public class Ttl74153 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74153";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte S0 = 14;
  public static final byte S1 = 2;

  public static final byte L1_En = 1;
  public static final byte L1_D0 = 6;
  public static final byte L1_D1 = 5;
  public static final byte L1_D2 = 4;
  public static final byte L1_D3 = 3;

  public static final byte L2_En = 15;
  public static final byte L2_D0 = 10;
  public static final byte L2_D1 = 11;
  public static final byte L2_D2 = 12;
  public static final byte L2_D3 = 13;

  // Outputs
  public static final byte L1_Y = 7;
  public static final byte L2_Y = 9;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private InstanceState _state;

  public Ttl74153() {
    super(
            _ID,
            (byte) 16,
            new byte[] {
              L1_Y, L2_Y
            },
            new String[] {
              "n1E", "S1", "1D3", "1D2", "1D1", "1D0", "1Y",
              "2Y", "2D0", "2D1", "2D2", "2D3", "S0", "n2E"
            },
            80,
            null);
  }

  /** Draw the mux shape (trapezoid) plus connectors for all inputs and outputs.
   *
   * @param graphics the graphics context
   * @param x the x coordinate of the bottom-left corner of the trapezoid
   * @param y the y coordinate of the bottom-left corner of the trapezoid
   * @param top the y coordinate of the top-edge of the device (!)
   * @param bottom the y coordinate of the bottom-edge of the device (!)
   * @param direction the direction that the output of the mux is pointing to. Only
   *                  NORTH and SOUTH are supported.
   */
  private void drawMux(Graphics2D graphics, int x, int y, int top, int bottom, Direction direction) {
    final var g =  (Graphics2D) graphics.create();
    final var defStroke = g.getStroke();
    final var metrics = g.getFontMetrics();

    Polygon mux;

    if (direction == Direction.NORTH) {
      mux = new Polygon(new int[] {x, x + 80, x + 74, x + 6}, new int[] {y, y, y - 18, y - 18}, 4);
    } else {
      mux = new Polygon(new int[] {x, x + 80, x + 74, x + 6}, new int[] {y, y, y + 18, y + 18}, 4);
    }

    g.setStroke(new BasicStroke(2));
    g.drawPolygon(mux);

    g.setStroke(defStroke);

    var height = metrics.getAscent();

    for (var i = 0; i < 4; i++) {
      var str = Integer.toString(3 - i);
      var width = metrics.stringWidth(str);

      if (direction == Direction.NORTH) {
        g.drawLine(x + 10 + i * 20, bottom - AbstractTtlGate.PIN_HEIGHT, x + 10 + i * 20, y + 1);
        g.drawString(str, x + 10 + i * 20 - (width / 2), y - 1);
      } else {
        g.drawLine(x + 10 + i * 20, top + AbstractTtlGate.PIN_HEIGHT, x + 10 + i * 20, y - 1);
        g.drawString(str, x + 10 + i * 20 - (width / 2), y + height - 1);
      }
    }

    if (direction == Direction.NORTH) {
      g.drawString("S", x + 1 + (metrics.stringWidth("S") / 2), y - 8 + (height / 2));
      g.drawString("E", x + 3 + (metrics.stringWidth("E") / 2), y - 14 + (height / 2));
      g.drawOval(x - 3 + (metrics.stringWidth("E") / 2), y - 18 + (height / 2), 4, 4);
      g.drawLine(x + 40, y - 19, x + 40, y - 22);
    } else {
      g.drawString("S", x + 1 + (metrics.stringWidth("S") / 2), y + 2 + height);
      g.drawString("E", x + 3 + (metrics.stringWidth("E") / 2), y + 8 + height);
      g.drawOval(x - 3 + (metrics.stringWidth("E") / 2), y + 18 - height, 4, 4);
      g.drawLine(x + 40, y + 19, x + 40, y + 22);
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);

    final var g = (Graphics2D) painter.getGraphics();

    drawMux(g, x + 40, y + 65, y, y + height, Direction.NORTH);
    // Y1
    g.drawPolyline(new int[] {x + 80, x + 130, x + 130}, new int[] {y + height - 37, y + height - 37, y + height - AbstractTtlGate.PIN_HEIGHT}, 3);
    // E1
    g.drawPolyline(new int[] {x + 10, x + 10, x + 38}, new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 28, y + height - 28}, 3);

    drawMux(g, x + 60, y + 15, y, y + height, Direction.SOUTH);
    // Y2
    g.drawPolyline(new int[] {x + 100, x + 150, x + 150}, new int[] {y + 37, y + 37, y + AbstractTtlGate.PIN_HEIGHT}, 3);
    // E2
    g.drawPolyline(new int[] {x + 30, x + 30, x + 58}, new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 28, y + 28}, 3);

    // Bus entries for S0, S1
    g.drawPolyline(new int[] {x + 30, x + 30, x + 33}, new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 68, y + 65}, 3);
    g.drawPolyline(new int[] {x + 50, x + 50, x + 53}, new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 12, y + 15}, 3);

    // S bus
    g.setStroke(new BasicStroke(2));
    g.drawPolyline(new int[] {x + 33, x + 33, x + 53, x + 53}, new int[] {y + 65, y + 40, y + 40, y + 15}, 4);
    g.drawLine(x + 33, y + height - 22, x + 41, y + height - 22);
    g.drawLine(x + 53, y + 22, x + 61, y + 22);
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

    final boolean[] data1 = {getPort(L1_D0), getPort(L1_D1), getPort(L1_D2), getPort(L1_D3)};
    final boolean[] data2 = {getPort(L2_D0), getPort(L2_D1), getPort(L2_D2), getPort(L2_D3)};
    final int select = (getPort(S1) ? 2 : 0) + (getPort(S0) ? 1 : 0);

    setPort(L1_Y, !getPort(L1_En) && data1[select]);
    setPort(L2_Y, !getPort(L2_En) && data2[select]);
  }
}
