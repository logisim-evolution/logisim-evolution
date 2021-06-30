/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.data.Value.FALSE_COLOR;
import static com.cburch.logisim.data.Value.TRUE_COLOR;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
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

  public Ttl74161() {
    super(
        _ID,
        (byte) 16,
        new byte[] {11, 12, 13, 14, 15},
        new String[] {
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
        });
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = TTLGetTranslatedXY(state, e);
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
      final var p = TTLGetTranslatedXY(state, e);
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
      if (!state.getAttributeValue(TTL.DRAW_INTERNAL_STRUCTURE).booleanValue()) return;
      if (isPressed && isInside(state, e)) {
        int index = getIndex(state, e);

        final var p = TTLGetTranslatedXY(state, e);
        System.out.print("x=");
        System.out.print(p.x);
        System.out.print(",y=");
        System.out.print(p.y);
        System.out.print(",i=");
        System.out.print(index);
        System.out.println(index);

        final var data = (TTLRegisterData) state.getData();
        if (data == null) return;
        var current = data.getValue().toLongValue();
        final long bitValue = 1 << index;
        current ^= bitValue;
        data.setValue(Value.createKnown(4, current));
        state.fireInvalidated();
      }
      isPressed = false;
    }
  }

  private TTLRegisterData getData(InstanceState state) {
    var data = (TTLRegisterData) state.getData();
    if (data == null) {
      data = new TTLRegisterData(BitWidth.create(4));
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
          "nClr", "Clk", "A", "B", "C", "D", "EnP", "nLD", "EnT", "Qd", "Qc", "Qb", "Qa", "RC0"
        });
    final var data = (TTLRegisterData) painter.getData();
    drawState(g, x, y, height, data);
  }

  private void drawState(Graphics2D g, int x, int y, int height, TTLRegisterData state) {
    if (state != null) {
      long value = state.getValue().toLongValue();
      for (var i = 0; i < 4; i++) {
        final var isSetBitValue = (value & (1 << (3 - i))) != 0;
        g.setColor(isSetBitValue ? TRUE_COLOR : FALSE_COLOR);
        g.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
        g.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(
            g, isSetBitValue ? "1" : "0", x + 56 + i * 10, y + height / 2);
      }
      g.setColor(Color.BLACK);
    }
  }

  @Override
  public void ttlpropagate(InstanceState state) {

    var data = (TTLRegisterData) state.getData();
    if (data == null) {
      data = new TTLRegisterData(BitWidth.create(4));
      state.setData(data);
    }

    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    if (triggered) {

      Value nClear = state.getPortValue(PORT_INDEX_nCLR);
      Value nLoad = state.getPortValue(PORT_INDEX_nLOAD);

      long counter;

      if (nClear.toLongValue() == 0) {
        counter = 0;
      } else if (nLoad.toLongValue() == 0) {
        counter = state.getPortValue(PORT_INDEX_A).toLongValue();
        counter += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
        counter += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
        counter += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      } else {
        counter = data.getValue().toLongValue();
        Value enpAndEnt =
            state.getPortValue(PORT_INDEX_EnP).and(state.getPortValue(PORT_INDEX_EnT));
        if (enpAndEnt.toLongValue() == 1) {
          counter++;
          if (counter > 15) {
            counter = 0;
          }
        }
      }
      data.setValue(Value.createKnown(BitWidth.create(4), counter));
    }

    final var vA = data.getValue().get(0);
    final var vB = data.getValue().get(1);
    final var vC = data.getValue().get(2);
    final var vD = data.getValue().get(3);

    state.setPort(PORT_INDEX_QA, vA, 1);
    state.setPort(PORT_INDEX_QB, vB, 1);
    state.setPort(PORT_INDEX_QC, vC, 1);
    state.setPort(PORT_INDEX_QD, vD, 1);

    // RC0 = QA AND QB AND QC AND QD AND ENT
    state.setPort(
        PORT_INDEX_RC0, state.getPortValue(PORT_INDEX_EnT).and(vA).and(vB).and(vC).and(vD), 1);
  }

  @Override
  public boolean CheckForGatedClocks(NetlistComponent comp) {
    return true;
  }

  @Override
  public int[] ClockPinIndex(NetlistComponent comp) {
    return new int[] {1};
  }
}
