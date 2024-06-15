package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;

import java.awt.Graphics2D;

public class TransistorIcon extends BaseIcon {
  private boolean nTypeGate = false;
  @Override
  protected void paintIcon(Graphics2D g2) {
    final int[] xp = {0, 3, 3, 13, 13, 16};
    final int[] yp = {12, 12, 8, 8, 12, 12};
    final int[] sxp = new int[xp.length];
    final int[] syp = new int[yp.length];
    for (int i = 0; i < xp.length; i++) {
      sxp[i] = AppPreferences.getScaled(xp[i]);
      syp[i] = AppPreferences.getScaled(yp[i]);
    }
    g2.drawPolyline(sxp, syp, xp.length);
    final int m = AppPreferences.getScaled(8); // Mid
    final int s = AppPreferences.getScaled(2); // Step
    g2.drawLine(sxp[1], syp[2] - s, sxp[sxp.length - 2], syp[2] - s);

    int gateInLength = 3;
    if (!nTypeGate) {
      g2.drawOval(m - s, s, s * 2, s * 2);
      gateInLength -= 2;
    }
    g2.drawLine(m, 0, m, gateInLength * s);
  }

  public void setNTypeGate(boolean nTypeGate) {
    this.nTypeGate = nTypeGate;
  }
}
