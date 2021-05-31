/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class ZOrder {
  private ZOrder() {}

  private static int getIndex(CanvasObject query, List<CanvasObject> objs) {
    int index = -1;
    for (CanvasObject o : objs) {
      index++;
      if (o == query) return index;
    }
    return -1;
  }

  // returns first object above query in the z-order that overlaps query
  public static CanvasObject getObjectAbove(
      CanvasObject query, CanvasModel model, Collection<? extends CanvasObject> ignore) {
    return getPrevious(query, model.getObjectsFromTop(), model, ignore);
  }

  // returns first object below query in the z-order that overlaps query
  public static CanvasObject getObjectBelow(
      CanvasObject query, CanvasModel model, Collection<? extends CanvasObject> ignore) {
    return getPrevious(query, model.getObjectsFromBottom(), model, ignore);
  }

  private static CanvasObject getPrevious(
      CanvasObject query,
      List<CanvasObject> objs,
      CanvasModel model,
      Collection<? extends CanvasObject> ignore) {
    int index = getIndex(query, objs);
    if (index > 0) {
      Set<CanvasObject> set = toSet(model.getObjectsOverlapping(query));
      ListIterator<CanvasObject> it = objs.listIterator(index);
      while (it.hasPrevious()) {
        CanvasObject o = it.previous();
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

  public static Map<CanvasObject, Integer> getZIndex(
      Collection<? extends CanvasObject> query, CanvasModel model) {
    // returns 0 for bottommost element, large number for topmost, ordered
    // from the bottom up.
    if (query == null) return Collections.emptyMap();

    Set<? extends CanvasObject> querySet = toSet(query);
    Map<CanvasObject, Integer> ret;
    ret = new LinkedHashMap<>(query.size());
    int z = -1;
    for (CanvasObject o : model.getObjectsFromBottom()) {
      z++;
      if (querySet.contains(o)) {
        ret.put(o, z);
      }
    }
    return ret;
  }

  public static <E extends CanvasObject> List<E> sortBottomFirst(
      Collection<E> objects, CanvasModel model) {
    return sortXFirst(objects, model, model.getObjectsFromTop());
  }

  public static <E extends CanvasObject> List<E> sortTopFirst(
      Collection<E> objects, CanvasModel model) {
    return sortXFirst(objects, model, model.getObjectsFromBottom());
  }

  private static <E extends CanvasObject> List<E> sortXFirst(
      Collection<E> objects, CanvasModel model, Collection<CanvasObject> objs) {
    Set<E> set = toSet(objects);
    List<E> ret = new ArrayList<>(objects.size());
    for (CanvasObject o : objs) {
      if (set.contains(o)) {
        @SuppressWarnings("unchecked")
        E toAdd = (E) o;
        ret.add(toAdd);
      }
    }
    return ret;
  }

  private static <E> Set<E> toSet(Collection<E> objects) {
    if (objects instanceof Set) {
      return (Set<E>) objects;
    } else {
      return new HashSet<>(objects);
    }
  }
}
