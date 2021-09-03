/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LedShape extends DynamicElement {
  static final int DEFAULT_RADIUS = 5;

  public LedShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 2 * DEFAULT_RADIUS, 2 * DEFAULT_RADIUS));
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    final var x = bounds.getX();
    final var y = bounds.getY();
    final var w = bounds.getWidth();
    final var h = bounds.getHeight();
    final var qx = loc.getX();
    final var qy = loc.getY();
    final var dx = qx - (x + 0.5 * w);
    final var dy = qy - (y + 0.5 * h);
    final var sum = (dx * dx) / (w * w) + (dy * dy) / (h * h);

    return sum <= 0.25;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {
          DrawAttr.STROKE_WIDTH, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR
        });
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    final var offColor = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_OFF_COLOR);
    final var onColor = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_ON_COLOR);
    final var x = bounds.getX() + 1;
    final var y = bounds.getY() + 1;
    final var w = bounds.getWidth() - 2;
    final var h = bounds.getHeight() - 2;
    GraphicsUtil.switchToWidth(g, strokeWidth);
    if (state == null) {
      g.setColor(offColor);
      g.fillOval(x, y, w, h);
      g.setColor(DynamicElement.COLOR);
    } else {
      final var activ = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_ACTIVE);
      Object desired = activ ? Value.TRUE : Value.FALSE;
      final var data = (InstanceDataSingleton) getData(state);
      final var val = data == null ? Value.FALSE : (Value) data.getValue();
      g.setColor(val == desired ? onColor : offColor);
      g.fillOval(x, y, w, h);
      g.setColor(Color.darkGray);
    }
    g.drawOval(x, y, w, h);
    drawLabel(g);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-led"));
  }

  @Override
  public String getDisplayName() {
    return S.get("ledComponent");
  }

  @Override
  public String toString() {
    return "Led:" + getBounds();
  }
}
