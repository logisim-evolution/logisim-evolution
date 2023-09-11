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

public class Ttl7464 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7464";

  public Ttl7464() {
    super(
        _ID,
        (byte) 14,
        new byte[] {8},
        new String[] {"A", "E", "F", "G", "H", "I", "Y", "J", "K", "B", "C", "D"},
        70,
        new Ttl7464HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var isIEC = AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR);
    final var AndOffset = isIEC ? 10 : 0;
    final var gfx = painter.getGraphics();
    Drawgates.paintOr(gfx, x + 125, y + 35, 10, isIEC ? 40 : 10, true, false);
    Drawgates.paintAnd(gfx, x + 105 + AndOffset, y + 20, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 105 + AndOffset, y + 30, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 105 + AndOffset, y + 40, 10, 10, false);
    Drawgates.paintAnd(gfx, x + 105 + AndOffset, y + 50, 10, 10, false);
    gfx.drawLine(x + 129, y + 35, x + 130, y + 35);
    gfx.drawLine(x + 130, y + 35, x + 130, y + AbstractTtlGate.PIN_HEIGHT);
    int[] posX, posY;
    for (var i = 0; i < 4; i++) {
      if (!isIEC) {
        int tmpOff = (i == 0) || (i == 3) ? 2 : 0;
        posX = new int[] {x + 105, x + 107 + tmpOff, x + 107 + tmpOff, x + 111};
        posY = new int[] {y + 20 + i * 10, y + 20 + i * 10, y + 32 + i * 2, y + 32 + i * 2};
        gfx.drawPolyline(posX, posY, 4);
      }
      posX = new int[] {x + 10 + i * 20, x + 10 + i * 20, x + 95 + AndOffset};
      posY =
          new int[] {
            i == 0 ? y + height - AbstractTtlGate.PIN_HEIGHT : y + AbstractTtlGate.PIN_HEIGHT,
            y + 33 - i * 2,
            y + 33 - i * 2
          };
      gfx.drawPolyline(posX, posY, 3);
      if (i < 2) {
        posX = new int[] {x + 30 + i * 20, x + 30 + i * 20, x + 95 + AndOffset};
        posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 38 + i * 5, y + 38 + i * 5};
        gfx.drawPolyline(posX, posY, 3);
        posX = new int[] {x + 70 + i * 20, x + 70 + i * 20, x + 95 + AndOffset};
        posY = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 47 + i * 3, y + 47 + i * 3};
        gfx.drawPolyline(posX, posY, 3);
      }
    }
    posX = new int[] {x + 90, x + 90, x + 95 + AndOffset};
    posY = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 23, y + 23};
    gfx.drawPolyline(posX, posY, 3);
    posX = new int[] {x + 110, x + 110, x + 93 + AndOffset, x + 93 + AndOffset, x + 95 + AndOffset};
    posY = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 12, y + 12, y + 18, y + 18};
    gfx.drawPolyline(posX, posY, 5);
    posY =
        new int[] {
          y + height - AbstractTtlGate.PIN_HEIGHT, y + height - 12, y + height - 12, y + 53, y + 53
        };
    gfx.drawPolyline(posX, posY, 5);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var val1 = state.getPortValue(1).and(state.getPortValue(2));
    final var val2 = state.getPortValue(3).and(state.getPortValue(4).and(state.getPortValue(5)));
    final var val3 = state.getPortValue(7).and(state.getPortValue(8));
    final var val4 =
        state
            .getPortValue(9)
            .and(state.getPortValue(10).and(state.getPortValue(11).and(state.getPortValue(0))));
    final var val5 = val1.or(val2.or(val3.or(val4)));
    state.setPort(6, val5.not(), 7);
  }
}
