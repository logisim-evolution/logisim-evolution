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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class CompileIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var page = new int[] {0, 0, 0, 15, 15, 15, 15, 5, 10, 5, 10, 0, 15, 5, 10, 0, 0, 0};
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1F)));
    final var xpos = new int[9];
    final var ypos = new int[9];
    for (var i = 0; i < 9; i++) {
      xpos[i] = AppPreferences.getScaled(page[i * 2]);
      ypos[i] = AppPreferences.getScaled(page[i * 2 + 1]);
    }
    g2.drawPolygon(xpos, ypos, 9);
    final var f = g2.getFont();
    g2.setFont(f.deriveFont(AppPreferences.getScaled(4F)));
    g2.setColor(Color.BLUE);
    GraphicsUtil.drawCenteredText(
        g2, "j r9", AppPreferences.getScaled(7), AppPreferences.getScaled(3));
    GraphicsUtil.drawCenteredText(
        g2, "nop", AppPreferences.getScaled(7), AppPreferences.getScaled(7));
    g2.setColor(Color.MAGENTA);
    GraphicsUtil.drawCenteredText(
        g2, "101..", AppPreferences.getScaled(7), AppPreferences.getScaled(11));
  }
}
