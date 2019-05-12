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

package com.cburch.draw.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.cburch.logisim.gui.icons.AbstractIcon;
import com.cburch.logisim.prefs.AppPreferences;

public class DrawShapeIcon extends AbstractIcon {

  public static final int RECTANGLE = 0;
  public static final int ROUNDEDRECTANGLE = 1;
  public static final int ELIPSE = 2;
  
  private int type;
  
  public DrawShapeIcon( int type ) {
    this.type = type;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.setColor(Color.BLUE.darker());
    int x = AppPreferences.getScaled(1);
    int width = AppPreferences.getScaled(14);
    int y = AppPreferences.getScaled(3);
    int height = AppPreferences.getScaled(8);
    if (type == RECTANGLE)
      g2.drawRect(x, y, width, height);
    else if (type == ROUNDEDRECTANGLE)
      g2.drawRoundRect(x, y, width, height, y, y);
    else
      g2.drawOval(x, y, width, height);
    g2.setColor(Color.GRAY);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.drawRect(0, y-x, y, y);
    g2.drawRect(width-x, y+height-x, y, y);
  }

}
