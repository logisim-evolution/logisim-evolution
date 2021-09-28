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
import java.awt.Color;
import java.awt.Graphics2D;

public class RunIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.GREEN.darker());
    g2.fillOval(
        AppPreferences.getScaled(2),
        AppPreferences.getScaled(2),
        AppPreferences.getScaled(13),
        AppPreferences.getScaled(13));
    final int[] posX =
        new int[] {
          AppPreferences.getScaled(6), AppPreferences.getScaled(11), AppPreferences.getScaled(6)
        };
    final int[] posY =
        new int[] {
          AppPreferences.getScaled(5), AppPreferences.getScaled(8), AppPreferences.getScaled(11)
        };
    g2.setColor(Color.WHITE);
    g2.fillPolygon(posX, posY, 3);
  }
}
