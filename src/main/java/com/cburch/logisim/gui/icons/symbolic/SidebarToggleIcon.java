/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons.symbolic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Vector icon for toggling the project sidebar pane.
 */
public class SidebarToggleIcon extends VectorBaseIcon {

  public SidebarToggleIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;

    float strokeW = 1.6f;
    g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.setColor(color);

    float x = s * 0.16f;
    float y = s * 0.18f;
    float w = s * 0.68f;
    float h = s * 0.64f;
    float arc = s * 0.22f;

    // Window frame
    g2.draw(new RoundRectangle2D.Float(x, y, w, h, arc, arc));

    // Vertical dividing line
    float splitX = x + w * 0.35f;
    g2.draw(new Line2D.Float(splitX, y, splitX, y + h));

    // Pane highlight
    float fillAlpha = 0.25f;
    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * fillAlpha)));
    g2.fill(new RoundRectangle2D.Float(x + 1f, y + 1f, w * 0.35f - 1f, h - 2f, arc * 0.8f, arc * 0.8f));
  }
}
