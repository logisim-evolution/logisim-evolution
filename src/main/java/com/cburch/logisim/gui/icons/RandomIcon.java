/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import java.awt.Graphics2D;

public class RandomIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.drawRoundRect(0, 0, scale(16), scale(16), scale(5), scale(5));
    g2.fillOval(scale(2), scale(2), scale(3), scale(3));
    g2.fillOval(scale(6), scale(6), scale(3), scale(3));
    g2.fillOval(scale(2), scale(10), scale(3), scale(3));
    g2.fillOval(scale(10), scale(2), scale(3), scale(3));
    g2.fillOval(scale(10), scale(10), scale(3), scale(3));
  }
}
