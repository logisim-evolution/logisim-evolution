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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;

public class DrcIcon extends BaseIcon {

  public final boolean drawEmpty;

  public DrcIcon(boolean isDrcError) {
    drawEmpty = !isDrcError;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    if (drawEmpty) return;

    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, getIconWidth(), getIconHeight());

    var p = new GeneralPath();
    p.moveTo(scale(15), 0);
    p.quadTo(scale(7), scale(7), 0, scale(6));
    p.lineTo(0, scale(8));
    p.quadTo(scale(7), scale(9), scale(15), scale(2));
    p.closePath();
    g2.setColor(Color.RED.brighter().brighter());
    g2.fill(p);
    paintText(g2);

    p = new GeneralPath();
    p.moveTo(0, scale(6));
    p.quadTo(scale(3), scale(14), scale(12), scale(11));
    p.lineTo(scale(12), scale(9));
    p.lineTo(scale(15), scale(12));
    p.lineTo(scale(12), scale(15));
    p.lineTo(scale(12), scale(13));
    p.quadTo(scale(3), scale(16), 0, scale(8));
    p.closePath();

    g2.setColor(Color.RED.darker());
    g2.fill(p);
  }

  private void paintText(Graphics2D g2) {
    final var f = g2.getFont().deriveFont(scale((float) getIconWidth() / 3)).deriveFont(Font.BOLD);
    final var t = new TextLayout("DRC", f, g2.getFontRenderContext());
    g2.setColor(Color.BLUE.darker().darker());
    t.draw(
        g2,
        getIconWidth() / 2 - (float) t.getBounds().getCenterX(),
        getIconHeight() / 2 - (float) t.getBounds().getCenterY());
  }

  private void paintText(Graphics2D g2, String s) {
    final var f =
        g2.getFont()
            .deriveFont(scale((float) getIconWidth() / (float) s.length()))
            .deriveFont(Font.BOLD);
    final var t = new TextLayout(s, f, g2.getFontRenderContext());
    g2.setColor(Color.RED.darker().darker());
    t.draw(
        g2,
        getIconWidth() / 2 - (float) t.getBounds().getCenterX(),
        getIconHeight() / 2 - (float) t.getBounds().getCenterY());
  }
}
