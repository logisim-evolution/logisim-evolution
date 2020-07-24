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

public class ShiftRegisterPoker extends InstancePoker {
  private int loc;

  private int computeStage(InstanceState state, MouseEvent e) {
    BitWidth widObj = state.getAttributeValue(StdAttr.WIDTH);
    Bounds bds = state.getInstance().getBounds();
    if (state.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      Integer lenObj = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
      Boolean loadObj = state.getAttributeValue(ShiftRegister.ATTR_LOAD);

      int y = bds.getY();
      String label = state.getAttributeValue(StdAttr.LABEL);
      if (label == null || label.equals("")) y += bds.getHeight() / 2;
      else y += 3 * bds.getHeight() / 4;
      y = e.getY() - y;
      if (y <= -6 || y >= 8) return -1;
      int x = e.getX() - (bds.getX() + 15);
      if (!loadObj.booleanValue() || widObj.getWidth() > 4) return -1;
      if (x < 0 || x >= lenObj.intValue() * 10) return -1;
      return x / 10;
    } else {
      int len = (widObj.getWidth() + 3) / 4;
      int boxXpos = ((ShiftRegister.SymbolWidth - 30) / 2 + 30) - (len * 4);
      int boxXend = boxXpos + 2 + len * 8;
      int y = e.getY() - bds.getY() - 80;
      if (y < 0) return -1;
      int x = e.getX() - bds.getX() - 10;
      if ((x < boxXpos) || (x > boxXend)) return -1;
      return (y / 20);
    }
  }

  @Override
  public boolean init(InstanceState state, MouseEvent e) {
    loc = computeStage(state, e);
    return loc >= 0;
  }

  @Override
  public void keyTyped(InstanceState state, KeyEvent e) {
    int loc = this.loc;
    if (loc < 0) return;
    char c = e.getKeyChar();
    if (c == ' ' || c == '\t') {
      Integer lenObj = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
      if (loc < lenObj.intValue() - 1) {
        this.loc = loc + 1;
        state.fireInvalidated();
      }
    } else if (c == '\u0008') {
      if (loc > 0) {
        this.loc = loc - 1;
        state.fireInvalidated();
      }
    } else {
      try {
        int val = Integer.parseInt("" + e.getKeyChar(), 16);
        BitWidth widObj = state.getAttributeValue(StdAttr.WIDTH);
        ShiftRegisterData data = (ShiftRegisterData) state.getData();
        int i = data.getLength() - 1 - loc;
        long value = data.get(i).toLongValue();
        value = ((value * 16) + val) & widObj.getMask();
        Value valObj = Value.createKnown(widObj, value);
        data.set(i, valObj);
        state.fireInvalidated();
      } catch (NumberFormatException ex) {
        return;
      }
    }
  }

  @Override
  public void keyPressed(InstanceState state, KeyEvent e) {
    int loc = this.loc;
    if (loc < 0) return;
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(8);
    ShiftRegisterData data = (ShiftRegisterData) state.getData();
    int i = data.getLength() - 1 - loc;
    long curValue = data.get(i).toLongValue();
    if (e.getKeyCode() == KeyEvent.VK_UP) {
      long maxVal = dataWidth.getMask();
      if (curValue != maxVal) {
        curValue = curValue + 1;
        data.set(i, Value.createKnown(dataWidth, curValue));
        state.fireInvalidated();
      }
    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      if (curValue != 0) {
        curValue = curValue - 1;
        data.set(i, Value.createKnown(dataWidth, curValue));
        state.fireInvalidated();
      }
    }
  }

  @Override
  public void mousePressed(InstanceState state, MouseEvent e) {
    loc = computeStage(state, e);
  }

  @Override
  public void mouseReleased(InstanceState state, MouseEvent e) {
    int oldLoc = loc;
    if (oldLoc < 0) return;
    BitWidth widObj = state.getAttributeValue(StdAttr.WIDTH);
    if (widObj.equals(BitWidth.ONE)) {
      int newLoc = computeStage(state, e);
      if (oldLoc == newLoc) {
        ShiftRegisterData data = (ShiftRegisterData) state.getData();
        int i = data.getLength() - 1 - loc;
        Value v = data.get(i);
        if (v == Value.FALSE) v = Value.TRUE;
        else v = Value.FALSE;
        data.set(i, v);
        state.fireInvalidated();
      }
    }
  }

  @Override
  public void paint(InstancePainter painter) {
    int loc = this.loc;
    if (loc < 0) return;
    BitWidth widObj = painter.getAttributeValue(StdAttr.WIDTH);
    Bounds bds = painter.getInstance().getBounds();
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      int x = bds.getX() + 15 + loc * 10;
      int y = bds.getY();
      String label = painter.getAttributeValue(StdAttr.LABEL);
      if (label == null || label.equals("")) y += bds.getHeight() / 2;
      else y += 3 * bds.getHeight() / 4;
      Graphics g = painter.getGraphics();
      g.setColor(Color.RED);
      g.drawRect(x, y - 6, 10, 13);
    } else {
      int len = (widObj.getWidth() + 3) / 4;
      int boxXpos = ((ShiftRegister.SymbolWidth - 30) / 2 + 30) - (len * 4) + bds.getX() + 10;
      int y = bds.getY() + 82 + loc * 20;
      Graphics g = painter.getGraphics();
      g.setColor(Color.RED);
      g.drawRect(boxXpos, y, 2 + len * 8, 16);
    }
  }
}
