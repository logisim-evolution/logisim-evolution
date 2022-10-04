/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.icons;

import com.cburch.logisim.gui.icons.BaseIcon;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class DrawLineIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D gfx) {
    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    final var wh = AppPreferences.getScaled(3);

    gfx.setStroke(new BasicStroke(scale(2)));
    gfx.setColor(Color.BLUE.darker());
    gfx.drawLine(scale(1), scale(14), scale(14), scale(1));

    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    gfx.drawRect(scale(13), 0, wh, wh);

    gfx.drawRect(0, scale(13), wh, wh);
  }
}
