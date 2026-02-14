/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;
import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import com.cburch.draw.util.EditableLabel;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.draw.shapes.SvgCreator;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProbeShape extends DynamicElement {
  static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 10);

  private EditableLabel label;

  public ProbeShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 1, 1));
    label = new EditableLabel(x, y, "xxx", DEFAULT_FONT);
    label.setColor(Color.BLACK);
    label.setHorizontalAlignment(EditableLabel.CENTER);
    label.setVerticalAlignment(EditableLabel.MIDDLE);
    calculateBounds();
  }

  void calculateBounds() {
    // Probe changes size dynamically, so let's just use a
    // modest example value for each radix. Or simpler yet,
    // let's assume 8 zeros is about the biggest anyone wants
    // to display.
    String text = "00000000";
    label.setText(text);
    int x = bounds.getX();
    int y = bounds.getY();
    bounds = StringUtil.estimateBounds(text, label.getFont()).translate(x, y);
    label.setLocation(bounds.getCenterX(), bounds.getCenterY());
  }

  @Override
  public void translate(int dx, int dy) {
    super.translate(dx, dy);
    label.setLocation(bounds.getX(), bounds.getY());
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(new Attribute<?>[] {
      Text.ATTR_FONT, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Text.ATTR_FONT)
      return (V) label.getFont();
    return super.getValue(attr);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == Text.ATTR_FONT) {
      label.setFont((Font) value);
      calculateBounds();
    } else {
      super.updateValue(attr, value);
    }
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    calculateBounds();
    int x = bounds.getX();
    int y = bounds.getY();
    int w = bounds.getWidth();
    int h = bounds.getHeight();
    if (state != null) {
      Value val = Probe.getValue(state.getInstanceState(path.leaf()));
      if (val == null)
        val = Value.NIL;
      RadixOption radix = path.leaf().getAttributeSet().getValue(RadixOption.ATTRIBUTE);
      String text;
      if (radix == null || radix == RadixOption.RADIX_2)
        text = val.toDisplayString();
      else
        text = radix.toString(val);
      label.setText(text);
    }
    label.paint(g);
    drawLabel(g);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-probe"));
  }

  public Element toSvgElement(Element ret) {
    ret = super.toSvgElement(ret);
    Font font = label.getFont();
    if (!font.equals(DEFAULT_FONT))
      SvgCreator.setFontAttribute(ret, font, "value-");
    return ret;
  }

  public void parseSvgElement(Element elt) {
    super.parseSvgElement(elt);
    updateValue(Text.ATTR_FONT, SvgReader.getFontAttribute(elt, "value-", "monospaced", 10));
  }

  @Override
  public String getDisplayName() {
    return S.get("probeComponent");
  }

  @Override
  public String toString() {
    return "Probe:" + getBounds();
  }
}
