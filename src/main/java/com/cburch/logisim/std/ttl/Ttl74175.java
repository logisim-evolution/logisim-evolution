/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;

public class Ttl74175 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74175";

  public Ttl74175() {
    super(
        _ID,
        (byte) 16,
        new byte[] {2, 3, 6, 7, 10, 11, 14, 15},
        new String[] {
          "nCLR", "Q1", "nQ1", "D1", "D2", "nQ2", "Q2", "CLK", "Q3", "nQ3", "D3", "D4", "nQ4", "Q4"
        },
        new Ttl74175HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    drawFlops(g, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      // changed = true;
      data = new TtlRegisterData(BitWidth.create(4));
      state.setData(data);
    }
    var triggered = data.updateClock(state.getPortValue(7));
    if (state.getPortValue(0) == Value.TRUE) {
      data.setValue(Value.createKnown(data.getWidth(), 0));
    } else if (triggered) {
      Value[] vals = data.getValue().getAll();
      vals[0] = state.getPortValue(3);
      vals[1] = state.getPortValue(4);
      vals[2] = state.getPortValue(10);
      vals[3] = state.getPortValue(11);
      data.setValue(Value.create(vals));
    }
    state.setPort(1, data.getValue().get(0), 8);
    state.setPort(2, data.getValue().get(0).not(), 8);
    state.setPort(6, data.getValue().get(1), 8);
    state.setPort(5, data.getValue().get(1).not(), 8);
    state.setPort(8, data.getValue().get(2), 8);
    state.setPort(9, data.getValue().get(2).not(), 8);
    state.setPort(13, data.getValue().get(3), 8);
    state.setPort(12, data.getValue().get(3).not(), 8);
  }

  private void drawFlops(Graphics g, int x, int y, int height) {
    // Reset line
    g.drawLine(x + 10, y + height - 10, x + 10, y + height - AbstractTtlGate.PIN_HEIGHT);
    g.drawLine(x + 10, y + height - 10, x + 140, y + height - 10);
    g.drawLine(x + 140, y + height - 10, x + 140, y + 10);
    g.drawLine(x + 60, y + 10, x + 140, y + 10);

    // Clock line
    g.drawLine(x + 150, y + AbstractTtlGate.PIN_HEIGHT, x + 150, y + 30);
    g.drawLine(x + 80, y + 30, x + 150, y + 30);

    // dff1
    g.drawRect(x + 57, y + 33, 6, 12);

    g.drawOval(x + 59, y + 45, 2, 2);
    g.fillOval(x + 59, y + 49, 2, 2);
    g.drawLine(x + 60, y + 47, x + 60, y + 50);

    g.drawOval(x + 55, y + 40, 2, 2);
    g.drawLine(x + 50, y + height - AbstractTtlGate.PIN_HEIGHT, x + 50, y + 41);
    g.drawLine(x + 50, y + 41, x + 55, y + 41);

    g.drawLine(x + 30, y + height - AbstractTtlGate.PIN_HEIGHT, x + 30, y + 37);
    g.drawLine(x + 30, y + 37, x + 57, y + 37);

    g.drawLine(x + 70, y + height - AbstractTtlGate.PIN_HEIGHT, x + 70, y + 37);
    g.drawLine(x + 63, y + 37, x + 70, y + 37);

    g.drawLine(x + 61, y + 41, x + 63, y + 42);
    g.drawLine(x + 61, y + 41, x + 63, y + 40);
    g.drawString("D", x + 64, y + 36);
    g.drawString("Q", x + 52, y + 36);
    g.drawLine(x + 63, y + 41, x + 97, y + 41);
    g.drawLine(x + 80, y + 30, x + 80, y + 41);
    g.fillOval(x + 79, y + 29, 2, 2);
    g.fillOval(x + 79, y + 40, 2, 2);
    // dff2
    g.drawRect(x + 97, y + 33, 6, 12);

    g.drawOval(x + 99, y + 45, 2, 2);
    g.fillOval(x + 99, y + 49, 2, 2);
    g.drawLine(x + 100, y + 47, x + 100, y + 50);

    g.drawOval(x + 103, y + 40, 2, 2);
    g.drawLine(x + 110, y + height - AbstractTtlGate.PIN_HEIGHT, x + 110, y + 41);
    g.drawLine(x + 105, y + 41, x + 110, y + 41);

    g.drawLine(x + 130, y + height - AbstractTtlGate.PIN_HEIGHT, x + 130, y + 37);
    g.drawLine(x + 130, y + 37, x + 103, y + 37);

    g.drawLine(x + 90, y + height - AbstractTtlGate.PIN_HEIGHT, x + 90, y + 37);
    g.drawLine(x + 90, y + 37, x + 97, y + 37);

    g.drawLine(x + 97, y + 42, x + 99, y + 41);
    g.drawLine(x + 97, y + 40, x + 99, y + 41);

    g.drawString("D", x + 92, y + 36);
    g.drawString("Q", x + 104, y + 36);

    // dff3
    g.drawRect(x + 97, y + 15, 6, 12);

    g.drawOval(x + 99, y + 13, 2, 2);
    g.fillOval(x + 99, y + 9, 2, 2);
    g.drawLine(x + 100, y + 13, x + 100, y + 10);

    g.drawOval(x + 103, y + 18, 2, 2);
    g.drawLine(x + 110, y + AbstractTtlGate.PIN_HEIGHT, x + 110, y + 19);
    g.drawLine(x + 105, y + 19, x + 110, y + 19);

    g.drawLine(x + 130, y + AbstractTtlGate.PIN_HEIGHT, x + 130, y + 23);
    g.drawLine(x + 130, y + 23, x + 103, y + 23);

    g.drawLine(x + 90, y + AbstractTtlGate.PIN_HEIGHT, x + 90, y + 23);
    g.drawLine(x + 90, y + 23, x + 97, y + 23);

    g.drawLine(x + 97, y + 20, x + 99, y + 19);
    g.drawLine(x + 97, y + 18, x + 99, y + 19);

    g.drawString("D", x + 92, y + 29);
    g.drawString("Q", x + 104, y + 29);

    // dff4
    g.drawRect(x + 57, y + 15, 6, 12);

    g.drawOval(x + 59, y + 13, 2, 2);
    g.drawLine(x + 60, y + 13, x + 60, y + 10);

    g.drawOval(x + 55, y + 18, 2, 2);
    g.drawLine(x + 50, y + AbstractTtlGate.PIN_HEIGHT, x + 50, y + 19);
    g.drawLine(x + 50, y + 19, x + 55, y + 19);

    g.drawLine(x + 30, y + AbstractTtlGate.PIN_HEIGHT, x + 30, y + 23);
    g.drawLine(x + 30, y + 23, x + 57, y + 23);

    g.drawLine(x + 70, y + AbstractTtlGate.PIN_HEIGHT, x + 70, y + 23);
    g.drawLine(x + 63, y + 23, x + 70, y + 23);

    g.drawLine(x + 61, y + 19, x + 63, y + 20);
    g.drawLine(x + 61, y + 19, x + 63, y + 18);
    g.drawString("D", x + 64, y + 29);
    g.drawString("Q", x + 52, y + 29);
    g.drawLine(x + 63, y + 19, x + 97, y + 19);
    g.drawLine(x + 80, y + 19, x + 80, y + 40);
    g.fillOval(x + 79, y + 18, 2, 2);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {7};
  }
}
