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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * The "clear the content" icon.
 */
public class ClearIcon implements Icon {
  // How much of the box is left empty around the mark as a fraction of its size.
  private static final double MARGIN = 0.3;
  private final int wh;

  public ClearIcon() {
    wh = AppPreferences.getIconSize();
  }

  @Override
  public void paintIcon(Component comp, Graphics gfx, int x, int y) {
    final var g2 = (Graphics2D) gfx.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.translate(x, y);
    g2.setColor((comp == null) ? Color.GRAY : comp.getForeground());
    g2.setStroke(new BasicStroke(
        AppPreferences.getScaled(1.5f), BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND));

    final var from = (int) Math.round(wh * MARGIN);
    final var to = wh - from - 1;
    g2.drawLine(from, from, to, to);
    g2.drawLine(from, to, to, from);

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
