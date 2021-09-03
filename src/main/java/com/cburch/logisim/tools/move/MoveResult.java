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
