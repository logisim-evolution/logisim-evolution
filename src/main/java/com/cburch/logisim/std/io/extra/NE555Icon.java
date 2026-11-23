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
public class NE555Icon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var currentColor = g2.getColor();

    // Draw rounded rectangle (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new RoundRectangle2D.Double(scale(2.5091), scale(1.5273), scale(10.9818), scale(13.0000), scale(0.0000), scale(0.5818)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(1.0182), scale(3.5273), scale(2.0182), scale(3.5273)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(1.0182), scale(6.5273), scale(2.0182), scale(6.5273)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(1.0182), scale(9.5091), scale(2.0182), scale(9.5091)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(1.0182), scale(12.5273), scale(2.0182), scale(12.5273)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(13.9818), scale(3.5273), scale(14.9818), scale(3.5273)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(13.9818), scale(6.5273), scale(14.9818), scale(6.5273)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(13.9818), scale(9.5273), scale(14.9818), scale(9.5273)));
    // Draw line (black stroke)
    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(scale(1.0000f)));
    g2.draw(new Line2D.Double(scale(13.9818), scale(12.5273), scale(14.9818), scale(12.5273)));
    // Draw shape (black fill)
    final var path0 = new Path2D.Double();
    path0.moveTo(scale(6.6566), scale(5.9650));
    path0.lineTo(scale(6.6566), scale(5.5178));
    path0.quadTo(scale(7.1983), scale(5.6697), scale(7.5594), scale(5.6697));
    path0.quadTo(scale(7.9234), scale(5.6697), scale(8.1369), scale(5.5126));
    path0.quadTo(scale(8.3505), scale(5.3556), scale(8.3505), scale(5.0874));
    path0.quadTo(scale(8.3505), scale(4.4261), scale(7.0406), scale(4.4261));
    path0.quadTo(scale(6.8858), scale(4.4261), scale(6.7311), scale(4.4448));
    path0.lineTo(scale(6.7311), scale(2.8996));
    path0.lineTo(scale(9.1673), scale(2.8996));
    path0.lineTo(scale(9.1673), scale(3.3863));
    path0.lineTo(scale(7.3502), scale(3.3863));
    path0.lineTo(scale(7.3072), scale(4.0497));
    path0.quadTo(scale(8.2243), scale(4.0497), scale(8.7388), scale(4.3221));
    path0.quadTo(scale(9.2533), scale(4.5945), scale(9.2533), scale(5.0832));
    path0.quadTo(scale(9.2533), scale(5.5345), scale(8.8033), scale(5.7944));
    path0.quadTo(scale(8.3533), scale(6.0544), scale(7.5709), scale(6.0544));
    path0.quadTo(scale(7.2384), scale(6.0544), scale(6.6566), scale(5.9650));
    path0.closePath();
    g2.setColor(currentColor);
    g2.fill(path0);
    // Draw shape (black fill)
    final var path1 = new Path2D.Double();
    path1.moveTo(scale(6.6566), scale(9.7944));
    path1.lineTo(scale(6.6566), scale(9.3473));
    path1.quadTo(scale(7.1983), scale(9.4991), scale(7.5594), scale(9.4991));
    path1.quadTo(scale(7.9234), scale(9.4991), scale(8.1369), scale(9.3421));
    path1.quadTo(scale(8.3505), scale(9.1851), scale(8.3505), scale(8.9168));
    path1.quadTo(scale(8.3505), scale(8.2555), scale(7.0406), scale(8.2555));
    path1.quadTo(scale(6.8858), scale(8.2555), scale(6.7311), scale(8.2742));
    path1.lineTo(scale(6.7311), scale(6.7291));
    path1.lineTo(scale(9.1673), scale(6.7291));
    path1.lineTo(scale(9.1673), scale(7.2157));
    path1.lineTo(scale(7.3502), scale(7.2157));
    path1.lineTo(scale(7.3072), scale(7.8791));
    path1.quadTo(scale(8.2243), scale(7.8791), scale(8.7388), scale(8.1515));
    path1.quadTo(scale(9.2533), scale(8.4239), scale(9.2533), scale(8.9127));
    path1.quadTo(scale(9.2533), scale(9.3639), scale(8.8033), scale(9.6239));
    path1.quadTo(scale(8.3533), scale(9.8838), scale(7.5709), scale(9.8838));
    path1.quadTo(scale(7.2384), scale(9.8838), scale(6.6566), scale(9.7944));
    path1.closePath();
    g2.setColor(currentColor);
    g2.fill(path1);
    // Draw shape (black fill)
    final var path2 = new Path2D.Double();
    path2.moveTo(scale(6.6566), scale(13.6238));
    path2.lineTo(scale(6.6566), scale(13.1767));
    path2.quadTo(scale(7.1983), scale(13.3285), scale(7.5594), scale(13.3285));
    path2.quadTo(scale(7.9234), scale(13.3285), scale(8.1369), scale(13.1715));
    path2.quadTo(scale(8.3505), scale(13.0145), scale(8.3505), scale(12.7463));
    path2.quadTo(scale(8.3505), scale(12.0849), scale(7.0406), scale(12.0849));
    path2.quadTo(scale(6.8858), scale(12.0849), scale(6.7311), scale(12.1037));
    path2.lineTo(scale(6.7311), scale(10.5585));
    path2.lineTo(scale(9.1673), scale(10.5585));
    path2.lineTo(scale(9.1673), scale(11.0451));
    path2.lineTo(scale(7.3502), scale(11.0451));
    path2.lineTo(scale(7.3072), scale(11.7085));
    path2.quadTo(scale(8.2243), scale(11.7085), scale(8.7388), scale(11.9810));
    path2.quadTo(scale(9.2533), scale(12.2534), scale(9.2533), scale(12.7421));
    path2.quadTo(scale(9.2533), scale(13.1934), scale(8.8033), scale(13.4533));
    path2.quadTo(scale(8.3533), scale(13.7133), scale(7.5709), scale(13.7133));
    path2.quadTo(scale(7.2384), scale(13.7133), scale(6.6566), scale(13.6238));
    path2.closePath();
    g2.setColor(currentColor);
    g2.fill(path2);
  }
}
