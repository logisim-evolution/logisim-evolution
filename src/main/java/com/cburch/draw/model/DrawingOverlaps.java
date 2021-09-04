/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.val;

class DrawingOverlaps {
  private final Map<CanvasObject, List<CanvasObject>> map;
  private final Set<CanvasObject> untested;

  public DrawingOverlaps() {
    map = new HashMap<>();
    untested = new HashSet<>();
  }

  private void addOverlap(CanvasObject a, CanvasObject b) {
    val alist = map.computeIfAbsent(a, k -> new ArrayList<>());
    if (!alist.contains(b)) alist.add(b);
  }

  public void addShape(CanvasObject shape) {
    untested.add(shape);
  }

  private void ensureUpdated() {
    for (val obj : untested) {
      val over = new ArrayList<CanvasObject>();
      for (val obj2 : map.keySet()) {
        if (obj != obj2 && obj.overlaps(obj2)) {
          over.add(obj2);
          addOverlap(obj2, obj);
        }
      }
      map.put(obj, over);
    }
    untested.clear();
  }

  public Collection<CanvasObject> getObjectsOverlapping(CanvasObject o) {
    ensureUpdated();

    val ret = map.get(o);
    return (ret == null || ret.isEmpty())
        ? Collections.emptyList()
        : Collections.unmodifiableList(ret);
  }

  public void invalidateShape(CanvasObject shape) {
    removeShape(shape);
    untested.add(shape);
  }

  public void invalidateShapes(Collection<? extends CanvasObject> shapes) {
    for (val obj : shapes) invalidateShape(obj);
  }

  public void removeShape(CanvasObject shape) {
    untested.remove(shape);
    val mapped = map.remove(shape);
    if (mapped != null) {
      for (val obj : mapped) {
        val reverse = map.get(obj);
        if (reverse != null) reverse.remove(shape);
      }
    }
  }
}
