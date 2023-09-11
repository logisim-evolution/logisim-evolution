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

/**
 * TTL 74x266: quad 2-input XNOR gate (open-collector)
 * Model based on https://www.ti.com/product/SN74LS266 datasheet.
 */
public class Ttl74266 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74266";

  public Ttl74266() {
    super(_ID, (byte) 14, new byte[] {3, 6, 8, 11}, true, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 18;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintXor(g, x + 44, youtput, portwidth - 4, portheight, true);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 48, youtput, up, height);
    // output type
    Drawgates.paintOpenCollector(g, x + 52, youtput);
    // input lines
    Drawgates.paintDoubleInputgate(
        g, x + 30, y, x + 44 - portwidth, youtput, portheight, up, false, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var i = 2; i < 6; i += 3) {
      state.setPort(i, (state.getPortValue(i - 1).xor(state.getPortValue(i - 2)).not() == Value.TRUE) ? Value.UNKNOWN : Value.FALSE, 1);
    }
    for (var i = 6; i < 12; i += 3) {
      state.setPort(i, (state.getPortValue(i + 1).xor(state.getPortValue(i + 2)).not() == Value.TRUE) ? Value.UNKNOWN : Value.FALSE, 1);
    }
  }
}
