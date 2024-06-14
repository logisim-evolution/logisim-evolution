package com.cburch.logisim.gui.icons;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;

import java.awt.*;

public class SplitterIcon extends BaseIcon {
    @Override
    protected void paintIcon(Graphics2D g2) {
        final int[] xp = {7, 9, 9, 5, 3, 7};
        final int[] yp = {0, 0, 10, 16, 14, 10};
        final int[] sxp = new int[xp.length];
        final int[] syp = new int[yp.length];
        for (int i = 0; i < xp.length; i++) {
            sxp[i] = AppPreferences.getScaled(xp[i]);
            syp[i] = AppPreferences.getScaled(yp[i]);
        }
        g2.fillPolygon(sxp, syp, xp.length);
        g2.setColor(Value.FALSE.getColor());
        g2.drawRect(sxp[1], 0, AppPreferences.getScaled(7), AppPreferences.getScaled(1));
        g2.drawRect(sxp[1], AppPreferences.getScaled(8), AppPreferences.getScaled(7), AppPreferences.getScaled(1));
        g2.setColor(Value.TRUE.getColor());
        g2.drawRect(sxp[1], AppPreferences.getScaled(4), AppPreferences.getScaled(7), AppPreferences.getScaled(1));
    }
}
