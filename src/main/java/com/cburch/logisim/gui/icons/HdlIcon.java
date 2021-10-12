/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.vhdl.gui.HdlToolbarModel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class HdlIcon extends BaseIcon {

  private final String type;

  public HdlIcon(String type) {
    this.type = type;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.WHITE);
    g2.fillRect(0, scale(4), scale(16), scale(12));
    g2.setColor(Color.BLACK);
    g2.drawRect(0, scale(4), scale(16), scale(12));
    g2.setColor(Color.LIGHT_GRAY);
    final var font = g2.getFont().deriveFont((float) getIconWidth() / (float) 4.5);
    var t = new TextLayout("LIBRARY", font, g2.getFontRenderContext());
    t.draw(
        g2,
        (float) (getIconWidth() / 2 - t.getBounds().getCenterX()),
        (float) ((3 * getIconHeight()) / 8 - t.getBounds().getCenterY()));
    t = new TextLayout("USE iee", font, g2.getFontRenderContext());
    t.draw(
        g2,
        (float) (getIconWidth() / 2 - t.getBounds().getCenterX()),
        (float) ((5 * getIconHeight()) / 8 - t.getBounds().getCenterY()));
    t = new TextLayout("ENTITY ", font, g2.getFontRenderContext());
    t.draw(
        g2,
        (float) (getIconWidth() / 2 - t.getBounds().getCenterX()),
        (float) ((7 * getIconHeight()) / 8 - t.getBounds().getCenterY()));
    if (type.equals(HdlToolbarModel.HDL_VALIDATE)) {
      g2.setColor(Color.RED);
      g2.setStroke(new BasicStroke(scale(2)));
      final int[] xpos = {scale(3), scale(6), scale(15)};
      final int[] ypos = {scale(3), scale(11), 0};
      g2.drawPolyline(xpos, ypos, 3);
    } else {
      if (type.equals(HdlToolbarModel.HDL_EXPORT)) {
        g2.translate(scale(8), scale(4));
        g2.rotate(Math.PI);
        g2.translate(-scale(8), -scale(4));
      }
      g2.setColor(Color.MAGENTA.darker());
      final int[] x = {scale(7), scale(7), scale(5), scale(8), scale(11), scale(9), scale(9)};
      final int[] y = {0, scale(5), scale(5), scale(8), scale(5), scale(5), 0};
      g2.fillPolygon(x, y, 7);
      g2.drawPolygon(x, y, 7);
    }
  }
}
