/**
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
  private ArrayList<ClockTreeContainer> sourcetrees;

  public ClockTreeFactory() {
    sourcetrees = new ArrayList<ClockTreeContainer>();
  }

  public void AddClockNet( ArrayList<String> HierarchyNames, int clocksourceid, 
                           ConnectionPoint connection, boolean isPinClock ) {
    ClockTreeContainer destination = null;
    for (ClockTreeContainer search : sourcetrees) {
      if (search.equals(HierarchyNames, clocksourceid)) {
        destination = search;
      }
    }
    if (destination == null) {
      destination = new ClockTreeContainer(HierarchyNames, clocksourceid, isPinClock);
      sourcetrees.add(destination);
    } else if (!destination.IsPinClockSource() && isPinClock) destination.setPinClock();
    destination.addNet(connection);
  }

  public void AddClockSource(
      ArrayList<String> HierarchyNames, int clocksourceid, ConnectionPoint connection) {
    ClockTreeContainer destination = null;
    for (ClockTreeContainer search : sourcetrees) {
      if (search.equals(HierarchyNames, clocksourceid)) {
        destination = search;
      }
    }
    if (destination == null) {
      destination = new ClockTreeContainer(HierarchyNames, clocksourceid,false);
      sourcetrees.add(destination);
    }
    destination.addSource(connection);
  }

  public void clean() {
    for (ClockTreeContainer tree : sourcetrees) {
      tree.clear();
    }
    sourcetrees.clear();
    if (sources != null) sources.clear();
  }

  public int GetClockSourceId(
      ArrayList<String> Hierarchy, Net SelectedNet, byte SelectedNetBitIndex) {
    for (int i = 0; i < sources.getNrofSources(); i++) {
      for (ClockTreeContainer ThisClockNet : sourcetrees) {
        if (ThisClockNet.equals(Hierarchy, i)) {
          /*
           * we found a clock net corresponding the Hierarchy and
           * clock source id
           */
          for (Byte ClockEntry : ThisClockNet.GetClockEntries(SelectedNet)) {
            if (ClockEntry == SelectedNetBitIndex) return i;
          }
        }
      }
    }
    return -1;
  }

  public int GetClockSourceId(Component comp) {
    if (sources == null) return -1;
    return sources.getClockId(comp);
  }

  public ClockSourceContainer GetSourceContainer() {
    if (sources == null) {
      sources = new ClockSourceContainer();
    }
    return sources;
  }

  public void SetSourceContainer(ClockSourceContainer source) {
    sources = source;
  }
}
