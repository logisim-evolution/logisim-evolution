/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.vga;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SocVgaShape extends DynamicElement {

  public SocVgaShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 160, 120));
  }

  public void setBounds(int width, int height) {
    bounds = Bounds.create(bounds.getX(), bounds.getY(), width, height);
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    VgaState.VgaDisplayState data =
        (state == null) ? null : (VgaState.VgaDisplayState) getData(state);
    if (state == null || data == null) {
      Color c = g.getColor();
      g.setColor(Color.BLUE);
      g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
      g.setColor(Color.YELLOW);
      GraphicsUtil.drawCenteredText(g, "VGA", bounds.getCenterX(), bounds.getCenterY());
      g.setColor(c);
    } else {
      BufferedImage image = data.getImage(state);
      g.drawImage(image, bounds.getX(), bounds.getY(), null);
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
  }

  @Override
  public String getDisplayName() {
    return S.get("SocVgaComponent");
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-vga"));
  }
}
