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

public class ImageIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.drawRect(scale(1), scale(1), scale(13), scale(13));

    // Enlarged yellow sun
    g2.setColor(new Color(0xFA, 0xCC, 0x15));
    g2.fillOval(scale(3), scale(3), scale(4), scale(4));

    g2.setColor(new Color(0x22, 0xC5, 0x5E));
    final int[] xPoints = {scale(2), scale(7), scale(10), scale(13), scale(13), scale(2)};
    final int[] yPoints = {scale(13), scale(7), scale(10), scale(7), scale(13), scale(13)};
    g2.fillPolygon(xPoints, yPoints, 6);
  }
}
