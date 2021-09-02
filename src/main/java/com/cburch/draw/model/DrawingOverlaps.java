/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
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

class DrawingOverlaps {
  private final Map<CanvasObject, List<CanvasObject>> map;
  private final Set<CanvasObject> untested;

  public DrawingOverlaps() {
    map = new HashMap<>();
    untested = new HashSet<>();
  }

  private void addOverlap(CanvasObject a, CanvasObject b) {
    List<CanvasObject> alist = map.computeIfAbsent(a, k -> new ArrayList<>());
    if (!alist.contains(b)) {
      alist.add(b);
    }
  }

  public void addShape(CanvasObject shape) {
    untested.add(shape);
  }

  private void ensureUpdated() {
    for (CanvasObject o : untested) {
      List<CanvasObject> over = new ArrayList<>();
      for (CanvasObject o2 : map.keySet()) {
        if (o != o2 && o.overlaps(o2)) {
          over.add(o2);
          addOverlap(o2, o);
        }
      }
      map.put(o, over);
    }
    untested.clear();
  }

  public Collection<CanvasObject> getObjectsOverlapping(CanvasObject o) {
    ensureUpdated();

    List<CanvasObject> ret = map.get(o);
    if (ret == null || ret.isEmpty()) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(ret);
    }
  }

  public void invalidateShape(CanvasObject shape) {
    removeShape(shape);
    untested.add(shape);
  }

  public void invalidateShapes(Collection<? extends CanvasObject> shapes) {
    for (CanvasObject o : shapes) {
      invalidateShape(o);
    }
  }

  public void removeShape(CanvasObject shape) {
    untested.remove(shape);
    List<CanvasObject> mapped = map.remove(shape);
    if (mapped != null) {
      for (CanvasObject o : mapped) {
        List<CanvasObject> reverse = map.get(o);
        if (reverse != null) {
          reverse.remove(shape);
        }
      }
    }
  }
}
