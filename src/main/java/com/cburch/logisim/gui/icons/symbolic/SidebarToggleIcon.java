package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern Libadwaita-style sidebar toggle icon.
 */
public class SidebarToggleIcon extends SymbolicIcon {

  public SidebarToggleIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float strokeW = scale(1.5f);
    g2.setStroke(createStroke(1.5f));

    float pad = scale(2f);
    float w = size - pad * 2;
    float h = size - pad * 2;
    float arc = scale(3f);

    // Outer window frame
    g2.draw(new RoundRectangle2D.Float(pad, pad, w, h, arc, arc));

    // Vertical dividing line for sidebar
    float splitX = pad + w * 0.35f;
    g2.drawLine((int) splitX, (int) pad, (int) splitX, (int) (pad + h));

    // Subtle fill for left sidebar pane
    Color fillCol = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
    g2.setColor(fillCol);
    g2.fill(new RoundRectangle2D.Float(pad + strokeW / 2, pad + strokeW / 2,
        w * 0.35f - strokeW, h - strokeW, arc, arc));
  }
}
