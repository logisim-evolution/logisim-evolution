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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class DipSwitchIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLUE);
    g2.fillRect(0, 0, getIconWidth(), getIconHeight());
    final var w = scale(8);
    final var h = scale(5);
    g2.setColor(Color.WHITE);
    Font f = g2.getFont().deriveFont((float) (getIconWidth() / 2.5));
    TextLayout t = new TextLayout("1", f, g2.getFontRenderContext());
    t.draw(
        g2,
        (float) ((3 * getIconWidth()) / 4 - t.getBounds().getCenterX()),
        (float) (getIconHeight() / 4 - t.getBounds().getCenterY()));
    t = new TextLayout("2", f, g2.getFontRenderContext());
    t.draw(
        g2,
        (float) ((3 * getIconWidth()) / 4 - t.getBounds().getCenterX()),
        (float) ((3 * getIconHeight()) / 4 - t.getBounds().getCenterY()));
    g2.fillRect(scale(2), scale(2), w, h);
    g2.fillRect(scale(2), scale(9), w, h);
    g2.setColor(Color.gray);
    final var x1 = scale(2) + w >> 1;
    final var x2 = scale(2);
    g2.fillRect(x1, scale(2), w >> 1, h);
    g2.fillRect(x2, scale(9), w >> 1, h);
  }
}
