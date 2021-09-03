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

  final int wh = AppPreferences.getIconSize();

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    g2.setColor(Color.CYAN.darker());
    g2.fillOval(0, 0, wh - 1, wh - 1);
    g2.drawOval(0, 0, wh - 1, wh - 1);
    g2.setColor(Color.BLUE.darker().darker().darker().darker());
    Font f = g2.getFont().deriveFont((float) wh / (float) 1.3).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout("i", f, g2.getFontRenderContext());
    float xc = (float) wh / (float) 2 - (float) t.getBounds().getCenterX();
    float yc = (float) wh / (float) 2 - (float) t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    g2.dispose();
  }

  @Override
  public int getIconWidth() {
    return wh;
  }

  @Override
  public int getIconHeight() {
    return wh;
  }
}
