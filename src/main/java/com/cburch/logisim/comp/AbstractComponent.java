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

package com.cburch.logisim.comp;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.List;

public abstract class AbstractComponent implements Component {
  protected AbstractComponent() {}

  public boolean contains(Location pt) {
    Bounds bds = getBounds();
    if (bds == null) return false;
    return bds.contains(pt, 1);
  }

  public boolean contains(Location pt, Graphics g) {
    Bounds bds = getBounds(g);
    if (bds == null) return false;
    return bds.contains(pt, 1);
  }

  public boolean endsAt(Location pt) {
    for (EndData data : getEnds()) {
      if (data.getLocation().equals(pt)) return true;
    }
    return false;
  }

  public abstract Bounds getBounds();

  public Bounds getBounds(Graphics g) {
    return getBounds();
  }

  public EndData getEnd(int index) {
    return getEnds().get(index);
  }

  //
  // propagation methods
  //
  public abstract List<EndData> getEnds();

  //
  // basic information methods
  //
  public abstract ComponentFactory getFactory();

  //
  // location/extent methods
  //
  public abstract Location getLocation();

  public abstract void propagate(CircuitState state);
}
