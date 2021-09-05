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
import com.cburch.draw.icons.DrawCurveIcon;
import com.cburch.draw.shapes.Curve;
import com.cburch.draw.shapes.CurveUtil;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;
import lombok.val;

public class CurveTool extends AbstractTool {
  private static final int BEFORE_CREATION = 0;
  private static final int ENDPOINT_DRAG = 1;
  private static final int CONTROL_DRAG = 2;

  private final DrawingAttributeSet attrs;
  private int state;
  private Location end0;
  private Location end1;
  private Curve curCurve;
  private boolean mouseDown;
  private int lastMouseX;
  private int lastMouseY;

  public CurveTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
    state = BEFORE_CREATION;
    mouseDown = false;
  }

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    gfx.setColor(Color.GRAY);
    switch (state) {
      case ENDPOINT_DRAG:
        gfx.drawLine(end0.getX(), end0.getY(), end1.getX(), end1.getY());
        break;
      case CONTROL_DRAG:
        ((Graphics2D) gfx).draw(curCurve.getCurve2D());
        break;
      default:
        break;
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
    return new DrawCurveIcon();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    int code = e.getKeyCode();
    if (mouseDown
        && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT)) {
      updateMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
      repaintArea(canvas);
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    keyPressed(canvas, e);
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    char ch = e.getKeyChar();
    if (ch == '\u001b') { // escape key
      state = BEFORE_CREATION;
      repaintArea(canvas);
      canvas.toolGestureComplete(this, null);
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, MouseEvent e) {
    updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
    repaintArea(canvas);
  }

  @Override
  public void mousePressed(Canvas canvas, MouseEvent e) {
    var mx = e.getX();
    var my = e.getY();
    lastMouseX = mx;
    lastMouseY = my;
    mouseDown = true;
    val mods = e.getModifiersEx();
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
      mx = canvas.snapX(mx);
      my = canvas.snapY(my);
    }

    switch (state) {
      case BEFORE_CREATION:
      case CONTROL_DRAG:
        end0 = Location.create(mx, my);
        end1 = end0;
        state = ENDPOINT_DRAG;
        break;
      case ENDPOINT_DRAG:
        curCurve = new Curve(end0, end1, Location.create(mx, my));
        state = CONTROL_DRAG;
        break;
      default:
        break;
    }
    repaintArea(canvas);
  }

  @Override
  public void mouseReleased(Canvas canvas, MouseEvent event) {
    val curve = updateMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
    mouseDown = false;
    if (state == CONTROL_DRAG) {
      if (curve != null) {
        attrs.applyTo(curve);
        val model = canvas.getModel();
        canvas.doAction(new ModelAddAction(model, curve));
        canvas.toolGestureComplete(this, curve);
      }
      state = BEFORE_CREATION;
    }
    repaintArea(canvas);
  }

  private void repaintArea(Canvas canvas) {
    canvas.repaint();
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    state = BEFORE_CREATION;
    repaintArea(canvas);
  }

  private Curve updateMouse(Canvas canvas, int mx, int my, int mods) {
    lastMouseX = mx;
    lastMouseY = my;

    val shiftDown = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
    val ctrlDown = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;
    val altDown = (mods & MouseEvent.ALT_DOWN_MASK) != 0;
    Curve ret = null;
    switch (state) {
      case ENDPOINT_DRAG:
        if (mouseDown) {
          if (shiftDown) {
            val p = LineUtil.snapTo8Cardinals(end0, mx, my);
            mx = p.getX();
            my = p.getY();
          }
          if (ctrlDown) {
            mx = canvas.snapX(mx);
            my = canvas.snapY(my);
          }
          end1 = Location.create(mx, my);
        }
        break;
      case CONTROL_DRAG:
        if (mouseDown) {
          var cx = mx;
          var cy = my;
          if (ctrlDown) {
            cx = canvas.snapX(cx);
            cy = canvas.snapY(cy);
          }
          if (shiftDown) {
            val x0 = end0.getX();
            val y0 = end0.getY();
            val x1 = end1.getX();
            val y1 = end1.getY();
            val midx = (x0 + x1) / 2;
            val midy = (y0 + y1) / 2;
            val dx = x1 - x0;
            val dy = y1 - y0;
            val p = LineUtil.nearestPointInfinite(cx, cy, midx, midy, midx - dy, midy + dx);
            cx = (int) Math.round(p[0]);
            cy = (int) Math.round(p[1]);
          }
          if (altDown) {
            double[] e0 = {end0.getX(), end0.getY()};
            double[] e1 = {end1.getX(), end1.getY()};
            double[] mid = {cx, cy};
            val ct = CurveUtil.interpolate(e0, e1, mid);
            cx = (int) Math.round(ct[0]);
            cy = (int) Math.round(ct[1]);
          }
          ret = new Curve(end0, end1, Location.create(cx, cy));
          curCurve = ret;
        }
        break;
      default:
        break;
    }
    return ret;
  }
}
