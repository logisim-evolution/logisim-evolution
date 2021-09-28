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
import java.awt.Graphics2D;

public class JoystickIcon extends BaseIcon {

  private int state = 1;

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLUE.darker().darker());
    final int[] xpos = {0, scale(6), scale(4), scale(2)};
    final int[] ypos = {scale(13), scale(13), scale(15), scale(15)};
    g2.fillPolygon(xpos, ypos, 4);
    for (var i = 0; i < 4; i++) {
      xpos[i] += scale(10);
    }
    g2.fillPolygon(xpos, ypos, 4);

    final int xbase = scale(9);
    final int ybase = scale(11);
    var xtop = scale(13);
    var ytop = scale(3);
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.BLACK);
    g2.drawLine(xtop, ytop, xbase, ybase);
    xtop -= scale(2);
    ytop -= scale(2);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.RED);
    g2.fillOval(xtop, ytop, scale(4), scale(4));
    g2.drawOval(xtop, ytop, scale(4), scale(4));
    g2.setColor(Color.BLUE);
    g2.fillRoundRect(0, scale(10), scale(16), scale(4), scale(2), scale(2));
    g2.drawRoundRect(0, scale(10), scale(16), scale(4), scale(2), scale(2));
  }
}
