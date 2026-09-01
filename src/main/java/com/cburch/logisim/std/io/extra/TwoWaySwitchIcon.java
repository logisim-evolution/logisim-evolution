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
public class TwoWaySwitchIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    g2.setColor(new Color(30, 64, 255));
    g2.fill(new Ellipse2D.Double(scale(13.5360), scale(10.3550), scale(2.0000), scale(2.0000)));
    g2.setColor(new Color(30, 64, 255));
    g2.fill(new Ellipse2D.Double(scale(13.5000), scale(4.4820), scale(2.0000), scale(2.0000)));
    g2.setColor(new Color(128, 128, 128));
    g2.fill(new Ellipse2D.Double(scale(0.4450), scale(7.1910), scale(2.0000), scale(2.0000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new RoundRectangle2D.Double(scale(1.9910), scale(2.5000), scale(12.0000), scale(11.0000), scale(3.0000), scale(3.0000)));
    g2.setColor(currentColor);
    g2.fill(new Ellipse2D.Double(scale(3.9910), scale(7.5000), scale(2.0000), scale(2.0000)));
    g2.setColor(currentColor);
    g2.fill(new Ellipse2D.Double(scale(9.9910), scale(4.5000), scale(2.0000), scale(2.0000)));
    g2.setColor(currentColor);
    g2.fill(new Ellipse2D.Double(scale(10.0090), scale(9.8820), scale(2.0000), scale(2.0000)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.5000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
    g2.draw(new Line2D.Double(scale(4.9180), scale(8.5000), scale(10.9180), scale(5.5000)));
  }
}
