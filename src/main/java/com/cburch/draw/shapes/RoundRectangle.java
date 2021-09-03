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
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RoundRectangle extends Rectangular {
  private int radius;

  public RoundRectangle(int x, int y, int w, int h) {
    super(x, y, w, h);
    this.radius = 10;
  }

  private static boolean inCircle(int qx, int qy, int cx, int cy, int rx, int ry) {
    double dx = qx - cx;
    double dy = qy - cy;
    double sum = (dx * dx) / (4 * rx * rx) + (dy * dy) / (4 * ry * ry);
    return sum <= 0.25;
  }

  @Override
  protected boolean contains(int x, int y, int w, int h, Location q) {
    int qx = q.getX();
    int qy = q.getY();
    int rx = radius;
    int ry = radius;
    if (2 * rx > w) rx = w / 2;
    if (2 * ry > h) ry = h / 2;
    if (!isInRect(qx, qy, x, y, w, h)) {
      return false;
    } else if (qx < x + rx) {
      if (qy < y + ry) return inCircle(qx, qy, x + rx, y + ry, rx, ry);
      else if (qy < y + h - ry) return true;
      else return inCircle(qx, qy, x + rx, y + h - ry, rx, ry);
    } else if (qx < x + w - rx) {
      return true;
    } else {
      if (qy < y + ry) return inCircle(qx, qy, x + w - rx, y + ry, rx, ry);
      else if (qy < y + h - ry) return true;
      else return inCircle(qx, qy, x + w - rx, y + h - ry, rx, ry);
    }
  }

  @Override
  public void draw(Graphics g, int x, int y, int w, int h) {
    int diam = 2 * radius;
    if (setForFill(g)) g.fillRoundRect(x, y, w, h, diam, diam);
    if (setForStroke(g)) g.drawRoundRect(x, y, w, h, diam, diam);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getRoundRectAttributes(getPaintType());
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeRoundRect");
  }

  @Override
  protected Location getRandomPoint(Bounds bds, Random rand) {
    if (getPaintType() == DrawAttr.PAINT_STROKE) {
      int w = getWidth();
      int h = getHeight();
      int r = radius;
      int horz = Math.max(0, w - 2 * r); // length of horizontal segment
      int vert = Math.max(0, h - 2 * r);
      double len = 2 * horz + 2 * vert + 2 * Math.PI * r;
      double u = len * rand.nextDouble();
      int x = getX();
      int y = getY();
      if (u < horz) {
        x += r + (int) u;
      } else if (u < 2 * horz) {
        x += r + (int) (u - horz);
        y += h;
      } else if (u < 2 * horz + vert) {
        y += r + (int) (u - 2 * horz);
      } else if (u < 2 * horz + 2 * vert) {
        x += w;
        y += (u - 2 * w - h);
      } else {
        int rx = radius;
        int ry = radius;
        if (2 * rx > w) rx = w / 2;
        if (2 * ry > h) ry = h / 2;
        u = 2 * Math.PI * rand.nextDouble();
        int dx = (int) Math.round(rx * Math.cos(u));
        int dy = (int) Math.round(ry * Math.sin(u));
        if (dx < 0) {
          x += r + dx;
        } else {
          x += r + horz + dx;
        }
        if (dy < 0) {
          y += r + dy;
        } else {
          y += r + vert + dy;
        }
      }

      int d = getStrokeWidth();
      if (d > 1) {
        x += rand.nextInt(d) - d / 2;
        y += rand.nextInt(d) - d / 2;
      }
      return Location.create(x, y);
    } else {
      return super.getRandomPoint(bds, rand);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == DrawAttr.CORNER_RADIUS) {
      return (V) Integer.valueOf(radius);
    } else {
      return super.getValue(attr);
    }
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof RoundRectangle) {
      RoundRectangle that = (RoundRectangle) other;
      return super.matches(other) && this.radius == that.radius;
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    return super.matchesHashCode() * 31 + radius;
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createRoundRectangle(doc, this);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == DrawAttr.CORNER_RADIUS) {
      radius = (Integer) value;
    } else {
      super.updateValue(attr, value);
    }
  }
}
