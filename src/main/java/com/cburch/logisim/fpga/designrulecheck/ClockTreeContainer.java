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
