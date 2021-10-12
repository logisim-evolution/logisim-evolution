/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class PortManager {
  private static Location computeDefaultLocation(CircuitAppearance appear, Instance pin, Map<Instance, AppearancePort> others) {
    // Determine which locations are being used in canvas, and look for
    // which instances facing the same way in layout
    final var usedLocs = new HashSet<Location>();
    final var sameWay = new ArrayList<Instance>();
    final var facing = pin.getAttributeValue(StdAttr.FACING);
    for (final var entry : others.entrySet()) {
      final var pin2 = entry.getKey();
      final var loc = entry.getValue().getLocation();
      usedLocs.add(loc);
      if (pin2.getAttributeValue(StdAttr.FACING) == facing) {
        sameWay.add(pin2);
      }
    }

    // if at least one faces the same way, place pin relative to that
    if (!sameWay.isEmpty()) {
      sameWay.add(pin);
      DefaultAppearance.sortPinList(sameWay, facing);
      var isFirst = false;
      Instance neighbor = null; // (preferably previous in map)
      for (final var p : sameWay) {
        if (p == pin) {
          break;
        } else {
          neighbor = p;
        }
      }
      if (neighbor == null) { // pin must have been first in list
        neighbor = sameWay.get(1);
      }
      int dx;
      int dy;
      if (facing == Direction.EAST || facing == Direction.WEST) {
        dx = 0;
        dy = isFirst ? -10 : 10;
      } else {
        dx = isFirst ? -10 : 10;
        dy = 0;
      }
      var loc = others.get(neighbor).getLocation();
      do {
        loc = loc.translate(dx, dy);
      } while (usedLocs.contains(loc));
      if (loc.getX() >= 0 && loc.getY() >= 0) {
        return loc;
      }
      do {
        loc = loc.translate(-dx, -dy);
      } while (usedLocs.contains(loc));
      return loc;
    }

    // otherwise place it on the boundary of the bounding rectangle
    final var bds = appear.getAbsoluteBounds();
    int x;
    int y;
    int dX = 0;
    int dY = 0;
    if (facing == Direction.EAST) { // on west side by default
      x = bds.getX() - 7;
      y = bds.getY() + 5;
      dY = 10;
    } else if (facing == Direction.WEST) { // on east side by default
      x = bds.getX() + bds.getWidth() - 3;
      y = bds.getY() + 5;
      dY = 10;
    } else if (facing == Direction.SOUTH) { // on north side by default
      x = bds.getX() + 5;
      y = bds.getY() - 7;
      dX = 10;
    } else { // on south side by default
      x = bds.getX() + 5;
      y = bds.getY() + bds.getHeight() - 3;
      dX = 10;
    }
    x = (x + 9) / 10 * 10; // round coordinates up to ensure they're on grid
    y = (y + 9) / 10 * 10;
    var loc = Location.create(x, y);
    while (usedLocs.contains(loc)) {
      loc = loc.translate(dX, dY);
    }
    return loc;
  }

  private final CircuitAppearance appearance;

  private boolean doingUpdate;

  PortManager(CircuitAppearance appearance) {
    this.appearance = appearance;
    this.doingUpdate = false;
  }

  private void performUpdate(
      Set<Instance> adds,
      Set<Instance> removes,
      Map<Instance, Instance> replaces,
      Collection<Instance> allPins) {
    // Find the current objects corresponding to pins
    final var oldObjects = new HashMap<Instance, AppearancePort>();
    AppearanceAnchor anchor = null;
    for (final var obj : appearance.getCustomObjectsFromBottom()) {
      if (obj instanceof AppearancePort appPort) {
        oldObjects.put(appPort.getPin(), appPort);
      } else if (obj instanceof AppearanceAnchor appAnchor) {
        anchor = appAnchor;
      }
    }

    // ensure we have the anchor in the circuit
    if (anchor == null) {
      for (final var obj : DefaultCustomAppearance.build(allPins)) {
        if (obj instanceof AppearanceAnchor appAnchor) {
          anchor = appAnchor;
        }
      }
      if (anchor == null) {
        anchor = new AppearanceAnchor(Location.create(100, 100));
      }
      final var dest = appearance.getCustomObjectsFromBottom().size();
      appearance.addObjects(dest, Collections.singleton(anchor));
    }

    // Compute how the ports should change
    final var portRemoves = new ArrayList<AppearancePort>(removes.size());
    final var portAdds = new ArrayList<AppearancePort>(adds.size());

    // handle removals
    for (final var pin : removes) {
      final var port = oldObjects.remove(pin);
      if (port != null) {
        portRemoves.add(port);
      }
    }
    // handle replacements
    final var addsCopy = new ArrayList<Instance>(adds);
    for (final var entry : replaces.entrySet()) {
      final var port = oldObjects.remove(entry.getKey());
      if (port != null) {
        port.setPin(entry.getValue());
        oldObjects.put(entry.getValue(), port);
      } else { // this really shouldn't happen, but just to make sure...
        addsCopy.add(entry.getValue());
      }
    }
    // handle additions
    DefaultAppearance.sortPinList(addsCopy, Direction.EAST);
    // They're probably not really all facing east.
    // I'm just sorting them so it works predictably.
    for (final var pin : addsCopy) {
      if (!oldObjects.containsKey(pin)) {
        final var loc = computeDefaultLocation(appearance, pin, oldObjects);
        final var o = new AppearancePort(loc, pin);
        portAdds.add(o);
        oldObjects.put(pin, o);
      }
    }

    // Now update the appearance
    appearance.replaceAutomatically(portRemoves, portAdds);
  }

  void updatePorts() {
    appearance.recomputePorts();
  }

  void updatePorts(
      Set<Instance> adds,
      Set<Instance> removes,
      Map<Instance, Instance> replaces,
      Collection<Instance> allPins) {
    if (!doingUpdate) {
      // "doingUpdate" ensures infinite recursion doesn't happen
      try {
        doingUpdate = true;
        performUpdate(adds, removes, replaces, allPins);
        appearance.recomputePorts();
      } finally {
        doingUpdate = false;
      }
    }
  }
}
