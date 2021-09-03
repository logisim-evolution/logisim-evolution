/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

public class BoardIcon implements Icon {
  private Image image;
  private static final int ICON_WIDTH = 240;
  private static final int ICON_HEIGHT = 130;

  public BoardIcon(BufferedImage BoardImage) {
    if (BoardImage == null) image = null;
    else
      image =
          BoardImage.getScaledInstance(
              this.getIconWidth(), this.getIconHeight(), BufferedImage.SCALE_SMOOTH);
  }

  public int getIconHeight() {
    return AppPreferences.getScaled(ICON_HEIGHT);
  }

  public int getIconWidth() {
    return AppPreferences.getScaled(ICON_WIDTH);
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (image != null) g.drawImage(image, x, y, null);
    else {
      g.setColor(Color.gray);
      g.fillRect(0, 0, this.getIconWidth(), this.getIconHeight());
    }
  }

  public void SetImage(BufferedImage BoardImage) {
    if (BoardImage == null) image = null;
    else
      image =
          BoardImage.getScaledInstance(
              this.getIconWidth(), this.getIconHeight(), BufferedImage.SCALE_SMOOTH);
  }
}
