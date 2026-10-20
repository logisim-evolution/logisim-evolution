/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    if (!(other instanceof WidthIncompatibilityData o)) return false;
    if (this == other) return true;

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
