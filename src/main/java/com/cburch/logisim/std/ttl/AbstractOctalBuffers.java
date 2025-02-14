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
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;

/**
 * TTL 74x24x octal buffers and line drivers with three-state outputs
 * Model based on https://www.ti.com/product/SN74LS240 datasheet.
 */
public class AbstractOctalBuffers extends AbstractTtlGate {

  private boolean ch1OutputInverted, ch1EnableInverted, ch2OutputInverted, ch2EnableInverted;

  protected AbstractOctalBuffers(String name, byte pins, byte[] outputPorts, String[] ttlPortNames, HdlGeneratorFactory generator) {
    super(name, pins, outputPorts, ttlPortNames, generator);
  }

  public void setOutputInverted(boolean ch1Invert, boolean ch2Invert) {
    ch1OutputInverted = ch1Invert;
    ch2OutputInverted = ch2Invert;
  }

  public void setEnableInverted(boolean ch1Invert, boolean ch2Invert) {
    ch1EnableInverted = ch1Invert; // channel 1 enable always inverted in known chips
    ch2EnableInverted = ch2Invert;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    drawBuffers(g, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    if (state.getPortValue(0) == (ch1EnableInverted ? Value.TRUE : Value.FALSE)) {
      // Channel 1 disabled
      state.setPort(16, Value.UNKNOWN, 1);
      state.setPort(14, Value.UNKNOWN, 1);
      state.setPort(12, Value.UNKNOWN, 1);
      state.setPort(10, Value.UNKNOWN, 1);
    } else if (ch1OutputInverted) {
      // Channel 1 enabled, inverted
      state.setPort(16, state.getPortValue(1).not(), 1);
      state.setPort(14, state.getPortValue(3).not(), 1);
      state.setPort(12, state.getPortValue(5).not(), 1);
      state.setPort(10, state.getPortValue(7).not(), 1);
    } else {
      // Channel 1 enabled, non-inverted
      state.setPort(16, state.getPortValue(1), 1);
      state.setPort(14, state.getPortValue(3), 1);
      state.setPort(12, state.getPortValue(5), 1);
      state.setPort(10, state.getPortValue(7), 1);
    }
    if (state.getPortValue(17) == (ch2EnableInverted ? Value.TRUE : Value.FALSE)) {
      // Channel 2 disabled
      state.setPort(8, Value.UNKNOWN, 1);
      state.setPort(6, Value.UNKNOWN, 1);
      state.setPort(4, Value.UNKNOWN, 1);
      state.setPort(2, Value.UNKNOWN, 1);
    } else if (ch2OutputInverted) {
      // Channel 2 enabled, inverted
      state.setPort(8, state.getPortValue(9).not(), 1);
      state.setPort(6, state.getPortValue(11).not(), 1);
      state.setPort(4, state.getPortValue(13).not(), 1);
      state.setPort(2, state.getPortValue(15).not(), 1);
    } else {
      // Channel 2 enabled, non-inverted
      state.setPort(8, state.getPortValue(9), 1);
      state.setPort(6, state.getPortValue(11), 1);
      state.setPort(4, state.getPortValue(13), 1);
      state.setPort(2, state.getPortValue(15), 1);
    }
  }

  private void drawBuffers(Graphics g, int x, int y, int height) {
    // channel 1 enable
    if (ch1EnableInverted) {
      g.drawPolyline(
          new int[] { x + 10, x + 10, x + 20, x + 20 },
          new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 10, y + height - 10, y + height - 13 },
          4);
      g.drawOval(x + 18, y + height - 17, 4, 4);
    } else {
      g.drawPolyline(
          new int[] { x + 10, x + 10, x + 20, x + 20 },
          new int[] { y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 10, y + height - 10, y + height - 17 },
          4);
    }

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
      if (i < x + 150) {
        g.fillOval(i + 4, y + height - 29, 2, 2);
      }
      g.drawPolyline(
          new int[] { i + 5, i + 5, i + 8 },
          new int[] { y + height - 28, y + height - 20, y + height - 20 },
          3);

      // output
      if (ch1OutputInverted) {
        g.drawOval(i + 9, y + height - 25, 2, 2);
        g.drawPolyline(
            new int[] { i + 10, i + 10, i + 20, i + 20 },
            new int[] { y + height - 25, y + 10, y + 10, y + AbstractTtlGate.PIN_HEIGHT },
            4);
      } else {
        g.drawPolyline(
            new int[] { i + 10, i + 10, i + 20, i + 20 },
            new int[] { y + height - 22, y + 10, y + 10, y + AbstractTtlGate.PIN_HEIGHT },
            4);
      }
    }

    // channel 2 enable
    if (ch2EnableInverted) {
      g.drawLine(x + 30, y + AbstractTtlGate.PIN_HEIGHT, x + 30, y + 12);
      g.drawOval(x + 28, y + 13, 4, 4);
    } else {
      g.drawLine(x + 30, y + AbstractTtlGate.PIN_HEIGHT, x + 30, y + 17);
    }

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
      if (i < x + 190) {
        g.fillOval(i - 16, y + 27, 2, 2);
      }
      g.drawPolyline(
          new int[] { i - 15, i - 15, i - 12 },
          new int[] { y + 28, y + 20, y + 20 },
          3);

      // output
      if (ch2OutputInverted) {
        g.drawOval(i - 11, y + 23, 2, 2);
        g.drawPolyline(
            new int[] { i - 10, i - 10, i - 20, i - 20 },
            new int[] { y + 25, y + height - 10, y + height - 10, y + height - AbstractTtlGate.PIN_HEIGHT },
            4);
      } else {
        g.drawPolyline(
            new int[] { i - 10, i - 10, i - 20, i - 20 },
            new int[] { y + 22, y + height - 10, y + height - 10, y + height - AbstractTtlGate.PIN_HEIGHT },
            4);
      }
    }
  }

}
