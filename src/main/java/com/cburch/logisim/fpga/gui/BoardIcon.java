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

  public BoardIcon(BufferedImage boardImage) {
    image = (boardImage == null)
            ? null
            : boardImage.getScaledInstance(this.getIconWidth(), this.getIconHeight(), BufferedImage.SCALE_SMOOTH);
  }

  @Override
  public int getIconHeight() {
    return AppPreferences.getScaled(ICON_HEIGHT);
  }

  @Override
  public int getIconWidth() {
    return AppPreferences.getScaled(ICON_WIDTH);
  }

  @Override
  public void paintIcon(Component comp, Graphics gfx, int x, int y) {
    if (image != null) {
      gfx.drawImage(image, x, y, null);
    } else {
      gfx.setColor(Color.gray);
      gfx.fillRect(0, 0, this.getIconWidth(), this.getIconHeight());
    }
  }

  public void setImage(BufferedImage boardImage) {
    image = (boardImage == null)
            ? null
            : boardImage.getScaledInstance(this.getIconWidth(), this.getIconHeight(), BufferedImage.SCALE_SMOOTH);
  }
}
