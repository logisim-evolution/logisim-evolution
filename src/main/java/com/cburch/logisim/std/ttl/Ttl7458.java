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

public class Ttl7458 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7458";

  public Ttl7458() {
    super(
        _ID,
        (byte) 14,
        new byte[] {6, 8},
        new String[] {"A0", "A1", "B1", "C1", "D1", "Y1", "Y0", "D0", "E0", "F0", "B0", "C0"},
        new Ttl7458HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var gfx = painter.getGraphics();
    Drawgates.paintOr(gfx, x + 107, y + 39, 10, 10, false, false);
    Drawgates.paintAnd(gfx, x + 86, y + 34, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 86, y + 44, 10, 10, false);
    final var OrOffset =
        (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) ? 4 : 0;
    var posX = new int[] {x + 86, x + 90, x + 90, x + 93 + OrOffset};
    var posY = new int[] {y + 34, y + 34, y + 36, y + 36};
    gfx.drawPolyline(posX, posY, 4);
    posY = new int[] {y + 44, y + 44, y + 42, y + 42};
    gfx.drawPolyline(posX, posY, 4);
    posX = new int[] {x + 107, x + 110, x + 110};
    posY = new int[] {y + 39, y + 39, y + height - AbstractTtlGate.PIN_HEIGHT};
    gfx.drawPolyline(posX, posY, 3);
    for (var i = 0; i < 3; i++) {
      gfx.drawLine(
          x + 30 + i * 20,
          y + 32 + i * 5,
          x + 30 + i * 20,
          y + height - AbstractTtlGate.PIN_HEIGHT);
      gfx.drawLine(x + 30 + i * 20, y + 32 + i * 5, x + 76, y + 32 + i * 5);
    }
    posX = new int[] {x + 76, x + 73, x + 73, x + 90, x + 90};
    posY = new int[] {y + 47, y + 47, y + 51, y + 51, y + height - AbstractTtlGate.PIN_HEIGHT};
    gfx.drawPolyline(posX, posY, 5);

    Drawgates.paintOr(gfx, x + 127, y + 21, 10, 10, false, false);
    Drawgates.paintAnd(gfx, x + 106, y + 16, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 106, y + 26, 10, 10, false);
    posX = new int[] {x + 106, x + 110, x + 110, x + 113 + OrOffset};
    posY = new int[] {y + 16, y + 16, y + 18, y + 18};
    gfx.drawPolyline(posX, posY, 4);
    posY = new int[] {y + 26, y + 26, y + 24, y + 24};
    gfx.drawPolyline(posX, posY, 4);
    posX = new int[] {x + 127, x + 130, x + 130};
    posY = new int[] {y + 21, y + 21, y + AbstractTtlGate.PIN_HEIGHT};
    gfx.drawPolyline(posX, posY, 3);
    for (var i = 0; i < 5; i++) {
      posX = new int[] {x + 10 + i * 20, x + 10 + i * 20, x + 95};
      posY =
          new int[] {
            i == 0 ? y + height - AbstractTtlGate.PIN_HEIGHT : y + AbstractTtlGate.PIN_HEIGHT,
            y + 28 - i * 3,
            y + 28 - i * 3
          };
      gfx.drawPolyline(posX, posY, 3);
    }
    posX = new int[] {x + 96, x + 93, x + 93, x + 110, x + 110};
    posY = new int[] {y + 13, y + 13, y + 9, y + 9, y + AbstractTtlGate.PIN_HEIGHT};
    gfx.drawPolyline(posX, posY, 5);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var val1 = state.getPortValue(1).and(state.getPortValue(2));
    var val2 = state.getPortValue(3).and(state.getPortValue(4));
    state.setPort(5, val1.or(val2), 5);
    val1 = state.getPortValue(0).and(state.getPortValue(11).and(state.getPortValue(10)));
    val2 = state.getPortValue(9).and(state.getPortValue(8).and(state.getPortValue(7)));
    state.setPort(6, val1.or(val2), 5);
  }
}
