/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

class CircuitPoints {
  private static class LocationData {
    BitWidth width = BitWidth.UNKNOWN;
    final ArrayList<Component> components = new ArrayList<>(4);
    final ArrayList<EndData> ends = new ArrayList<>(4);
    // these lists are parallel - ends corresponding to wires are null
  }

  private final HashMap<Location, LocationData> map = new HashMap<>();
  private final HashMap<Location, WidthIncompatibilityData> incompatibilityData = new HashMap<>();

  public CircuitPoints() {
    // Do nothing.
  }

  //
  // update methods
  //
  void add(Component comp) {
    if (comp instanceof Wire w) {
      addSub(w.getEnd0(), w, null);
      addSub(w.getEnd1(), w, null);
    } else {
      for (final var endData : comp.getEnds()) {
        if (endData != null) {
          addSub(endData.getLocation(), comp, endData);
        }
      }
    }
  }

  void add(Component comp, EndData endData) {
    if (endData != null) addSub(endData.getLocation(), comp, endData);
  }

  private void addSub(Location loc, Component comp, EndData endData) {
    var locData = map.get(loc);
    if (locData == null) {
      locData = new LocationData();
      map.put(loc, locData);
    }
    locData.components.add(comp);
    locData.ends.add(endData);
    computeIncompatibilityData(loc, locData);
  }

  private void computeIncompatibilityData(Location loc, LocationData locData) {
    WidthIncompatibilityData error = null;
    if (locData != null) {
      var width = BitWidth.UNKNOWN;
      for (final var endData : locData.ends) {
        if (endData != null) {
          final var endWidth = endData.getWidth();
          if (width == BitWidth.UNKNOWN) {
            width = endWidth;
          } else if (width != endWidth && endWidth != BitWidth.UNKNOWN) {
            if (error == null) {
              error = new WidthIncompatibilityData();
              error.add(loc, width);
            }
            error.add(loc, endWidth);
          }
        }
      }
      locData.width = width;
    }

    if (error == null) {
      incompatibilityData.remove(loc);
    } else {
      incompatibilityData.put(loc, error);
    }
  }

  private Collection<? extends Component> find(Location loc, boolean isWire) {
    final var locData = map.get(loc);
    if (locData == null) return Collections.emptySet();

    // first see how many elements we have; we can handle some simple
    // cases without creating any new lists
    final var components = locData.components;
    var retSize = 0;
    Component retValue = null;
    for (final var comp : components) {
      if ((comp instanceof Wire) == isWire) {
        retValue = comp;
        retSize++;
      }
    }
    if (retSize == components.size()) return components;
    if (retSize == 0) return Collections.emptySet();
    if (retSize == 1) return Collections.singleton(retValue);

    // otherwise we have to create our own list
    final var ret = new Component[retSize];
    var retPos = 0;
    for (final var comp : components) {
      if ((comp instanceof Wire) == isWire) {
        ret[retPos] = comp;
        retPos++;
      }
    }
    return Arrays.asList(ret);
  }

  int getComponentCount(Location loc) {
    final var locData = map.get(loc);
    return locData == null ? 0 : locData.components.size();
  }

  Collection<? extends Component> getComponents(Location loc) {
    final var locData = map.get(loc);
    if (locData == null) return Collections.emptySet();
    else return locData.components;
  }

  Component getExclusive(Location loc) {
    final var locData = map.get(loc);
    if (locData == null) return null;
    int i = -1;
    for (final var endData : locData.ends) {
      i++;
      if (endData != null && endData.isExclusive()) {
        return locData.components.get(i);
      }
    }
    return null;
  }

  Collection<? extends Component> getNonWires(Location loc) {
    return find(loc, false);
  }

  Collection<? extends Component> getSplitCauses(Location loc) {
    return getComponents(loc);
  }

  //
  // access methods
  //
  Set<Location> getSplitLocations() {
    return map.keySet();
  }

  BitWidth getWidth(Location loc) {
    final var locData = map.get(loc);
    return locData == null ? BitWidth.UNKNOWN : locData.width;
  }

  Collection<WidthIncompatibilityData> getWidthIncompatibilityData() {
    return incompatibilityData.values();
  }

  Collection<Wire> getWires(Location loc) {
    @SuppressWarnings("unchecked")
    Collection<Wire> ret = (Collection<Wire>) find(loc, true);
    return ret;
  }

  boolean hasConflict(Component comp) {
    if (!(comp instanceof Wire)) {
      for (final var endData : comp.getEnds()) {
        if (endData != null
            && endData.isExclusive()
            && getExclusive(endData.getLocation()) != null) {
          return true;
        }
      }
    }
    return false;
  }

  void remove(Component comp) {
    if (comp instanceof Wire wire) {
      removeSub(wire.getEnd0(), wire);
      removeSub(wire.getEnd1(), wire);
    } else {
      for (EndData endData : comp.getEnds()) {
        if (endData != null) {
          removeSub(endData.getLocation(), comp);
        }
      }
    }
  }

  void remove(Component comp, EndData endData) {
    if (endData != null) removeSub(endData.getLocation(), comp);
  }

  private void removeSub(Location loc, Component comp) {
    final var locData = map.get(loc);
    if (locData == null) return;

    int index = locData.components.indexOf(comp);
    if (index < 0) return;

    if (locData.components.size() == 1) {
      map.remove(loc);
      incompatibilityData.remove(loc);
    } else {
      locData.components.remove(index);
      locData.ends.remove(index);
      computeIncompatibilityData(loc, locData);
    }
  }
}
