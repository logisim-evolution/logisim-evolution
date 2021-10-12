/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7413 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7413";

  private boolean inverted = true;

  private static final byte pinCount = 14;
  private static final byte[] outPorts = {6, 8};
  private static final byte[] unusedPorts = {3, 11};
  private static final String[] portNames = {"A0", "B0", "C0", "D0", "Y0", "Y1", "D1", "C1", "B1", "A1"};

  public Ttl7413(String name, boolean inv) {
    super(name, pinCount, outPorts, unusedPorts, portNames, new Ttl7413HdlGenerator(inv));
    inverted = inv;
  }

  public Ttl7413(String name) {
    super(name, pinCount, outPorts, unusedPorts, portNames, new Ttl7413HdlGenerator(true));
  }

  public Ttl7413() {
    super(_ID, pinCount, outPorts, unusedPorts, portNames, new Ttl7413HdlGenerator(true));
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    Drawgates.paintAnd(g, x + 125, y + 20, 10, 10, inverted);
    Drawgates.paintAnd(g, x + 105, y + 40, 10, 10, inverted);
    final var offset = inverted ? 0 : -4;
    g.drawLine(x + 129 + offset, y + 20, x + 130, y + 20);
    g.drawLine(x + 130, y + AbstractTtlGate.PIN_HEIGHT, x + 130, y + 20);
    g.drawLine(x + 109 + offset, y + 40, x + 110, y + 40);
    g.drawLine(x + 110, y + height - AbstractTtlGate.PIN_HEIGHT, x + 110, y + 40);
    for (var i = 0; i < 5; i++) {
      if (i != 2) {
        g.drawLine(
            x + 10 + i * 20,
            y + height - AbstractTtlGate.PIN_HEIGHT,
            x + 10 + i * 20,
            y + 36 + i * 2);
        g.drawLine(x + 10 + i * 20, y + 36 + i * 2, x + 95, y + 36 + i * 2);
        g.drawLine(x + 30 + i * 20, y + AbstractTtlGate.PIN_HEIGHT, x + 30 + i * 20, y + 24 - i * 2);
        g.drawLine(x + 30 + i * 20, y + 24 - i * 2, x + 115, y + 24 - i * 2);
      }
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var val =
        state
            .getPortValue(0)
            .and(state.getPortValue(1).and(state.getPortValue(2).and(state.getPortValue(3))));
    state.setPort(4, inverted ? val.not() : val, 3);
    val =
        state
            .getPortValue(6)
            .and(state.getPortValue(7).and(state.getPortValue(8).and(state.getPortValue(9))));
    state.setPort(5, inverted ? val.not() : val, 4);
  }
}
