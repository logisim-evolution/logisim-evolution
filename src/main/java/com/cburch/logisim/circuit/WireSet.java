/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.Location;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WireSet {
  private static final Set<Wire> NULL_WIRES = Collections.emptySet();
  public static final WireSet EMPTY = new WireSet(NULL_WIRES);

  private final Set<Wire> wires;
  private final Set<Location> points;

  WireSet(Set<Wire> wires) {
    if (wires.isEmpty()) {
      this.wires = NULL_WIRES;
      points = Collections.emptySet();
    } else {
      this.wires = wires;
      points = new HashSet<>();
      for (final var wire : wires) {
        points.add(wire.e0);
        points.add(wire.e1);
      }
    }
  }

  public boolean containsLocation(Location loc) {
    return points.contains(loc);
  }

  public boolean containsWire(Wire w) {
    return wires.contains(w);
  }
}
