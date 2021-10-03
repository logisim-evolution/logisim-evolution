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

public final class AvoidanceMap {

  private final HashMap<Location, String> avoidanceMap;

  private AvoidanceMap(HashMap<Location, String> map) {
    avoidanceMap = map;
  }

  static AvoidanceMap create(Collection<Component> elements, int dx, int dy) {
    final var ret = new AvoidanceMap(new HashMap<>());
    ret.markAll(elements, dx, dy);
    return ret;
  }

  public AvoidanceMap cloneMap() {
    return new AvoidanceMap(new HashMap<>(avoidanceMap));
  }

  public Object get(Location loc) {
    return avoidanceMap.get(loc);
  }

  public void markAll(Collection<Component> elements, int dx, int dy) {
    // first we go through the components, saying that we should not
    // intersect with any point that lies within a component
    for (final var element : elements) {
      if (element instanceof Wire wire) {
        markWire(wire, dx, dy);
      } else {
        markComponent(element, dx, dy);
      }
    }
  }

  public void markComponent(Component comp, int dx, int dy) {
    final var avoid = this.avoidanceMap;
    final var translated = dx != 0 || dy != 0;
    final var bds = comp.getBounds();
    var x0 = bds.getX() + dx;
    var y0 = bds.getY() + dy;
    final var x1 = x0 + bds.getWidth();
    final var y1 = y0 + bds.getHeight();
    x0 += 9 - (x0 + 9) % 10;
    y0 += 9 - (y0 + 9) % 10;
    for (var x = x0; x <= x1; x += 10) {
      for (var y = y0; y <= y1; y += 10) {
        final var loc = Location.create(x, y);
        // loc is most likely in the component, so go ahead and
        // put it into the map as if it is - and in the rare event
        // that loc isn't in the component, we can remove it.
        final var prev = avoid.put(loc, Connector.ALLOW_NEITHER);
        if (!Connector.ALLOW_NEITHER.equals(prev)) {
          final var baseLoc = translated ? loc.translate(-dx, -dy) : loc;
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
    final var avoid = this.avoidanceMap;
    final var translated = dx != 0 || dy != 0;
    var loc0 = w.getEnd0();
    var loc1 = w.getEnd1();
    if (translated) {
      loc0 = loc0.translate(dx, dy);
      loc1 = loc1.translate(dx, dy);
    }
    avoid.put(loc0, Connector.ALLOW_NEITHER);
    avoid.put(loc1, Connector.ALLOW_NEITHER);
    final var x0 = loc0.getX();
    final var y0 = loc0.getY();
    final var x1 = loc1.getX();
    final var y1 = loc1.getY();
    if (x0 == x1) {
      // vertical wire
      for (final var loc : Wire.create(loc0, loc1)) {
        final var prev = avoid.put(loc, Connector.ALLOW_HORIZONTAL);
        if (Connector.ALLOW_NEITHER.equals(prev) || Connector.ALLOW_VERTICAL.equals(prev)) {
          avoid.put(loc, Connector.ALLOW_NEITHER);
        }
      }
    } else if (y0 == y1) {
      // horizontal wire
      for (final var loc : Wire.create(loc0, loc1)) {
        final var prev = avoid.put(loc, Connector.ALLOW_VERTICAL);
        if (Connector.ALLOW_NEITHER.equals(prev) || Connector.ALLOW_HORIZONTAL.equals(prev)) {
          avoid.put(loc, Connector.ALLOW_NEITHER);
        }
      }
    } else {
      // diagonal - shouldn't happen
      throw new RuntimeException("Diagonal wires are not supported.");
    }
  }

  public void print(PrintStream stream) {
    ArrayList<Location> list = new ArrayList<>(avoidanceMap.keySet());
    Collections.sort(list);
    for (Location location : list) {
      stream.println(location + ": " + avoidanceMap.get(location));
    }
  }

  public void unmarkLocation(Location loc) {
    avoidanceMap.remove(loc);
  }

  public void unmarkWire(Wire w, Location deletedEnd, Set<Location> unmarkable) {
    final var loc0 = w.getEnd0();
    final var loc1 = w.getEnd1();
    if (unmarkable == null || unmarkable.contains(deletedEnd)) {
      avoidanceMap.remove(deletedEnd);
    }
    final var x0 = loc0.getX();
    final var y0 = loc0.getY();
    final var x1 = loc1.getX();
    final var y1 = loc1.getY();
    if (x0 == x1) {
      // vertical wire
      for (final var loc : w) {
        if (unmarkable == null || unmarkable.contains(deletedEnd)) {
          final var prev = avoidanceMap.remove(loc);
          if (Connector.ALLOW_HORIZONTAL.equals(prev)) {
            avoidanceMap.put(loc, Connector.ALLOW_VERTICAL);
          }
        }
      }
    } else if (y0 == y1) {
      // horizontal wire
      for (final var loc : w) {
        if (unmarkable == null || unmarkable.contains(deletedEnd)) {
          final var prev = avoidanceMap.remove(loc);
          if (!Connector.ALLOW_VERTICAL.equals(prev)) {
            avoidanceMap.put(loc, Connector.ALLOW_HORIZONTAL);
          }
        }
      }
    } else {
      // diagonal - shouldn't happen
      throw new RuntimeException("Diagonal wires are not supported.");
    }
  }
}
