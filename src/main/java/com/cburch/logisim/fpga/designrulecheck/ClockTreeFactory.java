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
