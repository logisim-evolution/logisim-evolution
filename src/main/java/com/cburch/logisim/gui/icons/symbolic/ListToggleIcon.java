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
 * 16px vector icon for Explorer List View (3 horizontal bars).
 */
public class ListToggleIcon extends VectorBaseIcon {

  public ListToggleIcon() {
    super(16);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    g2.setColor(color);

    float x = s * 0.16f;
    float w = s * 0.68f;
    float h = s * 0.12f;
    float arc = s * 0.08f;

    g2.fill(new RoundRectangle2D.Float(x, s * 0.22f, w, h, arc, arc));
    g2.fill(new RoundRectangle2D.Float(x, s * 0.44f, w, h, arc, arc));
    g2.fill(new RoundRectangle2D.Float(x, s * 0.66f, w, h, arc, arc));
  }
}
