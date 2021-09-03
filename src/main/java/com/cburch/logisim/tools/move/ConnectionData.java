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
import lombok.Getter;

class ConnectionData {
  @Getter private final Location location;
  @Getter private final Direction direction;

  /**
   * The list of wires leading up to this point - we may well want to truncate this path somewhat.
   */
  @Getter private final List<Wire> wirePath;
  @Getter private final Location wirePathStart;

  public ConnectionData(Location loc, Direction dir, List<Wire> wirePath, Location wirePathStart) {
    this.location = loc;
    this.direction = dir;
    this.wirePath = wirePath;
    this.wirePathStart = wirePathStart;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ConnectionData) {
      ConnectionData o = (ConnectionData) other;
      return this.location.equals(o.location) && this.direction.equals(o.direction);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return location.hashCode() * 31 + (direction == null ? 0 : direction.hashCode());
  }
}
