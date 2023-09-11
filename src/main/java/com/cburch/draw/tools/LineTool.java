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
import com.cburch.draw.icons.DrawLineIcon;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.draw.shapes.Poly;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;

public class LineTool extends AbstractTool {
  private final DrawingAttributeSet attrs;
  private boolean active;
  private Location mouseStart;
  private Location mouseEnd;
  private int lastMouseX;
  private int lastMouseY;

  public LineTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
    active = false;
  }

  static Location snapTo4Cardinals(Location from, int mx, int my) {
    final var px = from.getX();
    final var py = from.getY();
    if (mx != px && my != py) {
      return (Math.abs(my - py) < Math.abs(mx - px))
          ? Location.create(mx, py, false)
          : Location.create(px, my, false);
    }
    return Location.create(mx, my, false); // should never happen
  }

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    if (active) {
      final var start = mouseStart;
      final var end = mouseEnd;
      gfx.setColor(Color.GRAY);
      gfx.drawLine(start.getX(), start.getY(), end.getX(), end.getY());
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.ATTRS_STROKE;
  }

  @Override
  public Cursor getCursor(Canvas canvas) {
    return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
  }

  @Override
  public Icon getIcon() {
    return new DrawLineIcon();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    final var code = e.getKeyCode();
    if (active && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL)) {
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
    var x = e.getX();
    var y = e.getY();
    final var mods = e.getModifiersEx();
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
      x = canvas.snapX(x);
      y = canvas.snapY(y);
    }
    final var loc = Location.create(x, y, false);
    mouseStart = loc;
    mouseEnd = loc;
    lastMouseX = loc.getX();
    lastMouseY = loc.getY();
    active = canvas.getModel() != null;
    repaintArea(canvas);
  }

  @Override
  public void mouseReleased(Canvas canvas, MouseEvent e) {
    if (active) {
      updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
      final var start = mouseStart;
      final var end = mouseEnd;
      CanvasObject add = null;
      if (!start.equals(end)) {
        active = false;
        final var model = canvas.getModel();
        Location[] ends = {start, end};
        final var locs = UnmodifiableList.create(ends);
        add = attrs.applyTo(new Poly(false, locs));
        add.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE);
        canvas.doAction(new ModelAddAction(model, add));
        repaintArea(canvas);
      }
      canvas.toolGestureComplete(this, add);
    }
  }

  private void repaintArea(Canvas canvas) {
    canvas.repaint();
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    active = false;
    repaintArea(canvas);
  }

  private void updateMouse(Canvas canvas, int mx, int my, int mods) {
    if (active) {
      final var shift = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
      var newEnd =
          (shift) ? LineUtil.snapTo8Cardinals(mouseStart, mx, my) : Location.create(mx, my, false);

      if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
        var x = newEnd.getX();
        var y = newEnd.getY();
        x = canvas.snapX(x);
        y = canvas.snapY(y);
        newEnd = Location.create(x, y, false);
      }

      if (!newEnd.equals(mouseEnd)) {
        mouseEnd = newEnd;
        repaintArea(canvas);
      }
    }
    lastMouseX = mx;
    lastMouseY = my;
  }
}
