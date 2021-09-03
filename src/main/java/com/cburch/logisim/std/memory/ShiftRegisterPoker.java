/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class ShiftRegisterPoker extends InstancePoker {
  private int loc;

  private int computeStage(InstanceState state, MouseEvent e) {
    final var widObj = state.getAttributeValue(StdAttr.WIDTH);
    final var bds = state.getInstance().getBounds();
    if (state.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      final var lenObj = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
      final var loadObj = state.getAttributeValue(ShiftRegister.ATTR_LOAD);

      var y = bds.getY();
      final var label = state.getAttributeValue(StdAttr.LABEL);
      if (label == null || label.equals(""))
        y += bds.getHeight() / 2;
      else
        y += 3 * bds.getHeight() / 4;
      y = e.getY() - y;
      if (y <= -6 || y >= 8)
        return -1;
      final var x = e.getX() - (bds.getX() + 15);
      if (!loadObj || widObj.getWidth() > 4) return -1;
      if (x < 0 || x >= lenObj * 10) return -1;
      return x / 10;
    } else {
      final var len = (widObj.getWidth() + 3) / 4;
      final var boxXpos = ((ShiftRegister.symbolWidth - 30) / 2 + 30) - (len * 4);
      final var boxXend = boxXpos + 2 + len * 8;
      final var y = e.getY() - bds.getY() - 80;
      if (y < 0) return -1;
      final var x = e.getX() - bds.getX() - 10;
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
    final var loc = this.loc;
    if (loc < 0) return;
    final var c = e.getKeyChar();
    if (c == ' ' || c == '\t') {
      final var lenObj = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
      if (loc < lenObj - 1) {
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
        final var val = Integer.parseInt("" + e.getKeyChar(), 16);
        final var widObj = state.getAttributeValue(StdAttr.WIDTH);
        final var data = (ShiftRegisterData) state.getData();
        final var i = data.getLength() - 1 - loc;
        var value = data.get(i).toLongValue();
        value = ((value * 16) + val) & widObj.getMask();
        final var valObj = Value.createKnown(widObj, value);
        data.set(i, valObj);
        state.fireInvalidated();
      } catch (NumberFormatException ex) {
        return;
      }
    }
  }

  @Override
  public void keyPressed(InstanceState state, KeyEvent e) {
    final var loc = this.loc;
    if (loc < 0) return;
    var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    if (dataWidth == null) dataWidth = BitWidth.create(8);
    final var data = (ShiftRegisterData) state.getData();
    final var i = data.getLength() - 1 - loc;
    var curValue = data.get(i).toLongValue();
    if (e.getKeyCode() == KeyEvent.VK_UP) {
      final var maxVal = dataWidth.getMask();
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
    final var oldLoc = loc;
    if (oldLoc < 0) return;
    final var widObj = state.getAttributeValue(StdAttr.WIDTH);
    if (widObj.equals(BitWidth.ONE)) {
      final var newLoc = computeStage(state, e);
      if (oldLoc == newLoc) {
        final var data = (ShiftRegisterData) state.getData();
        final var i = data.getLength() - 1 - loc;
        var v = data.get(i);
        v = (v == Value.FALSE) ? Value.TRUE : Value.FALSE;
        data.set(i, v);
        state.fireInvalidated();
      }
    }
  }

  @Override
  public void paint(InstancePainter painter) {
    final var loc = this.loc;
    if (loc < 0) return;
    final var widObj = painter.getAttributeValue(StdAttr.WIDTH);
    final var bds = painter.getInstance().getBounds();
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      final var x = bds.getX() + 15 + loc * 10;
      var y = bds.getY();
      final var label = painter.getAttributeValue(StdAttr.LABEL);
      if (label == null || label.equals(""))
        y += bds.getHeight() / 2;
      else
        y += 3 * bds.getHeight() / 4;
      final var g = painter.getGraphics();
      g.setColor(Color.RED);
      g.drawRect(x, y - 6, 10, 13);
    } else {
      final var len = (widObj.getWidth() + 3) / 4;
      final var boxXpos = ((ShiftRegister.symbolWidth - 30) / 2 + 30) - (len * 4) + bds.getX() + 10;
      final var y = bds.getY() + 82 + loc * 20;
      final var g = painter.getGraphics();
      g.setColor(Color.RED);
      g.drawRect(boxXpos, y, 2 + len * 8, 16);
    }
  }
}
