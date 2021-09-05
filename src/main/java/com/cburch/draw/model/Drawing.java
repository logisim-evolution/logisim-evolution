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
import lombok.val;

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
    val indexes = new LinkedHashMap<CanvasObject, Integer>();
    var i = index;
    for (val shape : shapes) {
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
    val event = CanvasModelEvent.forAdd(this, shapes.keySet());
    if (!shapes.isEmpty() && isChangeAllowed(event)) {
      for (val entry : shapes.entrySet()) {
        val shape = entry.getKey();
        val index = entry.getValue();
        canvasObjects.add(index, shape);
        overlaps.addShape(shape);
      }
      fireChanged(event);
    }
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    val eve = CanvasModelEvent.forDeleteHandle(this, handle);
    if (isChangeAllowed(eve)) {
      val obj = handle.getObject();
      val ret = obj.deleteHandle(handle);
      overlaps.invalidateShape(obj);
      fireChanged(eve);
      return ret;
    }
    return null;
  }

  private void fireChanged(CanvasModelEvent e) {
    for (val listener : listeners) listener.modelChanged(e);
  }

  @Override
  public List<CanvasObject> getObjectsFromBottom() {
    return Collections.unmodifiableList(canvasObjects);
  }

  @Override
  public List<CanvasObject> getObjectsFromTop() {
    val ret = new ArrayList<>(getObjectsFromBottom());
    Collections.reverse(ret);
    return ret;
  }

  @Override
  public Collection<CanvasObject> getObjectsIn(Bounds bds) {
    List<CanvasObject> ret = null;
    for (CanvasObject shape : getObjectsFromBottom()) {
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
    val obj = desired.getObject();
    val event = CanvasModelEvent.forInsertHandle(this, desired);
    if (isChangeAllowed(event)) {
      obj.insertHandle(desired, previous);
      overlaps.invalidateShape(obj);
      fireChanged(event);
    }
  }

  protected boolean isChangeAllowed(CanvasModelEvent e) {
    return true;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    val event = CanvasModelEvent.forMoveHandle(this, gesture);
    val obj = gesture.getHandle().getObject();
    if (canvasObjects.contains(obj)
        && (gesture.getDeltaX() != 0 || gesture.getDeltaY() != 0)
        && isChangeAllowed(event)) {
      Handle moved = obj.moveHandle(gesture);
      gesture.setResultingHandle(moved);
      overlaps.invalidateShape(obj);
      fireChanged(event);
      return moved;
    }
    return null;
  }

  @Override
  public void paint(Graphics gfx, Selection selection) {
    val suppressed = selection.getDrawsSuppressed();
    for (val shape : getObjectsFromBottom()) {
      val dup = gfx.create();
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
    val found = restrict(shapes);
    val event = CanvasModelEvent.forRemove(this, found);
    if (!found.isEmpty() && isChangeAllowed(event)) {
      for (val shape : found) {
        canvasObjects.remove(shape);
        overlaps.removeShape(shape);
      }
      fireChanged(event);
    }
  }

  @Override
  public void reorderObjects(List<ReorderRequest> requests) {
    var hasEffect = false;
    for (val req : requests) {
      if (req.getFromIndex() != req.getToIndex()) {
        hasEffect = true;
        break;
      }
    }
    val event = CanvasModelEvent.forReorder(this, requests);
    if (hasEffect && isChangeAllowed(event)) {
      for (val req : requests) {
        if (canvasObjects.get(req.getFromIndex()) != req.getObject()) {
          throw new IllegalArgumentException(
              "object not present at indicated index: " + req.getFromIndex());
        }
        canvasObjects.remove(req.getFromIndex());
        canvasObjects.add(req.getToIndex(), req.getObject());
      }
      fireChanged(event);
    }
  }

  private ArrayList<CanvasObject> restrict(Collection<? extends CanvasObject> shapes) {
    val ret = new ArrayList<CanvasObject>(shapes.size());
    for (val shape : shapes) {
      if (canvasObjects.contains(shape)) ret.add(shape);
    }
    return ret;
  }

  @Override
  public void setAttributeValues(Map<AttributeMapKey, Object> values) {
    val oldValues = new HashMap<AttributeMapKey, Object>();
    for (val key : values.keySet()) {
      @SuppressWarnings("unchecked")
      Attribute<Object> attr = (Attribute<Object>) key.getAttribute();
      val oldValue = key.getObject().getValue(attr);
      oldValues.put(key, oldValue);
    }
    val event = CanvasModelEvent.forChangeAttributes(this, oldValues, values);
    if (isChangeAllowed(event)) {
      for (val entry : values.entrySet()) {
        val key = entry.getKey();
        val shape = key.getObject();
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) key.getAttribute();
        shape.setValue(attr, entry.getValue());
        overlaps.invalidateShape(shape);
      }
      fireChanged(event);
    }
  }

  @Override
  public void setText(Text text, String value) {
    val oldValue = text.getText();
    val event = CanvasModelEvent.forChangeText(this, text, oldValue, value);
    if (canvasObjects.contains(text) && !oldValue.equals(value) && isChangeAllowed(event)) {
      text.setText(value);
      overlaps.invalidateShape(text);
      fireChanged(event);
    }
  }

  @Override
  public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    val found = restrict(shapes);
    val event = CanvasModelEvent.forTranslate(this, found, dx, dy);
    if (!found.isEmpty() && (dx != 0 || dy != 0) && isChangeAllowed(event)) {
      for (val shape : shapes) {
        shape.translate(dx, dy);
        overlaps.invalidateShape(shape);
      }
      fireChanged(event);
    }
  }
}
