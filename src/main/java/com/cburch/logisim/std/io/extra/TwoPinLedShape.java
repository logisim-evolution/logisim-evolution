/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.std.io.extra.TwoPinLed;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TwoPinLedShape extends DynamicElement {
  static final int DEFAULT_RADIUS = 5;
  static final Attribute<Integer> ATTR_RADIUS =
      Attributes.forIntegerRange("radius", S.getter("LedRadius"), 1, 20);

  int radius = DEFAULT_RADIUS;

  public TwoPinLedShape(int x, int y, DynamicElement.Path p) {
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
          DrawAttr.STROKE_WIDTH, ATTR_RADIUS, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR
        });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == ATTR_RADIUS) {
      return (V) Integer.valueOf(radius);
    }
    return super.getValue(attr);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == ATTR_RADIUS) {
      this.radius = ((Integer) value).intValue();
      this.bounds = Bounds.create(
          this.bounds.getX(), this.bounds.getY(),
          2 * this.radius, 2 * this.radius
      );
      return;
    }
    super.updateValue(attr, value);
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    final var attrs = path.leaf().getAttributeSet();
    final var offColor = attrs.getValue(IoLibrary.ATTR_OFF_COLOR);
    final var onColor = attrs.getValue(IoLibrary.ATTR_ON_COLOR);
    final var reverseColor = attrs.getValue(TwoPinLed.ATTR_BIPOLAR_REVERSE_COLOR);
    final var facing = attrs.getValue(StdAttr.FACING);
    final var bipolar = attrs.getValue(TwoPinLed.ATTR_BIPOLAR);
    final var ovalX = bounds.getX() + 1;
    final var ovalY = bounds.getY() + 1;
    final var ovalWidth = bounds.getWidth() - 2;
    final var ovalHeight = bounds.getHeight() - 2;

    GraphicsUtil.switchToWidth(g, strokeWidth);
    if (state == null) {
      g.setColor(offColor);
      g.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);
    } else {
      final var data = (InstanceDataSingleton) getData(state);
      final var ledState = TwoPinLed.getState(data);
      final Color ledColor;
      if (ledState == TwoPinLed.STATE_OFF) {
        ledColor = offColor;
      } else if (ledState == TwoPinLed.STATE_FORWARD) {
        ledColor = onColor;
      } else {
        ledColor = bipolar ? reverseColor : offColor;
      }
      g.setColor(ledColor);
      g.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);
    }
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawOval(ovalX, ovalY, ovalWidth, ovalHeight);

    final var centerX = bounds.getX() + bounds.getWidth() / 2;
    final var centerY = bounds.getY() + bounds.getHeight() / 2;
    final var radius = bounds.getWidth() / 2;
    drawDiodeSymbol(g, centerX, centerY, radius, facing != null ? facing : Direction.WEST, Math.max(1, radius / 8));

    drawLabel(g);
  }

  public static void drawDiodeSymbol(Graphics g, int centerX, int centerY, int radius, Direction facing, int xOffset) {
    GraphicsUtil.switchToWidth(g, 1);

    final int triangleBaseDistance = (int) Math.round(0.4 * radius);
    final int triangleTipDistance  = (int) Math.round(0.2 * radius);
    final int triangleHalfHeight   = (int) Math.round(0.45 * radius);
    final int cathodeBarHalfHeight = (int) Math.round(triangleHalfHeight * 0.8f);

    if (facing == Direction.WEST) {
      g.drawLine(centerX - radius, centerY, centerX + radius, centerY);

      final int triangleBaseX = centerX - triangleBaseDistance + xOffset;
      final int triangleTipX  = centerX + triangleTipDistance + xOffset;

      final var xPoints = new int[] { triangleBaseX, triangleBaseX, triangleTipX };
      final var yPoints = new int[] { centerY - triangleHalfHeight, centerY + triangleHalfHeight, centerY };
      g.fillPolygon(xPoints, yPoints, 3);
      g.drawLine(triangleTipX, centerY - cathodeBarHalfHeight, triangleTipX, centerY + cathodeBarHalfHeight);
    } else if (facing == Direction.EAST) {
      g.drawLine(centerX - radius, centerY, centerX + radius, centerY);

      final int triangleBaseX = centerX + triangleBaseDistance + xOffset;
      final int triangleTipX  = centerX - triangleTipDistance + xOffset;

      final var xPoints = new int[] { triangleBaseX, triangleBaseX, triangleTipX };
      final var yPoints = new int[] { centerY - triangleHalfHeight, centerY + triangleHalfHeight, centerY };
      g.fillPolygon(xPoints, yPoints, 3);
      g.drawLine(triangleTipX, centerY - cathodeBarHalfHeight, triangleTipX, centerY + cathodeBarHalfHeight);
    } else if (facing == Direction.NORTH) {
      g.drawLine(centerX, centerY - radius, centerX, centerY + radius);

      final int triangleBaseY = centerY + triangleBaseDistance;
      final int triangleTipY  = centerY - triangleTipDistance;

      final var xPoints = new int[] { centerX - triangleHalfHeight + xOffset, centerX + triangleHalfHeight + xOffset, centerX + xOffset };
      final var yPoints = new int[] { triangleBaseY, triangleBaseY, triangleTipY };
      g.fillPolygon(xPoints, yPoints, 3);
      g.drawLine(centerX - cathodeBarHalfHeight + xOffset, triangleTipY, centerX + cathodeBarHalfHeight + xOffset, triangleTipY);
    } else if (facing == Direction.SOUTH) {
      g.drawLine(centerX, centerY - radius, centerX, centerY + radius);

      final int triangleBaseY = centerY - triangleBaseDistance;
      final int triangleTipY  = centerY + triangleTipDistance;

      final var xPoints = new int[] { centerX - triangleHalfHeight + xOffset, centerX + triangleHalfHeight + xOffset, centerX + xOffset };
      final var yPoints = new int[] { triangleBaseY, triangleBaseY, triangleTipY };
      g.fillPolygon(xPoints, yPoints, 3);
      g.drawLine(centerX - cathodeBarHalfHeight + xOffset, triangleTipY, centerX + cathodeBarHalfHeight + xOffset, triangleTipY);
    }
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-two-pin-led"));
  }

  @Override
  public Element toSvgElement(Element ret) {
    ret = super.toSvgElement(ret);
    if (radius != DEFAULT_RADIUS) {
      ret.setAttribute("value-radius", "" + radius);
    }
    return ret;
  }

  @Override
  public void parseSvgElement(Element elt) {
    super.parseSvgElement(elt);
    if (elt.hasAttribute("value-radius")) {
      setValue(ATTR_RADIUS, Integer.valueOf(elt.getAttribute("value-radius")));
    }
  }

  @Override
  public String getDisplayName() {
    return S.get("twopinLEDComponent");
  }

  @Override
  public String toString() {
    return "TwoPinLed:" + getBounds();
  }
}
