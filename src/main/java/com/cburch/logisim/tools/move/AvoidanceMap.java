/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import lombok.val;

class AvoidanceMap {
  static AvoidanceMap create(Collection<Component> elements, int dx, int dy) {
    val ret = new AvoidanceMap(new HashMap<>());
    ret.markAll(elements, dx, dy);
    return ret;
  }

  private final HashMap<Location, String> avoid;

  private AvoidanceMap(HashMap<Location, String> map) {
    avoid = map;
  }

  public AvoidanceMap cloneMap() {
    return new AvoidanceMap(new HashMap<>(avoid));
  }

  public Object get(Location loc) {
    return avoid.get(loc);
  }

  public void markAll(Collection<Component> elements, int dx, int dy) {
    // first we go through the components, saying that we should not
    // intersect with any point that lies within a component
    for (val el : elements) {
      if (el instanceof Wire) {
        markWire((Wire) el, dx, dy);
      } else {
        markComponent(el, dx, dy);
      }
    }
  }

  public void markComponent(Component comp, int dx, int dy) {
    val avoid = this.avoid;
    val translated = dx != 0 || dy != 0;
    val bds = comp.getBounds();
    var x0 = bds.getX() + dx;
    var y0 = bds.getY() + dy;
    val x1 = x0 + bds.getWidth();
    val y1 = y0 + bds.getHeight();
    x0 += 9 - (x0 + 9) % 10;
    y0 += 9 - (y0 + 9) % 10;
    for (var x = x0; x <= x1; x += 10) {
      for (var y = y0; y <= y1; y += 10) {
        val loc = Location.create(x, y);
        // loc is most likely in the component, so go ahead and
        // put it into the map as if it is - and in the rare event
        // that loc isn't in the component, we can remove it.
        val prev = avoid.put(loc, Connector.ALLOW_NEITHER);
        if (!Connector.ALLOW_NEITHER.equals(prev)) {
          val baseLoc = translated ? loc.translate(-dx, -dy) : loc;
          if (!comp.contains(baseLoc)) {
            if (prev == null) {
              avoid.remove(loc);
            } else {
              avoid.put(loc, prev);
            }
          }
        }
      }
    }
  }

  public void markWire(Wire w, int dx, int dy) {
    val avoid = this.avoid;
    val translated = dx != 0 || dy != 0;
    var loc0 = w.getEnd0();
    var loc1 = w.getEnd1();
    if (translated) {
      loc0 = loc0.translate(dx, dy);
      loc1 = loc1.translate(dx, dy);
    }
    avoid.put(loc0, Connector.ALLOW_NEITHER);
    avoid.put(loc1, Connector.ALLOW_NEITHER);
    val x0 = loc0.getX();
    val y0 = loc0.getY();
    val x1 = loc1.getX();
    val y1 = loc1.getY();
    if (x0 == x1) { // vertical wire
      for (val loc : Wire.create(loc0, loc1)) {
        Object prev = avoid.put(loc, Connector.ALLOW_HORIZONTAL);
        if (prev == Connector.ALLOW_NEITHER || prev == Connector.ALLOW_VERTICAL) {
          avoid.put(loc, Connector.ALLOW_NEITHER);
        }
      }
    } else if (y0 == y1) { // horizontal wire
      for (val loc : Wire.create(loc0, loc1)) {
        Object prev = avoid.put(loc, Connector.ALLOW_VERTICAL);
        if (prev == Connector.ALLOW_NEITHER || prev == Connector.ALLOW_HORIZONTAL) {
          avoid.put(loc, Connector.ALLOW_NEITHER);
        }
      }
    } else { // diagonal - shouldn't happen
      throw new RuntimeException("diagonal wires not supported");
    }
  }

  public void print(PrintStream stream) {
    val list = new ArrayList<Location>(avoid.keySet());
    Collections.sort(list);
    for (val location : list) {
      stream.println(location + ": " + avoid.get(location));
    }
  }

  public void unmarkLocation(Location loc) {
    avoid.remove(loc);
  }

  public void unmarkWire(Wire w, Location deletedEnd, Set<Location> unmarkable) {
    val loc0 = w.getEnd0();
    val loc1 = w.getEnd1();
    if (unmarkable == null || unmarkable.contains(deletedEnd)) {
      avoid.remove(deletedEnd);
    }
    val x0 = loc0.getX();
    val y0 = loc0.getY();
    val x1 = loc1.getX();
    val y1 = loc1.getY();
    if (x0 == x1) { // vertical wire
      for (val loc : w) {
        if (unmarkable == null || unmarkable.contains(deletedEnd)) {
          Object prev = avoid.remove(loc);
          if (prev != Connector.ALLOW_HORIZONTAL && prev != null) {
            avoid.put(loc, Connector.ALLOW_VERTICAL);
          }
        }
      }
    } else if (y0 == y1) { // horizontal wire
      for (val loc : w) {
        if (unmarkable == null || unmarkable.contains(deletedEnd)) {
          Object prev = avoid.remove(loc);
          if (prev != Connector.ALLOW_VERTICAL && prev != null) {
            avoid.put(loc, Connector.ALLOW_HORIZONTAL);
          }
        }
      }
    } else { // diagonal - shouldn't happen
      throw new RuntimeException("diagonal wires not supported");
    }
  }
}
