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
import java.awt.font.TextLayout;

public class CounterIcon extends BaseIcon {

  private final int count = 42; // has to be between 10 - 89

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.drawRect(scale(1), 0, scale(6), scale(16));
    g2.drawRect(scale(9), 0, scale(6), scale(16));
    final var f = g2.getFont().deriveFont(scale((float) 6));

    for (int i = -1; i <= 1; i++) {
      final var ones = new TextLayout(Integer.toString((count + i) % 10), f, g2.getFontRenderContext());  // Math.abs()
      final var tens = new TextLayout(Integer.toString(count / 10 + i), f, g2.getFontRenderContext());
      final var y = scale((float) (8.5 + i * 5)) - (float) ones.getBounds().getCenterY();
      final var xOnes = scale((float) 11.5) - (float) ones.getBounds().getCenterX();
      final var xTens = scale((float) 3.5) - (float) tens.getBounds().getCenterX();

      ones.draw(g2, xOnes, y);
      tens.draw(g2, xTens, y);
    }
  }
}
