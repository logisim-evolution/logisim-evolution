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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;

public class FlipFlopIcon extends BaseIcon {

  public static final int D_FLIPFLOP = 0;
  public static final int T_FLIPFLOP = 1;
  public static final int JK_FLIPFLOP = 2;
  public static final int SR_FLIPFLOP = 3;
  public static final int REGISTER = 4;

  private final int type;

  public FlipFlopIcon(int type) {
    this.type = type;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    if (AppPreferences.getDefaultAppearance().equals(StdAttr.APPEAR_CLASSIC)) {
      paintClassicIcon(g2);
    } else {
      paintEvolutionIcon(g2);
    }
  }

  private void paintClassicIcon(Graphics2D g2) {
    g2.drawRect(scale(2), scale(2), scale(12), scale(12));
    var str = "";
    switch (type) {
      case D_FLIPFLOP -> str = "D";
      case T_FLIPFLOP -> str = "T";
      case JK_FLIPFLOP -> str = "JK";
      case SR_FLIPFLOP -> str = "SR";
      case REGISTER -> str = "00";
      default -> {
        // do nothing. Should not really happen.
      }
    }
    final var f = g2.getFont().deriveFont((float) ((double) AppPreferences.getIconSize() / 2.1));
    final var l = new TextLayout(str, f, g2.getFontRenderContext());
    l.draw(g2, (float) (getIconWidth() / 2 - l.getBounds().getCenterX()), (float) (getIconHeight() / 2 - l.getBounds().getCenterY()));
  }

  private void paintEvolutionIcon(Graphics2D g2) {
    if (type == REGISTER) {
      paintRegisterIcon(g2);
      return;
    }
    g2.drawRect(scale(4), 0, scale(8), scale(16));
    g2.drawLine(scale(0), scale(3), scale(4), scale(3));
    g2.drawLine(scale(12), scale(3), scale(15), scale(3));
    if (type == JK_FLIPFLOP || type == SR_FLIPFLOP) {
      g2.drawLine(scale(0), scale(7), scale(4), scale(7));
    }
    g2.drawLine(scale(0), scale(12), scale(4), scale(12));
    final int[] xp = {scale(4), scale(7), scale(4)};
    final int[] yp = {scale(11), scale(12), scale(13)};
    g2.drawPolygon(xp, yp, 3);
    g2.drawOval(scale(12), scale(10), scale(4), scale(4));
    final var p = new GeneralPath();
    switch (type) {
      case D_FLIPFLOP -> {
        p.moveTo(scale(7), scale(5));
        p.lineTo(scale(6), scale(5));
        p.lineTo(scale(6), scale(2));
        p.lineTo(scale(7), scale(2));
        p.quadTo(scale(10), scale(4), scale(7), scale(5));
      }
      case T_FLIPFLOP -> {
        p.moveTo(scale(6), scale(2));
        p.lineTo(scale(8), scale(2));
        p.moveTo(scale(7), scale(2));
        p.lineTo(scale(7), scale(4));
      }
      case JK_FLIPFLOP -> {
        p.moveTo(scale(6), scale(2));
        p.lineTo(scale(8), scale(2));
        p.lineTo(scale(8), scale(4));
        p.quadTo(scale(7), scale(6), scale(6), scale(4));
        p.moveTo(scale(6), scale(6));
        p.lineTo(scale(6), scale(8));
        p.moveTo(scale(8), scale(6));
        p.lineTo(scale(7), scale(7));
        p.lineTo(scale(8), scale(8));
      }
      case SR_FLIPFLOP -> {
        p.moveTo(scale(7), scale(1));
        p.curveTo(scale(4), scale(2), scale(9), scale(4), scale(6), scale(5));
        p.moveTo(scale(6), scale(9));
        p.lineTo(scale(6), scale(6));
        p.quadTo(scale(9), scale(7), scale(7), scale(8));
        p.lineTo(scale(8), scale(9));
      }
      default -> {
        // do nothing. Should not really happen.
      }
    }
    g2.draw(p);
  }

  private void paintRegisterIcon(Graphics2D g2) {
    for (var i = 2; i >= 0; i--) {
      g2.setColor(Color.WHITE);
      g2.fillRect(scale((i + 1) * 2), scale(4 - i * 2), scale(8), scale(12));
      g2.setColor(Color.BLACK);
      g2.drawRect(scale((i + 1) * 2), scale(4 - i * 2), scale(8), scale(12));
    }
    final int[] xp = {scale(2), scale(5), scale(2)};
    final int[] yp = {scale(11), scale(12), scale(13)};
    g2.drawPolygon(xp, yp, 3);
    g2.drawLine(scale(0), scale(12), scale(2), scale(12));
    g2.fillRect(0, scale(5), scale(2), scale(2));
    g2.fillRect(scale(10), scale(5), scale(6), scale(2));
  }
}
