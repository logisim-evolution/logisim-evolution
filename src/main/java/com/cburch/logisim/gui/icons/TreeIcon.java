/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;

public class TreeIcon extends BaseIcon {

  private final Rectangle paper =
      new Rectangle(
          AppPreferences.ICON_BORDER,
          AppPreferences.ICON_BORDER,
          AppPreferences.IconSize - 3 * AppPreferences.ICON_BORDER,
          AppPreferences.IconSize - 2 * AppPreferences.ICON_BORDER);
  private final int[] backsheet = new int[] {0, 0, 4, 0, 7, 3, 13, 3, 13, 15, 0, 15};
  private final int[] frontsheetClosed = new int[] {0, 3, 13, 3, 13, 15, 0, 15};
  private final int[] frontsheetOpen = new int[] {2, 11, 15, 11, 13, 15, 0, 15};
  private final int[] shape =
      new int[] {7, 3, 4, 3, 4, 4, 2, 4, 4, 4, 4, 8, 2, 8, 4, 8, 4, 9, 7, 9};
  private boolean closed = true;

  public TreeIcon(boolean closed) {
    this.closed = closed;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.yellow.darker().darker());
    var path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(backsheet[0]), AppPreferences.getScaled(backsheet[1]));
    for (var i = 2; i < backsheet.length; i += 2)
      path.lineTo(
          AppPreferences.getScaled(backsheet[i]), AppPreferences.getScaled(backsheet[i + 1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(Color.LIGHT_GRAY.brighter());
    g2.fill3DRect(
        AppPreferences.getScaled(paper.x),
        AppPreferences.getScaled(paper.y),
        AppPreferences.getScaled(paper.width),
        AppPreferences.getScaled(paper.height),
        true);
    g2.setColor(Color.BLACK);
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(shape[0]), AppPreferences.getScaled(shape[1]));
    for (int i = 2; i < shape.length; i += 2)
      path.lineTo(AppPreferences.getScaled(shape[i]), AppPreferences.getScaled(shape[i + 1]));
    g2.draw(path);
    g2.drawArc(
        AppPreferences.getScaled(4),
        AppPreferences.getScaled(3),
        AppPreferences.getScaled(6),
        AppPreferences.getScaled(6),
        -90,
        180);
    g2.drawLine(
        AppPreferences.getScaled(10),
        AppPreferences.getScaled(6),
        AppPreferences.getScaled(11),
        AppPreferences.getScaled(6));
    path = new GeneralPath();
    final var frontSheet = closed ? frontsheetClosed : frontsheetOpen;
    path.moveTo(AppPreferences.getScaled(frontSheet[0]), AppPreferences.getScaled(frontSheet[1]));
    for (var i = 2; i < frontSheet.length; i += 2)
      path.lineTo(
          AppPreferences.getScaled(frontSheet[i]), AppPreferences.getScaled(frontSheet[i + 1]));
    path.closePath();
    g2.setColor(Color.yellow.darker());
    g2.fill(path);
  }
}
