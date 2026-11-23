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
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class DotMatrixBaseShape extends DynamicElement {

  static final int DEFAULT_SCALE = 4;  // FOR-REVIEW: is this a good value?
  // FOR-REVIEW: should this be somewhere else?
  // TODO: internationalization
  static final Attribute<Integer> ATTR_SCALE =
      Attributes.forIntegerRange("scale", S.getter("ioMatrixScale"), 1, 10);
  int scale = DEFAULT_SCALE;

  public DotMatrixBaseShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 0, 0));
    bounds = matrixBounds(x, y, scale);
  }

  protected int matrixScaleX() {
    return ((DotMatrixBase) path.leaf().getFactory()).scaleX;
  }

  protected int matrixScaleY() {
    return ((DotMatrixBase) path.leaf().getFactory()).scaleY;
  }

  /** {@link Direction#EAST} draws segments to the right. Other values rotate that grid. */
  protected Direction matrixFacing() {
    return Direction.EAST;
  }

  protected Bounds matrixBounds(int x, int y, int cellScale) {
    final var factory = (DotMatrixBase) path.leaf().getFactory();
    final var cols = path.leafGetAttributeValue(factory.getAttributeColumns()).getWidth();
    final var rows = path.leafGetAttributeValue(factory.getAttributeRows()).getWidth();
    final var along = cols * cellScale * matrixScaleX();
    final var across = rows * cellScale * matrixScaleY();
    final var facing = matrixFacing();
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      return Bounds.create(x, y, across, along);
    }
    return Bounds.create(x, y, along, across);
  }

  protected void syncMatrixBounds() {
    bounds = matrixBounds(bounds.getX(), bounds.getY(), scale);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {
          DrawAttr.STROKE_WIDTH, ATTR_SCALE, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR
        }
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == ATTR_SCALE) {
      return (V) Integer.valueOf(scale);
    }
    return super.getValue(attr);
  }

  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == ATTR_SCALE) {
      this.scale = ((Integer) value).intValue();
      syncMatrixBounds();
    }
    super.updateValue(attr, value);
  }

  public void drawShape(Graphics g, int x, int y, AttributeOption shape, int scaleX, int scaleY) {
    if (DotMatrixBase.SHAPE_SQUARE.equals(shape)) {
      g.fillRect(x, y, scale * scaleX, scale * scaleY);
    } else if (DotMatrixBase.SHAPE_PADDED_SQUARE.equals(shape)) {
      final var padding = (int) Math.floor(scale / (1.0 + 4.0 + 1.0));
      g.fillRect(
          x + padding * scaleX, y + padding,
          (scale - padding * 2) * scaleX, (scale - padding * 2) * scaleY
      );
    } else {
      // DotMatrixBase.SHAPE_CIRCLE is default shape
      final var padding = (int) Math.round(scale / (1.0 + 8.0 + 1.0));
      g.fillOval(
          x + padding * scaleX, y + padding * scaleY,
          (scale - 2 * padding) * scaleX, (scale - 2 * padding) * scaleY
      );
    }
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    syncMatrixBounds();
    final var onColor = path.leafGetAttributeValue(IoLibrary.ATTR_ON_COLOR);
    final var offColor = path.leafGetAttributeValue(IoLibrary.ATTR_OFF_COLOR);

    final var factory = (DotMatrixBase) this.path.leaf().getFactory();

    final var scaleX = matrixScaleX();
    final var scaleY = matrixScaleY();
    final var shape = path.leafGetAttributeValue(factory.getAttributeShape());
    final var facing = matrixFacing();

    // FIXME: path.leaf().getInstanceStateImpl() always returns null here

    final var instanceState = state != null ? state.getInstanceState(path.leaf()) : null;
    final var data = instanceState != null ? factory.getState(instanceState) : null;
    final var ticks = instanceState != null ? instanceState.getTickCount() : null;

    final var rows = this.path.leafGetAttributeValue(factory.getAttributeRows()).getWidth();
    final var cols = this.path.leafGetAttributeValue(factory.getAttributeColumns()).getWidth();
    final var along = cols * scale * scaleX;
    final var across = rows * scale * scaleY;

    AffineTransform previous = null;
    if (g instanceof Graphics2D g2) {
      previous = g2.getTransform();
      g2.translate(bounds.getX(), bounds.getY());
      if (facing == Direction.NORTH) {
        g2.translate(0, along);
        g2.rotate(-Math.PI / 2);
      } else if (facing == Direction.WEST) {
        g2.translate(along, across);
        g2.rotate(Math.PI);
      } else if (facing == Direction.SOUTH) {
        g2.translate(across, 0);
        g2.rotate(Math.PI / 2);
      }
    }

    try {
      for (var j = 0; j < rows; j++) {
        for (var i = 0; i < cols; i++) {
          int x = i * scale * scaleX;
          int y = j * scale * scaleY;
          if (previous == null) {
            x += bounds.getX();
            y += bounds.getY();
          }

          if (state == null || data == null) {
            g.setColor(Value.errorColor);
            this.drawShape(g, x, y, shape, scaleX, scaleY);
            continue;
          }

          final var val = data.get(j, i, ticks);
          Color c;
          if (val == Value.TRUE) {
            c = onColor;
          } else if (val == Value.FALSE) {
            c = offColor;
          } else {
            c = Value.errorColor;
          }
          g.setColor(c);
          this.drawShape(g, x, y, shape, scaleX, scaleY);
        }
      }

      if (factory.drawBorder) {
        g.setColor(Color.DARK_GRAY);
        GraphicsUtil.switchToWidth(g, this.getValue(DrawAttr.STROKE_WIDTH));
        if (previous == null) {
          g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        } else {
          g.drawRect(0, 0, along, across);
        }
        GraphicsUtil.switchToWidth(g, 1);
      }
    } finally {
      if (previous != null) {
        ((Graphics2D) g).setTransform(previous);
      }
    }
    drawLabel(g);
  }

  @Override
  public abstract Element toSvgElement(Document doc);

  @Override
  public Element toSvgElement(Element ret) {
    ret = super.toSvgElement(ret);
    if (scale != DEFAULT_SCALE) {
      ret.setAttribute("value-scale", "" + scale);
    }
    return ret;
  }

  @Override
  public void parseSvgElement(Element elt) {
    super.parseSvgElement(elt);
    if (elt.hasAttribute("value-scale")) {
      setValue(ATTR_SCALE, Integer.valueOf(elt.getAttribute("value-scale")));
    }
  }

  @Override
  public abstract String getDisplayName();

  @Override
  public String toString() {
    return "DotMatrixBase:" + getBounds();
  }
}
