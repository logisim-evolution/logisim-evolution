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
import lombok.val;

public class PolyUtil {
  private PolyUtil() {
    // dummy
  }

  public static ClosestResult getClosestPoint(Location loc, boolean closed, Handle[] hs) {
    val xq = loc.getX();
    val yq = loc.getY();
    val ret = new ClosestResult();
    ret.distanceSq = Double.MAX_VALUE;
    if (hs.length > 0) {
      var h0 = hs[0];
      var x0 = h0.getX();
      var y0 = h0.getY();
      val stop = closed ? hs.length : (hs.length - 1);
      for (var i = 0; i < stop; i++) {
        val h1 = hs[(i + 1) % hs.length];
        val x1 = h1.getX();
        val y1 = h1.getY();
        val d = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
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
    }
    val h0 = ret.previousHandle;
    val h1 = ret.nextHandle;
    val p = LineUtil.nearestPointSegment(xq, yq, h0.getX(), h0.getY(), h1.getX(), h1.getY());
    ret.location = Location.create((int) Math.round(p[0]), (int) Math.round(p[1]));
    return ret;
  }

  @Getter
  public static class ClosestResult {
    private double distanceSq;
    private Location location;
    private Handle previousHandle;
    private Handle nextHandle;
  }
}
