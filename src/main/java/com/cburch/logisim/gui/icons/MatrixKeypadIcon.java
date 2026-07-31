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

public class MatrixKeypadIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var iconOffset = scale(1);
    final var bodySize = scale(14);
    final var cornerRadius = scale(2);

    g2.setColor(new Color(50, 50, 50));
    g2.fillRoundRect(iconOffset, iconOffset, bodySize, bodySize, cornerRadius, cornerRadius);
    g2.setColor(Color.BLACK);
    g2.drawRoundRect(iconOffset, iconOffset, bodySize, bodySize, cornerRadius, cornerRadius);

    final var keySize = scale(2);
    final var keyStep = scale(3);
    final var startX = iconOffset + scale(1);
    final var startY = iconOffset + scale(1);

    for (int row = 0; row < 4; row++) {
      for (int column = 0; column < 4; column++) {
        final var keyX = startX + column * keyStep;
        final var keyY = startY + row * keyStep;
        final var isRedKey = (column == 3 || row == 3);
        g2.setColor(isRedKey ? new Color(220, 40, 40) : new Color(0, 170, 220));
        g2.fillRect(keyX, keyY, keySize, keySize);
        g2.setColor(Color.BLACK);
        g2.drawRect(keyX, keyY, keySize, keySize);
      }
    }
  }
}
