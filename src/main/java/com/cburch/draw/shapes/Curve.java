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
import lombok.Getter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Curve extends FillableCanvasObject {
  @Getter private Location end0;
  @Getter private Location control;
  @Getter private Location end1;
  @Getter private Bounds bounds;

  public Curve(Location end0, Location end1, Location ctrl) {
    this.end0 = end0;
    this.control = ctrl;
    this.end1 = end1;
    bounds = CurveUtil.getBounds(toArray(this.end0), toArray(control), toArray(this.end1));
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
      final var q = toArray(loc);
      final var p0 = toArray(this.end0);
      final var p1 = toArray(this.control);
      final var p2 = toArray(this.end1);
      final var p = CurveUtil.findNearestPoint(q, p0, p1, p2);
      if (p == null) return false;

      final var stroke = getStrokeWidth();
      final var thr = (type == DrawAttr.PAINT_STROKE) ? Math.max(Line.ON_LINE_THRESH, stroke / 2) : stroke / 2;
      if (LineUtil.distanceSquared(p[0], p[1], q[0], q[1]) < thr * thr) return true;
    }
    return (type != DrawAttr.PAINT_STROKE) ? getCurve(null).contains(loc.getX(), loc.getY()) : false;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(getPaintType());
  }

  private QuadCurve2D getCurve(HandleGesture gesture) {
    final var p = getHandleArray(gesture);
    return new QuadCurve2D.Double(p[0].getX(), p[0].getY(), p[1].getX(), p[1].getY(), p[2].getX(), p[2].getY());
  }

  public QuadCurve2D getCurve2D() {
    return new QuadCurve2D.Double(end0.getX(), end0.getY(), control.getX(), control.getY(), end1.getX(), end1.getY());
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeCurve");
  }

  private Handle[] getHandleArray(HandleGesture gesture) {
    if (gesture == null) {
      return new Handle[] {new Handle(this, end0), new Handle(this, control), new Handle(this, end1)};
    }

    final var g = gesture.getHandle();
    var gx = g.getX() + gesture.getDeltaX();
    var gy = g.getY() + gesture.getDeltaY();
    Handle[] ret = {new Handle(this, end0), new Handle(this, control), new Handle(this, end1)};
    if (g.isAt(end0)) {
      ret[0] =
          gesture.isShiftDown()
              ? new Handle(this, LineUtil.snapTo8Cardinals(end1, gx, gy))
              : new Handle(this, gx, gy);
    } else if (g.isAt(end1)) {
      ret[2] =
          gesture.isShiftDown()
              ? new Handle(this, LineUtil.snapTo8Cardinals(end0, gx, gy))
              : new Handle(this, gx, gy);
    } else if (g.isAt(control)) {
      if (gesture.isShiftDown()) {
        final var x0 = end0.getX();
        final var y0 = end0.getY();
        final var x1 = end1.getX();
        final var y1 = end1.getY();
        final var midx = (x0 + x1) / 2;
        final var midy = (y0 + y1) / 2;
        final var dx = x1 - x0;
        final var dy = y1 - y0;
        final var p = LineUtil.nearestPointInfinite(gx, gy, midx, midy, midx - dy, midy + dx);
        gx = (int) Math.round(p[0]);
        gy = (int) Math.round(p[1]);
      }
      if (gesture.isAltDown()) {
        final double[] e0 = {end0.getX(), end0.getY()};
        final double[] e1 = {end1.getX(), end1.getY()};
        final double[] mid = {gx, gy};
        final var ct = CurveUtil.interpolate(e0, e1, mid);
        gx = (int) Math.round(ct[0]);
        gy = (int) Math.round(ct[1]);
      }
      ret[1] = new Handle(this, gx, gy);
    }
    return ret;
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
    return (other instanceof Curve that)
           ? this.end0.equals(that.end0) && this.control.equals(that.control) && this.end1.equals(that.end1) && super.matches(that)
           : false;
  }

  @Override
  public int matchesHashCode() {
    var ret = end0.hashCode();
    ret = ret * 31 * 31 + control.hashCode();
    ret = ret * 31 * 31 + end1.hashCode();
    ret = ret * 31 + super.matchesHashCode();
    return ret;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    final var hs = getHandleArray(gesture);
    Handle ret = null;
    if (!hs[0].getLocation().equals(end0)) {
      end0 = hs[0].getLocation();
      ret = hs[0];
    }
    if (!hs[1].getLocation().equals(control)) {
      control = hs[1].getLocation();
      ret = hs[1];
    }
    if (!hs[2].getLocation().equals(end1)) {
      end1 = hs[2].getLocation();
      ret = hs[2];
    }
    bounds = CurveUtil.getBounds(toArray(end0), toArray(control), toArray(end1));
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
    end0 = end0.translate(dx, dy);
    control = control.translate(dx, dy);
    end1 = end1.translate(dx, dy);
    bounds = bounds.translate(dx, dy);
  }
}
