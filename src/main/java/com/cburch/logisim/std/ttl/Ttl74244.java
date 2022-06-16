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
import java.awt.Graphics;

/**
 * TTL 74x244 octal buffers and line drivers with three-state outputs
 * Model based on https://www.ti.com/product/SN74LS244 datasheet.
 */
public class Ttl74244 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74244";

  public Ttl74244() {
    super(
      _ID,
      (byte) 20,
      new byte[] {3, 5, 7, 9, 12, 14, 16, 18},
      new String[] {
        "n1G", "1A1", "2Y4", "1A2", "2Y3", "1A3", "2Y2", "1A4", "2Y1",
        "2A1", "1Y4", "2A2", "1Y3", "2A3", "1Y2", "2A4", "1Y1", "n2G",
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
    if (state.getPortValue(0) == Value.TRUE) {
      // Channel 1 disabled
      state.setPort(16, Value.UNKNOWN, 1);
      state.setPort(14, Value.UNKNOWN, 1);
      state.setPort(12, Value.UNKNOWN, 1);
      state.setPort(10, Value.UNKNOWN, 1);
    } else {
      // Channel 1 enabled, 1A->1Y
      state.setPort(16, state.getPortValue(1), 1);
      state.setPort(14, state.getPortValue(3), 1);
      state.setPort(12, state.getPortValue(5), 1);
      state.setPort(10, state.getPortValue(7), 1);
    }
    if (state.getPortValue(17) == Value.TRUE) {
      // Channel 2 disabled
      state.setPort(8, Value.UNKNOWN, 1);
      state.setPort(6, Value.UNKNOWN, 1);
      state.setPort(4, Value.UNKNOWN, 1);
      state.setPort(2, Value.UNKNOWN, 1);
    } else {
      // Channel 2 enabled, 2A->2Y
      state.setPort(8, state.getPortValue(9), 1);
      state.setPort(6, state.getPortValue(11), 1);
      state.setPort(4, state.getPortValue(13), 1);
      state.setPort(2, state.getPortValue(15), 1);
    }
  }

  private void drawBuffers(Graphics g, int x, int y, int height) {
    // channel 1 enable
    g.drawPolyline(
      new int[] { x + 10, x + 10, x + 20, x + 20 },
      new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 10, y + height - 10, y + height - 13 },
      4);
    g.drawOval(x + 18, y + height - 17, 4, 4);
  
    g.drawPolyline(
      new int[] { x + 15, x + 20, x + 25, x + 15 },
      new int[] { y + height - 17, y + height - 25, y + height - 17, y + height - 17 },
      4);

    g.drawPolyline(
      new int[] { x + 20, x + 20, x + 155 },
      new int[] { y + height - 25, y + height - 28, y + height - 28 },
      3);

    // channel 1 buffers
    for (int i = x + 30; i < x + 190; i += 40) {
      // input
      g.drawPolyline(
        new int[] { i, i, i + 10, i + 10 },
        new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 10, y + height - 10, y + height - 16 },
        4);

      // buffer
      g.drawPolyline(
        new int[] { i + 6, i + 10, i + 14, i + 6 },
        new int[] { y + height - 16, y + height - 22, y + height - 16, y + height - 16 },
        4);
      
      // enable
      if ( i < x + 150 ) {
        g.fillOval(i + 4, y + height - 29, 2, 2);
      }
      g.drawPolyline(
        new int[] { i + 5, i + 5, i + 8 },
        new int[] { y + height - 28, y + height - 20, y + height - 20 },
        3);

      // output
      g.drawPolyline(
        new int[] { i + 10, i + 10, i + 20, i + 20 },
        new int[] { y + height - 22, y + 10, y + 10, y + AbstractTtlGate.PIN_HEIGHT },
        4);
    }

    // channel 2 enable
    g.drawLine(x + 30, y + AbstractTtlGate.PIN_HEIGHT, x + 30, y + 12);
    g.drawOval(x + 28, y + 13, 4, 4);

    g.drawPolyline(
      new int[] { x + 25, x + 30, x + 35, x + 25 },
      new int[] { y + 17, y + 25, y + 17, y + 17 },
      4);

    g.drawPolyline(
      new int[] { x + 30, x + 30, x + 175 },
      new int[] { y + 25, y + 28, y + 28 },
      3);

    // channel 2 buffers
    for (int i = x + 70; i < x + 230; i += 40) {
      // input
      g.drawPolyline(
        new int[] { i, i, i - 10, i - 10 },
        new int[] { y + AbstractTtlGate.PIN_HEIGHT, y +  10, y + 10, y + 16 },
        4);

      // buffer
      g.drawPolyline(
        new int[] { i - 6, i - 10, i - 14, i - 6 },
        new int[] { y + 16, y + 22, y + 16, y + 16 },
        4);
      
      // enable
      if ( i < x + 190 ) {
        g.fillOval(i - 16, y + 27, 2, 2);
      }
      g.drawPolyline(
        new int[] { i - 15, i - 15, i - 12 },
        new int[] { y + 28, y + 20, y + 20 },
        3);

      // output
      g.drawPolyline(
        new int[] { i - 10, i - 10, i - 20, i - 20 },
        new int[] { y + 22, y + height - 10, y + height - 10, y + height - AbstractTtlGate.PIN_HEIGHT },
        4);
    }
  }

}
