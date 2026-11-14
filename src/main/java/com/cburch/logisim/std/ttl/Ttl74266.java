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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;

/**
 * TTL 74x266: quad 2-input XNOR gate (open-collector).
 *
 * <p>Pinout follows the SN74HC266 functional diagram: outputs are 1Y, 2Y, 3Y and 4Y on pins 3, 4,
 * 10 and 11.
 */
public class Ttl74266 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74266";

  private static final String[] PORT_NAMES = {
    "1A", "1B", "1Y", "2Y", "2A", "2B", "3A", "3B", "3Y", "4Y", "4A", "4B"
  };

  public Ttl74266() {
    super(_ID, (byte) 14, new byte[] {3, 4, 10, 11}, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    drawXnor(g, x + 50, x + 30, x + 10, y, height, false, true);
    drawXnor(g, x + 70, x + 90, x + 110, y, height, false, false);
    drawXnor(g, x + 70, x + 50, x + 30, y, height, true, true);
    drawXnor(g, x + 90, x + 110, x + 130, y, height, true, false);
  }

  /**
   * One XNOR. {@code nearX} is the input pin closer to {@code outX}. The output faces that pin;
   * both inputs use the opposite face. The open-collector mark sits beside the output pin.
   */
  private static void drawXnor(
      Graphics g,
      int outX,
      int nearX,
      int farX,
      int y,
      int height,
      boolean up,
      boolean outputRight) {
    final var cy = y + (up ? 21 : height - 21);
    final var pinY = y + (up ? PIN_HEIGHT : height - PIN_HEIGHT);
    final var gateWidth = 18;
    final var gateHeight = 16;
    final int outputEdge = outputRight ? outX - 6 : outX + 10;
    final int inputEdge = outputRight ? outputEdge - gateWidth : outputEdge + gateWidth;
    final int jogX = outputRight ? inputEdge - 4 : inputEdge + 4;
    final int farY = up ? cy + 4 : cy - 4;
    final int nearY = up ? cy - 4 : cy + 4;
    final int jogY = up ? pinY + 3 : pinY - 3;
    final var rectangular =
        AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR);
    if (outputRight) {
      Drawgates.paintXor(g, outputEdge, cy, gateWidth, gateHeight, false);
    } else if (rectangular) {
      g.drawRect(outputEdge, cy - gateHeight / 2, gateWidth, gateHeight);
      GraphicsUtil.drawCenteredText(g, "=1", outputEdge + gateWidth / 2, cy);
    } else {
      Drawgates.paintOr(g, outputEdge, cy, gateWidth, gateHeight, false, true);
      GraphicsUtil.drawCenteredArc(g, outputEdge + 32, cy, 15, 153, 54);
    }
    if (outputRight) {
      g.drawOval(outputEdge, cy - 1, 2, 2);
      g.drawLine(outputEdge + 2, cy, outX, cy);
    } else {
      g.drawOval(outputEdge - 2, cy - 1, 2, 2);
      g.drawLine(outputEdge - 2, cy, outX, cy);
    }
    g.drawLine(outX, cy, outX, pinY);
    Drawgates.paintOpenCollector(g, outX + 2, up ? cy - 7 : cy + 7);
    g.drawLine(farX, pinY, farX, farY);
    g.drawLine(farX, farY, inputEdge, farY);
    g.drawLine(nearX, pinY, nearX, jogY);
    g.drawLine(nearX, jogY, jogX, jogY);
    g.drawLine(jogX, jogY, jogX, nearY);
    g.drawLine(jogX, nearY, inputEdge, nearY);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(2, openCollectorXnor(state.getPortValue(0), state.getPortValue(1)), 1);
    state.setPort(3, openCollectorXnor(state.getPortValue(4), state.getPortValue(5)), 1);
    state.setPort(8, openCollectorXnor(state.getPortValue(6), state.getPortValue(7)), 1);
    state.setPort(9, openCollectorXnor(state.getPortValue(10), state.getPortValue(11)), 1);
  }

  /** Open-collector XNOR: drives low when the inputs differ and releases otherwise. */
  private static Value openCollectorXnor(Value a, Value b) {
    return a.xor(b).not() == Value.TRUE ? Value.UNKNOWN : Value.FALSE;
  }
}
