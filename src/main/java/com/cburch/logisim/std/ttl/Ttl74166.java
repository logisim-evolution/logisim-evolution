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

/**
 * TTL 74x166 8-bit parallel-to-serial shift register with asynchronous clear
 * Model based on https://www.ti.com/product/SN74LS166A datasheet.
 */
public class Ttl74166 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74166";

  public Ttl74166() {
    super(
        _ID,
        (byte) 16,
        new byte[] {13},
        new String[] {
          "Serial Input",
          "P0",
          "P1",
          "P2",
          "P3",
          "Clock Inhibit",
          "Clock",
          "Clear",
          "P4",
          "P5",
          "P6",
          "Q7",
          "P7",
          "Shift/Load"
        },
        null);
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
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) {
        return;
      }
      if (isPressed && isInside(state, e)) {
        final var index = getIndex(state, e);
        final var myState = (ShiftRegisterData) state.getData();
        if (myState == null) {
          return;
        }
        if (myState.get(index).isFullyDefined()) {
          myState.set(index, myState.get(index).not());
        } else {
          myState.set(index, Value.createKnown(1, 0));
        }
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
          "SER", "P0", "P1", "P2", "P3", "CkIh", "CK", "nCLR", "P4", "P5", "P6", "Q7", "P7", "ShLd"
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
    final var triggered = data.updateClock(state.getPortValue(6), StdAttr.TRIG_RISING);
    if (state.getPortValue(7) == Value.FALSE) { // clear
      data.clear();
    } else if (triggered && state.getPortValue(5) != Value.TRUE) {
      if (state.getPortValue(13) == Value.FALSE) { // load
        data.clear();
        data.pushDown(state.getPortValue(12));
        data.pushDown(state.getPortValue(10));
        data.pushDown(state.getPortValue(9));
        data.pushDown(state.getPortValue(8));
        data.pushDown(state.getPortValue(4));
        data.pushDown(state.getPortValue(3));
        data.pushDown(state.getPortValue(2));
        data.pushDown(state.getPortValue(1));
      } else if (state.getPortValue(13) == Value.TRUE) { // shift
        data.pushDown(state.getPortValue(0));
      }
    }
    state.setPort(11, data.get(0), 4);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {6};
  }
}
