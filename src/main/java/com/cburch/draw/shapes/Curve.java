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
import java.awt.Graphics2D;
import java.awt.geom.QuadCurve2D;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Curve extends FillableCanvasObject {
  private Location p0;
  private Location p1;
  private Location p2;
  private Bounds bounds;

  public Curve(Location end0, Location end1, Location ctrl) {
    this.p0 = end0;
    this.p1 = ctrl;
    this.p2 = end1;
    bounds = CurveUtil.getBounds(toArray(p0), toArray(p1), toArray(p2));
  }

  private static double[] toArray(Location loc) {
    return new double[] {loc.getX(), loc.getY()};
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return true;
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    Object type = getPaintType();
    if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
      type = DrawAttr.PAINT_STROKE_FILL;
    }
    if (type != DrawAttr.PAINT_FILL) {
      final var stroke = getStrokeWidth();
      final var q = toArray(loc);
      final var p0 = toArray(this.p0);
      final var p1 = toArray(this.p1);
      final var p2 = toArray(this.p2);
      final var p = CurveUtil.findNearestPoint(q, p0, p1, p2);
      if (p == null) return false;

      final var thr = (type == DrawAttr.PAINT_STROKE) ? Math.max(Line.ON_LINE_THRESH, stroke / 2) : stroke / 2;
      if (LineUtil.distanceSquared(p[0], p[1], q[0], q[1]) < thr * thr) {
        return true;
      }
    }
    if (type != DrawAttr.PAINT_STROKE) {
      return getCurve(null).contains(loc.getX(), loc.getY());
    }
    return false;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(getPaintType());
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  public Location getControl() {
    return p1;
  }

  private QuadCurve2D getCurve(HandleGesture gesture) {
    final var p = getHandleArray(gesture);
    return new QuadCurve2D.Double(p[0].getX(), p[0].getY(), p[1].getX(), p[1].getY(), p[2].getX(), p[2].getY());
  }

  public QuadCurve2D getCurve2D() {
    return new QuadCurve2D.Double(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeCurve");
  }

  public Location getEnd0() {
    return p0;
  }

  public Location getEnd1() {
    return p2;
  }

  private Handle[] getHandleArray(HandleGesture gesture) {
    if (gesture == null) {
      return new Handle[] {new Handle(this, p0), new Handle(this, p1), new Handle(this, p2)};
    } else {
      Handle g = gesture.getHandle();
      int gx = g.getX() + gesture.getDeltaX();
      int gy = g.getY() + gesture.getDeltaY();
      Handle[] ret = {new Handle(this, p0), new Handle(this, p1), new Handle(this, p2)};
      if (g.isAt(p0)) {
        if (gesture.isShiftDown()) {
          Location p = LineUtil.snapTo8Cardinals(p2, gx, gy);
          ret[0] = new Handle(this, p);
        } else {
          ret[0] = new Handle(this, gx, gy);
        }
      } else if (g.isAt(p2)) {
        if (gesture.isShiftDown()) {
          Location p = LineUtil.snapTo8Cardinals(p0, gx, gy);
          ret[2] = new Handle(this, p);
        } else {
          ret[2] = new Handle(this, gx, gy);
        }
      } else if (g.isAt(p1)) {
        if (gesture.isShiftDown()) {
          double x0 = p0.getX();
          double y0 = p0.getY();
          double x1 = p2.getX();
          double y1 = p2.getY();
          double midx = (x0 + x1) / 2;
          double midy = (y0 + y1) / 2;
          double dx = x1 - x0;
          double dy = y1 - y0;
          double[] p = LineUtil.nearestPointInfinite(gx, gy, midx, midy, midx - dy, midy + dx);
          gx = (int) Math.round(p[0]);
          gy = (int) Math.round(p[1]);
        }
        if (gesture.isAltDown()) {
          double[] e0 = {p0.getX(), p0.getY()};
          double[] e1 = {p2.getX(), p2.getY()};
          double[] mid = {gx, gy};
          double[] ct = CurveUtil.interpolate(e0, e1, mid);
          gx = (int) Math.round(ct[0]);
          gy = (int) Math.round(ct[1]);
        }
        ret[1] = new Handle(this, gx, gy);
      }
      return ret;
    }
  }

  public List<Handle> getHandles() {
    return UnmodifiableList.create(getHandleArray(null));
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    return UnmodifiableList.create(getHandleArray(gesture));
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof Curve) {
      final var that = (Curve) other;
      return this.p0.equals(that.p0)
          && this.p1.equals(that.p1)
          && this.p2.equals(that.p2)
          && super.matches(that);
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    int ret = p0.hashCode();
    ret = ret * 31 * 31 + p1.hashCode();
    ret = ret * 31 * 31 + p2.hashCode();
    ret = ret * 31 + super.matchesHashCode();
    return ret;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    final var hs = getHandleArray(gesture);
    Handle ret = null;
    if (!hs[0].getLocation().equals(p0)) {
      p0 = hs[0].getLocation();
      ret = hs[0];
    }
    if (!hs[1].getLocation().equals(p1)) {
      p1 = hs[1].getLocation();
      ret = hs[1];
    }
    if (!hs[2].getLocation().equals(p2)) {
      p2 = hs[2].getLocation();
      ret = hs[2];
    }
    bounds = CurveUtil.getBounds(toArray(p0), toArray(p1), toArray(p2));
    return ret;
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    final var curve = getCurve(gesture);
    if (setForFill(g)) {
      ((Graphics2D) g).fill(curve);
    }
    if (setForStroke(g)) {
      ((Graphics2D) g).draw(curve);
    }
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createCurve(doc, this);
  }

  @Override
  public void translate(int dx, int dy) {
    p0 = p0.translate(dx, dy);
    p1 = p1.translate(dx, dy);
    p2 = p2.translate(dx, dy);
    bounds = bounds.translate(dx, dy);
  }
}
