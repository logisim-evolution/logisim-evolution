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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class ArithmeticIcon extends BaseIcon {

  private final String opp;
  private boolean invalid;
  private int nrOfChars = 2;

  public ArithmeticIcon(String operation) {
    opp = operation;
    invalid = false;
  }

  public ArithmeticIcon(String operation, int charsPerLine) {
    opp = operation;
    invalid = false;
    nrOfChars = charsPerLine;
  }

  public void setInvalid(boolean invalid) {
    this.invalid = invalid;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(scale(2)));
    final var scale = opp.length() >= nrOfChars ? nrOfChars : 1;
    final var yOff = opp.length() > nrOfChars ? getIconHeight() >> 2 : getIconHeight() >> 1;
    var font = g2.getFont().deriveFont((float) getIconWidth() / scale).deriveFont(Font.BOLD);
    g2.drawRect(scale(1), scale(1), getIconWidth() - scale(2), getIconHeight() - scale(2));
    var textLayout =
        new TextLayout(
            opp.length() > nrOfChars ? opp.substring(0, nrOfChars) : opp,
            font,
            g2.getFontRenderContext());
    textLayout.draw(
        g2,
        (float) (getIconWidth() / 2 - textLayout.getBounds().getCenterX()),
        (float) (yOff - textLayout.getBounds().getCenterY()));
    if (opp.length() > nrOfChars) {
      textLayout =
          new TextLayout(
              opp.length() > 2 * nrOfChars
                  ? opp.substring(nrOfChars, 2 * nrOfChars)
                  : opp.substring(nrOfChars),
              font,
              g2.getFontRenderContext());
      textLayout.draw(
          g2,
          (float) (getIconWidth() / 2 - textLayout.getBounds().getCenterX()),
          (float) (3 * yOff - textLayout.getBounds().getCenterY()));
    }
    if (invalid) {
      g2.setColor(Color.RED);
      g2.fillOval(0, getIconHeight() / 2, getIconWidth() / 2, getIconHeight() / 2);
      font =
          g2.getFont()
              .deriveFont(scale((float) getIconWidth() / (float) (2.8)))
              .deriveFont(Font.BOLD);
      textLayout = new TextLayout("!", font, g2.getFontRenderContext());
      g2.setColor(Color.WHITE);
      textLayout.draw(
          g2,
          (float) (getIconWidth() / 4 - textLayout.getBounds().getCenterX()),
          (float) ((3 * getIconHeight()) / 4 - textLayout.getBounds().getCenterY()));
    }
  }
}
