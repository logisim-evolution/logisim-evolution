/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import java.util.ArrayList;
import java.util.List;

public class ClockTreeContainer {

  private final ArrayList<ConnectionPoint> clockSources;
  private final ArrayList<ConnectionPoint> clockNets;
  private final int clockSourceId;
  private final ArrayList<String> hierarchyId;
  private boolean isPinClockSource;

  public ClockTreeContainer(List<String> hierarchy, int sourceId, boolean pinClockSource) {
    clockSources = new ArrayList<>();
    clockNets = new ArrayList<>();
    clockSourceId = sourceId;
    hierarchyId = new ArrayList<>();
    hierarchyId.addAll(hierarchy);
    isPinClockSource = pinClockSource;
  }

  public void addNet(ConnectionPoint netInfo) {
    clockNets.add(netInfo);
  }

  public void addSource(ConnectionPoint netInfo) {
    clockSources.add(netInfo);
  }

  public void clear() {
    clockSources.clear();
    clockNets.clear();
  }

  public void setPinClock() {
    isPinClockSource = true;
  }

  public boolean isPinClockSource() {
    return isPinClockSource;
  }

  public boolean equals(List<String> hierarchy, int sourceId) {
    return ((sourceId == clockSourceId) && hierarchyId.equals(hierarchy));
  }

  public List<Byte> getClockEntries(Net netInfo) {
    final var result = new ArrayList<Byte>();
    for (final var solderPoint : clockSources) {
      if (solderPoint.getParentNet().equals(netInfo))
        result.add(solderPoint.getParentNetBitIndex());
    }
    for (final var solderPoint : clockNets) {
      if (solderPoint.getParentNet().equals(netInfo))
        result.add(solderPoint.getParentNetBitIndex());
    }
    return result;
  }

  public boolean netContainsClockConnection(Net netInfo) {
    for (final var solderPoint : clockSources) {
      if (solderPoint.getParentNet().equals(netInfo)) return true;
    }
    for (final var solderPoint : clockNets) {
      if (solderPoint.getParentNet().equals(netInfo)) return true;
    }
    return false;
  }

  public boolean netContainsClockSource(Net netInfo) {
    for (final var solderPoint : clockSources) {
      if (solderPoint.getParentNet().equals(netInfo)) return true;
    }
    return false;
  }
}
