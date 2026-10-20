/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Drawing implements CanvasModel {
  private final EventSourceWeakSupport<CanvasModelListener> listeners;
  private final ArrayList<CanvasObject> canvasObjects;
  private final DrawingOverlaps overlaps;

  public Drawing() {
    listeners = new EventSourceWeakSupport<>();
    canvasObjects = new ArrayList<>();
    overlaps = new DrawingOverlaps();
  }

  @Override
  public void addCanvasModelListener(CanvasModelListener l) {
    listeners.add(l);
  }

  @Override
  public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
    final var indexes = new LinkedHashMap<CanvasObject, Integer>();
    var i = index;
    for (final var shape : shapes) {
      indexes.put(shape, i);
      i++;
    }
    addObjectsHelp(indexes);
  }

  @Override
  public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
    addObjectsHelp(shapes);
  }

  private void addObjectsHelp(Map<? extends CanvasObject, Integer> shapes) {
    // this is separate method so that subclass can call super.add to either
    // of the add methods, and it won't get redirected into the subclass
    // in calling the other add method
    final var e = CanvasModelEvent.forAdd(this, shapes.keySet());
    if (!shapes.isEmpty() && isChangeAllowed(e)) {
      for (final var entry : shapes.entrySet()) {
        final var shape = entry.getKey();
        final var index = entry.getValue();
        canvasObjects.add(index, shape);
        overlaps.addShape(shape);
      }
      fireChanged(e);
    }
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    final var e = CanvasModelEvent.forDeleteHandle(this, handle);
    if (!isChangeAllowed(e)) return null;

    final var o = handle.getObject();
    final var ret = o.deleteHandle(handle);
    overlaps.invalidateShape(o);
    fireChanged(e);
    return ret;
  }

  private void fireChanged(CanvasModelEvent e) {
    for (final var listener : listeners) {
      listener.modelChanged(e);
    }
  }

  @Override
  public List<CanvasObject> getObjectsFromBottom() {
    return Collections.unmodifiableList(canvasObjects);
  }

  @Override
  public List<CanvasObject> getObjectsFromTop() {
    final var ret = new ArrayList<>(getObjectsFromBottom());
    Collections.reverse(ret);
    return ret;
  }

  @Override
  public Collection<CanvasObject> getObjectsIn(Bounds bds) {
    List<CanvasObject> ret = null;
    for (final var shape : getObjectsFromBottom()) {
      if (bds.contains(shape.getBounds())) {
        if (ret == null) ret = new ArrayList<>();
        ret.add(shape);
      }
    }

    return (ret == null) ? Collections.emptyList() : ret;
  }

  @Override
  public Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape) {
    return overlaps.getObjectsOverlapping(shape);
  }

  @Override
  public void insertHandle(Handle desired, Handle previous) {
    final var obj = desired.getObject();
    final var e = CanvasModelEvent.forInsertHandle(this, desired);
    if (isChangeAllowed(e)) {
      obj.insertHandle(desired, previous);
      overlaps.invalidateShape(obj);
      fireChanged(e);
    }
  }

  protected boolean isChangeAllowed(CanvasModelEvent e) {
    return true;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    final var e = CanvasModelEvent.forMoveHandle(this, gesture);
    final var o = gesture.getHandle().getObject();
    if (canvasObjects.contains(o)
        && (gesture.getDeltaX() != 0 || gesture.getDeltaY() != 0)
        && isChangeAllowed(e)) {
      final var moved = o.moveHandle(gesture);
      gesture.setResultingHandle(moved);
      overlaps.invalidateShape(o);
      fireChanged(e);
      return moved;
    } else {
      return null;
    }
  }

  @Override
  public void paint(Graphics g, Selection selection) {
    final var suppressed = selection.getDrawsSuppressed();
    for (final var shape : getObjectsFromBottom()) {
      final var dup = g.create();
      if (suppressed.contains(shape)) {
        selection.drawSuppressed(dup, shape);
      } else {
        shape.paint(dup, null);
      }
      dup.dispose();
    }
  }

  @Override
  public void removeCanvasModelListener(CanvasModelListener l) {
    listeners.remove(l);
  }

  @Override
  public void removeObjects(Collection<? extends CanvasObject> shapes) {
    final var found = restrict(shapes);
    final var e = CanvasModelEvent.forRemove(this, found);
    if (!found.isEmpty() && isChangeAllowed(e)) {
      for (final var shape : found) {
        canvasObjects.remove(shape);
        overlaps.removeShape(shape);
      }
      fireChanged(e);
    }
  }

  @Override
  public void reorderObjects(List<ReorderRequest> requests) {
    var hasEffect = false;
    for (final var r : requests) {
      if (r.getFromIndex() != r.getToIndex()) {
        hasEffect = true;
        break;
      }
    }
    final var e = CanvasModelEvent.forReorder(this, requests);
    if (hasEffect && isChangeAllowed(e)) {
      for (final var r : requests) {
        if (canvasObjects.get(r.getFromIndex()) != r.getObject()) {
          throw new IllegalArgumentException(
              "object not present" + " at indicated index: " + r.getFromIndex());
        }
        canvasObjects.remove(r.getFromIndex());
        canvasObjects.add(r.getToIndex(), r.getObject());
      }
      fireChanged(e);
    }
  }

  private ArrayList<CanvasObject> restrict(Collection<? extends CanvasObject> shapes) {
    final var ret = new ArrayList<CanvasObject>(shapes.size());
    for (final var shape : shapes) {
      if (canvasObjects.contains(shape)) {
        ret.add(shape);
      }
    }
    return ret;
  }

  @Override
  public void setAttributeValues(Map<AttributeMapKey, Object> values) {
    final var oldValues = new HashMap<AttributeMapKey, Object>();
    for (final var key : values.keySet()) {
      @SuppressWarnings("unchecked")
      final var attr = (Attribute<Object>) key.getAttribute();
      final var oldValue = key.getObject().getValue(attr);
      oldValues.put(key, oldValue);
    }
    final var e = CanvasModelEvent.forChangeAttributes(this, oldValues, values);
    if (isChangeAllowed(e)) {
      for (final var entry : values.entrySet()) {
        final var key = entry.getKey();
        final var shape = key.getObject();
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) key.getAttribute();
        shape.setValue(attr, entry.getValue());
        overlaps.invalidateShape(shape);
      }
      fireChanged(e);
    }
  }

  @Override
  public void setText(Text text, String value) {
    final var oldValue = text.getText();
    final var e = CanvasModelEvent.forChangeText(this, text, oldValue, value);
    if (canvasObjects.contains(text) && !oldValue.equals(value) && isChangeAllowed(e)) {
      text.setText(value);
      overlaps.invalidateShape(text);
      fireChanged(e);
    }
  }

  @Override
  public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    final var found = restrict(shapes);
    final var e = CanvasModelEvent.forTranslate(this, found);
    if (!found.isEmpty() && (dx != 0 || dy != 0) && isChangeAllowed(e)) {
      for (final var shape : shapes) {
        shape.translate(dx, dy);
        overlaps.invalidateShape(shape);
      }
      fireChanged(e);
    }
  }
}
