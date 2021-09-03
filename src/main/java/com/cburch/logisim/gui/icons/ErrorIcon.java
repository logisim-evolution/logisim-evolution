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
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import javax.swing.Icon;

public class ErrorIcon implements Icon {

  private final int wh;
  private boolean forwardArrow = false;
  private boolean backwardArrow = false;

  public ErrorIcon() {
    wh = AppPreferences.getIconSize();
  }

  public ErrorIcon(double scale) {
    wh = (int) AppPreferences.getScaled(scale * AppPreferences.getIconSize());
  }

  public ErrorIcon(int size) {
    wh = AppPreferences.getScaled(size);
  }

  public ErrorIcon(boolean forward, boolean backward) {
    wh = AppPreferences.getIconSize();
    if (forward) {
      forwardArrow = true;
      backwardArrow = false;
    } else {
      forwardArrow = false;
      backwardArrow = backward;
    }
  }

  public static int scale(int v) {
    return AppPreferences.getScaled(v);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    int mywh = !forwardArrow && !backwardArrow ? wh : (3 * wh) >> 2;
    int xoff = !forwardArrow && !backwardArrow ? 0 : wh >> 3;
    double trd = mywh / 3;
    int[] xpos = {
      xoff,
      xoff + (int) trd,
      xoff + (int) (2 * trd),
      xoff + mywh - 1,
      xoff + mywh - 1,
      xoff + (int) (2 * trd),
      xoff + (int) trd,
      xoff
    };
    int[] ypos = {(int) trd, 0, 0, (int) trd, (int) (2 * trd), mywh - 1, mywh - 1, (int) (2 * trd)};
    g2.setColor(Color.RED.brighter().brighter());
    g2.fillPolygon(xpos, ypos, 8);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.RED.darker().darker());
    g2.drawPolygon(xpos, ypos, 8);
    g2.setColor(Color.WHITE);
    Font f = g2.getFont().deriveFont((float) mywh / (float) 1.3).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout("X", f, g2.getFontRenderContext());
    float xc = (float) mywh / (float) 2 - (float) t.getBounds().getCenterX() + (float) xoff;
    float yc = (float) mywh / (float) 2 - (float) t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    if (forwardArrow) {
      g2.setColor(Color.BLACK);
      int five = (5 * wh) >> 3;
      int six = (6 * wh) >> 3;
      int seven = (7 * wh) >> 3;
      int[] axpos = {xoff, five, five, seven, five, five, xoff};
      int yoff = AppPreferences.getScaled(1);
      int[] aypos = {seven - yoff, seven - yoff, six, seven, wh - 1, seven + yoff, seven + yoff};
      g2.fillPolygon(axpos, aypos, 7);
    }
    if (backwardArrow) {
      g2.setColor(Color.BLACK);
      int three = (3 * wh) >> 3;
      int six = (6 * wh) >> 3;
      int seven = (7 * wh) >> 3;
      int[] axpos = {seven, three, three, xoff, three, three, seven};
      int yoff = AppPreferences.getScaled(1);
      int[] aypos = {seven - yoff, seven - yoff, six, seven, wh - 1, seven + yoff, seven + yoff};
      g2.fillPolygon(axpos, aypos, 7);
    }
    g2.dispose();
  }

  @Override
  public int getIconWidth() {
    return wh;
  }

  @Override
  public int getIconHeight() {
    return wh;
  }
}
