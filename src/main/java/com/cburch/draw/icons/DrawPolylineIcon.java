/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.icons;

import com.cburch.logisim.gui.icons.BaseIcon;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class DrawPolylineIcon extends BaseIcon {

  private static final int[] points = {1, 14, 1, 1, 7, 8, 13, 4, 10, 13};
  private boolean isPolylineClosed = false;

  public DrawPolylineIcon(boolean closed) {
    this.isPolylineClosed = closed;
  }

  @Override
  protected void paintIcon(Graphics2D gfx) {
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    gfx.setColor(Color.BLUE.darker());
    final var p = new GeneralPath();
    var i = 0;
    p.moveTo(AppPreferences.getScaled(points[i++]), AppPreferences.getScaled(points[i++]));
    for (; i < points.length - 1; i += 2) {
      p.lineTo(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i + 1]));
    }
    if (isPolylineClosed) p.closePath();
    gfx.draw(p);
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    gfx.setColor(Color.GRAY);
    final var wh = AppPreferences.getScaled(3);
    for (i = 0; i <= points.length - 1; i += 2)
      gfx.drawRect(
          AppPreferences.getScaled(points[i] - 1),
          AppPreferences.getScaled(points[i + 1] - 1),
          wh,
          wh);
  }
}
