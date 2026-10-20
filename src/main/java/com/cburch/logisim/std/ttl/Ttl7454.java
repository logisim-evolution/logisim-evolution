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
import com.cburch.logisim.prefs.AppPreferences;

public class Ttl7454 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7454";

  public Ttl7454() {
    super(
        _ID,
        (byte) 14,
        new byte[] {8},
        new byte[] {6, 11, 12},
        new String[] {"A", "C", "D", "E", "F", "Y", "G", "H", "B"},
        new Ttl7454HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    Drawgates.paintOr(g, x + 125, y + 30, 10, 10, true, false);
    Drawgates.paintAnd(g, x + 105, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 105, y + 40, 10, 10, false);
    Drawgates.paintAnd(g, x + 65, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 65, y + 40, 10, 10, false);
    // TODO Auto-generated method stub
    final var offset =
        (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) ? 4 : 0;
    var xpos = new int[] {x + 105, x + 108, x + 108, x + 111 + offset};
    var ypos = new int[] {y + 20, y + 20, y + 27, y + 27};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 65, x + 68, x + 68, x + 111 + offset};
    ypos = new int[] {y + 20, y + 20, y + 29, y + 29};
    g.drawPolyline(xpos, ypos, 4);
    ypos = new int[] {y + 40, y + 40, y + 31, y + 31};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 105, x + 108, x + 108, x + 111 + offset};
    ypos = new int[] {y + 40, y + 40, y + 33, y + 33};
    g.drawPolyline(xpos, ypos, 4);
    xpos = new int[] {x + 129, x + 130, x + 130};
    ypos = new int[] {y + 30, y + 30, y + AbstractTtlGate.PIN_HEIGHT};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 55};
    ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 17, y + 17};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 10, x + 10, x + 55};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 55};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 37, y + 37};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 50, x + 50, x + 55};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 43, y + 43};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 70, x + 70, x + 95};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 37, y + 37};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 90, x + 90, x + 95};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 43, y + 43};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 90, x + 90, x + 95};
    ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 110, x + 110, x + 93, x + 93, x + 95};
    ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 10, y + 10, y + 17, y + 17};
    g.drawPolyline(xpos, ypos, 5);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var val1 = state.getPortValue(0).and(state.getPortValue(8));
    final var val2 = state.getPortValue(1).and(state.getPortValue(2));
    final var val3 = state.getPortValue(3).and(state.getPortValue(4));
    final var val4 = state.getPortValue(6).and(state.getPortValue(7));
    state.setPort(5, val1.or(val2.or(val3.or(val4))).not(), 3);
  }
}
