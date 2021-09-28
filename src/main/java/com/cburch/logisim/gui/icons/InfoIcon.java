/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import javax.swing.Icon;

public class InfoIcon implements Icon {

  final int iconWidth = AppPreferences.getIconSize();

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    final var g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    g2.setColor(Color.CYAN.darker());
    g2.fillOval(0, 0, iconWidth - 1, iconWidth - 1);
    g2.drawOval(0, 0, iconWidth - 1, iconWidth - 1);
    g2.setColor(Color.BLUE.darker().darker().darker().darker());
    final var f = g2.getFont().deriveFont((float) iconWidth / (float) 1.3).deriveFont(Font.BOLD);
    final var t = new TextLayout("i", f, g2.getFontRenderContext());
    final var xc = (float) iconWidth / (float) 2 - (float) t.getBounds().getCenterX();
    final var yc = (float) iconWidth / (float) 2 - (float) t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    g2.dispose();
  }

  @Override
  public int getIconWidth() {
    return iconWidth;
  }

  @Override
  public int getIconHeight() {
    return iconWidth;
  }
}
