/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Graphics;
import java.util.List;
import lombok.val;

abstract class Rectangular extends FillableCanvasObject {
  private Bounds bounds; // excluding the stroke's width

  public Rectangular(int x, int y, int w, int h) {
    bounds = Bounds.create(x, y, w, h);
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return true;
  }

  protected abstract boolean contains(int x, int y, int w, int h, Location q);

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    var type = getPaintType();
    if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
      type = DrawAttr.PAINT_STROKE_FILL;
    }
    val b = bounds;
    val x = b.getX();
    val y = b.getY();
    val w = b.getWidth();
    val h = b.getHeight();
    val qx = loc.getX();
    val qy = loc.getY();
    if (type == DrawAttr.PAINT_FILL) {
      return isInRect(qx, qy, x, y, w, h) && contains(x, y, w, h, loc);
    } else if (type == DrawAttr.PAINT_STROKE) {
      val stroke = getStrokeWidth();
      val tol2 = Math.max(2 * Line.ON_LINE_THRESH, stroke);
      val tol = tol2 / 2;
      return isInRect(qx, qy, x - tol, y - tol, w + tol2, h + tol2)
          && contains(x - tol, y - tol, w + tol2, h + tol2, loc)
          && !contains(x + tol, y + tol, w - tol2, h - tol2, loc);
    } else if (type == DrawAttr.PAINT_STROKE_FILL) {
      val tol = getStrokeWidth() / 2;
      return isInRect(qx, qy, x - tol, y - tol, w + getStrokeWidth(), h + getStrokeWidth())
          && contains(x - tol, y - tol, w + getStrokeWidth(), h + getStrokeWidth(), loc);
    }

    return false;
  }

  protected abstract void draw(Graphics g, int x, int y, int w, int h);

  @Override
  public Bounds getBounds() {
    val wid = getStrokeWidth();
    val type = getPaintType();
    return (wid < 2 || type == DrawAttr.PAINT_FILL) ? bounds : bounds.expand(wid / 2);
  }

  private Handle[] getHandleArray(HandleGesture gesture) {
    val bds = bounds;
    val x0 = bds.getX();
    val y0 = bds.getY();
    val x1 = x0 + bds.getWidth();
    val y1 = y0 + bds.getHeight();
    if (gesture == null) {
      return new Handle[] {
        new Handle(this, x0, y0),
        new Handle(this, x1, y0),
        new Handle(this, x1, y1),
        new Handle(this, x0, y1)
      };
    } else {
      val hx = gesture.getHandle().getX();
      val hy = gesture.getHandle().getY();
      val dx = gesture.getDeltaX();
      val dy = gesture.getDeltaY();
      var newX0 = x0 == hx ? x0 + dx : x0;
      var newY0 = y0 == hy ? y0 + dy : y0;
      var newX1 = x1 == hx ? x1 + dx : x1;
      var newY1 = y1 == hy ? y1 + dy : y1;
      if (gesture.isShiftDown()) {
        if (gesture.isAltDown()) {
          if (x0 == hx) newX1 -= dx;
          if (x1 == hx) newX0 -= dx;
          if (y0 == hy) newY1 -= dy;
          if (y1 == hy) newY0 -= dy;

          val w = Math.abs(newX1 - newX0);
          val h = Math.abs(newY1 - newY0);
          if (w > h) { // reduce width to h
            int dw = (w - h) / 2;
            newX0 -= (newX0 > newX1 ? 1 : -1) * dw;
            newX1 -= (newX1 > newX0 ? 1 : -1) * dw;
          } else {
            val dh = (h - w) / 2;
            newY0 -= (newY0 > newY1 ? 1 : -1) * dh;
            newY1 -= (newY1 > newY0 ? 1 : -1) * dh;
          }
        } else {
          val w = Math.abs(newX1 - newX0);
          val h = Math.abs(newY1 - newY0);
          if (w > h) { // reduce width to h
            if (x0 == hx) {
              newX0 = newX1 + (newX0 > newX1 ? 1 : -1) * h;
            }
            if (x1 == hx) {
              newX1 = newX0 + (newX1 > newX0 ? 1 : -1) * h;
            }
          } else { // reduce height to w
            if (y0 == hy) {
              newY0 = newY1 + (newY0 > newY1 ? 1 : -1) * w;
            }
            if (y1 == hy) {
              newY1 = newY0 + (newY1 > newY0 ? 1 : -1) * w;
            }
          }
        }
      } else {
        if (gesture.isAltDown()) {
          if (x0 == hx) newX1 -= dx;
          if (x1 == hx) newX0 -= dx;
          if (y0 == hy) newY1 -= dy;
          if (y1 == hy) newY0 -= dy;
        } else {
          // already handled
        }
      }
      return new Handle[] {
        new Handle(this, newX0, newY0),
        new Handle(this, newX1, newY0),
        new Handle(this, newX1, newY1),
        new Handle(this, newX0, newY1)
      };
    }
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    return UnmodifiableList.create(getHandleArray(gesture));
  }

  public int getHeight() {
    return bounds.getHeight();
  }

  public int getWidth() {
    return bounds.getWidth();
  }

  public int getX() {
    return bounds.getX();
  }

  public int getY() {
    return bounds.getY();
  }

  boolean isInRect(int qx, int qy, int x0, int y0, int w, int h) {
    return qx >= x0 && qx < x0 + w && qy >= y0 && qy < y0 + h;
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof Rectangular) {
      val that = (Rectangular) other;
      return this.bounds.equals(that.bounds) && super.matches(that);
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    return bounds.hashCode() * 31 + super.matchesHashCode();
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    val oldHandles = getHandleArray(null);
    val newHandles = getHandleArray(gesture);
    val moved = gesture == null ? null : gesture.getHandle();
    Handle result = null;
    var x0 = Integer.MAX_VALUE;
    var x1 = Integer.MIN_VALUE;
    var y0 = Integer.MAX_VALUE;
    var y1 = Integer.MIN_VALUE;
    var i = -1;
    for (val handle : newHandles) {
      i++;
      if (oldHandles[i].equals(moved)) {
        result = handle;
      }
      val hx = handle.getX();
      val hy = handle.getY();
      if (hx < x0) x0 = hx;
      if (hx > x1) x1 = hx;
      if (hy < y0) y0 = hy;
      if (hy > y1) y1 = hy;
    }
    bounds = Bounds.create(x0, y0, x1 - x0, y1 - y0);
    return result;
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    if (gesture == null) {
      val bds = bounds;
      draw(g, bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    } else {
      val handles = getHandleArray(gesture);
      val p0 = handles[0];
      val p1 = handles[2];
      var x0 = p0.getX();
      var y0 = p0.getY();
      var x1 = p1.getX();
      var y1 = p1.getY();
      if (x1 < x0) {
        val tmp = x0;
        x0 = x1;
        x1 = tmp;
      }
      if (y1 < y0) {
        val tmp = y0;
        y0 = y1;
        y1 = tmp;
      }

      draw(g, x0, y0, x1 - x0, y1 - y0);
    }
  }

  @Override
  public void translate(int dx, int dy) {
    bounds = bounds.translate(dx, dy);
  }
}
