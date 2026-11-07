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
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Vector icon for undo action.
 */
public class UndoIcon extends VectorBaseIcon {

  public UndoIcon() {
    super();
  }

  public UndoIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color) {
    float s = size;

    float strokeW = 1.9f;
    g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.setColor(color);

    float topY = s * 0.35f;
    float botY = s * 0.65f;
    float arrowTipX = s * 0.16f;
    float arrowStemX = s * 0.36f;
    float loopR = (botY - topY) / 2.0f;
    float loopCenterX = s * 0.60f;
    float botStartX = s * 0.32f;

    // Stem and loop curve
    g2.draw(new Line2D.Float(botStartX, botY, loopCenterX, botY));
    Arc2D.Float curve = new Arc2D.Float(loopCenterX - loopR, topY, loopR * 2.0f, loopR * 2.0f, -90, 180, Arc2D.OPEN);
    g2.draw(curve);
    g2.draw(new Line2D.Float(loopCenterX, topY, arrowStemX, topY));

    // Arrowhead pointing left
    Path2D.Float arrow = new Path2D.Float();
    float arrowH = s * 0.22f;
    arrow.moveTo(arrowTipX, topY);
    arrow.lineTo(arrowStemX + s * 0.04f, topY - arrowH);
    arrow.lineTo(arrowStemX + s * 0.04f, topY + arrowH);
    arrow.closePath();
    g2.fill(arrow);
  }
}
