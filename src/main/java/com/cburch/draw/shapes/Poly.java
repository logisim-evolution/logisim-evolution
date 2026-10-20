/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Graphics;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Poly extends FillableCanvasObject {
  private final boolean closed;
  // "handles" should be immutable - create a new array and change using
  // setHandles rather than changing contents
  private Handle[] handles;
  private GeneralPath path;
  private double[] lens;
  private Bounds bounds;

  public Poly(boolean closed, List<Location> locations) {
    final var hs = new Handle[locations.size()];
    var i = -1;
    for (final var loc : locations) {
      i++;
      hs[i] = new Handle(this, loc.getX(), loc.getY());
    }

    this.closed = closed;
    handles = hs;
    recomputeBounds();
  }

  @Override
  public Handle canDeleteHandle(Location loc) {
    final var minHandles = closed ? 3 : 2;
    final var hs = handles;
    if (hs.length > minHandles) {
      final var qx = loc.getX();
      final var qy = loc.getY();
      final var w = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
      for (final var h : hs) {
        final var hx = h.getX();
        final var hy = h.getY();
        if (LineUtil.distance(qx, qy, hx, hy) < w * w) {
          return h;
        }
      }
    }
    return null;
  }

  @Override
  public Handle canInsertHandle(Location loc) {
    final var result = PolyUtil.getClosestPoint(loc, closed, handles);
    final var thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
    if (result.getDistanceSq() < thresh * thresh) {
      final var resLoc = result.getLocation();
      return (result.getPreviousHandle().isAt(resLoc) || result.getNextHandle().isAt(resLoc))
          ? null
          : new Handle(this, result.getLocation());
    }
    return null;
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return true;
  }

  /**
   * Clone function taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  @Override
  public Poly clone() {
    final var ret = (Poly) super.clone();
    final var hs = this.handles.clone();

    for (int i = 0, n = hs.length; i < n; ++i) {
      final var oldHandle = hs[i];
      hs[i] = new Handle(ret, oldHandle.getX(), oldHandle.getY());
    }
    ret.handles = hs;

    return (ret);
  }

  @Override
  public final boolean contains(Location loc, boolean assumeFilled) {
    Object type = getPaintType();
    if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
      type = DrawAttr.PAINT_STROKE_FILL;
    }
    if (type == DrawAttr.PAINT_STROKE) {
      final var thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
      final var result = PolyUtil.getClosestPoint(loc, closed, handles);
      return result.getDistanceSq() < thresh * thresh;
    } else if (type == DrawAttr.PAINT_FILL) {
      final var path = getPath();
      return path.contains(loc.getX(), loc.getY());
    } else { // fill and stroke
      final var path = getPath();
      if (path.contains(loc.getX(), loc.getY())) return true;
      final var width = getStrokeWidth();
      final var result = PolyUtil.getClosestPoint(loc, closed, handles);
      return result.getDistanceSq() < (width * width) / 4;
    }
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    final var hs = handles;
    final var n = hs.length;
    final var is = new Handle[n - 1];
    Handle previous = null;
    var deleted = false;
    for (var i = 0; i < n; i++) {
      if (deleted) {
        is[i - 1] = hs[i];
      } else if (hs[i].equals(handle)) {
        if (previous == null) {
          previous = hs[n - 1];
        }
        deleted = true;
      } else {
        previous = hs[i];
        is[i] = hs[i];
      }
    }
    setHandles(is);
    return previous;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(getPaintType());
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  public String getDisplayName() {
    return (closed) ? S.get("shapePolygon") : S.get("shapePolyline");
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    final var hs = handles;
    if (gesture == null) {
      return UnmodifiableList.create(hs);
    }

    final var g = gesture.getHandle();
    final var ret = new Handle[hs.length];
    for (int i = 0, n = hs.length; i < n; i++) {
      final var h = hs[i];
      if (h.equals(g)) {
        final var x = h.getX() + gesture.getDeltaX();
        final var y = h.getY() + gesture.getDeltaY();
        Location r;
        if (gesture.isShiftDown()) {
          var prev = hs[(i + n - 1) % n].getLocation();
          var next = hs[(i + 1) % n].getLocation();
          if (!closed) {
            if (i == 0) prev = null;
            if (i == n - 1) next = null;
          }
          if (prev == null) {
            r = LineUtil.snapTo8Cardinals(next, x, y);
          } else if (next == null) {
            r = LineUtil.snapTo8Cardinals(prev, x, y);
          } else {
            final var to = Location.create(x, y, false);
            final var a = LineUtil.snapTo8Cardinals(prev, x, y);
            final var b = LineUtil.snapTo8Cardinals(next, x, y);
            final var ad = a.manhattanDistanceTo(to);
            final var bd = b.manhattanDistanceTo(to);
            r = ad < bd ? a : b;
          }
        } else {
          r = Location.create(x, y, false);
        }
        ret[i] = new Handle(this, r);
      } else {
        ret[i] = h;
      }
    }
    return UnmodifiableList.create(ret);
  }

  private GeneralPath getPath() {
    var p = path;
    if (p != null) return p;

    p = new GeneralPath();
    final var hs = handles;
    if (hs.length > 0) {
      var first = true;
      for (final var h : hs) {
        if (first) {
          p.moveTo(h.getX(), h.getY());
          first = false;
        } else {
          p.lineTo(h.getX(), h.getY());
        }
      }
    }
    path = p;
    return p;
  }

  private Location getRandomBoundaryPoint(Random rand) {
    final var hs = handles;
    var ls = lens;
    if (ls == null) {
      ls = new double[hs.length + (closed ? 1 : 0)];
      var total = 0.0;
      for (var i = 0; i < ls.length; i++) {
        final var j = (i + 1) % hs.length;
        total += LineUtil.distance(hs[i].getX(), hs[i].getY(), hs[j].getX(), hs[j].getY());
        ls[i] = total;
      }
      lens = ls;
    }
    final var pos = ls[ls.length - 1] * rand.nextDouble();
    for (var i = 0; true; i++) {
      if (pos < ls[i]) {
        final var p = hs[i];
        final var q = hs[(i + 1) % hs.length];
        final var u = Math.random();
        final var x = (int) Math.round(p.getX() + u * (q.getX() - p.getX()));
        final var y = (int) Math.round(p.getY() + u * (q.getY() - p.getY()));
        return Location.create(x, y, false);
      }
    }
  }

  @Override
  public final Location getRandomPoint(Bounds bds, Random rand) {
    if (getPaintType() != DrawAttr.PAINT_STROKE) {
      return super.getRandomPoint(bds, rand);
    }
    var ret = getRandomBoundaryPoint(rand);
    final var w = getStrokeWidth();
    if (w > 1) {
      final var dx = rand.nextInt(w) - w / 2;
      final var dy = rand.nextInt(w) - w / 2;
      ret = ret.translate(dx, dy);
    }
    return ret;
  }

  @Override
  public void insertHandle(Handle desired, Handle previous) {
    final var loc = desired.getLocation();
    final var hs = handles;

    final var prev =
        (previous == null)
            ? PolyUtil.getClosestPoint(loc, closed, hs).getPreviousHandle()
            : previous;
    final var is = new Handle[hs.length + 1];
    var inserted = false;
    for (var i = 0; i < hs.length; i++) {
      if (inserted) {
        is[i + 1] = hs[i];
      } else if (hs[i].equals(prev)) {
        inserted = true;
        is[i] = hs[i];
        is[i + 1] = desired;
      } else {
        is[i] = hs[i];
      }
    }
    if (!inserted) {
      throw new IllegalArgumentException("no such handle");
    }
    setHandles(is);
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof Poly that) {
      final var a = this.handles;
      final var b = that.handles;
      if (this.closed != that.closed || a.length != b.length) {
        return false;
      } else {
        for (int i = 0, n = a.length; i < n; i++) {
          if (!a[i].equals(b[i])) return false;
        }
        return super.matches(that);
      }
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    var ret = super.matchesHashCode();
    ret = ret * 3 + (closed ? 1 : 0);
    final var hs = handles;
    for (final var h : hs) {
      ret = ret * 31 + h.hashCode();
    }
    return ret;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    final var hs = getHandles(gesture);
    final var is = new Handle[hs.size()];
    var i = 0;
    for (final var h : hs) {
      is[i++] = h;
    }
    setHandles(is);
    return null;
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    final var hs = getHandles(gesture);
    final var xs = new int[hs.size()];
    final var ys = new int[hs.size()];
    var i = 0;
    for (final var h : hs) {
      xs[i] = h.getX();
      ys[i] = h.getY();
      i++;
    }

    if (setForFill(g)) {
      g.fillPolygon(xs, ys, xs.length);
    }
    if (setForStroke(g)) {
      if (closed) g.drawPolygon(xs, ys, xs.length);
      else g.drawPolyline(xs, ys, xs.length);
    }
  }

  private void recomputeBounds() {
    final var hs = handles;
    var x0 = hs[0].getX();
    var y0 = hs[0].getY();
    var x1 = x0;
    var y1 = y0;
    for (var i = 1; i < hs.length; i++) {
      int x = hs[i].getX();
      int y = hs[i].getY();
      if (x < x0) x0 = x;
      if (x > x1) x1 = x;
      if (y < y0) y0 = y;
      if (y > y1) y1 = y;
    }
    final var bds = Bounds.create(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    final var stroke = getStrokeWidth();
    bounds = stroke < 2 ? bds : bds.expand(stroke / 2);
  }

  private void setHandles(Handle[] hs) {
    handles = hs;
    lens = null;
    path = null;
    recomputeBounds();
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createPoly(doc, this);
  }

  @Override
  public void translate(int dx, int dy) {
    final var hs = handles;
    final var is = new Handle[hs.length];
    for (var i = 0; i < hs.length; i++) {
      is[i] = new Handle(this, hs[i].getX() + dx, hs[i].getY() + dy);
    }
    setHandles(is);
  }
}
