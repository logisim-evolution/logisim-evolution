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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

public class Ttl7476 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT
   * change as it will
   * prevent project files from loading.
   */
  public static final String _ID = "7476";

  public Ttl7476() {
    super(
        _ID,
        (byte) 16,
        new byte[] { 10, 11, 13, 14 }, // Output physical UI pins mapping to nQ2, Q2, nQ1, Q1
        new String[] {
            "CLK1", "nPRE1", "nCLR1", "J1", /* Logisim mapped VCC is skipped in array physically */
            "CLK2", "nPRE2", "nCLR2", /* Logisim mapped GND is skipped in array physically */
            "J2", "nQ2", "Q2", "K2", "nQ1", "Q1", "K1"
        },
        new Ttl7476HdlGenerator());
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      var dx = p.x - 37;
      var dy = p.y - 35;
      var d2 = dx * dx + dy * dy;
      dx = p.x - 107;
      dy = p.y - 32;
      final var d3 = dx * dx + dy * dy;
      return ((d2 < 5 * 5) || (d3 < 5 * 5));
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      final var dx = p.x - 37;
      final var dy = p.y - 35;
      final var d2 = dx * dx + dy * dy;
      return (d2 < 5 * 5) ? 0 : 1;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE))
        return;
      if (isPressed && isInside(state, e)) {
        final var index = getIndex(state, e);
        final var myState = (TtlRegisterData) state.getData();
        if (myState == null)
          return;
        final var values = myState.getValue().getAll();
        if (values[index].isFullyDefined())
          values[index] = values[index].not();
        else
          values[index] = Value.createKnown(1, 0);
        myState.setValue(Value.create(values));
        state.fireInvalidated();
      }
      isPressed = false;
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var state = (TtlRegisterData) painter.getData();
    super.paintBase(painter, false, false);

    // Abstract logic draws 2 flip-flop rects
    drawflop(g, x, y + 1);
    drawflop(g, x + 70, y - 2);

    drawState(g, x, y + 1, 0, state);
    drawState(g, x + 70, y - 2, 1, state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(2));
      state.setData(data);
    }

    // Ensure negative edge-triggering for standard 7476
    final var triggered1 = data.updateClock(state.getPortValue(0), 0, StdAttr.TRIG_FALLING);
    final var triggered2 = data.updateClock(state.getPortValue(4), 1, StdAttr.TRIG_FALLING);
    final var values = data.getValue().getAll();

    // Map labels index references to Flip-Flop 1 Logisim Arrays
    // 0:CLK1, 1:nPRE1, 2:nCLR1, 3:J1, 13:K1
    Value pre1 = state.getPortValue(1);
    Value clr1 = state.getPortValue(2);
    Value j1 = state.getPortValue(3);
    Value k1 = state.getPortValue(13);

    // Map labels index references to Flip-Flop 2 Logisim Arrays
    // 4:CLK2, 5:nPRE2, 6:nCLR2, 7:J2, 10:K2
    Value pre2 = state.getPortValue(5);
    Value clr2 = state.getPortValue(6);
    Value j2 = state.getPortValue(7);
    Value k2 = state.getPortValue(10);

    // Evaluate FF1 state
    if (pre1 == Value.FALSE && clr1 == Value.FALSE) {
      values[0] = Value.createUnknown(BitWidth.create(1)); // Race contention
    } else if (pre1 == Value.FALSE) {
      values[0] = Value.createKnown(BitWidth.create(1), 1);
    } else if (clr1 == Value.FALSE) {
      values[0] = Value.createKnown(BitWidth.create(1), 0);
    } else if (triggered1) {
      if (j1 == Value.TRUE && k1 == Value.TRUE) {
        values[0] = values[0].not();
      } else if (j1 == Value.TRUE && k1 == Value.FALSE) {
        values[0] = Value.createKnown(BitWidth.create(1), 1);
      } else if (j1 == Value.FALSE && k1 == Value.TRUE) {
        values[0] = Value.createKnown(BitWidth.create(1), 0);
      }
    }

    // Evaluate FF2 state
    if (pre2 == Value.FALSE && clr2 == Value.FALSE) {
      values[1] = Value.createUnknown(BitWidth.create(1)); // Race contention
    } else if (pre2 == Value.FALSE) {
      values[1] = Value.createKnown(BitWidth.create(1), 1);
    } else if (clr2 == Value.FALSE) {
      values[1] = Value.createKnown(BitWidth.create(1), 0);
    } else if (triggered2) {
      if (j2 == Value.TRUE && k2 == Value.TRUE) {
        values[1] = values[1].not();
      } else if (j2 == Value.TRUE && k2 == Value.FALSE) {
        values[1] = Value.createKnown(BitWidth.create(1), 1);
      } else if (j2 == Value.FALSE && k2 == Value.TRUE) {
        values[1] = Value.createKnown(BitWidth.create(1), 0);
      }
    }

    data.setValue(Value.create(values));

    // Update Output Pins using Port Indexes matching String array
    // 8:"nQ2", 9:"Q2", 11:"nQ1", 12:"Q1"
    state.setPort(8, data.getValue().get(1).not(), 8);
    state.setPort(9, data.getValue().get(1), 8);
    state.setPort(11, data.getValue().get(0).not(), 8);
    state.setPort(12, data.getValue().get(0), 8);
  }

  private void drawState(Graphics g, int x, int y, int ID, TtlRegisterData state) {
    if (state == null)
      return;
    g.setColor(state.getValue().get(ID).getColor());
    g.fillOval(x + 33, y + 30, 8, 8);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, state.getValue().get(ID).toDisplayString(), x + 36, y + 33);
    g.setColor(Color.BLACK);
  }

  private void drawflop(Graphics g, int x, int y) {
    g.drawRect(x + 27, y + 20, 16, 20);
    g.drawOval(x + 33, y + 16, 4, 4);
    g.drawOval(x + 33, y + 40, 4, 4);
    g.drawOval(x + 43, y + 33, 4, 4);
  }

  @Override
  public boolean checkForGatedClocks(com.cburch.logisim.fpga.designrulecheck.netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(com.cburch.logisim.fpga.designrulecheck.netlistComponent comp) {
    return new int[] { 0, 4 };
  }
}