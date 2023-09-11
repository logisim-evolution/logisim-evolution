/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class ButtonIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final int state = 0;

    final var wh = scale(12);
    var x = scale(state);
    var y = scale(11) + scale(state);
    final int[] xpos = {x, x + wh, scale(14), scale(3)};
    final int[] ypos = {y, y, scale(14), scale(14)};
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillPolygon(xpos, ypos, 4);
    g2.setColor(Color.BLACK);
    g2.drawPolygon(xpos, ypos, 4);
    x = wh + scale(state);
    y = scale(state);
    final int[] xpos1 = {x, x, scale(14), scale(14)};
    final int[] ypos1 = {y, y + wh, scale(14), scale(3)};
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillPolygon(xpos1, ypos1, 4);
    g2.setColor(Color.BLACK);
    g2.drawPolygon(xpos1, ypos1, 4);
    g2.setColor(Color.WHITE);
    g2.fillRect(scale(state), scale(state), wh, wh);
    g2.setColor(Color.BLACK);
    g2.drawRect(scale(state), scale(state), wh, wh);

    final var s = "B";
    final var f = g2.getFont().deriveFont((float) wh);
    final var t = new TextLayout(s, f, g2.getFontRenderContext());
    g2.setColor(Color.BLUE);
    final var center = scale(state) + wh / 2;
    t.draw(
        g2,
        center - (float) t.getBounds().getCenterX(),
        center - (float) t.getBounds().getCenterY());
  }
}
