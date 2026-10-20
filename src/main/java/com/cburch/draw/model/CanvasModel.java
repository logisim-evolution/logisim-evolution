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
import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CanvasModel {
  // listener methods
  void addCanvasModelListener(CanvasModelListener l);

  // methods that alter the model
  void addObjects(int index, Collection<? extends CanvasObject> shapes);

  void addObjects(Map<? extends CanvasObject, Integer> shapes);

  Handle deleteHandle(Handle handle);

  List<CanvasObject> getObjectsFromBottom();

  List<CanvasObject> getObjectsFromTop();

  Collection<CanvasObject> getObjectsIn(Bounds bds);

  Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape);

  void insertHandle(Handle desired, Handle previous);

  Handle moveHandle(HandleGesture gesture);

  // methods that don't change any data in the model
  void paint(Graphics g, Selection selection);

  void removeCanvasModelListener(CanvasModelListener l);

  void removeObjects(Collection<? extends CanvasObject> shapes);

  void reorderObjects(List<ReorderRequest> requests);

  void setAttributeValues(Map<AttributeMapKey, Object> values);

  void setText(Text text, String value);

  void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy);
}
