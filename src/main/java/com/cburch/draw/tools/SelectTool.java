/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelMoveHandleAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.actions.ModelTranslateAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.SelectIcon;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;

public class SelectTool extends AbstractTool {
  private static final int IDLE = 0;
  private static final int MOVE_ALL = 1;
  private static final int RECT_SELECT = 2;
  private static final int RECT_TOGGLE = 3;
  private static final int MOVE_HANDLE = 4;
  private static final int DRAG_TOLERANCE = 2;
  private static final int HANDLE_SIZE = 8;
  private static final Color RECT_SELECT_BACKGROUND = new Color(0, 0, 0, 32);
  private static final SelectIcon icon = new SelectIcon();
  private int curAction;
  private List<CanvasObject> beforePressSelection;
  private Handle beforePressHandle;
  private Location dragStart;
  private Location dragEnd;
  private boolean dragEffective;
  private int lastMouseX;
  private int lastMouseY;
  private HandleGesture curGesture;

  public SelectTool() {
    curAction = IDLE;
    dragStart = Location.create(0, 0);
    dragEnd = dragStart;
    dragEffective = false;
  }

  private static CanvasObject getObjectAt(CanvasModel model, int x, int y, boolean assumeFilled) {
    final var loc = Location.create(x, y);
    for (final var o : model.getObjectsFromTop()) {
      if (o.contains(loc, assumeFilled)) return o;
    }
    return null;
  }

  @Override
  public void cancelMousePress(Canvas canvas) {
    final var before = beforePressSelection;
    final var handle = beforePressHandle;
    beforePressSelection = null;
    beforePressHandle = null;
    if (before != null) {
      curAction = IDLE;
      final var sel = canvas.getSelection();
      sel.clearDrawsSuppressed();
      sel.setMovingShapes(Collections.emptySet(), 0, 0);
      sel.clearSelected();
      sel.setSelected(before, true);
      sel.setHandleSelected(handle);
      repaintArea(canvas);
    }
  }

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    final var selection = canvas.getSelection();
    final var action = curAction;

    final var start = dragStart;
    final var end = dragEnd;
    HandleGesture gesture = null;
    boolean drawHandles;
    switch (action) {
      case MOVE_ALL:
        drawHandles = !dragEffective;
        break;
      case MOVE_HANDLE:
        drawHandles = !dragEffective;
        if (dragEffective) gesture = curGesture;
        break;
      default:
        drawHandles = true;
    }

    CanvasObject moveHandleObj = null;
    if (gesture != null) {
      moveHandleObj = gesture.getHandle().getObject();
    }
    if (drawHandles) {
      // unscale the coordinate system so that the stroke width isn't scaled
      var zoom = 1.0;
      final var gfxCopy = gfx.create();
      if (gfxCopy instanceof Graphics2D g2d) {
        zoom = canvas.getZoomFactor();
        if (zoom != 1.0) {
          g2d.scale(1.0 / zoom, 1.0 / zoom);
        }
      }
      GraphicsUtil.switchToWidth(gfxCopy, 1);

      final var size = (int) Math.ceil(HANDLE_SIZE * Math.sqrt(zoom));
      final var offs = size / 2;
      for (final var obj : selection.getSelected()) {
        final var handles =
            (action == MOVE_HANDLE && obj == moveHandleObj)
                ? obj.getHandles(gesture)
                : obj.getHandles(null);

        for (final var han : handles) {
          var x = han.getX();
          var y = han.getY();
          if (action == MOVE_ALL && dragEffective) {
            final var delta = selection.getMovingDelta();
            x += delta.getX();
            y += delta.getY();
          }
          x = (int) Math.round(zoom * x);
          y = (int) Math.round(zoom * y);
          gfxCopy.clearRect(x - offs, y - offs, size, size);
          gfxCopy.drawRect(x - offs, y - offs, size, size);
        }
      }
      final var selHandle = selection.getSelectedHandle();
      if (selHandle != null) {
        var x = selHandle.getX();
        var y = selHandle.getY();
        if (action == MOVE_ALL && dragEffective) {
          final var delta = selection.getMovingDelta();
          x += delta.getX();
          y += delta.getY();
        }
        x = (int) Math.round(zoom * x);
        y = (int) Math.round(zoom * y);
        final int[] xs = {x - offs, x, x + offs, x};
        final int[] ys = {y, y - offs, y, y + offs};
        gfxCopy.setColor(Color.WHITE);
        gfxCopy.fillPolygon(xs, ys, 4);
        gfxCopy.setColor(Color.BLACK);
        gfxCopy.drawPolygon(xs, ys, 4);
      }
    }

