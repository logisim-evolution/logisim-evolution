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

public class Ttl7430 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7430";

  public Ttl7430() {
    super(
        _ID,
        (byte) 14,
        new byte[] {8},
        new byte[] {9, 10, 13},
        new String[] {"A", "B", "C", "D", "E", "F", "Y", "G", "H"},
        new Ttl7430HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    Drawgates.paintAnd(g, x + 123, y + 30, 10, 18, true);
    g.drawLine(x + 70, y + AbstractTtlGate.PIN_HEIGHT, x + 70, y + 23);
    g.drawLine(x + 50, y + AbstractTtlGate.PIN_HEIGHT, x + 50, y + 25);
    g.drawLine(x + 10, y + height - AbstractTtlGate.PIN_HEIGHT, x + 10, y + 27);
    g.drawLine(x + 30, y + height - AbstractTtlGate.PIN_HEIGHT, x + 30, y + 29);
    g.drawLine(x + 50, y + height - AbstractTtlGate.PIN_HEIGHT, x + 50, y + 31);
    g.drawLine(x + 70, y + height - AbstractTtlGate.PIN_HEIGHT, x + 70, y + 33);
    g.drawLine(x + 90, y + height - AbstractTtlGate.PIN_HEIGHT, x + 90, y + 35);
    g.drawLine(x + 110, y + height - AbstractTtlGate.PIN_HEIGHT, x + 110, y + 37);
    g.drawLine(x + 70, y + 23, x + 113, y + 23);
    g.drawLine(x + 50, y + 25, x + 113, y + 25);
    g.drawLine(x + 10, y + 27, x + 113, y + 27);
    g.drawLine(x + 30, y + 29, x + 113, y + 29);
    g.drawLine(x + 50, y + 31, x + 113, y + 31);
    g.drawLine(x + 70, y + 33, x + 113, y + 33);
    g.drawLine(x + 90, y + 35, x + 113, y + 35);
    g.drawLine(x + 110, y + 37, x + 113, y + 37);
    g.drawLine(x + 128, y + 30, x + 130, y + 30);
    g.drawLine(x + 130, y + AbstractTtlGate.PIN_HEIGHT, x + 130, y + 30);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var val1 =
        state
            .getPortValue(0)
            .and(state.getPortValue(1).and(state.getPortValue(2).and(state.getPortValue(3))));
    Value val2 =
        val1.and(
            state
                .getPortValue(4)
                .and(state.getPortValue(5).and(state.getPortValue(7).and(state.getPortValue(8)))));
    state.setPort(6, val2.not(), 1);
  }
}
