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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

public class Ttl7474 extends AbstractTtlGate {

  public Ttl7474() {
    super(
        "7474",
        (byte) 14,
        new byte[] {5, 6, 8, 9},
        new String[] {
          "nCLR1", "D1", "CLK1", "nPRE1", "Q1", "nQ1", "nQ2", "Q2", "nPRE2", "CLK2", "D2", "nCLR2"
        });
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      Point p = TTLGetTranslatedXY(state, e);
      int dx = p.x - 37;
      int dy = p.y - 35;
      int d2 = dx * dx + dy * dy;
      dx = p.x - 107;
      dy = p.y - 32;
      int d3 = dx * dx + dy * dy;
      return ((d2 < 5 * 5) || (d3 < 5 * 5));
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      Point p = TTLGetTranslatedXY(state, e);
      int dx = p.x - 37;
      int dy = p.y - 35;
      int d2 = dx * dx + dy * dy;
      if (d2 < 5 * 5) return 0;
      return 1;
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
        TTLRegisterData myState = (TTLRegisterData) state.getData();
        if (myState == null) return;
        Value[] values = myState.getValue().getAll();
        if (values[index].isFullyDefined()) values[index] = values[index].not();
        else values[index] = Value.createKnown(1, 0);
        myState.setValue(Value.create(values));
        state.fireInvalidated();
      }
      isPressed = false;
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    Graphics g = painter.getGraphics();
    TTLRegisterData state = (TTLRegisterData) painter.getData();
    super.paintBase(painter, false, false);
    drawflop(g, x, y + 1);
    drawflop(g, x + 70, y - 2);
    drawCon1(g, x, y, height);
    drawCon2(g, x, y);
    drawState(g, x, y + 1, 0, state);
    drawState(g, x + 70, y - 2, 1, state);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    TTLRegisterData data = (TTLRegisterData) state.getData();
    if (data == null) {
      data = new TTLRegisterData(BitWidth.create(2));
      state.setData(data);
    }
    boolean triggered1 = data.updateClock(state.getPortValue(2), 0);
    boolean triggered2 = data.updateClock(state.getPortValue(9), 1);
    Value[] values = data.getValue().getAll();
    if ((state.getPortValue(0) == Value.FALSE) && (state.getPortValue(3) == Value.FALSE)) {
      values[0] = Value.createUnknown(BitWidth.create(1));
    } else if (state.getPortValue(0) == Value.FALSE) {
      values[0] = Value.createKnown(BitWidth.create(1), 0);
    } else if (state.getPortValue(3) == Value.FALSE) {
      values[0] = Value.createKnown(BitWidth.create(1), 1);
    } else if (triggered1) {
      values[0] = state.getPortValue(1);
    }
    if ((state.getPortValue(11) == Value.FALSE) && (state.getPortValue(8) == Value.FALSE)) {
      values[1] = Value.createUnknown(BitWidth.create(1));
    } else if (state.getPortValue(11) == Value.FALSE) {
      values[1] = Value.createKnown(BitWidth.create(1), 0);
    } else if (state.getPortValue(8) == Value.FALSE) {
      values[1] = Value.createKnown(BitWidth.create(1), 1);
    } else if (triggered2) {
      values[1] = state.getPortValue(10);
    }
    data.setValue(Value.create(values));

