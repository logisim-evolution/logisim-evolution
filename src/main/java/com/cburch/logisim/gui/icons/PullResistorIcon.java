package com.cburch.logisim.gui.icons;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;

import java.awt.*;
import java.util.Objects;

public class PullResistorIcon extends BaseIcon {
    @Override
    protected void paintIcon(Graphics2D g2) {
        final int[] xp = {8, 6, 10, 6, 10, 6, 8};
        final int[] yp = {4, 5, 7, 9, 11, 13, 14};
        final int[] sxp = new int[xp.length];
        final int[] syp = new int[yp.length];
        for (int i = 0; i < xp.length; i++) {
            sxp[i] = AppPreferences.getScaled(xp[i]);
            syp[i] = AppPreferences.getScaled(yp[i]);
        }
        if (Objects.equals(AppPreferences.GATE_SHAPE.get(), AppPreferences.SHAPE_SHAPED))
        {
            g2.drawPolyline(sxp, syp, xp.length);
        }
        else
        {
            g2.drawRect(sxp[1], syp[0], sxp[2] - sxp[1], syp[syp.length-1] - syp[0]);
        }
        g2.drawLine(sxp[0], syp[syp.length - 1], sxp[0], AppPreferences.getScaled(16));
        g2.setColor(Value.FALSE.getColor());
        g2.drawLine(sxp[1], 0, sxp[2], 0);
        g2.drawLine(sxp[0], 0, sxp[0], syp[0]);
    }
}
