/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.std.io.HexDigit;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class SevenSegmentIcon extends BaseIcon {

  private final boolean isHexDisplay;

  public SevenSegmentIcon(boolean isHexDisplay) {
    this.isHexDisplay = isHexDisplay;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    // see HexDigit.getSegs()
    final var segson = HexDigit.getSegs(isHexDisplay ? 10 : 7);
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.WHITE);
    g2.fillRect(scale(2), 0, scale(10), scale(16));
    g2.setColor(Color.BLACK);
    g2.drawRect(scale(2), 0, scale(10), scale(16));
    g2.setColor((segson & HexDigit.SEG_A_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(5), scale(3), scale(8), scale(3));
    g2.setColor((segson & HexDigit.SEG_B_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(9), scale(4), scale(9), scale(7));
    g2.setColor((segson & HexDigit.SEG_C_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(9), scale(9), scale(9), scale(12));
    g2.setColor((segson & HexDigit.SEG_D_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(5), scale(13), scale(8), scale(13));
    g2.setColor((segson & HexDigit.SEG_F_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(4), scale(4), scale(4), scale(7));
    g2.setColor((segson & HexDigit.SEG_E_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(4), scale(9), scale(4), scale(12));
    g2.setColor((segson & HexDigit.SEG_G_MASK) != 0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(5), scale(8), scale(8), scale(8));
  }
}
