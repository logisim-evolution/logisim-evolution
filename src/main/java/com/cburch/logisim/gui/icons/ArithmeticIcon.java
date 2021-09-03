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

  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.BLACK);
    float scale = opp.length() >= nrOfChars ? nrOfChars : 1;
    int yoff = opp.length() > nrOfChars ? getIconHeight() >> 2 : getIconHeight() >> 1;
    Font f = g2.getFont().deriveFont((float) getIconWidth() / scale).deriveFont(Font.BOLD);
    g2.drawRect(scale(1), scale(1), getIconWidth() - scale(2), getIconHeight() - scale(2));
    TextLayout t =
        new TextLayout(
            opp.length() > nrOfChars ? opp.substring(0, nrOfChars) : opp,
            f,
            g2.getFontRenderContext());
    t.draw(
        g2,
        (float) (getIconWidth() / 2 - t.getBounds().getCenterX()),
        (float) (yoff - t.getBounds().getCenterY()));
    if (opp.length() > nrOfChars) {
      t =
          new TextLayout(
              opp.length() > 2 * nrOfChars
                  ? opp.substring(nrOfChars, 2 * nrOfChars)
                  : opp.substring(nrOfChars),
              f,
              g2.getFontRenderContext());
      t.draw(
          g2,
          (float) (getIconWidth() / 2 - t.getBounds().getCenterX()),
          (float) (3 * yoff - t.getBounds().getCenterY()));
    }
    if (invalid) {
      g2.setColor(Color.RED);
      g2.fillOval(0, getIconHeight() / 2, getIconWidth() / 2, getIconHeight() / 2);
      f =
          g2.getFont()
              .deriveFont(scale((float) getIconWidth() / (float) (2.8)))
              .deriveFont(Font.BOLD);
      t = new TextLayout("!", f, g2.getFontRenderContext());
      g2.setColor(Color.WHITE);
      t.draw(
          g2,
          (float) (getIconWidth() / 4 - t.getBounds().getCenterX()),
          (float) ((3 * getIconHeight()) / 4 - t.getBounds().getCenterY()));
    }
  }
}
