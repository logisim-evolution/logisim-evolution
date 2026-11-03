package com.cburch.logisim.gui.icons.symbolic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * Modern vector Undo icon with smooth arc and clean arrowhead.
 */
public class UndoIcon extends SymbolicIcon {

  public UndoIcon() {
    super();
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    g2.setStroke(createStroke(1.8f));

    float s = size;
    // Circular arc for undo curve
    Arc2D.Float arc = new Arc2D.Float(s * 0.28f, s * 0.32f, s * 0.52f, s * 0.52f, 0, 180, Arc2D.OPEN);
    g2.draw(arc);

    // Arrowhead pointing left-down
    Path2D.Float arrow = new Path2D.Float();
    float tipX = s * 0.28f;
    float tipY = s * 0.58f;
    arrow.moveTo(tipX, tipY);
    arrow.lineTo(tipX - s * 0.16f, tipY - s * 0.16f);
    arrow.moveTo(tipX, tipY);
    arrow.lineTo(tipX + s * 0.16f, tipY - s * 0.16f);
    g2.draw(arrow);
  }
}
