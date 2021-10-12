/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class TextIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var f = StdAttr.DEFAULT_LABEL_FONT.deriveFont((float) AppPreferences.getIconSize());
    final var l = new TextLayout("A", f, g2.getFontRenderContext());
    l.draw(
        g2,
        (float) (AppPreferences.getIconSize() / 2 - l.getBounds().getCenterX()),
        (float) (AppPreferences.getIconSize() / 2 - l.getBounds().getCenterY()));
  }
}
