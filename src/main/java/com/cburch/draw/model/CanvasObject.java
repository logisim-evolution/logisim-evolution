/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.List;

public interface CanvasObject {
  Handle canDeleteHandle(Location desired);

  Handle canInsertHandle(Location desired);

  boolean canMoveHandle(Handle handle);

  boolean canRemove();

  CanvasObject clone();

  boolean contains(Location loc, boolean assumeFilled);

  Handle deleteHandle(Handle handle);

  AttributeSet getAttributeSet();

  Bounds getBounds();

  String getDisplayName();

  String getDisplayNameAndLabel();

  List<Handle> getHandles(HandleGesture gesture);

  <V> V getValue(Attribute<V> attr);

  void insertHandle(Handle desired, Handle previous);

  boolean matches(CanvasObject other);

  int matchesHashCode();

  Handle moveHandle(HandleGesture gesture);

  boolean overlaps(CanvasObject other);

  void paint(Graphics g, HandleGesture gesture);

  <V> void setValue(Attribute<V> attr, V value);

  void translate(int dx, int dy);
}
