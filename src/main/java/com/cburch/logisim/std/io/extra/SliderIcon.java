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
public class SliderIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    final var path0 = new Path2D.Double();
    path0.moveTo(scale(1.5180), scale(6.4360));
    path0.lineTo(scale(1.5180), scale(9.5460));
    path0.curveTo(scale(1.5180), scale(10.3730), scale(2.1900), scale(11.0440), scale(3.0180), scale(11.0460));
    path0.lineTo(scale(12.9820), scale(11.0640));
    path0.curveTo(scale(13.8100), scale(11.0650), scale(14.4820), scale(10.3920), scale(14.4820), scale(9.5640));
    path0.lineTo(scale(14.4820), scale(6.4540));
    path0.curveTo(scale(14.4820), scale(5.6270), scale(13.8100), scale(4.9560), scale(12.9820), scale(4.9540));
    path0.lineTo(scale(3.0180), scale(4.9360));
    path0.curveTo(scale(2.1900), scale(4.9350), scale(1.5180), scale(5.6080), scale(1.5180), scale(6.4360));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path0);
    g2.setColor(currentColor);
    g2.fill(new Ellipse2D.Double(scale(2.6450), scale(6.6000), scale(2.8000), scale(2.8000)));
    final var path1 = new Path2D.Double();
    path1.moveTo(scale(4.6920), scale(8.0180));
    path1.lineTo(scale(12.6900), scale(7.9820));
    g2.setColor(currentColor);
    g2.fill(path1);
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(path1);
  }
}
