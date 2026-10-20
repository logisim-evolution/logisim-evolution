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
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class ShowStateIcon extends BaseIcon {

  private final boolean pressed;

  public ShowStateIcon(boolean pressed) {
    this.pressed = pressed;
  }

  @Override
  protected void paintIcon(Graphics2D gfx) {
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    if (pressed) {
      gfx.setColor(Color.MAGENTA.brighter().brighter().brighter());
      gfx.fillRect(0, 0, getIconWidth(), getIconHeight());
    }
    gfx.setColor(Color.BLACK);
    gfx.drawRect(0, 0, getIconWidth(), getIconHeight() / 2);
    final var font = gfx.getFont().deriveFont((float) getIconWidth() / (float) 2);
    final var textLayout = new TextLayout("101", font, gfx.getFontRenderContext());
    textLayout.draw(
        gfx,
        (float) ((double) getIconWidth() / 2.0 - textLayout.getBounds().getCenterX()),
        (float) ((double) getIconHeight() / 4.0 - textLayout.getBounds().getCenterY()));
    final var iconBorder = AppPreferences.ICON_BORDER;
    final var wh = AppPreferences.getScaled(AppPreferences.IconSize / 2 - iconBorder);
    final var offset = AppPreferences.getScaled(iconBorder);
    gfx.setColor(Color.RED);
    gfx.fillOval(offset, offset + getIconHeight() / 2, wh, wh);
    gfx.setColor(Color.GREEN);
    gfx.fillOval(offset + getIconWidth() / 2, offset + getIconHeight() / 2, wh, wh);
    gfx.setColor(Color.BLACK);
    gfx.drawOval(offset, offset + getIconHeight() / 2, wh, wh);
    gfx.drawOval(offset + getIconWidth() / 2, offset + getIconHeight() / 2, wh, wh);
  }
}
