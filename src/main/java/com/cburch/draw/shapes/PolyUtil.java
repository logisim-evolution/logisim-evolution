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
import lombok.Getter;

public final class PolyUtil {
  private PolyUtil() {
    // dummy
  }

  public static ClosestResult getClosestPoint(Location loc, boolean closed, Handle[] hs) {
    final var xq = loc.getX();
    final var yq = loc.getY();
    final var ret = new ClosestResult();
    ret.distanceSq = Double.MAX_VALUE;
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
        if (d < ret.distanceSq) {
          ret.distanceSq = d;
          ret.previousHandle = h0;
          ret.nextHandle = h1;
        }
        h0 = h1;
        x0 = x1;
        y0 = y1;
      }
    }
    if (ret.distanceSq == Double.MAX_VALUE) {
      return null;
    } else {
      final var h0 = ret.previousHandle;
      final var h1 = ret.nextHandle;
      final var p = LineUtil.nearestPointSegment(xq, yq, h0.getX(), h0.getY(), h1.getX(), h1.getY());
      ret.location = Location.create((int) Math.round(p[0]), (int) Math.round(p[1]));
      return ret;
    }
  }

  public static class ClosestResult {
    @Getter private double distanceSq;
    @Getter private Location location;
    @Getter private Handle previousHandle;
    @Getter private Handle nextHandle;
  }
}
