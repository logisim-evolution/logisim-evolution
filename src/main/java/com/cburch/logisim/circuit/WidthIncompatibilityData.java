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

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;

public class WidthIncompatibilityData {
  private ArrayList<Location> points;
  private ArrayList<BitWidth> widths;

  public WidthIncompatibilityData() {
    points = new ArrayList<Location>();
    widths = new ArrayList<BitWidth>();
  }

  public void add(Location p, BitWidth w) {
    for (int i = 0; i < points.size(); i++) {
      if (p.equals(points.get(i)) && w.equals(widths.get(i))) return;
    }
    points.add(p);
    widths.add(w);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof WidthIncompatibilityData)) return false;
    if (this == other) return true;

    WidthIncompatibilityData o = (WidthIncompatibilityData) other;
    if (this.size() != o.size()) return false;
    for (int i = 0; i < this.size(); i++) {
      Location p = o.getPoint(i);
      BitWidth w = o.getBitWidth(i);
      boolean matched = false;
      for (int j = 0; j < o.size(); j++) {
        Location q = this.getPoint(j);
        BitWidth x = this.getBitWidth(j);
        if (p.equals(q) && w.equals(x)) {
          matched = true;
          break;
        }
      }
      if (!matched) return false;
    }
    return true;
  }

  public BitWidth getBitWidth(int i) {
    return widths.get(i);
  }

  public Location getPoint(int i) {
    return points.get(i);
  }

  public BitWidth getCommonBitWidth() {
    int hist[] = new int[65];
    BitWidth maxwidth = null;
    int maxcount = 0;
    for (BitWidth bw : widths) {
      int w = bw.getWidth();
      int n = ++hist[w];
      if (n > maxcount) {
        maxcount = n;
        maxwidth = bw;
      } else if (n == maxcount) {
        maxwidth = null;
      }
    }
    return maxwidth;
  }

  @Override
  public int hashCode() {
    return this.size();
  }

  public int size() {
    return points.size();
  }
}
