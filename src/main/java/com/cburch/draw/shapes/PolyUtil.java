/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import com.cburch.draw.model.Handle;
import com.cburch.logisim.data.Location;

public final class PolyUtil {
  private PolyUtil() {
    // dummy
  }

  public static ClosestResult getClosestPoint(Location loc, boolean closed, Handle[] hs) {
    final var xq = loc.getX();
    final var yq = loc.getY();
    final var ret = new ClosestResult();
    ret.dist = Double.MAX_VALUE;
    if (hs.length > 0) {
      var h0 = hs[0];
      var x0 = h0.getX();
      var y0 = h0.getY();
      final var stop = closed ? hs.length : (hs.length - 1);
      for (var i = 0; i < stop; i++) {
        final var h1 = hs[(i + 1) % hs.length];
        final var x1 = h1.getX();
        final var y1 = h1.getY();
        final var d = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
        if (d < ret.dist) {
          ret.dist = d;
          ret.prevHandle = h0;
          ret.nextHandle = h1;
        }
        h0 = h1;
        x0 = x1;
        y0 = y1;
      }
    }
    if (ret.dist == Double.MAX_VALUE) {
      return null;
    } else {
      final var h0 = ret.prevHandle;
      final var h1 = ret.nextHandle;
      final var p =
          LineUtil.nearestPointSegment(xq, yq, h0.getX(), h0.getY(), h1.getX(), h1.getY());
      ret.loc = Location.create((int) Math.round(p[0]), (int) Math.round(p[1]), false);
      return ret;
    }
  }

  public static class ClosestResult {
    private double dist;
    private Location loc;
    private Handle prevHandle;
    private Handle nextHandle;

    public double getDistanceSq() {
      return dist;
    }

    public Location getLocation() {
      return loc;
    }

    public Handle getNextHandle() {
      return nextHandle;
    }

    public Handle getPreviousHandle() {
      return prevHandle;
    }
  }
}
