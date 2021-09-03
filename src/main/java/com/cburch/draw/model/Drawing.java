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
import java.util.Set;

public class Drawing implements CanvasModel {
  private final EventSourceWeakSupport<CanvasModelListener> listeners;
  private final ArrayList<CanvasObject> canvasObjects;
  private final DrawingOverlaps overlaps;

  public Drawing() {
    listeners = new EventSourceWeakSupport<>();
    canvasObjects = new ArrayList<>();
    overlaps = new DrawingOverlaps();
  }

  public void addCanvasModelListener(CanvasModelListener l) {
    listeners.add(l);
  }

  public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
    Map<CanvasObject, Integer> indexes;
    indexes = new LinkedHashMap<>();
    int i = index;
    for (CanvasObject shape : shapes) {
      indexes.put(shape, i);
      i++;
    }
    addObjectsHelp(indexes);
  }

  public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
    addObjectsHelp(shapes);
  }

  private void addObjectsHelp(Map<? extends CanvasObject, Integer> shapes) {
    // this is separate method so that subclass can call super.add to either
    // of the add methods, and it won't get redirected into the subclass
    // in calling the other add method
    CanvasModelEvent e = CanvasModelEvent.forAdd(this, shapes.keySet());
    if (!shapes.isEmpty() && isChangeAllowed(e)) {
      for (Map.Entry<? extends CanvasObject, Integer> entry : shapes.entrySet()) {
        CanvasObject shape = entry.getKey();
        int index = entry.getValue();
        canvasObjects.add(index, shape);
        overlaps.addShape(shape);
      }
      fireChanged(e);
    }
  }

  public Handle deleteHandle(Handle handle) {
    CanvasModelEvent e = CanvasModelEvent.forDeleteHandle(this, handle);
    if (isChangeAllowed(e)) {
      CanvasObject o = handle.getObject();
      Handle ret = o.deleteHandle(handle);
      overlaps.invalidateShape(o);
      fireChanged(e);
      return ret;
    } else {
      return null;
    }
  }

  private void fireChanged(CanvasModelEvent e) {
    for (CanvasModelListener listener : listeners) {
      listener.modelChanged(e);
    }
  }

  public List<CanvasObject> getObjectsFromBottom() {
    return Collections.unmodifiableList(canvasObjects);
  }

  public List<CanvasObject> getObjectsFromTop() {
    List<CanvasObject> ret = new ArrayList<>(getObjectsFromBottom());
    Collections.reverse(ret);
    return ret;
  }

  public Collection<CanvasObject> getObjectsIn(Bounds bds) {
    List<CanvasObject> ret = null;
    for (CanvasObject shape : getObjectsFromBottom()) {
      if (bds.contains(shape.getBounds())) {
        if (ret == null) ret = new ArrayList<>();
        ret.add(shape);
      }
    }
    if (ret == null) {
      return Collections.emptyList();
    } else {
      return ret;
    }
  }

  public Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape) {
    return overlaps.getObjectsOverlapping(shape);
  }

  public void insertHandle(Handle desired, Handle previous) {
    CanvasObject obj = desired.getObject();
    CanvasModelEvent e = CanvasModelEvent.forInsertHandle(this, desired);
    if (isChangeAllowed(e)) {
      obj.insertHandle(desired, previous);
      overlaps.invalidateShape(obj);
      fireChanged(e);
    }
  }

  protected boolean isChangeAllowed(CanvasModelEvent e) {
    return true;
  }

  public Handle moveHandle(HandleGesture gesture) {
    CanvasModelEvent e = CanvasModelEvent.forMoveHandle(this, gesture);
    CanvasObject o = gesture.getHandle().getObject();
    if (canvasObjects.contains(o)
        && (gesture.getDeltaX() != 0 || gesture.getDeltaY() != 0)
        && isChangeAllowed(e)) {
      Handle moved = o.moveHandle(gesture);
      gesture.setResultingHandle(moved);
      overlaps.invalidateShape(o);
      fireChanged(e);
      return moved;
    } else {
      return null;
    }
  }

  public void paint(Graphics g, Selection selection) {
    Set<CanvasObject> suppressed = selection.getDrawsSuppressed();
    for (CanvasObject shape : getObjectsFromBottom()) {
      Graphics dup = g.create();
      if (suppressed.contains(shape)) {
        selection.drawSuppressed(dup, shape);
      } else {
        shape.paint(dup, null);
      }
      dup.dispose();
    }
  }

  public void removeCanvasModelListener(CanvasModelListener l) {
    listeners.remove(l);
  }

  public void removeObjects(Collection<? extends CanvasObject> shapes) {
    List<CanvasObject> found = restrict(shapes);
    CanvasModelEvent e = CanvasModelEvent.forRemove(this, found);
    if (!found.isEmpty() && isChangeAllowed(e)) {
      for (CanvasObject shape : found) {
        canvasObjects.remove(shape);
        overlaps.removeShape(shape);
      }
      fireChanged(e);
    }
  }

  public void reorderObjects(List<ReorderRequest> requests) {
    boolean hasEffect = false;
    for (ReorderRequest r : requests) {
      if (r.getFromIndex() != r.getToIndex()) {
        hasEffect = true;
        break;
      }
    }
    CanvasModelEvent e = CanvasModelEvent.forReorder(this, requests);
    if (hasEffect && isChangeAllowed(e)) {
      for (ReorderRequest r : requests) {
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
    ArrayList<CanvasObject> ret;
    ret = new ArrayList<>(shapes.size());
    for (CanvasObject shape : shapes) {
      if (canvasObjects.contains(shape)) {
        ret.add(shape);
      }
    }
    return ret;
  }

  public void setAttributeValues(Map<AttributeMapKey, Object> values) {
    Map<AttributeMapKey, Object> oldValues;
    oldValues = new HashMap<>();
    for (AttributeMapKey key : values.keySet()) {
      @SuppressWarnings("unchecked")
      Attribute<Object> attr = (Attribute<Object>) key.getAttribute();
      Object oldValue = key.getObject().getValue(attr);
      oldValues.put(key, oldValue);
    }
    CanvasModelEvent e = CanvasModelEvent.forChangeAttributes(this, oldValues, values);
    if (isChangeAllowed(e)) {
      for (Map.Entry<AttributeMapKey, Object> entry : values.entrySet()) {
        AttributeMapKey key = entry.getKey();
        CanvasObject shape = key.getObject();
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) key.getAttribute();
        shape.setValue(attr, entry.getValue());
        overlaps.invalidateShape(shape);
      }
      fireChanged(e);
    }
  }

  public void setText(Text text, String value) {
    String oldValue = text.getText();
    CanvasModelEvent e = CanvasModelEvent.forChangeText(this, text, oldValue, value);
    if (canvasObjects.contains(text) && !oldValue.equals(value) && isChangeAllowed(e)) {
      text.setText(value);
      overlaps.invalidateShape(text);
      fireChanged(e);
    }
  }

  public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    List<CanvasObject> found = restrict(shapes);
    CanvasModelEvent e = CanvasModelEvent.forTranslate(this, found, dx, dy);
    if (!found.isEmpty() && (dx != 0 || dy != 0) && isChangeAllowed(e)) {
      for (CanvasObject shape : shapes) {
        shape.translate(dx, dy);
        overlaps.invalidateShape(shape);
      }
      fireChanged(e);
    }
  }
}
