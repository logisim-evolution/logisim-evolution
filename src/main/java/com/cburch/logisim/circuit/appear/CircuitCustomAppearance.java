/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Drawing;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.draw.model.ReorderRequest;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.data.Bounds;

public class CircuitCustomAppearance extends Drawing {

  private final CircuitAppearance parent;

  public CircuitCustomAppearance(CircuitAppearance parent) {
    this.parent = parent;
  }

  @Override
  public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
    parent.addObjects(index, shapes);
  }

  @Override
  public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
    parent.addObjects(shapes);
  }

  @Override
  public List<CanvasObject> getObjectsFromBottom() {
    return parent.getCustomObjectsFromBottom();
  }

  @Override
  public List<CanvasObject> getObjectsFromTop() {
    final var ret = new ArrayList<>(getObjectsFromBottom());
    Collections.reverse(ret);
    return ret;
  }

  @Override
  public void removeObjects(Collection<? extends CanvasObject> shapes) {
    parent.removeObjects(shapes);
  }

  @Override
  public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    parent.translateObjects(shapes, dx, dy);
  }

  @Override
  public void addCanvasModelListener(CanvasModelListener l) {
    parent.addCanvasModelListener(l);
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    return parent.deleteHandle(handle);
  }

  @Override
  public Collection<CanvasObject> getObjectsIn(Bounds bds) {
    return parent.getObjectsIn(bds);
  }

  @Override
  public Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape) {
    return parent.getObjectsOverlapping(shape);
  }

  @Override
  public void insertHandle(Handle desired, Handle previous) {
    parent.insertHandle(desired, previous);
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    return parent.moveHandle(gesture);
  }

  @Override
  public void removeCanvasModelListener(CanvasModelListener l) {
    parent.removeCanvasModelListener(l);
  }

  @Override
  public void reorderObjects(List<ReorderRequest> requests) {
    parent.reorderObjects(requests);
  }

  @Override
  public void setAttributeValues(Map<AttributeMapKey, Object> values) {
    parent.setAttributeValues(values);
  }

  @Override
  public void setText(Text text, String value) {
    parent.setText(text, value);
  }
}
