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

  public SevenSegmentShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 14, 20));
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
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
        g.fillRect(x + seg[0], y + seg[1], seg[2], seg[3]);
      } else {
        g.fillOval(x + 11, y + 17, 2, 2); // draw decimal point
      }
    }
    drawLabel(g);
  }

  static final int[][] SEGMENTS =
      new int[][] {
        new int[] {3, 1, 6, 2},
        new int[] {9, 3, 2, 6},
        new int[] {9, 11, 2, 6},
        new int[] {3, 17, 6, 2},
        new int[] {1, 11, 2, 6},
        new int[] {1, 3, 2, 6},
        new int[] {3, 9, 6, 2},
      };

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-sevensegment"));
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
