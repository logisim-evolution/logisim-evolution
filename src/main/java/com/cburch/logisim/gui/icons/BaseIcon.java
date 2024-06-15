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

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

public abstract class BaseIcon implements javax.swing.Icon {

  /**
   * Returns value scaled by user selected scaling zoom-factor (Window->Zoom Factor).
   *
   * @param val Value to scale by user selected zoom-factor.
   * @return Returns scaled value.
   */
  public int scale(int val) {
    return AppPreferences.getScaled(val);
  }

  /**
   * Returns value scaled by user selected scaling zoom-factor (Window->Zoom Factor).
   *
   * @param val Value to scale by user selected zoom-factor.
   * @return Returns scaled value.
   */
  public double scale(double val) {
    return AppPreferences.getScaled(val);
  }

  /**
   * Returns value scaled by user selected scaling zoom-factor (Window->Zoom Factor).
   *
   * @param val Value to scale by user selected zoom-factor.
   * @return Returns scaled value.
   */
  public float scale(float val) {
    return AppPreferences.getScaled(val);
  }

  /**
   * Paints the icon image.
   *
   * @param comp Component
   * @param gfx Instance of java.awt.Graphics
   * @param x Starting X coordinate.
   * @param y Starting Y coordinate.
   */
  @Override
  public void paintIcon(Component comp, Graphics gfx, int x, int y) {
    final var g2 = (Graphics2D) gfx.create();
    g2.setColor(new Color(AppPreferences.COMPONENT_ICON_COLOR.get()));
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.translate(x, y);
    paintIcon(g2);
    g2.dispose();
  }

  protected abstract void paintIcon(Graphics2D g2);

  /**
   * Calculates icon width to match current user zoom scale factor.
   *
   * @return Icon width as calculated according to user zoom scale factor.
   */
  @Override
  public int getIconWidth() {
    return AppPreferences.getIconSize();
  }

  /**
   * Calculates icon height to match current user zoom scale factor.
   *
   * @return Icon width as calculated according to user zoom scale factor.
   */
  @Override
  public int getIconHeight() {
    return AppPreferences.getIconSize();
  }
}
