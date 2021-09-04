/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.canvas;

import com.cburch.contracts.BaseKeyListenerContract;
import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseMouseMotionListenerContract;
import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Location;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import lombok.Getter;
import lombok.val;

class CanvasListener implements BaseMouseListenerContract, BaseMouseMotionListenerContract, BaseKeyListenerContract, CanvasModelListener {
  private final Canvas canvas;
  @Getter private CanvasTool tool;

  public CanvasListener(Canvas canvas) {
    this.canvas = canvas;
    tool = null;
  }

  public void setTool(CanvasTool value) {
    val oldValue = tool;
    if (value != oldValue) {
      tool = value;
      if (oldValue != null) oldValue.toolDeselected(canvas);
      if (value != null) {
        value.toolSelected(canvas);
        canvas.setCursor(value.getCursor(canvas));
      } else {
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  private void handlePopupTrigger(MouseEvent e) {
    val loc = Location.create(e.getX(), e.getY());
    val objects = canvas.getModel().getObjectsFromTop();
    CanvasObject clicked = null;
    for (val o : objects) {
      if (o.contains(loc, false)) {
        clicked = o;
        break;
      }
    }
    if (clicked == null) {
      for (val o : objects) {
        if (o.contains(loc, true)) {
          clicked = o;
          break;
        }
      }
    }
    canvas.showPopupMenu(e, clicked);
  }

  private boolean isButton1(MouseEvent e) {
    return (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (tool != null) tool.keyPressed(canvas, e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (tool != null) tool.keyReleased(canvas, e);
  }

  @Override
  public void keyTyped(KeyEvent e) {
    if (tool != null) tool.keyTyped(canvas, e);
  }

  @Override
  public void modelChanged(CanvasModelEvent event) {
    canvas.getSelection().modelChanged(event);
    canvas.repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // no-op implementation
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (isButton1(e)) {
      if (tool != null) tool.mouseDragged(canvas, e);
    } else {
      if (tool != null) tool.mouseMoved(canvas, e);
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    if (tool != null) tool.mouseEntered(canvas, e);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if (tool != null) tool.mouseExited(canvas, e);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (tool != null) tool.mouseMoved(canvas, e);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    canvas.requestFocus();
    if (e.isPopupTrigger()) {
      handlePopupTrigger(e);
    } else if (e.getButton() == 1 && tool != null) {
      tool.mousePressed(canvas, e);
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (e.isPopupTrigger()) {
      if (tool != null) tool.cancelMousePress(canvas);
      handlePopupTrigger(e);
    } else if (e.getButton() == 1 && tool != null) {
      tool.mouseReleased(canvas, e);
    }
  }
}
