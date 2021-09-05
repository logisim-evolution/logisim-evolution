/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

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
    Graphics2D g2 = (Graphics2D) gfx.create();
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
