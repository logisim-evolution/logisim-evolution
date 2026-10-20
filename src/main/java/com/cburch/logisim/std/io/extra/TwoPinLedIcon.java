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
public class TwoPinLedIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    final var path0 = new Path2D.Double();
    path0.moveTo(scale(14.5970), scale(8.0000));
    path0.curveTo(scale(14.5970), scale(11.5970), scale(11.6430), scale(14.5130), scale(8.0000), scale(14.5130));
    path0.curveTo(scale(4.3570), scale(14.5130), scale(1.4030), scale(11.5970), scale(1.4030), scale(8.0000));
    path0.curveTo(scale(1.4030), scale(4.4030), scale(4.3570), scale(1.4870), scale(8.0000), scale(1.4870));
    path0.curveTo(scale(11.6430), scale(1.4870), scale(14.5970), scale(4.4030), scale(14.5970), scale(8.0000));
    g2.setColor(new Color(240, 0, 0));
    g2.fill(path0);
    final var path1 = new Path2D.Double();
    path1.moveTo(scale(1.6490), scale(8.0404));
    path1.lineTo(scale(14.3302), scale(8.0404));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path1);
    final var path2 = new Path2D.Double();
    path2.moveTo(scale(5.6300), scale(4.0440));
    path2.lineTo(scale(5.6300), scale(12.0360));
    path2.lineTo(scale(10.5400), scale(8.0400));
    path2.closePath();
    g2.setColor(currentColor);
    g2.fill(path2);
    final var path3 = new Path2D.Double();
    path3.moveTo(scale(10.5409), scale(5.0435));
    path3.lineTo(scale(10.5409), scale(11.0372));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path3);
    final var path4 = new Path2D.Double();
    path4.moveTo(scale(14.5965), scale(8.0007));
    path4.curveTo(scale(14.5965), scale(11.5972), scale(11.6428), scale(14.5132), scale(8.0001), scale(14.5132));
    path4.curveTo(scale(4.3567), scale(14.5132), scale(1.4030), scale(11.5972), scale(1.4030), scale(8.0007));
    path4.curveTo(scale(1.4030), scale(4.4042), scale(4.3567), scale(1.4875), scale(8.0001), scale(1.4875));
    path4.curveTo(scale(11.6428), scale(1.4875), scale(14.5965), scale(4.4034), scale(14.5965), scale(8.0007));
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.2307f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path4);
  }
}
