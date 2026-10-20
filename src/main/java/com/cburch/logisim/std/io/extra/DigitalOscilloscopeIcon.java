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
public class DigitalOscilloscopeIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    g2.setColor(new Color(0, 192, 192));
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new RoundRectangle2D.Double(scale(1.5000), scale(1.5000), scale(13.0000), scale(13.0000), scale(3.0000), scale(3.0000)));
    final var path0 = new Path2D.Double();
    path0.moveTo(scale(3.1730), scale(6.2500));
    path0.lineTo(scale(5.0000), scale(6.2500));
    path0.lineTo(scale(5.0000), scale(4.7500));
    path0.lineTo(scale(7.0000), scale(4.7500));
    path0.lineTo(scale(7.0000), scale(6.2500));
    path0.lineTo(scale(9.0000), scale(6.2500));
    path0.lineTo(scale(9.0000), scale(4.7500));
    path0.lineTo(scale(11.0000), scale(4.7500));
    path0.lineTo(scale(11.0000), scale(6.2500));
    path0.lineTo(scale(12.8450), scale(6.2500));
    path0.moveTo(scale(3.1730), scale(11.2500));
    path0.lineTo(scale(4.8000), scale(11.2500));
    path0.lineTo(scale(4.8000), scale(9.7500));
    path0.lineTo(scale(9.1000), scale(9.7500));
    path0.lineTo(scale(9.1000), scale(11.2500));
    path0.lineTo(scale(12.8450), scale(11.2500));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path0);
  }
}
