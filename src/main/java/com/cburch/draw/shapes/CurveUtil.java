/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import com.cburch.logisim.data.Bounds;

public final class CurveUtil {
  /**
   * getBounds and findNearestPoint are based translated from the ActionScript of Olivier Besson's
   * Bezier class for collision detection. Code from:
   * http://blog.gludion.com/2009/08/distance-to-quadratic-bezier-curve.html
   */

  // a value we consider "small enough" to equal it to zero:
  // (this is used for double solutions in 2nd or 3d degree equation)
  private static final double zeroMax = 0.0000001;

  private CurveUtil() {
    // dummy
  }

  private static double[] computeA(double[] p0, double[] p1) {
    return new double[] {p1[0] - p0[0], p1[1] - p0[1]};
  }

  private static double[] computeB(double[] p0, double[] p1, double[] p2) {
    return new double[] {p0[0] - 2 * p1[0] + p2[0], p0[1] - 2 * p1[1] + p2[1]};
  }

  // returns { t:Number, pos:Point, dist:Number, nor:Point }
  // (costs about 80 multiplications+additions)
  // note: p0 and p2 are endpoints, p1 is control point
  public static double[] findNearestPoint(double[] q, double[] p0, double[] p1, double[] p2) {
    final var A = computeA(p0, p1);
    final var B = computeB(p0, p1, p2);

    // a temporary util vect = p0 - (x,y)
    double[] pos = {p0[0] - q[0], p0[1] - q[1]};
    // search points P of bezier curve with PM.(dP / dt) = 0
    // a calculus leads to a 3d degree equation :
    final var a = B[0] * B[0] + B[1] * B[1];
    final var b = 3 * (A[0] * B[0] + A[1] * B[1]);
    final var c = 2 * (A[0] * A[0] + A[1] * A[1]) + pos[0] * B[0] + pos[1] * B[1];
    final var d = pos[0] * A[0] + pos[1] * A[1];
    final var roots = solveCubic(a, b, c, d);
    if (roots == null) return null;

    // find the closest point:
    var tMin = Double.MAX_VALUE;
    var dist2Min = Double.MAX_VALUE;
    final var posMin = new double[2];
    for (final var root : roots) {
      double t;
      if (root < 0) {
        t = 0;
      } else if (root <= 1) {
        t = root;
      } else {
        t = 1;
      }

      getPos(pos, t, p0, p1, p2);
      final var lx = q[0] - pos[0];
      final var ly = q[1] - pos[1];
      final var dist2 = lx * lx + ly * ly;
      if (dist2 < dist2Min) {
        // minimum found!
        tMin = root;
        dist2Min = dist2;
        posMin[0] = pos[0];
        posMin[1] = pos[1];
      }
    }

    return (tMin == Double.MAX_VALUE) ? null : posMin;
  }

  // note: p0 and p2 are endpoints, p1 is control point
  public static Bounds getBounds(double[] p0, double[] p1, double[] p2) {
    final var A = computeA(p0, p1);
    final var B = computeB(p0, p1, p2);

    // rough evaluation of bounds:
    var xMin = Math.min(p0[0], Math.min(p1[0], p2[0]));
    var xMax = Math.max(p0[0], Math.max(p1[0], p2[0]));
    var yMin = Math.min(p0[1], Math.min(p1[1], p2[1]));
    var yMax = Math.max(p0[1], Math.max(p1[1], p2[1]));

    // more accurate evaluation:
    // see Andree Michelle for a faster but less readable method
    if (xMin == p1[0] || xMax == p1[0]) {
      var u = -A[0] / B[0]; // u where getTan(u)[0] == 0
      u = (1 - u) * (1 - u) * p0[0] + 2 * u * (1 - u) * p1[0] + u * u * p2[0];
      if (xMin == p1[0]) {
        xMin = u;
      } else {
        xMax = u;
      }
    }
    if (yMin == p1[1] || yMax == p1[1]) {
      var u = -A[1] / B[1]; // u where getTan(u)[1] == 0
      u = (1 - u) * (1 - u) * p0[1] + 2 * u * (1 - u) * p1[1] + u * u * p2[1];
      if (yMin == p1[1]) {
        yMin = u;
      } else {
        yMax = u;
      }
    }

    final var x = (int) xMin;
    final var y = (int) yMin;
    final var w = (int) Math.ceil(xMax) - x;
    final var h = (int) Math.ceil(yMax) - y;

    return Bounds.create(x, y, w, h);
  }

