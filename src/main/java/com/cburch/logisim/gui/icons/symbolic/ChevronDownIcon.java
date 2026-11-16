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
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Vector icon for dropdown chevron indicators.
 */
public class ChevronDownIcon extends VectorBaseIcon {

  public ChevronDownIcon() {
    super(12);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    g2.setStroke(createStroke(1.6f));

    float s = size;
    Path2D.Float chevron = new Path2D.Float();
    float left = s * 0.20f;
    float right = s * 0.80f;
    float midX = s * 0.50f;
    float top = s * 0.35f;
    float bottom = s * 0.65f;

    chevron.moveTo(left, top);
    chevron.lineTo(midX, bottom);
    chevron.lineTo(right, top);

    g2.draw(chevron);
  }
}
