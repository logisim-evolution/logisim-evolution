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

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;
import lombok.val;

public class MoveResult {
  @Getter private final ReplacementMap replacementMap;
  @Getter private final Collection<ConnectionData> unsatisfiedConnections;
  @Getter private final Collection<Location> unconnectedLocations;
  @Getter private final int totalDistance;

  public MoveResult(
      MoveRequest request,
      ReplacementMap replacements,
      Collection<ConnectionData> unsatisfiedConnections,
      int totalDistance) {
    this.replacementMap = replacements;
    this.unsatisfiedConnections = unsatisfiedConnections;
    this.totalDistance = totalDistance;

    val unconnected = new ArrayList<Location>();
    for (val conn : unsatisfiedConnections) {
      unconnected.add(conn.getLocation());
    }
    unconnectedLocations = unconnected;
  }

  void addUnsatisfiedConnections(Collection<ConnectionData> toAdd) {
    unsatisfiedConnections.addAll(toAdd);
    for (val conn : toAdd) {
      unconnectedLocations.add(conn.getLocation());
    }
  }

  public Collection<Wire> getWiresToAdd() {
    @SuppressWarnings("unchecked")
    Collection<Wire> ret = (Collection<Wire>) replacementMap.getAdditions();
    return ret;
  }

  public void print(PrintStream out) {
    out.print("MoveResult: ");
    replacementMap.print(out);
  }

  @Override
  public String toString() {
    return "MoveResult: " + replacementMap.toString();
  }
}
