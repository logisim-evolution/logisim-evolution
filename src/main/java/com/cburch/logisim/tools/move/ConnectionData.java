/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.List;

class ConnectionData {
  private final Location loc;

  private final Direction dir;

  /**
   * The list of wires leading up to this point - we may well want to truncate this path somewhat.
   */
  private final List<Wire> wirePath;

  private final Location wirePathStart;

  public ConnectionData(Location loc, Direction dir, List<Wire> wirePath, Location wirePathStart) {
    this.loc = loc;
    this.dir = dir;
    this.wirePath = wirePath;
    this.wirePathStart = wirePathStart;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ConnectionData o) {
      return this.loc.equals(o.loc) && this.dir.equals(o.dir);
    } else {
      return false;
    }
  }

  public Direction getDirection() {
    return dir;
  }

  public Location getLocation() {
    return loc;
  }

  public List<Wire> getWirePath() {
    return wirePath;
  }

  public Location getWirePathStart() {
    return wirePathStart;
  }

  @Override
  public int hashCode() {
    return loc.hashCode() * 31 + (dir == null ? 0 : dir.hashCode());
  }
}
