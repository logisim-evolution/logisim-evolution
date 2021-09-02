/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.comp.Component;
import java.util.ArrayList;

public class ClockTreeFactory {

  private ClockSourceContainer sources;
  private final ArrayList<ClockTreeContainer> sourceTrees;

  public ClockTreeFactory() {
    sourceTrees = new ArrayList<>();
  }

  public void addClockNet(ArrayList<String> hierarchyNames, int clocksourceid, ConnectionPoint connection, boolean isPinClock) {
    ClockTreeContainer destination = null;
    for (final var search : sourceTrees) {
      if (search.equals(hierarchyNames, clocksourceid)) {
        destination = search;
      }
    }
    if (destination == null) {
      destination = new ClockTreeContainer(hierarchyNames, clocksourceid, isPinClock);
      sourceTrees.add(destination);
    } else if (!destination.IsPinClockSource() && isPinClock) destination.setPinClock();
    destination.addNet(connection);
  }

  public void addClockSource(ArrayList<String> HierarchyNames, int clocksourceid, ConnectionPoint connection) {
    ClockTreeContainer destination = null;
    for (final var search : sourceTrees) {
      if (search.equals(HierarchyNames, clocksourceid)) {
        destination = search;
      }
    }
    if (destination == null) {
      destination = new ClockTreeContainer(HierarchyNames, clocksourceid, false);
      sourceTrees.add(destination);
    }
    destination.addSource(connection);
  }

  public void clean() {
    for (final var tree : sourceTrees) tree.clear();
    sourceTrees.clear();
    if (sources != null) sources.clear();
  }

  public int getClockSourceId(ArrayList<String> hierarchy, Net selectedNet, byte selectedNetBitIndex) {
    for (var i = 0; i < sources.getNrofSources(); i++) {
      for (final var ThisClockNet : sourceTrees) {
        if (ThisClockNet.equals(hierarchy, i)) {
          /*
           * we found a clock net corresponding the Hierarchy and
           * clock source id
           */
          for (final var clockEntry : ThisClockNet.GetClockEntries(selectedNet)) {
            if (clockEntry == selectedNetBitIndex) return i;
          }
        }
      }
    }
    return -1;
  }

  public int getClockSourceId(Component comp) {
    if (sources == null) return -1;
    return sources.getClockId(comp);
  }

  public ClockSourceContainer getSourceContainer() {
    if (sources == null) sources = new ClockSourceContainer();
    return sources;
  }

  public void setSourceContainer(ClockSourceContainer source) {
    sources = source;
  }
}
