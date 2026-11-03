package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern vector Step icon (play arrow + vertical bar).
 */
public class StepIcon extends SymbolicIcon {

  public StepIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;

    // Arrow
    Path2D.Float triangle = new Path2D.Float();
    float left = s * 0.22f;
    float right = s * 0.60f;
    float top = s * 0.24f;
    float bottom = s * 0.76f;
    float midY = s * 0.50f;

    triangle.moveTo(left, top);
    triangle.lineTo(right, midY);
    triangle.lineTo(left, bottom);
    triangle.closePath();
    g2.fill(triangle);

    // Stop bar
    float barX = s * 0.68f;
    float barW = s * 0.14f;
    float barH = s * 0.52f;
    float barY = s * 0.24f;
    g2.fill(new RoundRectangle2D.Float(barX, barY, barW, barH, barW, barW));
  }
}
