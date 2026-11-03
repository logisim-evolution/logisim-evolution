package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Modern vector Downward Chevron icon (⌄).
 */
public class ChevronDownIcon extends SymbolicIcon {

  public ChevronDownIcon() {
    super(12);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    g2.setStroke(createStroke(1.8f));

    float s = size;
    Path2D.Float chevron = new Path2D.Float();
    chevron.moveTo(s * 0.20f, s * 0.38f);
    chevron.lineTo(s * 0.50f, s * 0.68f);
    chevron.lineTo(s * 0.80f, s * 0.38f);

    g2.draw(chevron);
  }
}
