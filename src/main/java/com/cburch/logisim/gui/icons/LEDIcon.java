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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.cburch.logisim.prefs.AppPreferences;

public class LEDIcon extends AnnimatedIcon {

  private boolean isRGB;
  private int showstate;
  
  public LEDIcon(boolean RGB) {
    super();
    isRGB = RGB;
    showstate = -1;
  }
  @Override
  protected void paintIcon(Graphics2D g2) {
    int xy = AppPreferences.getScaled(2);
    int wh = AppPreferences.getScaled(12);
    int r = (showstate&1) == 1 ? 255 : 0;
    int g = (showstate&2) == 2 ? 255 : 0;
    int b = (showstate&4) == 4 ? 255 : 0;
    Color c = (showstate >= 0) ? new Color(r,g,b) : Color.RED;
    g2.setColor(c);
    g2.fillOval(xy, xy, wh, wh);
    if (isRGB && showstate < 0) {
      g2.setColor(Color.GREEN);
      g2.fillArc(xy, xy, wh, wh, 0, 120);
      g2.setColor(Color.BLUE);
      g2.fillArc(xy, xy, wh, wh, 120, 120);
    }
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.drawOval(xy, xy, wh, wh);
  }

  @Override
  public void annimationUpdate() {
      showstate++;
      showstate %= isRGB ? 8:2;
  }

  @Override
  public void resetToStatic() {
    showstate = -1;
  }

}
