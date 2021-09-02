/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import java.util.ArrayList;

public class ClockTreeContainer {

  private final ArrayList<ConnectionPoint> clockSources;
  private final ArrayList<ConnectionPoint> clockNets;
  private final int clockSourceId;
  private final ArrayList<String> hierarchyId;
  private boolean isPinClockSource;

  public ClockTreeContainer(ArrayList<String> hierarchy, int sourceId, boolean pinClockSource) {
    clockSources = new ArrayList<>();
    clockNets = new ArrayList<>();
    clockSourceId = sourceId;
    hierarchyId = new ArrayList<>();
    hierarchyId.addAll(hierarchy);
    isPinClockSource = pinClockSource;
  }

  public void addNet(ConnectionPoint NetInfo) {
    clockNets.add(NetInfo);
  }

  public void addSource(ConnectionPoint NetInfo) {
    clockSources.add(NetInfo);
  }

  public void clear() {
    clockSources.clear();
    clockNets.clear();
  }

  public void setPinClock() {
    isPinClockSource = true;
  }

  public boolean IsPinClockSource() {
    return isPinClockSource;
  }

  public boolean equals(ArrayList<String> Hierarchy, int sourceId) {
    return ((sourceId == clockSourceId) && hierarchyId.equals(Hierarchy));
  }

  public ArrayList<Byte> GetClockEntries(Net NetInfo) {
    final var result = new ArrayList<Byte>();
    for (final var SolderPoint : clockSources) {
      if (SolderPoint.getParentNet().equals(NetInfo))
        result.add(SolderPoint.getParentNetBitIndex());
    }
    for (final var SolderPoint : clockNets) {
      if (SolderPoint.getParentNet().equals(NetInfo))
        result.add(SolderPoint.getParentNetBitIndex());
    }
    return result;
  }

  public boolean NetContainsClockConnection(Net NetInfo) {
    for (final var SolderPoint : clockSources) {
      if (SolderPoint.getParentNet().equals(NetInfo)) return true;
    }
    for (final var SolderPoint : clockNets) {
      if (SolderPoint.getParentNet().equals(NetInfo)) return true;
    }
    return false;
  }

  public boolean NetContainsClockSource(Net NetInfo) {
    for (final var SolderPoint : clockSources) {
      if (SolderPoint.getParentNet().equals(NetInfo)) return true;
    }
    return false;
  }
}
