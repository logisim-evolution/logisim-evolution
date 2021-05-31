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
      for (Wire w : wires) {
        points.add(w.e0);
        points.add(w.e1);
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
