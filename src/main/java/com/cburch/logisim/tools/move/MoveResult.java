/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
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

    final var unconnected = new ArrayList<Location>();
    for (final var conn : unsatisfiedConnections) {
      unconnected.add(conn.getLocation());
    }
    unconnectedLocations = unconnected;
  }

  void addUnsatisfiedConnections(Collection<ConnectionData> toAdd) {
    unsatisfiedConnections.addAll(toAdd);
    for (final var conn : toAdd) {
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

  @SuppressWarnings("unchecked")
  public Collection<Wire> getWiresToAdd() {
    return (Collection<Wire>) replacements.getAdditions();
  }

  public void print(PrintStream out) {
    out.print("MoveResult: ");
    replacements.print(out);
  }

  @Override
  public String toString() {
    return "MoveResult: " + replacements.toString();
  }
}