    state.setPort(4, data.getValue().get(0), 8);
    state.setPort(5, data.getValue().get(0).not(), 8);
    state.setPort(6, data.getValue().get(1).not(), 8);
    state.setPort(7, data.getValue().get(1), 8);
  }

  private void drawState(Graphics g, int x, int y, int ID, TTLRegisterData state) {
    if (state != null) {
      g.setColor(state.getValue().get(ID).getColor());
      g.fillOval(x + 33, y + 30, 8, 8);
      g.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(g, state.getValue().get(ID).toDisplayString(), x + 36, y + 33);
      g.setColor(Color.BLACK);
    }
  }

  private void drawflop(Graphics g, int x, int y) {
    g.drawRect(x + 27, y + 20, 16, 20);
    g.drawOval(x + 33, y + 16, 4, 4);
    g.drawOval(x + 33, y + 40, 4, 4);
    g.drawOval(x + 43, y + 33, 4, 4);
    g.drawLine(x + 27, y + 33, x + 30, y + 35);
    g.drawLine(x + 27, y + 37, x + 30, y + 35);
    g.drawString("D", x + 28, y + 28);
    g.drawString("Q", x + 38, y + 28);
  }

  private void drawCon1(Graphics g, int x, int y, int height) {
    g.drawLine(x + 70, y + height - AbstractTtlGate.pinheight, x + 70, y + 16);
    g.drawLine(x + 35, y + 16, x + 70, y + 16);
    g.drawLine(x + 35, y + 16, x + 35, y + 17);

    g.drawLine(x + 10, y + height - AbstractTtlGate.pinheight, x + 10, y + 46);
    g.drawLine(x + 10, y + 46, x + 35, y + 46);
    g.drawLine(x + 35, y + 45, x + 35, y + 46);

    g.drawLine(x + 30, y + height - AbstractTtlGate.pinheight, x + 30, y + 50);
    g.drawLine(x + 20, y + 50, x + 30, y + 50);
    g.drawLine(x + 20, y + 26, x + 20, y + 50);
    g.drawLine(x + 20, y + 26, x + 27, y + 26);

    g.drawLine(x + 50, y + height - AbstractTtlGate.pinheight, x + 50, y + 48);
    g.drawLine(x + 22, y + 48, x + 50, y + 48);
    g.drawLine(x + 22, y + 36, x + 22, y + 48);
    g.drawLine(x + 22, y + 36, x + 27, y + 36);

    g.drawLine(x + 90, y + height - AbstractTtlGate.pinheight, x + 90, y + 48);
    g.drawLine(x + 68, y + 48, x + 90, y + 48);
    g.drawLine(x + 68, y + 26, x + 68, y + 48);
    g.drawLine(x + 43, y + 26, x + 68, y + 26);

    g.drawLine(x + 110, y + height - AbstractTtlGate.pinheight, x + 110, y + 50);
    g.drawLine(x + 66, y + 50, x + 110, y + 50);
    g.drawLine(x + 66, y + 36, x + 66, y + 50);
    g.drawLine(x + 47, y + 36, x + 66, y + 36);
  }

  private void drawCon2(Graphics g, int x, int y) {
    g.drawLine(x + 130, y + AbstractTtlGate.pinheight, x + 130, y + 33);
    g.drawLine(x + 117, y + 33, x + 130, y + 33);

    g.drawLine(x + 110, y + AbstractTtlGate.pinheight, x + 110, y + 10);
    g.drawLine(x + 110, y + 10, x + 120, y + 10);
    g.drawLine(x + 120, y + 10, x + 120, y + 23);
    g.drawLine(x + 113, y + 23, x + 120, y + 23);

    g.drawLine(x + 90, y + AbstractTtlGate.pinheight, x + 90, y + 10);
    g.drawLine(x + 90, y + 10, x + 105, y + 10);
    g.drawLine(x + 105, y + 10, x + 105, y + 14);

    g.drawLine(x + 70, y + AbstractTtlGate.pinheight, x + 70, y + 10);
    g.drawLine(x + 70, y + 10, x + 88, y + 10);
    g.drawLine(x + 88, y + 10, x + 88, y + 33);
    g.drawLine(x + 88, y + 33, x + 97, y + 33);

    g.drawLine(x + 50, y + AbstractTtlGate.pinheight, x + 50, y + 12);
    g.drawLine(x + 50, y + 12, x + 86, y + 12);
    g.drawLine(x + 86, y + 12, x + 86, y + 23);
    g.drawLine(x + 86, y + 23, x + 97, y + 23);

    g.drawLine(x + 30, y + AbstractTtlGate.pinheight, x + 30, y + 14);
    g.drawLine(x + 30, y + 14, x + 84, y + 14);
    g.drawLine(x + 84, y + 14, x + 84, y + 44);
    g.drawLine(x + 84, y + 44, x + 105, y + 44);
    g.drawLine(x + 105, y + 43, x + 105, y + 44);
  }

  @Override
  public boolean CheckForGatedClocks(NetlistComponent comp) {
    return true;
  }

  @Override
  public int[] ClockPinIndex(NetlistComponent comp) {
    return new int[] {2, 9};
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return CorrectLabel.getCorrectLabel("TTL" + this.getName()).toUpperCase();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl7474HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
