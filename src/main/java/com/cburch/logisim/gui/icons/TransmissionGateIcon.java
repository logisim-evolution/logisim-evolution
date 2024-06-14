package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;

import java.awt.*;

public class TransmissionGateIcon extends BaseIcon {
    @Override
    protected void paintIcon(Graphics2D g2) {
        final int[] xp = {4, 12};
        final int[] yp = {6, 10};
        final int[] sxp = new int[xp.length];
        final int[] syp = new int[yp.length];
        for (int i = 0; i < xp.length; i++) {
            sxp[i] = AppPreferences.getScaled(xp[i]);
            syp[i] = AppPreferences.getScaled(yp[i]);
        }
        final int e = AppPreferences.getScaled(16); // Edge
        final int m = AppPreferences.getScaled(8); // Mid
        final int s = AppPreferences.getScaled(2); // Step
        g2.drawRect(sxp[0], syp[0], sxp[1] - sxp[0],syp[1] - syp[0]);
        g2.drawLine(sxp[0], syp[0] - s, sxp[1], syp[0] - s);
        g2.drawLine(sxp[0], syp[1] + s, sxp[1], syp[1] + s);
        g2.drawLine(m, syp[1] + s, m, e);
        g2.drawOval(m-s, 0, s*2, s*2);
        g2.drawLine(0, m, sxp[0], m);
        g2.drawLine(e, m, sxp[1], m);
    }
}
