package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;

import java.awt.Graphics2D;

public class GroundIcon extends BaseIcon {
  @Override
  protected void paintIcon(Graphics2D g2) {
    final int[] xp = {8, 8, 2, 14};
    final int[] yp = {0, 8, 8, 8};
    final int[] sxp = new int[xp.length];
    final int[] syp = new int[yp.length];
    final int[] lxp = {4, 2, 1};
    final int[] lyp = {10, 12, 14};
    final int[] slxp = new int[xp.length];
    final int[] slyp = new int[yp.length];
    for (int i = 0; i < xp.length; i++) {
      sxp[i] = AppPreferences.getScaled(xp[i]);
      syp[i] = AppPreferences.getScaled(yp[i]);
    }
    final int m = AppPreferences.getScaled(8); // Step

    g2.drawPolyline(sxp, syp, xp.length);
    for (int i = 0; i < lxp.length; i++) {
      slxp[i] = AppPreferences.getScaled(lxp[i]);
      slyp[i] = AppPreferences.getScaled(lyp[i]);
      g2.drawLine(m - slxp[i], slyp[i], m + slxp[i], slyp[i]);
    }
  }
}
