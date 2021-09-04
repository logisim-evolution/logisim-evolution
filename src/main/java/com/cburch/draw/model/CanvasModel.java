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
  public void addCanvasModelListener(CanvasModelListener l);

  // methods that alter the model
  public void addObjects(int index, Collection<? extends CanvasObject> shapes);

  public void addObjects(Map<? extends CanvasObject, Integer> shapes);

  public Handle deleteHandle(Handle handle);

  public List<CanvasObject> getObjectsFromBottom();

  public List<CanvasObject> getObjectsFromTop();

  public Collection<CanvasObject> getObjectsIn(Bounds bds);

  public Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape);

  public void insertHandle(Handle desired, Handle previous);

  public Handle moveHandle(HandleGesture gesture);

  // methods that don't change any data in the model
  public void paint(Graphics g, Selection selection);

  public void removeCanvasModelListener(CanvasModelListener l);

  public void removeObjects(Collection<? extends CanvasObject> shapes);

  public void reorderObjects(List<ReorderRequest> requests);

  public void setAttributeValues(Map<AttributeMapKey, Object> values);

  public void setText(Text text, String value);

  public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy);
}
