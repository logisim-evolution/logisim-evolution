/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
    Color offColor = path.leaf().getAttributeSet().getValue(Io.ATTR_OFF_COLOR);
    Color onColor = path.leaf().getAttributeSet().getValue(Io.ATTR_ON_COLOR);
    Color bgColor = path.leaf().getAttributeSet().getValue(Io.ATTR_BACKGROUND);
    int x = bounds.getX();
    int y = bounds.getY();
    int w = bounds.getWidth();
    int h = bounds.getHeight();
    GraphicsUtil.switchToWidth(g, 1);
    if (bgColor.getAlpha() != 0) {
      g.setColor(bgColor);
      g.fillRect(x, y, w, h);
    }
    g.setColor(Color.BLACK);
    g.drawRect(x, y, w, h);
    g.setColor(Color.DARK_GRAY);
    int summ = 0, desired = 1;
    if (state != null) {
      InstanceDataSingleton data = (InstanceDataSingleton) getData(state);
      summ = (data == null ? 0 : ((Integer) data.getValue()).intValue());
      Boolean activ = path.leaf().getAttributeSet().getValue(Io.ATTR_ACTIVE);
      desired = activ == null || activ.booleanValue() ? 1 : 0;
    }
    g.setColor(Color.DARK_GRAY);
    for (int i = 0; i <= 7; i++) {
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

  static final int SEGMENTS[][] =
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
