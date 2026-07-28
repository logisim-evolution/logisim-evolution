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

public class TwoPinLedIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var offset = AppPreferences.getScaled(2);
    final var diameter = AppPreferences.getScaled(12);
    final var currentColor = g2.getColor();

    g2.setColor(new Color(240, 0, 0));
    g2.fillOval(offset, offset, diameter, diameter);

    final var centerX = AppPreferences.getScaled(9);
    final var centerY = AppPreferences.getScaled(8);

    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.setColor(currentColor);
    g2.drawLine(offset, centerY, offset + diameter, centerY);

    final var xPoints = new int[] {
        centerX - AppPreferences.getScaled(3),
        centerX - AppPreferences.getScaled(3),
        centerX + AppPreferences.getScaled(1)
    };
    final var yPoints = new int[] {
        centerY - AppPreferences.getScaled(3),
        centerY + AppPreferences.getScaled(3),
        centerY
    };
    g2.fillPolygon(xPoints, yPoints, 3);

    final var cathodeBarHalfHeight = Math.round(AppPreferences.getScaled(3) * 0.8f);
    g2.drawLine(
        centerX + AppPreferences.getScaled(1),
        centerY - cathodeBarHalfHeight,
        centerX + AppPreferences.getScaled(1),
        centerY + cathodeBarHalfHeight
    );

    g2.setColor(currentColor);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.drawOval(offset, offset, diameter, diameter);
  }
}
