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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.Icon;

public class BreakpointIcon implements Icon {

  private final int wh = AppPreferences.getScaled(12);

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.setColor(Color.RED);
    g.fillOval(x, y, wh, wh);
    g.setColor(Color.DARK_GRAY);
    g.drawOval(x, y, wh, wh);
    g.setColor(Color.YELLOW);
    final var f = g.getFont();
    g.setFont(f.deriveFont(Font.BOLD, (f.getSize() * 6) / 10));
    GraphicsUtil.drawCenteredText(g, "B", x + wh / 2, y + wh / 2);
    g.setFont(f);
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
