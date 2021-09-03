/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.std.io.Tty;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class TtyIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var display = "Hello World!";

    g2.setColor(Color.BLUE);
    g2.fillRoundRect(0, scale(3), scale(16), scale(10), scale(3), scale(3));
    g2.setColor(Color.BLACK);
    g2.drawRoundRect(0, scale(3), scale(16), scale(10), scale(3), scale(3));
    final var f = Tty.DEFAULT_FONT.deriveFont(scale((float) 5)).deriveFont(Font.BOLD);
    final var t = new TextLayout(display.substring(0, 3), f, g2.getFontRenderContext());
    g2.setColor(Color.yellow);
    t.draw(g2, (float) (getIconWidth() / 2 - t.getBounds().getCenterX()), (float) (getIconHeight() / 2 - t.getBounds().getCenterY()));
  }
}
