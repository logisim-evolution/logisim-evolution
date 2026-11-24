/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 16px vector icon for Explorer Tile / Grid View (2x2 rounded squares).
 */
public class GridToggleIcon extends VectorBaseIcon {

  public GridToggleIcon() {
    super(16);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    g2.setColor(color);

    float tileSize = s * 0.28f;
    float arc = s * 0.08f;
    float p1 = s * 0.16f;
    float p2 = s * 0.56f;

    g2.fill(new RoundRectangle2D.Float(p1, p1, tileSize, tileSize, arc, arc));
    g2.fill(new RoundRectangle2D.Float(p2, p1, tileSize, tileSize, arc, arc));
    g2.fill(new RoundRectangle2D.Float(p1, p2, tileSize, tileSize, arc, arc));
    g2.fill(new RoundRectangle2D.Float(p2, p2, tileSize, tileSize, arc, arc));
  }
}
