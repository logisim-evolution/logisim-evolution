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

public class Rectangle extends Rectangular {
  public Rectangle(int x, int y, int w, int h) {
    super(x, y, w, h);
  }

  @Override
  protected boolean contains(int x, int y, int w, int h, Location q) {
    return isInRect(q.getX(), q.getY(), x, y, w, h);
  }

  @Override
  public void draw(Graphics g, int x, int y, int w, int h) {
    if (setForFill(g)) g.fillRect(x, y, w, h);
    if (setForStroke(g)) g.drawRect(x, y, w, h);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(getPaintType());
  }

  @Override
  public String getDisplayName() {
    return S.get("shapeRect");
  }

  @Override
  protected Location getRandomPoint(Bounds bds, Random rand) {
    if (getPaintType() != DrawAttr.PAINT_STROKE) {
      return super.getRandomPoint(bds, rand);
    }

    final var w = getWidth();
    final var h = getHeight();
    final var u = rand.nextInt(2 * w + 2 * h);
    var x = getX();
    var y = getY();
    if (u < w) {
      x += u;
    } else if (u < 2 * w) {
      x += (u - w);
      y += h;
    } else if (u < 2 * w + h) {
      y += (u - 2 * w);
    } else {
      x += w;
      y += (u - 2 * w - h);
    }
    final var d = getStrokeWidth();
    if (d > 1) {
      x += rand.nextInt(d) - d / 2;
      y += rand.nextInt(d) - d / 2;
    }
    return Location.create(x, y, false);
  }

  @Override
  public boolean matches(CanvasObject other) {
    return (other instanceof Rectangle) && super.matches(other);
  }

  @Override
  public String toString() {
    return "Rectangle:" + getBounds();
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createRectangle(doc, this);
  }
}
