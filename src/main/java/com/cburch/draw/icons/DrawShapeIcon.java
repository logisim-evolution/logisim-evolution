/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.icons;

import com.cburch.logisim.gui.icons.BaseIcon;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class DrawShapeIcon extends BaseIcon {

  public static final int RECTANGLE = 0;
  public static final int ROUNDED_RECTANGLE = 1;
  public static final int ELIPSE = 2;

  private static final int[] points = {3, 2, 8, 4, 14, 8};
  private final int shapeType;

  public DrawShapeIcon(int type) {
    this.shapeType = type;
  }

  @Override
  protected void paintIcon(Graphics2D gfx) {
    final var state = 3;

    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    final var x = scale(1);
    final var y = scale(3);
    final var width = scale(points[(state - 1) * 2]);
    final var height = scale(points[(state - 1) * 2 + 1]);

    switch (shapeType) {
      case RECTANGLE -> {
        gfx.setColor(Color.BLUE.darker());
        gfx.drawRect(x, y, width, height);
      }
      case ROUNDED_RECTANGLE -> {
        gfx.setColor(Color.RED.darker());
        // FIXME: rounded rect shape looks almost as regular rectangle on smaller zoom factor
        gfx.drawRoundRect(x, y, width, height, y, y);
      }
      default -> {
        gfx.setColor(Color.BLUE);
        gfx.drawOval(x, y, width, height);
      }
    }

    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    gfx.drawRect(0, y - x, y, y);
    gfx.drawRect(width - x, y + height - x, y, y);
  }

}
