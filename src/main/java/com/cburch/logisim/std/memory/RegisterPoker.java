/**
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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class RegisterPoker extends InstancePoker {
  private long initValue;
  private long curValue;

  @Override
  public boolean init(InstanceState state, MouseEvent e) {
    RegisterData data = (RegisterData) state.getData();
    if (data == null) {
      data = new RegisterData(state.getAttributeValue(StdAttr.WIDTH));
      state.setData(data);
    }
    initValue = (data.value.isFullyDefined()) ? data.value.toLongValue() : 0;
    curValue = initValue;
    return true;
  }

  @Override
  public void keyTyped(InstanceState state, KeyEvent e) {
    int val = Character.digit(e.getKeyChar(), 16);
    if (val < 0) return;

    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(8);
    curValue = (curValue * 16 + val) & dataWidth.getMask();
    RegisterData data = (RegisterData) state.getData();
    data.value = Value.createKnown(dataWidth, curValue);
    state.fireInvalidated();
  }

  @Override
  public void keyPressed(InstanceState state, KeyEvent e) {
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(8);
    if (e.getKeyCode() == KeyEvent.VK_UP) {
      long maxVal = dataWidth.getMask();
      if (curValue != maxVal) {
        curValue = curValue + 1;
        RegisterData data = (RegisterData) state.getData();
        data.value = Value.createKnown(dataWidth, curValue);
        state.fireInvalidated();
      }
    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      if (curValue != 0) {
        curValue = curValue - 1;
        RegisterData data = (RegisterData) state.getData();
        data.value = Value.createKnown(dataWidth, curValue);
        state.fireInvalidated();
      }
    }
  }

  @Override
  public void paint(InstancePainter painter) {
    Bounds bds = painter.getBounds();
    BitWidth dataWidth = painter.getAttributeValue(StdAttr.WIDTH);
    int width = dataWidth == null ? 8 : dataWidth.getWidth();
    int len = (width + 3) / 4;

    Graphics g = painter.getGraphics();
    g.setColor(Color.RED);
    int wid = 8 * len + 2;
    g.drawRect(bds.getX() + (bds.getWidth() - wid) / 2, bds.getY(), wid, 16);
    g.setColor(Color.BLACK);
  }
}
