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
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

public class Ttl74165 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74165";

  public Ttl74165() {
    super(
        _ID,
        (byte) 16,
        new byte[] {7, 9},
        new String[] {
          "Shift/Load",
          "Clock",
          "P4",
          "P5",
          "P6",
          "P7",
          "Q7n",
          "Q7",
          "Serial Input",
          "P0",
          "P1",
          "P2",
          "P3",
          "Clock Inhibit"
        },
        new Ttl74165HdlGenerator());
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      var inside = false;
      for (var i = 0; i < 8; i++) {
        final var dx = p.x - (40 + i * 10);
        final var dy = p.y - 30;
        final var d2 = dx * dx + dy * dy;
        inside |= (d2 < 4 * 4);
      }
      return inside;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      for (var i = 0; i < 8; i++) {
        final var dx = p.x - (40 + i * 10);
        final var dy = p.y - 30;
        final var d2 = dx * dx + dy * dy;
        if (d2 < 4 * 4) return 7 - i;
      }
      return 0;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) return;
      if (isPressed && isInside(state, e)) {
        final var index = getIndex(state, e);
        System.out.println(index);
        final var myState = (ShiftRegisterData) state.getData();
        if (myState == null) return;
        if (myState.get(index).isFullyDefined())
          myState.set(index, myState.get(index).not());
        else
          myState.set(index, Value.createKnown(1, 0));
        state.fireInvalidated();
      }
      isPressed = false;
    }
  }

  private ShiftRegisterData getData(InstanceState state) {
    var data = (ShiftRegisterData) state.getData();
    if (data == null) {
      data = new ShiftRegisterData(BitWidth.ONE, 8);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNames(
        painter,
        x,
        y,
        height,
        new String[] {
          "ShLd", "CK", "P4", "P5", "P6", "P7", "Q7n", "Q7", "SER", "P0", "P1", "P2", "P3", "CkIh"
        });
    ShiftRegisterData data = getData(painter);
    drawState(g, x, y, height, data);
  }

  private void drawState(Graphics2D g, int x, int y, int height, ShiftRegisterData state) {
    if (state != null) {
      for (var i = 0; i < 8; i++) {
        g.setColor(state.get(7 - i).getColor());
        g.fillOval(x + 36 + i * 10, y + height / 2 - 4, 8, 8);
        g.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(g, state.get(7 - i).toDisplayString(), x + 40 + i * 10, y + height / 2);
      }
      g.setColor(Color.BLACK);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var triggered = data.updateClock(state.getPortValue(1), StdAttr.TRIG_RISING);
    if (triggered && state.getPortValue(13) != Value.TRUE) {
      if (state.getPortValue(0) == Value.FALSE) { // load
        data.clear();
        data.push(state.getPortValue(9));
        data.push(state.getPortValue(10));
        data.push(state.getPortValue(11));
        data.push(state.getPortValue(12));
        data.push(state.getPortValue(2));
        data.push(state.getPortValue(3));
        data.push(state.getPortValue(4));
        data.push(state.getPortValue(5));
      } else if (state.getPortValue(0) == Value.TRUE) { // shift
        data.push(state.getPortValue(8));
      }
    }
    state.setPort(6, data.get(0).not(), 4);
    state.setPort(7, data.get(0), 4);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {1};
  }
}
