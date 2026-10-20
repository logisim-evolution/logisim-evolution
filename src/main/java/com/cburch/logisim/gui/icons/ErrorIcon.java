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
    final var g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    final var mywh = !forwardArrow && !backwardArrow ? wh : (3 * wh) >> 2;
    final var xoff = !forwardArrow && !backwardArrow ? 0 : wh >> 3;
    final var trd = mywh / 3;
    int[] xPos = {
      xoff,
      xoff + trd,
      xoff + (2 * trd),
      xoff + mywh - 1,
      xoff + mywh - 1,
      xoff + (2 * trd),
      xoff + trd,
      xoff
    };
    int[] ypos = {trd, 0, 0, trd, 2 * trd, mywh - 1, mywh - 1, 2 * trd};
    g2.setColor(Color.RED.brighter().brighter());
    g2.fillPolygon(xPos, ypos, 8);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.RED.darker().darker());
    g2.drawPolygon(xPos, ypos, 8);
    g2.setColor(Color.WHITE);
    final var f = g2.getFont().deriveFont((float) mywh / (float) 1.3).deriveFont(Font.BOLD);
    final var t = new TextLayout("X", f, g2.getFontRenderContext());
    final var xc = (float) mywh / (float) 2 - (float) t.getBounds().getCenterX() + (float) xoff;
    final var yc = (float) mywh / (float) 2 - (float) t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    if (forwardArrow) {
      g2.setColor(Color.BLACK);
      final var five = (5 * wh) >> 3;
      final var six = (6 * wh) >> 3;
      final var seven = (7 * wh) >> 3;
      final int[] axPos = {xoff, five, five, seven, five, five, xoff};
      final var yOff = AppPreferences.getScaled(1);
      final int[] ayPos = {
        seven - yOff, seven - yOff, six, seven, wh - 1, seven + yOff, seven + yOff
      };
      g2.fillPolygon(axPos, ayPos, 7);
    }
    if (backwardArrow) {
      g2.setColor(Color.BLACK);
      final var three = (3 * wh) >> 3;
      final var six = (6 * wh) >> 3;
      final var seven = (7 * wh) >> 3;
      final int[] axPos = {seven, three, three, xoff, three, three, seven};
      final var yOff = AppPreferences.getScaled(1);
      final int[] ayPos = {
        seven - yOff, seven - yOff, six, seven, wh - 1, seven + yOff, seven + yOff
      };
      g2.fillPolygon(axPos, ayPos, 7);
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
