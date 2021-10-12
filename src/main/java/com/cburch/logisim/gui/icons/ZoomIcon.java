/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class ZoomIcon extends BaseIcon {

  public static final int ZOOMIN = 0;
  public static final int ZOOMOUT = 1;
  public static final int NOZOOM = 2;

  private final int zoomType;

  public ZoomIcon() {
    zoomType = NOZOOM;
  }

  public ZoomIcon(int type) {
    zoomType = type;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke((int) AppPreferences.getScaled(1.5)));
    g2.setColor(g2.getBackground().darker());
    final var scaledOne = AppPreferences.getScaled(1);
    final var scaledEleven = AppPreferences.getScaled(10);
    g2.fillOval(scaledOne, scaledOne, scaledEleven, scaledEleven);
    g2.setColor(g2.getBackground().darker().darker().darker());
    if (zoomType != NOZOOM) {
      g2.drawLine(
          AppPreferences.getScaled(4),
          AppPreferences.getScaled(6),
          AppPreferences.getScaled(8),
          AppPreferences.getScaled(6));
      if (zoomType == ZOOMIN)
        g2.drawLine(
            AppPreferences.getScaled(6),
            AppPreferences.getScaled(4),
            AppPreferences.getScaled(6),
            AppPreferences.getScaled(8));
    }
    g2.setColor(Color.BLACK);
    g2.drawOval(scaledOne, scaledOne, scaledEleven, scaledEleven);
    final var xyPoint = AppPreferences.getScaled(6.0 + Math.sqrt(12.5));
    GeneralPath path = new GeneralPath();
    path.moveTo(xyPoint, xyPoint);
    path.lineTo(AppPreferences.getScaled(15), AppPreferences.getScaled(13));
    path.lineTo(AppPreferences.getScaled(13), AppPreferences.getScaled(15));
    path.closePath();
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.setColor(new Color(139, 69, 19));
    g2.fill(path);
  }
}
