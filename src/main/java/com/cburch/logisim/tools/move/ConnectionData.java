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
    if (other instanceof ConnectionData) {
      ConnectionData o = (ConnectionData) other;
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
