/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.util;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ZOrder {
  private ZOrder() {
    // dummy
  }

  private static int getIndex(CanvasObject query, List<CanvasObject> objs) {
    var index = -1;
    for (final var o : objs) {
      index++;
      if (o == query) return index;
    }
    return -1;
  }

  // returns first object above query in the z-order that overlaps query
  public static CanvasObject getObjectAbove(CanvasObject query, CanvasModel model, Collection<? extends CanvasObject> ignore) {
    return getPrevious(query, model.getObjectsFromTop(), model, ignore);
  }

  // returns first object below query in the z-order that overlaps query
  public static CanvasObject getObjectBelow(CanvasObject query, CanvasModel model, Collection<? extends CanvasObject> ignore) {
    return getPrevious(query, model.getObjectsFromBottom(), model, ignore);
  }

  private static CanvasObject getPrevious(CanvasObject query, List<CanvasObject> objs, CanvasModel model, Collection<? extends CanvasObject> ignore) {
    var index = getIndex(query, objs);
    if (index > 0) {
      final var set = toSet(model.getObjectsOverlapping(query));
      final var it = objs.listIterator(index);
      while (it.hasPrevious()) {
        final var o = it.previous();
        if (set.contains(o) && !ignore.contains(o))
          return o;
      }
    }
    return null;
  }

  public static int getZIndex(CanvasObject query, CanvasModel model) {
    // returns 0 for bottommost element, large number for topmost
    return getIndex(query, model.getObjectsFromBottom());
  }

  public static Map<CanvasObject, Integer> getZIndex(Collection<? extends CanvasObject> query, CanvasModel model) {
    // returns 0 for bottommost element, large number for topmost, ordered
    // from the bottom up.
    if (query == null) return Collections.emptyMap();

    final var querySet = toSet(query);
    final var ret = new LinkedHashMap<CanvasObject, Integer>(query.size());
    var z = -1;
    for (final var o : model.getObjectsFromBottom()) {
      z++;
      if (querySet.contains(o)) {
        ret.put(o, z);
      }
    }
    return ret;
  }

  public static <E extends CanvasObject> List<E> sortBottomFirst(Collection<E> objects, CanvasModel model) {
    return sortXFirst(objects, model, model.getObjectsFromTop());
  }

  public static <E extends CanvasObject> List<E> sortTopFirst(Collection<E> objects, CanvasModel model) {
    return sortXFirst(objects, model, model.getObjectsFromBottom());
  }

  private static <E extends CanvasObject> List<E> sortXFirst(Collection<E> objects, CanvasModel model, Collection<CanvasObject> objs) {
    Set<E> set = toSet(objects);
    List<E> ret = new ArrayList<>(objects.size());
    for (final var o : objs) {
      if (set.contains(o)) {
        @SuppressWarnings("unchecked")
        E toAdd = (E) o;
        ret.add(toAdd);
      }
    }
    return ret;
  }

  private static <E> Set<E> toSet(Collection<E> objects) {
    return (objects instanceof Set)
            ? (Set<E>) objects
            : new HashSet<>(objects);
  }
}