  private static void getPos(double[] result, double t, double[] p0, double[] p1, double[] p2) {
    double a = (1 - t) * (1 - t);
    double b = 2 * t * (1 - t);
    double c = t * t;
    result[0] = a * p0[0] + b * p1[0] + c * p2[0];
    result[1] = a * p0[1] + b * p1[1] + c * p2[1];
  }

  // Translated from ActionScript written by Jim Armstrong, at
  // www.algorithmist.net. ActionScript is (c) 2006-2007, Jim Armstrong.
  // All rights reserved.
  //
  // This software program is supplied 'as is' without any warranty, express,
  // implied, or otherwise, including without limitation all warranties of
  // merchantability or fitness for a particular purpose. Jim Armstrong shall
  // not be liable for any special incidental, or consequential damages,
  // including, without limitation, lost revenues, lost profits, or loss of
  // prospective economic advantage, resulting from the use or misuse of this
  // software program.
  public static double[] interpolate(double[] end0, double[] end1, double[] mid) {
    var dx = mid[0] - end0[0];
    var dy = mid[1] - end0[1];
    final var d0 = Math.sqrt(dx * dx + dy * dy);

    dx = mid[0] - end1[0];
    dy = mid[1] - end1[1];
    final var d1 = Math.sqrt(dx * dx + dy * dy);

    if (d0 < zeroMax || d1 < zeroMax) {
      return new double[] {(end0[0] + end1[0]) / 2, (end0[1] + end1[1]) / 2};
    }

    final var t = d0 / (d0 + d1);
    final var u = 1.0 - t;
    final var t2 = t * t;
    final var u2 = u * u;
    final var den = 2 * t * u;

    final var xNum = mid[0] - u2 * end0[0] - t2 * end1[0];
    final var yNum = mid[1] - u2 * end0[1] - t2 * end1[1];

    return new double[] {xNum / den, yNum / den};
  }

  // a local duplicate & optimized version of
  // com.gludion.utils.MathUtils.thirdDegreeEquation(a,b,c,d):Object
  // WARNING: s2, s3 may be non - null if count = 1.
  // use only result["s"+i] where i <= count
  private static double[] solveCubic(double a, double b, double c, double d) {
    if (Math.abs(a) > zeroMax) {
      // let's adopt form: x3 + ax2 + bx + d = 0
      var z = a; // multi-purpose util variable
      a = b / z;
      b = c / z;
      c = d / z;
      // we solve using Cardan formula:
      // http://fr.wikipedia.org/wiki/M%C3%A9thode_de_Cardan
      final var p = b - a * a / 3;
      final var q = a * (2 * a * a - 9 * b) / 27 + c;
      final var p3 = p * p * p;
      final var D = q * q + 4 * p3 / 27;
      final var offset = -a / 3;
      if (D > zeroMax) {
        // D positive
        z = Math.sqrt(D);
        double u = (-q + z) / 2;
        double v = (-q - z) / 2;
        u = (u >= 0) ? Math.pow(u, 1. / 3) : -Math.pow(-u, 1. / 3);
        v = (v >= 0) ? Math.pow(v, 1. / 3) : -Math.pow(-v, 1. / 3);
        return new double[] {u + v + offset};
      } else if (D < -zeroMax) {
        // D negative
        final var u = 2 * Math.sqrt(-p / 3);
        final var v = Math.acos(-Math.sqrt(-27 / p3) * q / 2) / 3;
        return new double[] {
          u * Math.cos(v) + offset,
          u * Math.cos(v + 2 * Math.PI / 3) + offset,
          u * Math.cos(v + 4 * Math.PI / 3) + offset
        };
      } else {
        // D zero
        final var u = (q < 0) ? Math.pow(-q / 2, 1. / 3) : -Math.pow(q / 2, 1. / 3);
        return new double[] {2 * u + offset, -u + offset};
      }
    } else if (Math.abs(b) > zeroMax) {
      // a = 0, then actually a 2nd degree equation:
      // form : ax2 + bx + c = 0;
      a = b;
      b = c;
      c = d;
      var D = b * b - 4 * a * c;
      if (D <= -zeroMax) {
        // D negative
        return null;
      } else if (D > zeroMax) {
        // D positive
        D = Math.sqrt(D);
        return new double[] {(-b - D) / (2 * a), (-b + D) / (2 * a)};
      } else {
        // D zero
        return new double[] {-b / (2 * a)};
      }
    } else if (Math.abs(c) > zeroMax) {
      // a and b are both 0 - we're looking at a linear equation
      return new double[] {-d / c};
    } else {
      // a, b, and c are all 0 - this is a constant equation
      return null;
    }
  }
}
