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

public class Ttl74164 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74164";

  public static final int PORT_INDEX_A = 0;
  public static final int PORT_INDEX_B = 1;
  public static final int PORT_INDEX_QA = 2;
  public static final int PORT_INDEX_QB = 3;
  public static final int PORT_INDEX_QC = 4;
  public static final int PORT_INDEX_QD = 5;
  public static final int PORT_INDEX_CLK = 6;
  public static final int PORT_INDEX_CLR = 7;
  public static final int PORT_INDEX_QE = 8;
  public static final int PORT_INDEX_QF = 9;
  public static final int PORT_INDEX_QG = 10;
  public static final int PORT_INDEX_QH = 11;




  public Ttl74164() {
    super(
        _ID,
        (byte) 14,
        new byte[] {3, 4, 5, 6, 10, 11, 12, 13},
        new String[] {
          "A",
          "B",
          "QA",
          "QB",
          "QC",
          "QD",
          "Clock",
          "Clear",
          "QE",
          "QF",
          "QG",
          "QH"
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
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) return;
      if (isPressed && isInside(state, e)) {
        final var index = getIndex(state, e);
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
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNames(
        painter,
        x,
        y,
        height,
        new String[] {
          "A", "B", "QA", "QB", "QC", "QD", "CLK", "CLR", "QE", "QF", "QG", "QH"
        });
    ShiftRegisterData data = getData(painter);
    drawState(gfx, x, y, height, data);
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, ShiftRegisterData state) {
    if (state == null) return;
    for (var i = 0; i < 8; i++) {
      gfx.setColor(state.get(7 - i).getColor());
      gfx.fillOval(x + 36 + i * 10, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, state.get(7 - i).toDisplayString(), x + 40 + i * 10, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    if (state.getPortValue(PORT_INDEX_CLR) == Value.FALSE) { // Clear
      data.clear();
    } else if (triggered) {
      data.clear();

      data.pushDown(state.getPortValue(PORT_INDEX_A) == Value.TRUE
                && state.getPortValue(PORT_INDEX_B) == Value.TRUE
                ? Value.TRUE : Value.FALSE);

      data.pushDown(state.getPortValue(PORT_INDEX_QA));
      data.pushDown(state.getPortValue(PORT_INDEX_QB));
      data.pushDown(state.getPortValue(PORT_INDEX_QC));
      data.pushDown(state.getPortValue(PORT_INDEX_QD));
      data.pushDown(state.getPortValue(PORT_INDEX_QE));
      data.pushDown(state.getPortValue(PORT_INDEX_QF));
      data.pushDown(state.getPortValue(PORT_INDEX_QG));
    }
    state.setPort(PORT_INDEX_QA, data.get(0), 4);
    state.setPort(PORT_INDEX_QB, data.get(1), 4);
    state.setPort(PORT_INDEX_QC, data.get(2), 4);
    state.setPort(PORT_INDEX_QD, data.get(3), 4);
    state.setPort(PORT_INDEX_QE, data.get(4), 4);
    state.setPort(PORT_INDEX_QF, data.get(5), 4);
    state.setPort(PORT_INDEX_QG, data.get(6), 4);
    state.setPort(PORT_INDEX_QH, data.get(7), 4);
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
