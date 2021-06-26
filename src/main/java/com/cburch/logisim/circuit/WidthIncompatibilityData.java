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

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;

public class WidthIncompatibilityData {
  private final ArrayList<Location> points;
  private final ArrayList<BitWidth> widths;

  public WidthIncompatibilityData() {
    points = new ArrayList<>();
    widths = new ArrayList<>();
  }

  public void add(Location p, BitWidth w) {
    for (var i = 0; i < points.size(); i++) {
      if (p.equals(points.get(i)) && w.equals(widths.get(i))) return;
    }
    points.add(p);
    widths.add(w);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof WidthIncompatibilityData)) return false;
    if (this == other) return true;

    final var o = (WidthIncompatibilityData) other;
    if (this.size() != o.size()) return false;
    for (var i = 0; i < this.size(); i++) {
      final var p = o.getPoint(i);
      final var w = o.getBitWidth(i);
      var matched = false;
      for (var j = 0; j < o.size(); j++) {
        final var q = this.getPoint(j);
        final var x = this.getBitWidth(j);
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
    final var hist = new int[65];
    BitWidth maxwidth = null;
    var maxcount = 0;
    for (final var bw : widths) {
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
