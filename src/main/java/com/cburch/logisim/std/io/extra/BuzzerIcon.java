/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import com.cburch.logisim.gui.icons.BaseIcon;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

// Generated BaseIcon
public class BuzzerIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Ellipse2D.Double(scale(1.0000), scale(1.0000), scale(14.0000), scale(14.0000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Ellipse2D.Double(scale(2.8000), scale(2.8000), scale(10.4000), scale(10.4000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Ellipse2D.Double(scale(4.2000), scale(4.2000), scale(7.6000), scale(7.6000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Ellipse2D.Double(scale(5.6000), scale(5.6000), scale(4.8000), scale(4.8000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(1.0000), scale(8.0000), scale(15.0000), scale(8.0000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(8.0000), scale(1.0000), scale(8.0000), scale(15.0000)));
    g2.setColor(currentColor);
    g2.fill(new Ellipse2D.Double(scale(6.6500), scale(6.6500), scale(2.7000), scale(2.7000)));
  }
}
