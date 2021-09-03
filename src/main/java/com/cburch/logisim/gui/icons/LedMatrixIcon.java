/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class LedMatrixIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, scale(16), scale(16));
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 0, scale(16), scale(16));
    final var xint = 2;
    final var yint = 1;
    for (var i = 0; i < 4; i++)
      for (var j = 0; j < 4; j++) {
        g2.setColor((i == xint) && (j == yint) ? Color.RED.darker() : Color.GREEN.darker());
        g2.fillOval(scale(2 + i * 3), scale(2 + j * 3), scale(3), scale(3));
      }
  }
}
