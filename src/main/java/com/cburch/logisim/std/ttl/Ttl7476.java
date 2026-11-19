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
import java.awt.Font;
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
        new byte[] { 10, 11, 14, 15 }, // nQ2, Q2, nQ1, Q1
        null,
        new String[] {
            "CLK1", "nPRE1", "nCLR1", "J1",
            "CLK2", "nPRE2", "nCLR2",
            "J2", "nQ2", "Q2", "K2", "nQ1", "Q1", "K1"
        },
        (byte) 5, // VCC
        (byte) 13, // GND
        new Ttl7476HdlGenerator());
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      return near(p.x, p.y, 40, 30) || near(p.x, p.y, 125, 30);
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      return near(p.x, p.y, 40, 30) ? 0 : 1;
    }

    private boolean near(int x, int y, int cx, int cy) {
      final var dx = x - cx;
      final var dy = y - cy;
      return dx * dx + dy * dy < 5 * 5;
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
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    drawFirstFlipFlop(g, x, y, top, bottom);
    drawSecondFlipFlop(g, x, y, top, bottom);
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

  /**
   * First flip-flop, laid on its long side. Clock, preset, clear and J meet the bottom edge. K, Q
   * and its complement meet the top edge. Preset, clear and nQ keep the bubble; the clock
   * does not, because the master is transparent while CLK is high.
   */
  private static void drawFirstFlipFlop(Graphics g, int x, int y, int top, int bottom) {
    final var left = x + 18;
    final var blockTop = y + 22;
    final var blockBottom = y + 38;
    g.drawRect(left, blockTop, 44, 16);
    label(g, x + 40, y + 30);
    clockWedgeBottom(g, x + 25, blockBottom);
    bubble(g, x + 35, blockBottom + 1);
    bubble(g, x + 45, blockBottom + 1);
    bubble(g, x + 50, blockTop - 1);

    g.drawLine(x + 10, bottom, x + 10, y + 44);
    g.drawLine(x + 10, y + 44, x + 25, y + 44);
    g.drawLine(x + 25, y + 44, x + 25, blockBottom);

    g.drawLine(x + 10, top, x + 10, y + 16);
    g.drawLine(x + 10, y + 16, x + 30, y + 16);
    g.drawLine(x + 30, y + 16, x + 30, blockTop);

    g.drawLine(x + 30, bottom, x + 30, y + 44);
    g.drawLine(x + 30, y + 44, x + 35, y + 44);
    g.drawLine(x + 35, y + 44, x + 35, y + 40);

    g.drawLine(x + 50, bottom, x + 50, y + 44);
    g.drawLine(x + 50, y + 44, x + 45, y + 44);
    g.drawLine(x + 45, y + 44, x + 45, y + 40);

    g.drawLine(x + 70, bottom, x + 70, y + 44);
    g.drawLine(x + 70, y + 44, x + 55, y + 44);
    g.drawLine(x + 55, y + 44, x + 55, blockBottom);

    g.drawLine(x + 30, top, x + 30, y + 12);
    g.drawLine(x + 30, y + 12, x + 40, y + 12);
    g.drawLine(x + 40, y + 12, x + 40, blockTop);

    g.drawLine(x + 50, top, x + 50, y + 20);
  }

  /**
   * Second flip-flop, same size and the same contact offsets. J still meets the bottom edge.
   * Its pin is the top of column 150, so the wire stops above clear and turns left.
   */
  private static void drawSecondFlipFlop(Graphics g, int x, int y, int top, int bottom) {
    final var left = x + 103;
    final var blockTop = y + 22;
    final var blockBottom = y + 38;
    g.drawRect(left, blockTop, 44, 16);
    label(g, x + 125, y + 30);
    clockWedgeBottom(g, x + 110, blockBottom);
    bubble(g, x + 120, blockBottom + 1);
    bubble(g, x + 130, blockBottom + 1);
    bubble(g, x + 135, blockTop - 1);

    g.drawLine(x + 110, bottom, x + 110, blockBottom);

    g.drawLine(x + 90, top, x + 90, y + 16);
    g.drawLine(x + 90, y + 16, x + 115, y + 16);
    g.drawLine(x + 115, y + 16, x + 115, blockTop);

    g.drawLine(x + 130, bottom, x + 130, y + 48);
    g.drawLine(x + 130, y + 48, x + 120, y + 48);
    g.drawLine(x + 120, y + 48, x + 120, y + 40);

    g.drawLine(x + 150, bottom, x + 150, y + 44);
    g.drawLine(x + 150, y + 44, x + 130, y + 44);
    g.drawLine(x + 130, y + 44, x + 130, y + 40);

    g.drawLine(x + 150, top, x + 150, y + 40);
    g.drawLine(x + 150, y + 40, x + 140, y + 40);
    g.drawLine(x + 140, y + 40, x + 140, blockBottom);

    g.drawLine(x + 110, top, x + 110, y + 12);
    g.drawLine(x + 110, y + 12, x + 125, y + 12);
    g.drawLine(x + 125, y + 12, x + 125, blockTop);

    g.drawLine(x + 130, top, x + 130, y + 16);
    g.drawLine(x + 130, y + 16, x + 135, y + 16);
    g.drawLine(x + 135, y + 16, x + 135, y + 20);
  }

  /** Block name centered on the text bounds, clear of the clock wedge. */
  private static void label(Graphics g, int cx, int cy) {
    g.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 7));
    GraphicsUtil.drawText(g, "JK", cx, cy, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  /** Falling-edge mark: base on the bottom edge, tip four pixels inside. */
  private static void clockWedgeBottom(Graphics g, int axisX, int edgeY) {
    final var tipY = edgeY - 4;
    g.drawLine(axisX - 3, edgeY, axisX, tipY);
    g.drawLine(axisX + 3, edgeY, axisX, tipY);
  }

  /** Inversion bubble, 2×2, centered on the wire axis and touching the symbol face. */
  private static void bubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
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