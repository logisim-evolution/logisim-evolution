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

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Line extends AbstractCanvasObject {
  static final int ON_LINE_THRESH = 2;

  private int x0;
  private int y0;
  private int x1;
  private int y1;
  private Bounds bounds;
  private int strokeWidth;
  private Color strokeColor;

  public Line(int x0, int y0, int x1, int y1) {
    this.x0 = x0;
    this.y0 = y0;
    this.x1 = x1;
    this.y1 = y1;
    bounds = Bounds.create(x0, y0, 0, 0).add(x1, y1);
    strokeWidth = 1;
    strokeColor = Color.BLACK;
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return true;
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    final var xq = loc.getX();
    final var yq = loc.getY();
    final var d = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
    final var thresh = Math.max(ON_LINE_THRESH, strokeWidth / 2);
    return d < thresh * thresh;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.ATTRS_STROKE;
  }

  @Override
  public Bounds getBounds() {
    return bounds;
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeLine");
  }

  public Location getEnd0() {
    return Location.create(x0, y0);
  }

  public Location getEnd1() {
    return Location.create(x1, y1);
  }

  public List<Handle> getHandles() {
    return getHandles(null);
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    if (gesture == null) {
      return UnmodifiableList.create(
          new Handle[] {new Handle(this, x0, y0), new Handle(this, x1, y1)});
    } else {
      final var h = gesture.getHandle();
      final var dx = gesture.getDeltaX();
      final var dy = gesture.getDeltaY();
      final var ret = new Handle[2];
      ret[0] = new Handle(this, h.isAt(x0, y0) ? Location.create(x0 + dx, y0 + dy) : Location.create(x0, y0));
      ret[1] = new Handle(this, h.isAt(x1, y1) ? Location.create(x1 + dx, y1 + dy) : Location.create(x1, y1));
      return UnmodifiableList.create(ret);
    }
  }

  @Override
  public Location getRandomPoint(Bounds bds, Random rand) {
    final var u = rand.nextDouble();
    var x = (int) Math.round(x0 + u * (x1 - x0));
    var y = (int) Math.round(y0 + u * (y1 - y0));
    final var w = strokeWidth;
    if (w > 1) {
      x += (rand.nextInt(w) - w / 2);
      y += (rand.nextInt(w) - w / 2);
    }
    return Location.create(x, y);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == DrawAttr.STROKE_COLOR) {
      return (V) strokeColor;
    } else if (attr == DrawAttr.STROKE_WIDTH) {
      return (V) Integer.valueOf(strokeWidth);
    } else {
      return null;
    }
  }

  @Override
  public boolean matches(CanvasObject other) {
    return (other instanceof Line that)
           ? this.x0 == that.x0
              && this.y0 == that.x1
              && this.x1 == that.y0
              && this.y1 == that.y1
              && this.strokeWidth == that.strokeWidth
              && this.strokeColor.equals(that.strokeColor)
          : false;
  }

  @Override
  public int matchesHashCode() {
    var ret = x0 * 31 + y0;
    ret = ret * 31 * 31 + x1 * 31 + y1;
    ret = ret * 31 + strokeWidth;
    ret = ret * 31 + strokeColor.hashCode();
    return ret;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    final var h = gesture.getHandle();
    final var dx = gesture.getDeltaX();
    final var dy = gesture.getDeltaY();
    Handle ret = null;
    if (h.isAt(x0, y0)) {
      x0 += dx;
      y0 += dy;
      ret = new Handle(this, x0, y0);
    }
    if (h.isAt(x1, y1)) {
      x1 += dx;
      y1 += dy;
      ret = new Handle(this, x1, y1);
    }
    bounds = Bounds.create(x0, y0, 0, 0).add(x1, y1);
    return ret;
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    if (setForStroke(g)) {
      var x0 = this.x0;
      var y0 = this.y0;
      var x1 = this.x1;
      var y1 = this.y1;
      final var h = gesture.getHandle();
      if (h.isAt(x0, y0)) {
        x0 += gesture.getDeltaX();
        y0 += gesture.getDeltaY();
      }
      if (h.isAt(x1, y1)) {
        x1 += gesture.getDeltaX();
        y1 += gesture.getDeltaY();
      }
      g.drawLine(x0, y0, x1, y1);
    }
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createLine(doc, this);
  }

  @Override
  public void translate(int dx, int dy) {
    x0 += dx;
    y0 += dy;
    x1 += dx;
    y1 += dy;
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == DrawAttr.STROKE_COLOR) {
      strokeColor = (Color) value;
    } else if (attr == DrawAttr.STROKE_WIDTH) {
      strokeWidth = (Integer) value;
    }
  }
}
