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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class DrawCurveIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D gfx) {
    final var wh = scale(3);
    gfx.setStroke(new BasicStroke(scale(1)));
    gfx.setColor(Color.GRAY);

    gfx.drawRect(scale(9), scale(0), wh, wh);

    gfx.setStroke(new BasicStroke(scale(2)));
    gfx.setColor(Color.BLUE.darker());

    final var p = new GeneralPath();
    p.moveTo(scale(1), scale(5));
    p.quadTo(scale(10), scale(1), scale(14), scale(14));
    gfx.draw(p);

    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(scale(1)));
    gfx.drawRect(scale(13), scale(13), wh, wh);
    gfx.drawRect(scale(0), scale(5), wh, wh);
  }
}
