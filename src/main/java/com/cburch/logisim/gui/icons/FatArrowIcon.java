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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.prefs.AppPreferences;

public class FatArrowIcon extends AbstractIcon {

  private Direction dir;
  private static int[] points = {2,7,7,2,12,7,9,7,9,12,5,12,5,7};
  
  public FatArrowIcon(Direction dir) {
    this.dir = dir;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.translate(AppPreferences.getScaled(7), AppPreferences.getScaled(7));
    if (dir.equals(Direction.WEST))
      g2.rotate((6*Math.PI)/4);
    else if (dir.equals(Direction.SOUTH))
      g2.rotate(Math.PI);
    else if (dir.equals(Direction.EAST))
      g2.rotate(Math.PI/2);
    g2.translate(-AppPreferences.getScaled(7), -AppPreferences.getScaled(7));
    g2.setColor(Color.blue);
    GeneralPath path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(points[0]), AppPreferences.getScaled(points[1]));
    for (int i = 2 ; i < points.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i+1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(Color.blue.darker());
    g2.draw(path);
  }

}
