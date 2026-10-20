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
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RgbLedShape extends LedShape {

  public RgbLedShape(int x, int y, DynamicElement.Path p) {
    super(x, y, p);
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    int x = bounds.getX() + 1;
    int y = bounds.getY() + 1;
    int w = bounds.getWidth() - 2;
    int h = bounds.getHeight() - 2;
    GraphicsUtil.switchToWidth(g, strokeWidth);
    if (state == null) {
      g.setColor(Color.lightGray);
      g.fillOval(x, y, w, h);
      g.setColor(DynamicElement.COLOR);
    } else {
      final var activ = path.leaf().getAttributeSet().getValue(IoLibrary.ATTR_ACTIVE);
      final var data = (InstanceDataSingleton) getData(state);
      var summ = (data == null ? 0 : (Integer) data.getValue());
      final var mask = activ ? 0 : 7;
      summ ^= mask;
      final var red = ((summ >> RgbLed.RED) & 1) * 0xFF;
      final var green = ((summ >> RgbLed.GREEN) & 1) * 0xFF;
      final var blue = ((summ >> RgbLed.BLUE) & 1) * 0xFF;
      g.setColor(new Color(red, green, blue));
      g.fillOval(x, y, w, h);
      g.setColor(Color.darkGray);
    }
    g.drawOval(x, y, w, h);
    drawLabel(g);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-rgbled"));
  }

  @Override
  public String getDisplayName() {
    return S.get("RGBledComponent");
  }

  @Override
  public String toString() {
    return "RgbLed:" + getBounds();
  }
}
