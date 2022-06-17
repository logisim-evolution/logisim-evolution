/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Graphics2D;

public class OpenSaveIcon extends BaseIcon {

  public static final int FILE_OPEN = 0;
  public static final int FILE_SAVE = 1;
  public static final int FILE_SAVE_AS = 2;
  private static final int[] arrowUp = new int[] {4, 3, 7, 0, 10, 3, 8, 3, 8, 6, 6, 6, 6, 3};
  private static final int[] arrowDown = new int[] {6, 0, 8, 0, 8, 3, 10, 3, 7, 6, 4, 3, 6, 3};
  private final int myType;

  public OpenSaveIcon(int type) {
    myType = type;
  }

  private Bounds getScaled(int x, int y, int width, int height) {
    return Bounds.create(
        AppPreferences.getScaled(x),
        AppPreferences.getScaled(y),
        AppPreferences.getScaled(width),
        AppPreferences.getScaled(height));
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var discCol = myType == FILE_SAVE_AS ? Color.GRAY : Color.BLUE;
    var bds = getScaled(2, 2, 12, 12);
    g2.setColor(discCol);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.YELLOW);
    bds = getScaled(4, 2, 8, 7);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.LIGHT_GRAY);
    bds = getScaled(6, 10, 4, 4);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    bds = getScaled(8, 11, 1, 2);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.MAGENTA);
    final int[] xPoints;
    final int[] yPoints;
    switch (myType) {
      case FILE_OPEN -> {
        xPoints = new int[7];
        yPoints = new int[7];
        for (var i = 0; i < 7; i++) {
          xPoints[i] = AppPreferences.getScaled(arrowUp[i * 2]);
          yPoints[i] = AppPreferences.getScaled(arrowUp[i * 2 + 1]);
        }
        g2.fillPolygon(xPoints, yPoints, 7);
      }
      case FILE_SAVE_AS, FILE_SAVE -> {
        xPoints = new int[7];
        yPoints = new int[7];
        for (var i = 0; i < 7; i++) {
          xPoints[i] = AppPreferences.getScaled(arrowDown[i * 2]);
          yPoints[i] = AppPreferences.getScaled(arrowDown[i * 2 + 1]);
        }
        g2.fillPolygon(xPoints, yPoints, 7);
      }
      default -> {
        // do nothing. should not really happen.
      }
    }
  }
}
