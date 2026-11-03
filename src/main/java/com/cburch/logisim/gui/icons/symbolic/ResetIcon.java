package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * Modern vector Reset icon (circular arrow).
 */
public class ResetIcon extends SymbolicIcon {

  public ResetIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    g2.setStroke(createStroke(1.8f));

    float s = size;
    // Circular arc for 270 degrees
    Arc2D.Float arc = new Arc2D.Float(s * 0.22f, s * 0.22f, s * 0.56f, s * 0.56f, 45, 270, Arc2D.OPEN);
    g2.draw(arc);

    // Arrowhead at top-right
    Path2D.Float arrow = new Path2D.Float();
    float tipX = s * 0.70f;
    float tipY = s * 0.30f;
    arrow.moveTo(tipX, tipY);
    arrow.lineTo(tipX + s * 0.16f, tipY - s * 0.04f);
    arrow.moveTo(tipX, tipY);
    arrow.lineTo(tipX + s * 0.04f, tipY + s * 0.16f);
    g2.draw(arrow);
  }
}
