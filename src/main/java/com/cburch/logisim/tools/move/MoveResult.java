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

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

public class MoveResult {
  private ReplacementMap replacements;
  private Collection<ConnectionData> unsatisfiedConnections;
  private Collection<Location> unconnectedLocations;
  private int totalDistance;

  public MoveResult(
      MoveRequest request,
      ReplacementMap replacements,
      Collection<ConnectionData> unsatisfiedConnections,
      int totalDistance) {
    this.replacements = replacements;
    this.unsatisfiedConnections = unsatisfiedConnections;
    this.totalDistance = totalDistance;

    ArrayList<Location> unconnected = new ArrayList<Location>();
    for (ConnectionData conn : unsatisfiedConnections) {
      unconnected.add(conn.getLocation());
    }
    unconnectedLocations = unconnected;
  }

  void addUnsatisfiedConnections(Collection<ConnectionData> toAdd) {
    unsatisfiedConnections.addAll(toAdd);
    for (ConnectionData conn : toAdd) {
      unconnectedLocations.add(conn.getLocation());
    }
  }

  public ReplacementMap getReplacementMap() {
    return replacements;
  }

  int getTotalDistance() {
    return totalDistance;
  }

  public Collection<Location> getUnconnectedLocations() {
    return unconnectedLocations;
  }

  Collection<ConnectionData> getUnsatisifiedConnections() {
    return unsatisfiedConnections;
  }

  public Collection<Wire> getWiresToAdd() {
    @SuppressWarnings("unchecked")
    Collection<Wire> ret = (Collection<Wire>) replacements.getAdditions();
    return ret;
  }

  public Collection<Wire> getWiresToRemove() {
    @SuppressWarnings("unchecked")
    Collection<Wire> ret = (Collection<Wire>) replacements.getAdditions();
    return ret;
  }

  public void print(PrintStream out) {
    boolean printed = false;
    for (Component w : replacements.getAdditions()) {
      printed = true;
      out.println("add " + w);
    }
    for (Component w : replacements.getRemovals()) {
      printed = true;
      out.println("del " + w);
    }
    for (Component w : replacements.getReplacedComponents()) {
      printed = true;
      out.print("repl " + w + " by");
      for (Component w2 : replacements.getComponentsReplacing(w)) {
        out.print(" " + w2);
      }
      out.println();
    }
    if (!printed) {
      out.println("no replacements");
    }
  }
}
