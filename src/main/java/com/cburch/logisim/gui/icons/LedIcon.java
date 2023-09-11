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

public class LedIcon extends BaseIcon {

  private final boolean isRgb;

  public LedIcon(boolean isRgb) {
    super();
    this.isRgb = isRgb;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    int xy = AppPreferences.getScaled(2);
    int wh = AppPreferences.getScaled(12);
    var currColor = g2.getColor();
    if (isRgb) {
      g2.setColor(Color.GREEN);
      g2.fillArc(xy, xy, wh, wh, 0, 120);
      g2.setColor(Color.RED);
      g2.fillArc(xy, xy, wh, wh, 120, 120);
      g2.setColor(Color.BLUE);
      g2.fillArc(xy, xy, wh, wh, 240, 120);
    } else {
      g2.setColor(Color.RED);
      g2.fillOval(xy, xy, wh, wh);
    }
    g2.setColor(currColor);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.drawOval(xy, xy, wh, wh);
  }
}
