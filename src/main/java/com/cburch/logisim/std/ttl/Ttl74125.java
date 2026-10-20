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

public class Ttl74125 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74125";

  public Ttl74125() {
    super(_ID, (byte) 14, new byte[] {3, 6, 8, 11}, true, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 15;
    final var portheight = 8;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintBuffer(g, x + 50, youtput, portwidth, portheight);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 45, youtput, up, height);
    // input line
    Drawgates.paintSingleInputgate(g, x + 30, y, x + 35, youtput, up, height);
    // enable line
    if (!up) {
      Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, youtput - 7, up, height);
      g.drawLine(x + 41, youtput - 5, x + 41, youtput - 7);
      g.drawOval(x + 40, youtput - 5, 3, 3);
    } else {
      Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, youtput + 7, up, height);
      g.drawLine(x + 41, youtput + 5, x + 41, youtput + 7);
      g.drawOval(x + 40, youtput + 2, 3, 3);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var i = 2; i < 6; i += 3) {

      if (state.getPortValue(i - 2) == Value.TRUE) state.setPort(i, Value.UNKNOWN, 1);
      else state.setPort(i, state.getPortValue(i - 1), 1);
    }
    for (var i = 6; i < 11; i += 3) {
      if (state.getPortValue(i + 2) == Value.TRUE) state.setPort(i, Value.UNKNOWN, 1);
      else state.setPort(i, state.getPortValue(i + 1), 1);
    }
  }
}
