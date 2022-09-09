/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.canvas;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Selection {
  private static final String MOVING_HANDLE = "movingHandle";
  private static final String TRANSLATING = "translating";
  private static final String HIDDEN = "hidden";

  private final List<SelectionListener> listeners;
  private final Set<CanvasObject> selected;
  private final Set<CanvasObject> selectedView;
  private final Map<CanvasObject, String> suppressed;
  private final Set<CanvasObject> suppressedView;
  private Handle selectedHandle;
  private HandleGesture curHandleGesture;
  private int moveDx;
  private int moveDy;

  protected Selection() {
    listeners = new ArrayList<>();
    selected = new HashSet<>();
    suppressed = new HashMap<>();
    selectedView = Collections.unmodifiableSet(selected);
    suppressedView = Collections.unmodifiableSet(suppressed.keySet());
  }

  public void addSelectionListener(SelectionListener l) {
    listeners.add(l);
  }

  public void clearDrawsSuppressed() {
    suppressed.clear();
    curHandleGesture = null;
  }

  public void clearSelected() {
    if (!selected.isEmpty()) {
      final var oldSelected = new ArrayList<>(selected);
      selected.clear();
      suppressed.clear();
      setHandleSelected(null);
      fireChanged(SelectionEvent.ACTION_REMOVED, oldSelected);
    }
  }

  public void drawSuppressed(Graphics g, CanvasObject shape) {
    final var state = suppressed.get(shape);
    if (state.equals(MOVING_HANDLE)) {
      shape.paint(g, curHandleGesture);
    } else if (state.equals(TRANSLATING)) {
      g.translate(moveDx, moveDy);
      shape.paint(g, null);
    }
  }

  private void fireChanged(int action, Collection<CanvasObject> affected) {
    SelectionEvent e = null;
    for (final var listener : listeners) {
      if (e == null) e = new SelectionEvent(this, action, affected);
      listener.selectionChanged(e);
    }
  }

  public Set<CanvasObject> getDrawsSuppressed() {
    return suppressedView;
  }

  public Location getMovingDelta() {
    return Location.create(moveDx, moveDy, false);
  }

  public Set<CanvasObject> getSelected() {
    return selectedView;
  }

  public Handle getSelectedHandle() {
    return selectedHandle;
  }

  public boolean isEmpty() {
    return selected.isEmpty();
  }

  public boolean isSelected(CanvasObject shape) {
    return selected.contains(shape);
  }

  void modelChanged(CanvasModelEvent event) {
    switch (event.getAction()) {
      case CanvasModelEvent.ACTION_REMOVED:
        final var affected = event.getAffected();
        if (affected != null) {
          selected.removeAll(affected);
          suppressed.keySet().removeAll(affected);
          final var handle = selectedHandle;
          if (handle != null && affected.contains(handle.getObject())) {
            setHandleSelected(null);
          }
        }
        break;
      case CanvasModelEvent.ACTION_HANDLE_DELETED:
        if (event.getHandle().equals(selectedHandle)) {
          setHandleSelected(null);
        }
        break;
      case CanvasModelEvent.ACTION_HANDLE_MOVED:
        final var gesture = event.getHandleGesture();
        if (gesture.getHandle().equals(selectedHandle)) {
          setHandleSelected(gesture.getResultingHandle());
        }
        break;
      default:
        break;
    }
  }

  public void removeSelectionListener(SelectionListener l) {
    listeners.remove(l);
  }

  public void setHandleGesture(HandleGesture gesture) {
    final var g = curHandleGesture;
    if (g != null) suppressed.remove(g.getHandle().getObject());

    final var h = gesture.getHandle();
    suppressed.put(h.getObject(), MOVING_HANDLE);
    curHandleGesture = gesture;
  }

  public void setHandleSelected(Handle handle) {
    final var cur = selectedHandle;
    final var same = Objects.equals(cur, handle);
    if (!same) {
      selectedHandle = handle;
      curHandleGesture = null;
      final Collection<CanvasObject> objs =
          (handle == null) ? Collections.emptySet() : Collections.singleton(handle.getObject());
      fireChanged(SelectionEvent.ACTION_HANDLE, objs);
    }
  }

  public void setHidden(Collection<? extends CanvasObject> shapes, boolean value) {
    if (value) {
      for (final var o : shapes) {
        suppressed.put(o, HIDDEN);
      }
    } else {
      suppressed.keySet().removeAll(shapes);
    }
  }

  public void setMovingDelta(int dx, int dy) {
    moveDx = dx;
    moveDy = dy;
  }

  public void setMovingShapes(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    for (final var o : shapes) {
      suppressed.put(o, TRANSLATING);
    }
    moveDx = dx;
    moveDy = dy;
  }

  public void setSelected(CanvasObject shape, boolean value) {
    setSelected(Collections.singleton(shape), value);
  }

  public void setSelected(Collection<CanvasObject> shapes, boolean value) {
    if (value) {
      final var added = new ArrayList<CanvasObject>(shapes.size());
      for (final var shape : shapes) {
        if (selected.add(shape)) {
          added.add(shape);
        }
      }
      if (!added.isEmpty()) {
        fireChanged(SelectionEvent.ACTION_ADDED, added);
      }
    } else {
      final var removed = new ArrayList<CanvasObject>(shapes.size());
      for (final var shape : shapes) {
        if (selected.remove(shape)) {
          suppressed.remove(shape);
          final var h = selectedHandle;
          if (h != null && h.getObject() == shape) setHandleSelected(null);
          removed.add(shape);
        }
      }
      if (!removed.isEmpty()) {
        fireChanged(SelectionEvent.ACTION_REMOVED, removed);
      }
    }
  }

  public void toggleSelected(Collection<CanvasObject> shapes) {
    final var added = new ArrayList<CanvasObject>(shapes.size());
    final var removed = new ArrayList<CanvasObject>(shapes.size());
    for (final var shape : shapes) {
      if (selected.contains(shape)) {
        selected.remove(shape);
        suppressed.remove(shape);
        final var h = selectedHandle;
        if (h != null && h.getObject() == shape) setHandleSelected(null);
        removed.add(shape);
      } else {
        selected.add(shape);
        added.add(shape);
      }
    }
    if (!removed.isEmpty()) {
      fireChanged(SelectionEvent.ACTION_REMOVED, removed);
    }
    if (!added.isEmpty()) {
      fireChanged(SelectionEvent.ACTION_ADDED, added);
    }
  }
}
