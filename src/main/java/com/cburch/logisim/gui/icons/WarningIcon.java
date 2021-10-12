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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import javax.swing.Icon;

public class WarningIcon implements Icon {
  private final int wixth;

  public WarningIcon() {
    wixth = AppPreferences.getIconSize();
  }

  public WarningIcon(double scale) {
    wixth = (int) AppPreferences.getScaled(scale * AppPreferences.getIconSize());
  }

  public static int scale(int v) {
    return AppPreferences.getScaled(v);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    final int[] xpos = {0, wixth / 2 - 1, wixth - 1};
    final int[] ypos = {wixth - 1, 0, wixth - 1};
    g2.setColor(Color.YELLOW.brighter().brighter());
    g2.fillPolygon(xpos, ypos, 3);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.BLACK);
    g2.drawPolygon(xpos, ypos, 3);
    final var f = g2.getFont().deriveFont((float) wixth / (float) 1.3).deriveFont(Font.BOLD);
    final var t = new TextLayout("!", f, g2.getFontRenderContext());
    final var xc = (float) wixth / (float) 2 - (float) t.getBounds().getCenterX();
    final var yc = (float) (5 * wixth) / (float) 8 - (float) t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    g2.dispose();
  }

  @Override
  public int getIconWidth() {
    return wixth;
  }

  @Override
  public int getIconHeight() {
    return wixth;
  }
}
