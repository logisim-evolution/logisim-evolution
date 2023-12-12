package com.cburch.draw.icons;

import com.cburch.logisim.gui.icons.BaseIcon;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

/**
 * Image shape tool icon.
 */
public class DrawImageIcon extends BaseIcon {
  protected void paintIcon(Graphics2D gfx) {
    final var wh = AppPreferences.getScaled(3);

    gfx.setStroke(new BasicStroke(scale(2)));
    gfx.setColor(Color.BLUE.darker());
    gfx.drawRect(scale(1), scale(1), scale(15), scale(15));

    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    gfx.drawRect(scale(13), 0, wh, wh);

    gfx.drawRect(0, scale(13), wh, wh);

    final var f = StdAttr.DEFAULT_LABEL_FONT.deriveFont(
            (float) AppPreferences.getIconSize() * 0.4F
    );
    final var l = new TextLayout("IMG", f, gfx.getFontRenderContext());
    l.draw(
            gfx,
            (float) (AppPreferences.getIconSize() / 2 - l.getBounds().getCenterX()),
            (float) (AppPreferences.getIconSize() / 2 - l.getBounds().getCenterY()));
  }
}
