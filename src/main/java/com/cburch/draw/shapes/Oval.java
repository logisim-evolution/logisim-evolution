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

public class Oval extends Rectangular {
  public Oval(int x, int y, int w, int h) {
    super(x, y, w, h);
  }

  @Override
  protected boolean contains(int x, int y, int w, int h, Location q) {
    int qx = q.getX();
    int qy = q.getY();
    double dx = qx - (x + 0.5 * w);
    double dy = qy - (y + 0.5 * h);
    double sum = (dx * dx) / (w * w) + (dy * dy) / (h * h);
    return sum <= 0.25;
  }

  @Override
  public void draw(Graphics g, int x, int y, int w, int h) {
    if (setForFill(g)) g.fillOval(x, y, w, h);
    if (setForStroke(g)) g.drawOval(x, y, w, h);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(getPaintType());
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeOval");
  }

  @Override
  protected Location getRandomPoint(Bounds bds, Random rand) {
    if (getPaintType() == DrawAttr.PAINT_STROKE) {
      double rx = getWidth() / 2.0;
      double ry = getHeight() / 2.0;
      double u = 2 * Math.PI * rand.nextDouble();
      int x = (int) Math.round(getX() + rx + rx * Math.cos(u));
      int y = (int) Math.round(getY() + ry + ry * Math.sin(u));
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
  public boolean matches(CanvasObject other) {
    if (other instanceof Oval) {
      return super.matches(other);
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    return super.matchesHashCode();
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createOval(doc, this);
  }
}
