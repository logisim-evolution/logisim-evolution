package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;

import java.awt.Graphics2D;

public class PowerIcon extends BaseIcon {
  @Override
  protected void paintIcon(Graphics2D g2) {
    final int[] xp = {8, 8, 2, 8, 14, 8};
    final int[] yp = {16, 8, 8, 2, 8, 8};
    final int[] sxp = new int[xp.length];
    final int[] syp = new int[yp.length];
    for (int i = 0; i < xp.length; i++) {
      sxp[i] = AppPreferences.getScaled(xp[i]);
      syp[i] = AppPreferences.getScaled(yp[i]);
    }
    g2.drawPolyline(sxp, syp, xp.length);
  }
}
