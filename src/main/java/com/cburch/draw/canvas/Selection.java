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
      List<CanvasObject> oldSelected;
      oldSelected = new ArrayList<>(selected);
      selected.clear();
      suppressed.clear();
      setHandleSelected(null);
      fireChanged(SelectionEvent.ACTION_REMOVED, oldSelected);
    }
  }

  public void drawSuppressed(Graphics g, CanvasObject shape) {
    String state = suppressed.get(shape);
    if (state.equals(MOVING_HANDLE)) {
      shape.paint(g, curHandleGesture);
    } else if (state.equals(TRANSLATING)) {
      g.translate(moveDx, moveDy);
      shape.paint(g, null);
    }
  }

  private void fireChanged(int action, Collection<CanvasObject> affected) {
    SelectionEvent e = null;
    for (SelectionListener listener : listeners) {
      if (e == null) e = new SelectionEvent(this, action, affected);
      listener.selectionChanged(e);
    }
  }

  public Set<CanvasObject> getDrawsSuppressed() {
    return suppressedView;
  }

  public Location getMovingDelta() {
    return Location.create(moveDx, moveDy);
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
    int action = event.getAction();
    switch (action) {
      case CanvasModelEvent.ACTION_REMOVED:
        Collection<? extends CanvasObject> affected = event.getAffected();
        if (affected != null) {
          selected.removeAll(affected);
          suppressed.keySet().removeAll(affected);
          Handle h = selectedHandle;
          if (h != null && affected.contains(h.getObject())) {
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
        HandleGesture gesture = event.getHandleGesture();
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
    HandleGesture g = curHandleGesture;
    if (g != null) suppressed.remove(g.getHandle().getObject());

    Handle h = gesture.getHandle();
    suppressed.put(h.getObject(), MOVING_HANDLE);
    curHandleGesture = gesture;
  }

  public void setHandleSelected(Handle handle) {
    Handle cur = selectedHandle;
    boolean same = Objects.equals(cur, handle);
    if (!same) {
      selectedHandle = handle;
      curHandleGesture = null;
      Collection<CanvasObject> objs;
      if (handle == null) {
        objs = Collections.emptySet();
      } else {
        objs = Collections.singleton(handle.getObject());
      }
      fireChanged(SelectionEvent.ACTION_HANDLE, objs);
    }
  }

  public void setHidden(Collection<? extends CanvasObject> shapes, boolean value) {
    if (value) {
      for (CanvasObject o : shapes) {
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
    for (CanvasObject o : shapes) {
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
      List<CanvasObject> added;
      added = new ArrayList<>(shapes.size());
      for (CanvasObject shape : shapes) {
        if (selected.add(shape)) {
          added.add(shape);
        }
      }
      if (!added.isEmpty()) {
        fireChanged(SelectionEvent.ACTION_ADDED, added);
      }
    } else {
      List<CanvasObject> removed;
      removed = new ArrayList<>(shapes.size());
      for (CanvasObject shape : shapes) {
        if (selected.remove(shape)) {
          suppressed.remove(shape);
          Handle h = selectedHandle;
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
    List<CanvasObject> added;
    added = new ArrayList<>(shapes.size());
    List<CanvasObject> removed;
    removed = new ArrayList<>(shapes.size());
    for (CanvasObject shape : shapes) {
      if (selected.contains(shape)) {
        selected.remove(shape);
        suppressed.remove(shape);
        Handle h = selectedHandle;
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
