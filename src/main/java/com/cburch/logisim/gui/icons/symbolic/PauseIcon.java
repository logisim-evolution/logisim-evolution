package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern vector Pause icon with rounded pill bars.
 */
public class PauseIcon extends SymbolicIcon {

  public PauseIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;
    float barW = s * 0.16f;
    float barH = s * 0.54f;
    float topY = s * 0.23f;
    float arc = barW;

    float bar1X = s * 0.28f;
    float bar2X = s * 0.56f;

    g2.fill(new RoundRectangle2D.Float(bar1X, topY, barW, barH, arc, arc));
    g2.fill(new RoundRectangle2D.Float(bar2X, topY, barW, barH, arc, arc));
  }
}
