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
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Vector icon for half clock cycle propagation.
 */
public class TickHalfIcon extends VectorBaseIcon {

  public static final Color MAGENTA_ARROW = new Color(0xFF, 0x00, 0xCC);
  public static final Color RED_HAND      = new Color(0xEE, 0x11, 0x11);

  public TickHalfIcon() {
    super();
  }

  public TickHalfIcon(int baseSize) {
    super(baseSize);
  }

  @Override
  protected void paintSymbolic(Graphics2D g2, int size, Color color, Component c) {
    float s = size;
    boolean isEnabled = (c == null || c.isEnabled());
    Color arrowCol = isEnabled ? MAGENTA_ARROW : toDisabled(MAGENTA_ARROW);
    Color redCol   = isEnabled ? RED_HAND : toDisabled(RED_HAND);

    float cx = s * 0.48f;
    float cy = s * 0.46f;
    float r  = s * 0.36f;

    // Dial background and border
    g2.setColor(c != null && !isEnabled ? new Color(240, 240, 240, 160) : Color.WHITE);
    g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f));

    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.setColor(isEnabled ? new Color(0x44, 0x44, 0x44) : toDisabled(new Color(0x44, 0x44, 0x44)));
    g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f));

    // Clock hands
    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.setColor(redCol);
    g2.draw(new Line2D.Float(cx, cy, cx - r * 0.55f, cy - r * 0.55f));

    g2.setColor(isEnabled ? new Color(0x18, 0x18, 0x18) : toDisabled(new Color(0x18, 0x18, 0x18)));
    g2.draw(new Line2D.Float(cx, cy, cx + r * 0.55f, cy - r * 0.55f));

    g2.setColor(new Color(0x22, 0x22, 0x22));
    g2.fill(new Ellipse2D.Float(cx - 1.2f, cy - 1.2f, 2.4f, 2.4f));

    // Magenta arc
    float arcR = r;
    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g2.setColor(arrowCol);
    Arc2D.Float arc = new Arc2D.Float(cx - arcR, cy - arcR, arcR * 2f, arcR * 2f, 45, -180, Arc2D.OPEN);
    g2.draw(arc);

    float tipX = cx - arcR * 0.707f;
    float tipY = cy + arcR * 0.707f;
    Path2D.Float arrowHead = new Path2D.Float();
    arrowHead.moveTo(tipX - s * 0.12f, tipY);
    arrowHead.lineTo(tipX + s * 0.22f, tipY - s * 0.08f);
    arrowHead.lineTo(tipX + s * 0.08f, tipY + s * 0.26f);
    arrowHead.closePath();
    g2.fill(arrowHead);
  }
}
