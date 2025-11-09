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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SevenSegmentShape extends DynamicElement {
  static final int PADDING = 1;

  static final int DEFAULT_SCALE = 2;
  // TODO: localization
  static final Attribute<Integer> ATTR_SCALE =
      Attributes.forIntegerRange("scale", 1, 6);

  int scale = DEFAULT_SCALE;

  public SevenSegmentShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 14, 20));
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {ATTR_SCALE, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == ATTR_SCALE) {
      return (V) Integer.valueOf(this.scale);
    }
    return super.getValue(attr);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == ATTR_SCALE) {
      final var w = 6;
      final var h = 9;
      this.scale = ((Integer) value).intValue();
      this.bounds = Bounds.create(
        this.bounds.getX(), this.bounds.getY(),
        this.scale * w + PADDING * 2, this.scale * h + PADDING * 2
      );
      return;
    }
    super.updateValue(attr, value);
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    final var offColor = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_OFF_COLOR);
    final var onColor = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_ON_COLOR);
    final var bgColor = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_BACKGROUND);
    final var x = bounds.getX();
    final var y = bounds.getY();
    final var w = bounds.getWidth();
    final var h = bounds.getHeight();
    GraphicsUtil.switchToWidth(g, 1);
    if (bgColor.getAlpha() != 0) {
      g.setColor(bgColor);
      g.fillRect(x, y, w, h);
    }
    g.setColor(Color.BLACK);
    g.drawRect(x, y, w, h);
    g.setColor(Color.DARK_GRAY);
    var summ = 0;
    var desired = 1;
    if (state != null) {
      InstanceDataSingleton data = (InstanceDataSingleton) getData(state);
      summ = (data == null ? 0 : (Integer) data.getValue());
      final var activ = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_ACTIVE);
      desired = activ == null || activ ? 1 : 0;
    }
    g.setColor(Color.DARK_GRAY);
    for (var i = 0; i <= 7; i++) {
      if (state != null) {
        g.setColor(((summ >> i) & 1) == desired ? onColor : offColor);
      }
      if (i < 7) {
        int[] seg = SEGMENTS[i];
        g.fillRect(
          x + PADDING + seg[0] * scale, y + PADDING + seg[1] * scale,
          seg[2] * scale, seg[3] * scale
        );
      } else {
        g.fillOval(
          x + PADDING + 5 * scale, y + PADDING + 8 * scale,
          1 * scale, 1 * scale
        ); // draw decimal point
      }
    }
    drawLabel(g);
  }

  static final int[][] SEGMENTS =
      new int[][] {
        new int[] {1, 0, 3, 1},
        new int[] {4, 1, 1, 3},
        new int[] {4, 5, 1, 3},
        new int[] {1, 8, 3, 1},
        new int[] {0, 5, 1, 3},
        new int[] {0, 1, 1, 3},
        new int[] {1, 4, 3, 1},
      };

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-sevensegment"));
  }
  
  @Override
  public Element toSvgElement(Element ret) {
    ret = super.toSvgElement(ret);
    if (this.scale != DEFAULT_SCALE) {
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
  public String getDisplayName() {
    return S.get("sevenSegmentComponent");
  }

  @Override
  public String toString() {
    return "Seven Segment:" + getBounds();
  }
}
