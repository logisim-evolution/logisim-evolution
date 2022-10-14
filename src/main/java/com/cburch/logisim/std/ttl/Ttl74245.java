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
import java.awt.Graphics;

/**
 * TTL 74x245 octal bus transceivers with three-state outputs
 * Model based on https://www.ti.com/product/SN74LS245 datasheet.
 */
public class Ttl74245 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74245";

  public Ttl74245() {
    super(
        _ID,
        (byte) 20,
        new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18 },
        new String[] {
          "DIR", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8",
          "B8", "B7", "B6", "B5", "B4", "B3", "B2", "B1", "nOE",
        },
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    drawBuffers(g, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    if (state.getPortValue(17) == Value.TRUE) {
      // Output disabled
      state.setPort(1, Value.UNKNOWN, 1);
      state.setPort(2, Value.UNKNOWN, 1);
      state.setPort(3, Value.UNKNOWN, 1);
      state.setPort(4, Value.UNKNOWN, 1);
      state.setPort(5, Value.UNKNOWN, 1);
      state.setPort(6, Value.UNKNOWN, 1);
      state.setPort(7, Value.UNKNOWN, 1);
      state.setPort(8, Value.UNKNOWN, 1);
      state.setPort(9, Value.UNKNOWN, 1);
      state.setPort(10, Value.UNKNOWN, 1);
      state.setPort(11, Value.UNKNOWN, 1);
      state.setPort(12, Value.UNKNOWN, 1);
      state.setPort(13, Value.UNKNOWN, 1);
      state.setPort(14, Value.UNKNOWN, 1);
      state.setPort(15, Value.UNKNOWN, 1);
      state.setPort(16, Value.UNKNOWN, 1);
    } else if (state.getPortValue(0) == Value.TRUE) {
      // DIR HIGH = A->B
      state.setPort(1, Value.UNKNOWN, 1);
      state.setPort(2, Value.UNKNOWN, 1);
      state.setPort(3, Value.UNKNOWN, 1);
      state.setPort(4, Value.UNKNOWN, 1);
      state.setPort(5, Value.UNKNOWN, 1);
      state.setPort(6, Value.UNKNOWN, 1);
      state.setPort(7, Value.UNKNOWN, 1);
      state.setPort(8, Value.UNKNOWN, 1);
      state.setPort(9, state.getPortValue(8), 1);
      state.setPort(10, state.getPortValue(7), 1);
      state.setPort(11, state.getPortValue(6), 1);
      state.setPort(12, state.getPortValue(5), 1);
      state.setPort(13, state.getPortValue(4), 1);
      state.setPort(14, state.getPortValue(3), 1);
      state.setPort(15, state.getPortValue(2), 1);
      state.setPort(16, state.getPortValue(1), 1);
    } else {
      // DIR LOW = B->A
      state.setPort(1, state.getPortValue(16), 1);
      state.setPort(2, state.getPortValue(15), 1);
      state.setPort(3, state.getPortValue(14), 1);
      state.setPort(4, state.getPortValue(13), 1);
      state.setPort(5, state.getPortValue(12), 1);
      state.setPort(6, state.getPortValue(11), 1);
      state.setPort(7, state.getPortValue(10), 1);
      state.setPort(8, state.getPortValue(9), 1);
      state.setPort(9, Value.UNKNOWN, 1);
      state.setPort(10, Value.UNKNOWN, 1);
      state.setPort(11, Value.UNKNOWN, 1);
      state.setPort(12, Value.UNKNOWN, 1);
      state.setPort(13, Value.UNKNOWN, 1);
      state.setPort(14, Value.UNKNOWN, 1);
      state.setPort(15, Value.UNKNOWN, 1);
      state.setPort(16, Value.UNKNOWN, 1);
    }
  }

  private void drawBuffers(Graphics g, int x, int y, int height) {
    // DIR
    g.drawPolyline(
        new int[] { x + 10, x + 10, x + 17, x + 17 },
        new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + 10, y + 10, y + 13 },
        4);
    g.drawOval(x + 16, y + 13, 2, 2);

    g.fillOval(x + 9, y + height - 11, 2, 2);
    g.drawPolyline(
        new int[] { x + 10, x + 17, x + 17},
        new int[] { y + height - 10, y + height - 10, y + height - 15 },
        3);

    // nOE
    g.drawPolyline(
        new int[] { x + 30, x + 30, x + 27, x + 23, x + 23 },
        new int[] { y + AbstractTtlGate.PIN_HEIGHT, y + height - 13, y + height - 10, y + height - 10, y + height - 13 },
        5);
    g.drawOval(x + 22, y + height - 15, 2, 2);

    g.fillOval(x + 29, y + 9, 2, 2);
    g.drawPolyline(
        new int[] { x + 30, x + 23, x + 23},
        new int[] { y + 10, y + 10, y + 13 },
        3);
    g.drawOval(x + 22, y + 13, 2, 2);

    // A enable
    g.drawPolyline(
        new int[] { x + 15, x + 15, x + 25, x + 25 },
        new int[] { y + height - 20, y + height - 15, y + height - 15, y + height - 20 },
        4);
    GraphicsUtil.drawCenteredArc(g, x + 20, y + height - 20, 5, 0, 180);
    g.drawPolyline(
        new int[] { x + 20, x + 20, x + 175 },
        new int[] { y + height - 25, y + height - 28, y + height - 28 },
        3);

    // A buffers
    for (int i = x + 30; i < x + 190; i += 20) {
      // input
      g.drawPolyline(
          new int[] { i, i + 3, i + 10, i + 10 },
          new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 10, y + height - 10, y + height - 17 },
          4);

      // buffer
      g.drawPolyline(
          new int[] { i + 6, i + 10, i + 14, i + 6 },
          new int[] { y + height - 17, y + height - 23, y + height - 17, y + height - 17 },
          4);
      
      // enable
      if (i < x + 170) {
        g.fillOval(i + 4, y + height - 29, 2, 2);
      }
      g.drawPolyline(
          new int[] { i + 5, i + 5, i + 8 },
          new int[] { y + height - 28, y + height - 21, y + height - 21 },
          3);

      // output
      g.drawPolyline(
          new int[] { i + 10, i + 10, i + 16, i + 20 },
          new int[] { y + height - 23, y + 16, y + 10, y + 10 },
          4);
      g.fillOval(i + 19, y + 9, 2, 2);
    }

    // B enable
    g.drawPolyline(
        new int[] { x + 15, x + 15, x + 25, x + 25 },
        new int[] { y + 20, y + 15, y + 15, y + 20 },
        4);
    GraphicsUtil.drawCenteredArc(g, x + 20, y + 20, 5, 180, 180);
    g.drawPolyline(
        new int[] { x + 20, x + 20, x + 185 },
        new int[] { y + 25, y + 28, y + 28 },
        3);

    // B buffers
    for (int i = x + 50; i < x + 210; i += 20) {
      // input
      g.drawLine(i, y + AbstractTtlGate.PIN_HEIGHT, i, y + 17);

      // buffer
      g.drawPolyline(
          new int[] { i + 4, i, i - 4, i + 4 },
          new int[] { y + 17, y + 23, y + 17, y + 17 },
          4);
      
      // enable
      if (i < x + 190) {
        g.fillOval(i - 6, y + 27, 2, 2);
      }
      g.drawPolyline(
          new int[] { i - 5, i - 5, i - 2 },
          new int[] { y + 28, y + 21, y + 21 },
          3);

      // output
      g.drawPolyline(
          new int[] { i, i, i - 6, i - 10 },
          new int[] { y + 23, y + height - 16, y + height - 10, y + height - 10 },
          4);
      g.fillOval(i - 11, y + height - 11, 2, 2);
    }
  }

}
