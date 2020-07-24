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

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.draw.shapes.SvgCreator;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RegisterShape extends DynamicElement {
  static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 10);

  private EditableLabel label;

  public RegisterShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 1, 1));
    label = new EditableLabel(x, y, "0", DEFAULT_FONT);
    label.setColor(Color.BLACK);
    label.setHorizontalAlignment(EditableLabel.CENTER);
    label.setVerticalAlignment(EditableLabel.MIDDLE);
    calculateBounds();
  }

  void calculateBounds() {
    BitWidth widthVal = path.leaf().getAttributeSet().getValue(StdAttr.WIDTH);
    int width = (widthVal == null ? 8 : widthVal.getWidth());
    String zeros = StringUtil.toHexString(width, 0);
    label.setText(zeros);
    int x = bounds.getX();
    int y = bounds.getY();
    bounds = StringUtil.estimateBounds(zeros, label.getFont()).translate(x, y);
    label.setLocation(bounds.getCenterX(), bounds.getCenterY());
  }

  @Override
  public void translate(int dx, int dy) {
    super.translate(dx, dy);
    label.setLocation(bounds.getX(), bounds.getY());
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {Text.ATTR_FONT, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Text.ATTR_FONT) {
      return (V) label.getFont();
    }
    return super.getValue(attr);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == Text.ATTR_FONT) {
      label.setFont((Font) value);
      calculateBounds();
    }
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    calculateBounds();
    int x = bounds.getX();
    int y = bounds.getY();
    int w = bounds.getWidth();
    int h = bounds.getHeight();
    GraphicsUtil.switchToWidth(g, 1);
    if (state == null) {
      g.setColor(Color.lightGray);
      g.fillRect(x, y, w, h);
    }
    g.setColor(Color.BLACK);
    g.drawRect(x, y, w, h);
    if (state != null) {
      BitWidth widthVal = path.leaf().getAttributeSet().getValue(StdAttr.WIDTH);
      int width = (widthVal == null ? 8 : widthVal.getWidth());
      RegisterData data = (RegisterData) getData(state);
      long val = data == null ? 0 : data.value.toLongValue();
      label.setText(StringUtil.toHexString(width, val));
    }
    label.paint(g);
    drawLabel(g);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-register"));
  }

  public Element toSvgElement(Element ret) {
    ret = super.toSvgElement(ret);
    Font font = label.getFont();
    if (!font.equals(DEFAULT_FONT)) SvgCreator.setFontAttribute(ret, font, "value-");
    return ret;
  }

  public void parseSvgElement(Element elt) {
    super.parseSvgElement(elt);
    setValue(Text.ATTR_FONT, SvgReader.getFontAttribute(elt, "value-", "monospaced", 10));
  }

  @Override
  public String getDisplayName() {
    return S.get("registerComponent");
  }

  @Override
  public String toString() {
    return "Register:" + getBounds();
  }
}
