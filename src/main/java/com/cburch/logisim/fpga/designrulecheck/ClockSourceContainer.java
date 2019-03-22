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
import com.cburch.logisim.std.wiring.Clock;
import java.util.ArrayList;

public class ClockSourceContainer {

  ArrayList<Component> sources;
  boolean RequiresFPGAGlobalClock;

  public ClockSourceContainer() {
    sources = new ArrayList<Component>();
    RequiresFPGAGlobalClock = false;
  }

  public void clear() {
    sources.clear();
    RequiresFPGAGlobalClock = false;
  }

  private boolean equals(Component comp1, Component comp2) {
    if (comp1.getAttributeSet().getValue(Clock.ATTR_HIGH).intValue()
        != comp2.getAttributeSet().getValue(Clock.ATTR_HIGH).intValue()) {
      return false;
    }
    if (comp1.getAttributeSet().getValue(Clock.ATTR_LOW).intValue()
        != comp2.getAttributeSet().getValue(Clock.ATTR_LOW).intValue()) {
      return false;
    }
    return true;
  }

  public int getClockId(Component comp) {
    if (!(comp.getFactory() instanceof Clock)) {
      return -1;
    }
    for (Component clock : sources) {
      if (equals(comp, clock)) {
        return sources.indexOf(clock);
      }
    }
    sources.add(comp);
    return sources.indexOf(comp);
  }

  public int getNrofSources() {
    return sources.size();
  }

  public ArrayList<Component> getSources() {
    return sources;
  }

  public boolean RequiresFPGAGlobalClock() {
    return RequiresFPGAGlobalClock;
  }

  public void SetGloblaClockRequirement() {
    RequiresFPGAGlobalClock = true;
  }
}
