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

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

public class Ttl74165 extends AbstractTtlGate {

  public Ttl74165() {
    super(
        "74165",
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
        });
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      Point p = TTLGetTranslatedXY(state, e);
      boolean inside = false;
      for (int i = 0; i < 8; i++) {
        int dx = p.x - (40 + i * 10);
        int dy = p.y - 30;
        int d2 = dx * dx + dy * dy;
        inside |= (d2 < 4 * 4);
      }
      return inside;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      Point p = TTLGetTranslatedXY(state, e);
      for (int i = 0; i < 8; i++) {
        int dx = p.x - (40 + i * 10);
        int dy = p.y - 30;
        int d2 = dx * dx + dy * dy;
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
      if (!state.getAttributeValue(TTL.DRAW_INTERNAL_STRUCTURE)) return;
      if (isPressed && isInside(state, e)) {
        int index = getIndex(state, e);
        System.out.println(index);
        ShiftRegisterData myState = (ShiftRegisterData) state.getData();
        if (myState == null) return;
        if (myState.get(index).isFullyDefined()) myState.set(index, myState.get(index).not());
        else myState.set(index, Value.createKnown(1, 0));
        state.fireInvalidated();
      }
      isPressed = false;
    }
  }

  private ShiftRegisterData getData(InstanceState state) {
    ShiftRegisterData data = (ShiftRegisterData) state.getData();
    if (data == null) {
      data = new ShiftRegisterData(BitWidth.ONE, 8);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    Graphics2D g = (Graphics2D) painter.getGraphics();
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
      for (int i = 0; i < 8; i++) {
        g.setColor(state.get(7 - i).getColor());
        g.fillOval(x + 36 + i * 10, y + height / 2 - 4, 8, 8);
        g.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(
            g, state.get(7 - i).toDisplayString(), x + 40 + i * 10, y + height / 2);
      }
      g.setColor(Color.BLACK);
    }
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    ShiftRegisterData data = getData(state);
    boolean triggered = data.updateClock(state.getPortValue(1), StdAttr.TRIG_RISING);
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
  public boolean CheckForGatedClocks(NetlistComponent comp) {
    return true;
  }

  @Override
  public int[] ClockPinIndex(NetlistComponent comp) {
    return new int[] {1};
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl74165HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
