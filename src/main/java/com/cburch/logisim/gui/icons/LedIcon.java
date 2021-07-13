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
import java.awt.Color;
import java.awt.Graphics2D;

public class LedIcon extends BaseIcon {

  private final boolean isRgb;

  public LedIcon(boolean isRgb) {
    super();
    this.isRgb = isRgb;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    int xy = AppPreferences.getScaled(2);
    int wh = AppPreferences.getScaled(12);
    if (isRgb) {
      g2.setColor(Color.GREEN);
      g2.fillArc(xy, xy, wh, wh, 0, 120);
      g2.setColor(Color.RED);
      g2.fillArc(xy, xy, wh, wh, 120, 120);
      g2.setColor(Color.BLUE);
      g2.fillArc(xy, xy, wh, wh, 240, 120);
    } else {
      g2.setColor(Color.RED);
      g2.fillOval(xy, xy, wh, wh);
    }
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.drawOval(xy, xy, wh, wh);
  }
}
