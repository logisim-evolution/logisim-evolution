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
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;

public class ProjectAddIcon extends BaseIcon {

  private static final int[] points = {
    2, 6, 6, 6, 6, 2, 9, 2, 9, 6, 13, 6, 13, 9, 9, 9, 9, 13, 6, 13, 6, 9, 2, 9
  };
  private boolean removeIcon = false;
  private boolean vhdl = false;
  private boolean deselect = false;

  public ProjectAddIcon() {
    vhdl = true;
  }

  public ProjectAddIcon(boolean removeSymbol) {
    removeIcon = removeSymbol;
  }

  public void setDeselect(boolean val) {
    deselect = val;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    if (deselect) {
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
    }
    if (vhdl) {
      g2.setColor(Color.GREEN.darker().darker());
      final var f =
          g2.getFont()
              .deriveFont((float) AppPreferences.getIconSize() / (float) 1.6)
              .deriveFont(Font.BOLD);
      var l1 = new TextLayout("VH", f, g2.getFontRenderContext());
      var top = AppPreferences.getIconSize() / 4 - l1.getBounds().getCenterY();
      var left = AppPreferences.getIconSize() / 2 - l1.getBounds().getCenterX();
      l1.draw(g2, (float) left, (float) top);
      l1 = new TextLayout("DL", f, g2.getFontRenderContext());
      top = (3 * AppPreferences.getIconSize()) / 4 - l1.getBounds().getCenterY();
      left = AppPreferences.getIconSize() / 2 - l1.getBounds().getCenterX();
      l1.draw(g2, (float) left, (float) top);
      g2.dispose();
      return;
    }
    if (removeIcon) {
      g2.setColor(Color.RED);
      g2.translate(AppPreferences.getIconSize() / 2, AppPreferences.getIconSize() / 2);
      g2.rotate(Math.PI / 4);
      g2.translate(-AppPreferences.getIconSize() / 2, -AppPreferences.getIconSize() / 2);
    } else g2.setColor(Color.GREEN.darker());
    final var path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(points[0]), AppPreferences.getScaled(points[1]));
    for (var i = 2; i < points.length; i += 2)
      path.lineTo(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i + 1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(removeIcon ? Color.RED.darker() : Color.GREEN.darker().darker());
    g2.draw(path);
  }
}
