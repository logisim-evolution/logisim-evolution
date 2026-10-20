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
public class SwitchIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    final var path0 = new Path2D.Double();
    path0.moveTo(scale(3.2747), scale(12.9999));
    path0.lineTo(scale(5.6097), scale(0.6400));
    path0.lineTo(scale(12.6146), scale(0.6400));
    path0.lineTo(scale(12.6146), scale(15.4718));
    path0.lineTo(scale(5.6097), scale(15.4718));
    path0.closePath();
    g2.setColor(new Color(180, 180, 180));
    g2.fill(path0);
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
    g2.draw(path0);
    final var path1 = new Path2D.Double();
    path1.moveTo(scale(5.6097), scale(0.6400));
    path1.lineTo(scale(12.6146), scale(0.6400));
    path1.lineTo(scale(10.2796), scale(12.9999));
    path1.lineTo(scale(3.2747), scale(12.9999));
    path1.closePath();
    g2.setColor(AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(40, 40, 40) : Color.WHITE);
    g2.fill(path1);
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
    g2.draw(path1);
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
    g2.draw(new Line2D.Double(scale(10.2795), scale(12.9995), scale(12.6144), scale(15.4715)));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
    g2.draw(new Line2D.Double(scale(4.6107), scale(6.8197), scale(11.2368), scale(6.8197)));
    final var path2 = new Path2D.Double();
    path2.moveTo(scale(9.1447), scale(3.6502));
    path2.curveTo(scale(9.0760), scale(4.1964), scale(8.7067), scale(4.6392), scale(8.3200), scale(4.6392));
    path2.curveTo(scale(7.9332), scale(4.6392), scale(7.6754), scale(4.1964), scale(7.7441), scale(3.6502));
    path2.curveTo(scale(7.8128), scale(3.1040), scale(8.1820), scale(2.6612), scale(8.5688), scale(2.6612));
    path2.curveTo(scale(8.9555), scale(2.6612), scale(9.2133), scale(3.1040), scale(9.1447), scale(3.6502));
    path2.closePath();
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(path2);
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
    g2.draw(new Line2D.Double(scale(7.0853), scale(10.9376), scale(7.4731), scale(8.8763)));
  }
}