    switch (action) {
      case RECT_SELECT:
      case RECT_TOGGLE:
        if (dragEffective) {
          // find rectangle currently to show
          var x0 = start.getX();
          var y0 = start.getY();
          var x1 = end.getX();
          var y1 = end.getY();
          if (x1 < x0) {
            final var t = x0;
            x0 = x1;
            x1 = t;
          }
          if (y1 < y0) {
            final var t = y0;
            y0 = y1;
            y1 = t;
          }

          // make the region that's not being selected darker
          final var w = canvas.getWidth();
          final var h = canvas.getHeight();
          gfx.setColor(RECT_SELECT_BACKGROUND);
          gfx.fillRect(0, 0, w, y0);
          gfx.fillRect(0, y0, x0, y1 - y0);
          gfx.fillRect(x1, y0, w - x1, y1 - y0);
          gfx.fillRect(0, y1, w, h - y1);

          // now draw the rectangle
          gfx.setColor(Color.GRAY);
          gfx.drawRect(x0, y0, x1 - x0, y1 - y0);
        }
        break;
      default:
        break;
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return Collections.emptyList();
  }

  @Override
  public Cursor getCursor(Canvas canvas) {
    return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
  }

  private int getHandleSize(Canvas canvas) {
    final var zoom = canvas.getZoomFactor();
    return (int) Math.ceil(HANDLE_SIZE / Math.sqrt(zoom));
  }

