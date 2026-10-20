/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.data.Value;
import java.awt.BasicStroke;
import java.awt.Graphics2D;

public class PlexerIcon extends BaseIcon {

  private static final int[] xpos = {4, 4, 10, 10};
  private static final int[] ypos = {0, 14, 9, 5};
  private final boolean inverted;
  private final boolean singleInput;

  public PlexerIcon(boolean demux, boolean singleInput) {
    inverted = demux;
    this.singleInput = singleInput;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(scale(2)));
    final var realPosX = new int[4];
    final var realPosY = new int[4];
    var xOffset = inverted ? 2 : 0;
    for (var i = 0; i < 4; i++) {
      realPosX[i] = scale(xpos[(i + xOffset) & 3]);
      realPosY[i] = scale(ypos[i]);
    }
    g2.drawPolygon(realPosX, realPosY, 4);
    xOffset = inverted ? scale(7) : scale(8);
    g2.drawLine(xOffset, scale(11), xOffset, scale(15));
    /* draw output */
    xOffset = inverted ? scale(xpos[0] - 1) : scale(xpos[2] - 1);
    var yOffset = scale(ypos[3] + 1);
    g2.setColor(Value.trueColor);
    g2.fillOval(xOffset, yOffset, scale(3), scale(3));
    xOffset = inverted ? scale(xpos[2] - 1) : scale(xpos[0] - 1);
    if (singleInput) {
      g2.fillOval(xOffset, yOffset, scale(3), scale(3));
    } else {
      g2.fillOval(xOffset, scale(1), scale(3), scale(3));
      g2.fillOval(xOffset, scale(11), scale(3), scale(3));
    }
  }
}
