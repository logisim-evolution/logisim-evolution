/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class AppearEditIcon extends BaseIcon {

  private final int[] tip = {0, 14, 1, 15, 0, 15};
  private final int[] body = {0, 13, 13, 0, 15, 2, 2, 15};
  private final int[] extendedtip = {0, 13, 1, 12, 3, 15, 2, 15};
  private final int[] cleantip = {12, 1, 13, 0, 15, 2, 14, 3};

  @Override
  protected void paintIcon(Graphics2D g2) {
    final var g = (Graphics2D) g2.create();
    SubcircuitFactory.paintClasicIcon(g);
    g.dispose();
    g2.setColor(Color.MAGENTA);
    var path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(tip[0]), AppPreferences.getScaled(tip[1]));
    for (int i = 2; i < tip.length; i += 2) {
      path.lineTo(AppPreferences.getScaled(tip[i]), AppPreferences.getScaled(tip[i + 1]));
    }
    path.closePath();
    g2.fill(path);
    g2.setColor(new Color(139, 69, 19));
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(body[0]), AppPreferences.getScaled(body[1]));
    for (int i = 2; i < body.length; i += 2) {
      path.lineTo(AppPreferences.getScaled(body[i]), AppPreferences.getScaled(body[i + 1]));
    }
    path.closePath();
    g2.fill(path);
    g2.setColor(new Color(210, 180, 140));
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(extendedtip[0]), AppPreferences.getScaled(extendedtip[1]));
    for (int i = 2; i < extendedtip.length; i += 2) {
      path.lineTo(
          AppPreferences.getScaled(extendedtip[i]), AppPreferences.getScaled(extendedtip[i + 1]));
    }
    path.closePath();
    g2.fill(path);
    g2.setColor(Color.GRAY);
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(cleantip[0]), AppPreferences.getScaled(cleantip[1]));
    for (int i = 2; i < cleantip.length; i += 2) {
      path.lineTo(AppPreferences.getScaled(cleantip[i]), AppPreferences.getScaled(cleantip[i + 1]));
    }
    path.closePath();
    g2.fill(path);
  }
}
