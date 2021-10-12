/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.data.Value.falseColor;
import static com.cburch.logisim.data.Value.trueColor;

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

public class Ttl74161 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74161";

  public static final int PORT_INDEX_nCLR = 0;
  public static final int PORT_INDEX_CLK = 1;
  public static final int PORT_INDEX_A = 2;
  public static final int PORT_INDEX_B = 3;
  public static final int PORT_INDEX_C = 4;
  public static final int PORT_INDEX_D = 5;
  public static final int PORT_INDEX_EnP = 6;
  public static final int PORT_INDEX_nLOAD = 7;
  public static final int PORT_INDEX_EnT = 8;
  public static final int PORT_INDEX_QD = 9;
  public static final int PORT_INDEX_QC = 10;
  public static final int PORT_INDEX_QB = 11;
  public static final int PORT_INDEX_QA = 12;
  public static final int PORT_INDEX_RC0 = 13;
  private static final String[] PORT_NAMES = {
      "MR/CLR (Reset, active LOW)",
      "CP/CLK (Clock)",
      "D0/A",
      "D1/B",
      "D2/C",
      "D3/D",
      "CE/ENP (Count Enable)",
      "PE/LOAD (Parallel Enable, active LOW)",
      "CET/ENT (Count Enable Carry)",
      "Q3/QD",
      "Q2/QC",
      "A1/QB",
      "A0/QA",
      "TC/RC0 (Terminal Count)"
  };
  private static final byte[] OUTPUT_PORTS = {11, 12, 13, 14, 15};

  public Ttl74161() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
    super.setInstancePoker(Poker.class);
  }

  public Ttl74161(String name) {
    super(name, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      var inside = false;
      for (var i = 0; i < 4; i++) {
        final var dx = p.x - (56 + i * 10);
        final var dy = p.y - 30;
        final var d2 = dx * dx + dy * dy;
        inside |= (d2 < 4 * 4);
      }
      return inside;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      for (var i = 0; i < 4; i++) {
        int dx = p.x - (56 + i * 10);
        int dy = p.y - 30;
        int d2 = dx * dx + dy * dy;
        if (d2 < 4 * 4) return 3 - i;
      }
      return 0;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE).booleanValue()) return;
      if (isPressed && isInside(state, e)) {
        var index = getIndex(state, e);
        final var data = (TtlRegisterData) state.getData();
        if (data == null) return;
        var current = data.getValue().toLongValue();
        final long bitValue = 1 << index;
        current ^= bitValue;
        updateState(state, current);
      }
      isPressed = false;
    }
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
          "nClr", "Clk", "A", "B", "C", "D", "EnP", "nLD", "EnT", "Qd", "Qc", "Qb", "Qa", "RC0"
        });
    TtlRegisterData data = (TtlRegisterData) painter.getData();
    drawState(gfx, x, y, height, data);
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state != null) {
      long value = state.getValue().toLongValue();
      for (var i = 0; i < 4; i++) {
        final var isSetBitValue = (value & (1 << (3 - i))) != 0;
        gfx.setColor(isSetBitValue ? trueColor : falseColor);
        gfx.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
        gfx.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(gfx, isSetBitValue ? "1" : "0", x + 56 + i * 10, y + height / 2);
      }
      gfx.setColor(Color.BLACK);
    }
  }

  public static void updateState(InstanceState state, Long value) {
    var data = getStateData(state);

    data.setValue(Value.createKnown(BitWidth.create(4), value));
    final var vA = data.getValue().get(0);
    final var vB = data.getValue().get(1);
    final var vC = data.getValue().get(2);
    final var vD = data.getValue().get(3);

    state.setPort(PORT_INDEX_QA, vA, 1);
    state.setPort(PORT_INDEX_QB, vB, 1);
    state.setPort(PORT_INDEX_QC, vC, 1);
    state.setPort(PORT_INDEX_QD, vD, 1);

    // RC0 = QA AND QB AND QC AND QD AND ENT
    state.setPort(PORT_INDEX_RC0, state.getPortValue(PORT_INDEX_EnT).and(vA).and(vB).and(vC).and(vD), 1);
  }

  public static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(4));
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = getStateData(state);
    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    final var nClear = state.getPortValue(PORT_INDEX_nCLR).toLongValue();
    var counter = data.getValue().toLongValue();

    if (nClear == 0) {
      counter = 0;
    } else if (triggered) {
      final var nLoad = state.getPortValue(PORT_INDEX_nLOAD);
      if (nLoad.toLongValue() == 0) {
        counter = state.getPortValue(PORT_INDEX_A).toLongValue();
        counter += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
        counter += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
        counter += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      } else {
        final var enpAndEnt = state.getPortValue(PORT_INDEX_EnP).and(state.getPortValue(PORT_INDEX_EnT)).toLongValue();
        if (enpAndEnt == 1) {
          counter++;
          if (counter > 15) {
            counter = 0;
          }
        }
      }
    }
    updateState(state, counter);
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
