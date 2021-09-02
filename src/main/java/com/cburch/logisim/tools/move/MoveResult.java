/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

public class MoveResult {
  private final ReplacementMap replacements;
  private final Collection<ConnectionData> unsatisfiedConnections;
  private final Collection<Location> unconnectedLocations;
  private final int totalDistance;

  public MoveResult(
      MoveRequest request,
      ReplacementMap replacements,
      Collection<ConnectionData> unsatisfiedConnections,
      int totalDistance) {
    this.replacements = replacements;
    this.unsatisfiedConnections = unsatisfiedConnections;
    this.totalDistance = totalDistance;

    ArrayList<Location> unconnected = new ArrayList<>();
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

  public void print(PrintStream out) {
    out.print("MoveResult: ");
    replacements.print(out);
  }
  
  public String toString() {
    return "MoveResult: " + replacements.toString();
  }
}
