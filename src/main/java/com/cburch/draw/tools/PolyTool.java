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

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.icons.DrawPolylineIcon;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.draw.shapes.Poly;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

public class PolyTool extends AbstractTool {
  // how close we need to be to the start point to count as "closing the loop"
  private static final int CLOSE_TOLERANCE = 2;

  private final boolean closed; // whether we are drawing polygons or polylines
  private final DrawingAttributeSet attrs;
  private final List<Location> locations;
  private boolean active;
  private boolean mouseDown;
  private int lastMouseX;
  private int lastMouseY;

  public PolyTool(boolean closed, DrawingAttributeSet attrs) {
    this.closed = closed;
    this.attrs = attrs;
    active = false;
    locations = new ArrayList<>();
  }

  private CanvasObject commit(Canvas canvas) {
    if (!active) return null;
    CanvasObject add = null;
    active = false;
    final var locs = locations;
    for (var i = locs.size() - 2; i >= 0; i--) {
      if (locs.get(i).equals(locs.get(i + 1))) locs.remove(i);
    }
    if (locs.size() > 1) {
      final var model = canvas.getModel();
      add = new Poly(closed, locs);
      canvas.doAction(new ModelAddAction(model, add));
      repaintArea(canvas);
    }
    locs.clear();
    return add;
  }

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    if (active) {
      gfx.setColor(Color.GRAY);
      int size = locations.size();
      final var xs = new int[size];
      final var ys = new int[size];
      for (var i = 0; i < size; i++) {
        final var loc = locations.get(i);
        xs[i] = loc.getX();
        ys[i] = loc.getY();
      }
      gfx.drawPolyline(xs, ys, size);
      final var lastX = xs[xs.length - 1];
      final var lastY = ys[ys.length - 1];
      gfx.fillOval(lastX - 2, lastY - 2, 4, 4);
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(attrs.getValue(DrawAttr.PAINT_TYPE));
  }

  @Override
  public Cursor getCursor(Canvas canvas) {
    return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
  }

  @Override
  public Icon getIcon() {
    return new DrawPolylineIcon(closed);
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    final var code = e.getKeyCode();
    if (active && mouseDown && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL)) {
      updateMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    keyPressed(canvas, e);
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    if (active) {
      final var ch = e.getKeyChar();
      if (ch == '\u001b') { // escape key
        active = false;
        locations.clear();
        repaintArea(canvas);
        canvas.toolGestureComplete(this, null);
      } else if (ch == '\n') { // enter key
        final var add = commit(canvas);
        canvas.toolGestureComplete(this, add);
      }
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, MouseEvent e) {
    updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
  }

  @Override
  public void mousePressed(Canvas canvas, MouseEvent e) {
    var mx = e.getX();
    var my = e.getY();
    lastMouseX = mx;
    lastMouseY = my;
    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
      mx = canvas.snapX(mx);
      my = canvas.snapY(my);
    }

    if (active && e.getClickCount() > 1) {
      final var add = commit(canvas);
      canvas.toolGestureComplete(this, add);
      return;
    }

    final var loc = Location.create(mx, my);
    final var locs = locations;
    if (!active) {
      locs.clear();
      locs.add(loc);
    }
    locs.add(loc);

    mouseDown = true;
    active = canvas.getModel() != null;
    repaintArea(canvas);
  }

  @Override
  public void mouseReleased(Canvas canvas, MouseEvent e) {
    if (active) {
      updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
      mouseDown = false;
      int size = locations.size();
      if (size >= 3) {
        final var first = locations.get(0);
        final var last = locations.get(size - 1);
        if (first.manhattanDistanceTo(last) <= CLOSE_TOLERANCE) {
          locations.remove(size - 1);
          final var add = commit(canvas);
          canvas.toolGestureComplete(this, add);
        }
      }
    }
  }

  private void repaintArea(Canvas canvas) {
    canvas.repaint();
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    final var add = commit(canvas);
    canvas.toolGestureComplete(this, add);
    repaintArea(canvas);
  }

  private void updateMouse(Canvas canvas, int mx, int my, int mods) {
    lastMouseX = mx;
    lastMouseY = my;
    if (active) {
      int index = locations.size() - 1;
      final var last = locations.get(index);
      Location newLast;
      if ((mods & MouseEvent.SHIFT_DOWN_MASK) != 0 && index > 0) {
        final var nextLast = locations.get(index - 1);
        newLast = LineUtil.snapTo8Cardinals(nextLast, mx, my);
      } else {
        newLast = Location.create(mx, my);
      }
      if ((mods & MouseEvent.CTRL_DOWN_MASK) != 0) {
        var lastX = newLast.getX();
        var lastY = newLast.getY();
        lastX = canvas.snapX(lastX);
        lastY = canvas.snapY(lastY);
        newLast = Location.create(lastX, lastY);
      }

      if (!newLast.equals(last)) {
        locations.set(index, newLast);
        repaintArea(canvas);
      }
    }
  }
}
