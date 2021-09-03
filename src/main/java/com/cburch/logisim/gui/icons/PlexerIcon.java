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
    int[] realXpos = new int[4];
    int[] realYpos = new int[4];
    int xoff = inverted ? 2 : 0;
    for (int i = 0; i < 4; i++) {
      realXpos[i] = scale(xpos[(i + xoff) & 3]);
      realYpos[i] = scale(ypos[i]);
    }
    g2.drawPolygon(realXpos, realYpos, 4);
    xoff = inverted ? scale(7) : scale(8);
    g2.drawLine(xoff, scale(11), xoff, scale(15));
    /* draw output */
    xoff = inverted ? scale(xpos[0] - 1) : scale(xpos[2] - 1);
    int yoff = scale(ypos[3] + 1);
    g2.setColor(Value.TRUE_COLOR);
    g2.fillOval(xoff, yoff, scale(3), scale(3));
    xoff = inverted ? scale(xpos[2] - 1) : scale(xpos[0] - 1);
    if (singleInput) {
      g2.fillOval(xoff, yoff, scale(3), scale(3));
    } else {
      g2.fillOval(xoff, scale(1), scale(3), scale(3));
      g2.fillOval(xoff, scale(11), scale(3), scale(3));
    }
  }
}
