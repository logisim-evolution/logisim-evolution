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

class CanvasListener implements BaseMouseListenerContract, BaseMouseMotionListenerContract, BaseKeyListenerContract, CanvasModelListener {
  private final Canvas canvas;
  private CanvasTool tool;

  public CanvasListener(Canvas canvas) {
    this.canvas = canvas;
    tool = null;
  }

  public CanvasTool getTool() {
    return tool;
  }

  public void setTool(CanvasTool value) {
    final var oldValue = tool;
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
    final var loc = Location.create(e.getX(), e.getY());
    final var objects = canvas.getModel().getObjectsFromTop();
    CanvasObject clicked = null;
    for (final var o : objects) {
      if (o.contains(loc, false)) {
        clicked = o;
        break;
      }
    }
    if (clicked == null) {
      for (final var o : objects) {
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
