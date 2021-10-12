/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

abstract class RectangularTool extends AbstractTool {
  private boolean active;
  private Location dragStart;
  private int lastMouseX;
  private int lastMouseY;
  private Bounds currentBounds;

  public RectangularTool() {
    active = false;
    currentBounds = Bounds.EMPTY_BOUNDS;
  }

  private Bounds computeBounds(Canvas canvas, int mx, int my, int mods) {
    lastMouseX = mx;
    lastMouseY = my;

    if (!active) return Bounds.EMPTY_BOUNDS;

    final var start = dragStart;
    var x0 = start.getX();
    var y0 = start.getY();
    var x1 = mx;
    var y1 = my;
    if (x0 == x1 && y0 == y1) {
      return Bounds.EMPTY_BOUNDS;
    }

    final var ctrlDown = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;
    if (ctrlDown) {
      x0 = canvas.snapX(x0);
      y0 = canvas.snapY(y0);
      x1 = canvas.snapX(x1);
      y1 = canvas.snapY(y1);
    }

    final var altDown = (mods & MouseEvent.ALT_DOWN_MASK) != 0;
    final var shiftDown = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
    if (altDown) {
      if (shiftDown) {
        final var r = Math.min(Math.abs(x0 - x1), Math.abs(y0 - y1));
        x1 = x0 + r;
        y1 = y0 + r;
        x0 -= r;
        y0 -= r;
      } else {
        x0 = x0 - (x1 - x0);
        y0 = y0 - (y1 - y0);
      }
    } else {
      if (shiftDown) {
        final var r = Math.min(Math.abs(x0 - x1), Math.abs(y0 - y1));
        y1 = y1 < y0 ? y0 - r : y0 + r;
        x1 = x1 < x0 ? x0 - r : x0 + r;
      }
    }

    var x = x0;
    var y = y0;
    var w = x1 - x0;
    var h = y1 - y0;
    if (w < 0) {
      x = x1;
      w = -w;
    }
    if (h < 0) {
      y = y1;
      h = -h;
    }
    return Bounds.create(x, y, w, h);
  }

  public abstract CanvasObject createShape(int x, int y, int w, int h);

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    final var bds = currentBounds;
    if (active && bds != null && bds != Bounds.EMPTY_BOUNDS) {
      gfx.setColor(Color.GRAY);
      drawShape(gfx, bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    }
  }

  public abstract void drawShape(Graphics g, int x, int y, int w, int h);

  public abstract void fillShape(Graphics g, int x, int y, int w, int h);

  @Override
  public Cursor getCursor(Canvas canvas) {
    return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    final var code = e.getKeyCode();
    if (active && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_ALT || code == KeyEvent.VK_CONTROL)) {
      updateMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    keyPressed(canvas, e);
  }

  @Override
  public void mouseDragged(Canvas canvas, MouseEvent e) {
    updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
  }

  @Override
  public void mousePressed(Canvas canvas, MouseEvent e) {
    final var loc = Location.create(e.getX(), e.getY());
    final var bds = Bounds.create(loc);
    dragStart = loc;
    lastMouseX = loc.getX();
    lastMouseY = loc.getY();
    active = canvas.getModel() != null;
    repaintArea(canvas, bds);
  }

  @Override
  public void mouseReleased(Canvas canvas, MouseEvent e) {
    if (active) {
      final var oldBounds = currentBounds;
      final var bds = computeBounds(canvas, e.getX(), e.getY(), e.getModifiersEx());
      currentBounds = Bounds.EMPTY_BOUNDS;
      active = false;
      CanvasObject add = null;
      if (bds.getWidth() != 0 && bds.getHeight() != 0) {
        final var model = canvas.getModel();
        add = createShape(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
        canvas.doAction(new ModelAddAction(model, add));
        repaintArea(canvas, oldBounds.add(bds));
      }
      canvas.toolGestureComplete(this, add);
    }
  }

  private void repaintArea(Canvas canvas, Bounds bds) {
    canvas.repaint();
    /*
     * The below doesn't work because Java doesn't deal correctly with
     * stroke widths that go outside the clip area
     * canvas.repaintCanvasCoords(bds.getX() - 10, bds.getY() - 10,
     * bds.getWidth() + 20, bds.getHeight() + 20);
     */
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    final var bds = currentBounds;
    active = false;
    repaintArea(canvas, bds);
  }

  private void updateMouse(Canvas canvas, int mx, int my, int mods) {
    final var oldBounds = currentBounds;
    final var bds = computeBounds(canvas, mx, my, mods);
    if (!bds.equals(oldBounds)) {
      currentBounds = bds;
      repaintArea(canvas, oldBounds.add(bds));
    }
  }
}
