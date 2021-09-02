/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import javax.swing.Icon;

public class WarningIcon implements Icon {
  private final int wh;

  public WarningIcon() {
    wh = AppPreferences.getIconSize();
  }

  public WarningIcon(double scale) {
    wh = (int) AppPreferences.getScaled(scale * AppPreferences.getIconSize());
  }

  public static int scale(int v) {
    return AppPreferences.getScaled(v);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    int[] xpos = {0, wh / 2 - 1, wh - 1};
    int[] ypos = {wh - 1, 0, wh - 1};
    g2.setColor(Color.YELLOW.brighter().brighter());
    g2.fillPolygon(xpos, ypos, 3);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.BLACK);
    g2.drawPolygon(xpos, ypos, 3);
    Font f = g2.getFont().deriveFont((float) wh / (float) 1.3).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout("!", f, g2.getFontRenderContext());
    float xc = (float) wh / (float) 2 - (float) t.getBounds().getCenterX();
    float yc = (float) (5 * wh) / (float) 8 - (float) t.getBounds().getCenterY();
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
