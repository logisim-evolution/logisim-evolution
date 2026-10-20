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
import java.awt.Graphics2D;

public class SelectIcon extends BaseIcon {

  public static void paint(Graphics2D g2) {
    final int[] xp = {3, 3, 7, 10, 11, 9, 14};
    final int[] yp = {0, 17, 12, 16, 16, 12, 12};
    final int[] sxp = new int[xp.length];
    final int[] syp = new int[yp.length];
    for (int i = 0; i < xp.length; i++) {
      sxp[i] = AppPreferences.getScaled(xp[i]);
      syp[i] = AppPreferences.getScaled(yp[i]);
    }
    g2.fillPolygon(sxp, syp, xp.length);
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    paint(g2);
  }
}
