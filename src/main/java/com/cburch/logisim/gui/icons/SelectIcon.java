/**
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

import java.awt.Graphics2D;

import com.cburch.logisim.prefs.AppPreferences;

public class SelectIcon extends AbstractIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    paint(g2);
  }
  
  public static void paint(Graphics2D g2) {
    int[] xp = {3, 3, 7, 10, 11, 9, 14};
    int[] yp = {0, 17, 12, 16, 16, 12, 12};
    int[] sxp = new int[xp.length];
    int[] syp = new int[yp.length];
    for (int i = 0 ; i < xp.length ; i++) {
      sxp[i] = AppPreferences.getScaled(xp[i]);
      syp[i] = AppPreferences.getScaled(yp[i]);
    }
    g2.setColor(java.awt.Color.black);
    g2.fillPolygon(sxp, syp, xp.length);
  }

}
