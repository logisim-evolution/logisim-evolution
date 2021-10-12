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

/**
 * TTL 74x10: triple 3-input NAND gate
 */
public class Ttl7410 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7410";

  private boolean inverted = true;
  private boolean isAND = true;

  private static final byte pinCount = 14;
  private static final byte[] outPorts = {6, 8, 12};

  public Ttl7410() {
    super(_ID, pinCount, outPorts, new Ttl7410HdlGenerator(true, true));
  }

  public Ttl7410(String val, boolean inverted) {
    super(val, (byte) pinCount, outPorts, new Ttl7410HdlGenerator(inverted, true));
    this.inverted = inverted;
  }

  public Ttl7410(String val, boolean inverted, boolean isOR) {
    super(val, (byte) pinCount, outPorts, new Ttl7410HdlGenerator(inverted, !isOR));
    this.inverted = inverted;
    isAND = !isOR;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    final var LineOffset =
        ((!isAND) & (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_SHAPED))) ? -4 : 0;
    if (isAND) {
      Drawgates.paintAnd(g, x + 45, y + 20, 10, 10, inverted);
      Drawgates.paintAnd(g, x + 125, y + 20, 10, 10, inverted);
      Drawgates.paintAnd(g, x + 105, y + 40, 10, 10, inverted);
    } else {
      Drawgates.paintOr(g, x + 45, y + 20, 10, 10, inverted, false);
      Drawgates.paintOr(g, x + 125, y + 20, 10, 10, inverted, false);
      Drawgates.paintOr(g, x + 105, y + 40, 10, 10, inverted, false);
    }
    final var offset = inverted ? 0 : -4;
    var xpos = new int[] {x + 49 + offset, x + 50, x + 50};
    var ypos = new int[] {y + 20, y + 20, y + AbstractTtlGate.PIN_HEIGHT};
    g.drawPolyline(xpos, ypos, 3);
    xpos[0] = x + 129 + offset;
    xpos[1] = xpos[2] = x + 130;
    g.drawPolyline(xpos, ypos, 3);
    xpos[0] = x + 109 + offset;
    xpos[1] = xpos[2] = x + 110;
    ypos[0] = ypos[1] = y + 40;
    ypos[2] = y + height - AbstractTtlGate.PIN_HEIGHT;
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 35 + LineOffset};
    ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 17, y + 17};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 10, x + 10, x + 35 + LineOffset};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 20, y + 20};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 35 + LineOffset};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);

    for (var i = 0; i < 3; i++) {
      xpos = new int[] {x + 70 + i * 20, x + 70 + i * 20, x + 115 + LineOffset};
      ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 23 - i * 3, y + 23 - i * 3};
      g.drawPolyline(xpos, ypos, 3);
      xpos = new int[] {x + 50 + i * 20, x + 50 + i * 20, x + 95 + LineOffset};
      ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 37 + i * 3, y + 37 + i * 3};
      g.drawPolyline(xpos, ypos, 3);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var val =
        (isAND)
            ? state.getPortValue(2).and(state.getPortValue(3).and(state.getPortValue(4)))
            : state.getPortValue(2).or(state.getPortValue(3).or(state.getPortValue(4)));
    state.setPort(5, inverted ? val.not() : val, 2);
    val =
        (isAND)
            ? state.getPortValue(0).and(state.getPortValue(1).and(state.getPortValue(11)))
            : state.getPortValue(0).or(state.getPortValue(1).or(state.getPortValue(11)));
    state.setPort(10, inverted ? val.not() : val, 2);
    val =
        (isAND)
            ? state.getPortValue(7).and(state.getPortValue(8).and(state.getPortValue(9)))
            : state.getPortValue(7).or(state.getPortValue(8).or(state.getPortValue(9)));
    state.setPort(6, inverted ? val.not() : val, 2);
  }
}
