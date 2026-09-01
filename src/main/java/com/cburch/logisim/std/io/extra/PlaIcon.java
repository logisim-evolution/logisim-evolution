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
public class PlaIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.4000f)));
    g2.draw(new RoundRectangle2D.Double(scale(1.5000), scale(1.5000), scale(13.0000), scale(13.0000), scale(0.2000), scale(0.0000)));
    final var path0 = new Path2D.Double();
    path0.moveTo(scale(3.2000), scale(10.5000));
    path0.lineTo(scale(3.2000), scale(5.5000));
    path0.lineTo(scale(4.4180), scale(5.5000));
    path0.curveTo(scale(5.3180), scale(5.5000), scale(5.7640), scale(5.8100), scale(5.7640), scale(6.5100));
    path0.curveTo(scale(5.7640), scale(7.2100), scale(5.2640), scale(7.6680), scale(4.3640), scale(7.6820));
    path0.lineTo(scale(3.2000), scale(7.7000));
    path0.moveTo(scale(7.1640), scale(5.5000));
    path0.lineTo(scale(7.1640), scale(10.5000));
    path0.lineTo(scale(9.3640), scale(10.5000));
    path0.moveTo(scale(10.1820), scale(10.5000));
    path0.lineTo(scale(11.4820), scale(5.5000));
    path0.lineTo(scale(12.7820), scale(10.5000));
    path0.moveTo(scale(10.6820), scale(9.0000));
    path0.lineTo(scale(12.2820), scale(9.0000));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.2000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path0);
  }
}
