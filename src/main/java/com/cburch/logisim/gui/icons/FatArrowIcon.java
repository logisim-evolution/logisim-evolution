/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class FatArrowIcon extends BaseIcon {

  private static final int[] points = {2, 7, 7, 2, 12, 7, 9, 7, 9, 12, 5, 12, 5, 7};
  private final Direction dir;

  public FatArrowIcon(Direction dir) {
    this.dir = dir;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.translate(AppPreferences.getScaled(7), AppPreferences.getScaled(7));
    if (dir.equals(Direction.WEST)) g2.rotate((6 * Math.PI) / 4);
    else if (dir.equals(Direction.SOUTH)) g2.rotate(Math.PI);
    else if (dir.equals(Direction.EAST)) g2.rotate(Math.PI / 2);
    g2.translate(-AppPreferences.getScaled(7), -AppPreferences.getScaled(7));
    g2.setColor(Color.blue);
    final var path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(points[0]), AppPreferences.getScaled(points[1]));
    for (var i = 2; i < points.length; i += 2)
      path.lineTo(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i + 1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(Color.blue.darker());
    g2.draw(path);
  }
}