  @Override
  public Icon getIcon() {
    return icon;
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    final var code = e.getKeyCode();
    if ((code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT)
        && curAction != IDLE) {
      setMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    keyPressed(canvas, e);
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    final var ch = e.getKeyChar();
    final var selected = canvas.getSelection();
    if ((ch == '\u0008' || ch == '\u007F') && !selected.isEmpty()) {
      final var toRemove = new ArrayList<CanvasObject>();
      for (final var shape : selected.getSelected()) {
        if (shape.canRemove()) {
          toRemove.add(shape);
        }
      }
      if (!toRemove.isEmpty()) {
        e.consume();
        final var model = canvas.getModel();
        canvas.doAction(new ModelRemoveAction(model, toRemove));
        selected.clearSelected();
        repaintArea(canvas);
      }
    } else if (ch == '\u001b' && !selected.isEmpty()) {
      selected.clearSelected();
      repaintArea(canvas);
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, MouseEvent e) {
    setMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
  }

  @Override
  public void mousePressed(Canvas canvas, MouseEvent e) {
    beforePressSelection = new ArrayList<>(canvas.getSelection().getSelected());
    beforePressHandle = canvas.getSelection().getSelectedHandle();
    final var mx = e.getX();
    final var my = e.getY();
    dragStart = Location.create(mx, my);
    dragEffective = false;
    dragEnd = dragStart;
    lastMouseX = mx;
    lastMouseY = my;
    final var selection = canvas.getSelection();
    selection.setHandleSelected(null);

    // see whether user is pressing within an existing handle
    final var halfSize = getHandleSize(canvas) / 2;
    CanvasObject clicked = null;
    for (final var shape : selection.getSelected()) {
      final var handles = shape.getHandles(null);
      for (final var han : handles) {
        final var dx = han.getX() - mx;
        final var dy = han.getY() - my;
        if (dx >= -halfSize && dx <= halfSize && dy >= -halfSize && dy <= halfSize) {
          if (shape.canMoveHandle(han)) {
            curAction = MOVE_HANDLE;
            curGesture = new HandleGesture(han, 0, 0, e.getModifiersEx());
            repaintArea(canvas);
            return;
          } else if (clicked == null) {
            clicked = shape;
          }
        }
      }
    }

    // see whether the user is clicking within a shape
    if (clicked == null) {
      clicked = getObjectAt(canvas.getModel(), e.getX(), e.getY(), false);
    }
    final var shiftPressed = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
    if (clicked != null) {
      if (shiftPressed && selection.isSelected(clicked)) {
        selection.setSelected(clicked, false);
        curAction = IDLE;
      } else {
        if (!shiftPressed && !selection.isSelected(clicked)) {
          selection.clearSelected();
        }
        selection.setSelected(clicked, true);
        selection.setMovingShapes(selection.getSelected(), 0, 0);
        curAction = MOVE_ALL;
      }
      repaintArea(canvas);
      return;
    }

    clicked = getObjectAt(canvas.getModel(), e.getX(), e.getY(), true);
    if (clicked != null && selection.isSelected(clicked)) {
      if (shiftPressed) {
        selection.setSelected(clicked, false);
        curAction = IDLE;
      } else {
        selection.setMovingShapes(selection.getSelected(), 0, 0);
        curAction = MOVE_ALL;
      }
      repaintArea(canvas);
      return;
    }

    if (shiftPressed) {
      curAction = RECT_TOGGLE;
    } else {
      selection.clearSelected();
      curAction = RECT_SELECT;
    }
    repaintArea(canvas);
  }

  @Override
  public void mouseReleased(Canvas canvas, MouseEvent e) {
    beforePressSelection = null;
    beforePressHandle = null;
    setMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());

    final var model = canvas.getModel();
    final var selection = canvas.getSelection();
    final var selected = selection.getSelected();
    final var action = curAction;
    curAction = IDLE;

    if (!dragEffective) {
      final var loc = dragEnd;
      final var o = getObjectAt(model, loc.getX(), loc.getY(), false);
      if (o != null) {
        var han = o.canDeleteHandle(loc);
        if (han != null) {
          selection.setHandleSelected(han);
        } else {
          han = o.canInsertHandle(loc);
          if (han != null) {
            selection.setHandleSelected(han);
          }
        }
      }
    }

    final var start = dragStart;
    final var x1 = e.getX();
    final var y1 = e.getY();
    switch (action) {
      case MOVE_ALL:
        final var moveDelta = selection.getMovingDelta();
        if (dragEffective && !moveDelta.equals(Location.create(0, 0))) {
          canvas.doAction(
              new ModelTranslateAction(model, selected, moveDelta.getX(), moveDelta.getY()));
        }
        break;
      case MOVE_HANDLE:
        final var gesture = curGesture;
        curGesture = null;
        if (dragEffective && gesture != null) {
          final var act = new ModelMoveHandleAction(model, gesture);
          canvas.doAction(act);
          final var result = act.getNewHandle();
          if (result != null) {
            selection.setHandleSelected(result.getObject().canDeleteHandle(result.getLocation()));
          }
        }
        break;
      case RECT_SELECT:
        if (dragEffective) {
          final var bds = Bounds.create(start).add(x1, y1);
          selection.setSelected(canvas.getModel().getObjectsIn(bds), true);
        } else {
          final var clicked = getObjectAt(model, start.getX(), start.getY(), true);
          if (clicked != null) {
            selection.clearSelected();
            selection.setSelected(clicked, true);
          }
        }
        break;
      case RECT_TOGGLE:
        if (dragEffective) {
          final var bds = Bounds.create(start).add(x1, y1);
          selection.toggleSelected(canvas.getModel().getObjectsIn(bds));
        } else {
          final var clicked = getObjectAt(model, start.getX(), start.getY(), true);
          if (clicked != null) selection.setSelected(clicked, !selected.contains(clicked));
        }
        break;
      default:
        break;
    }
    selection.clearDrawsSuppressed();
    repaintArea(canvas);
  }

  private void repaintArea(Canvas canvas) {
    canvas.repaint();
  }

  private void setMouse(Canvas canvas, int mx, int my, int mods) {
    lastMouseX = mx;
    lastMouseY = my;
    final var newEnd = Location.create(mx, my);
    dragEnd = newEnd;

    final var start = dragStart;
    var dx = newEnd.getX() - start.getX();
    var dy = newEnd.getY() - start.getY();
    if (!dragEffective) {
      if (Math.abs(dx) + Math.abs(dy) > DRAG_TOLERANCE) {
        dragEffective = true;
      } else {
        return;
      }
    }

    final var shiftPressed = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
    final var ctrlPressed = (mods & InputEvent.CTRL_DOWN_MASK) != 0;

    switch (curAction) {
      case MOVE_HANDLE:
        final var gesture = curGesture;
        if (ctrlPressed) {
          final var h = gesture.getHandle();
          dx = canvas.snapX(h.getX() + dx) - h.getX();
          dy = canvas.snapY(h.getY() + dy) - h.getY();
        }
        curGesture = new HandleGesture(gesture.getHandle(), dx, dy, mods);
        canvas.getSelection().setHandleGesture(curGesture);
        break;
      case MOVE_ALL:
        if (ctrlPressed) {
          var minX = Integer.MAX_VALUE;
          var minY = Integer.MAX_VALUE;
          for (final var o : canvas.getSelection().getSelected()) {
            for (final var handle : o.getHandles(null)) {
              final var x = handle.getX();
              final var y = handle.getY();
              if (x < minX) minX = x;
              if (y < minY) minY = y;
            }
          }
          dx = canvas.snapX(minX + dx) - minX;
          dy = canvas.snapY(minY + dy) - minY;
        }
        if (shiftPressed) {
          if (Math.abs(dx) > Math.abs(dy)) {
            dy = 0;
          } else {
            dx = 0;
          }
        }
        canvas.getSelection().setMovingDelta(dx, dy);
        break;
      default:
        break;
    }
    repaintArea(canvas);
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    curAction = IDLE;
    canvas.getSelection().clearSelected();
    repaintArea(canvas);
  }

  @Override
  public void toolSelected(Canvas canvas) {
    curAction = IDLE;
    canvas.getSelection().clearSelected();
    repaintArea(canvas);
  }
}
