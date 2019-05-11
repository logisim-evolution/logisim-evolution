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
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;

import com.cburch.logisim.prefs.AppPreferences;

public class TreeIcon extends AbstractIcon{

  private boolean closed = true;
  private Rectangle paper = new Rectangle(AppPreferences.IconBorder,AppPreferences.IconBorder,
      AppPreferences.IconSize-3*AppPreferences.IconBorder,AppPreferences.IconSize-2*AppPreferences.IconBorder);
  private int[] backsheet = new int[] {0,0,4,0,7,3,13,3,13,15,0,15};
  private int[] frontsheetClosed = new int[] {0,3,13,3,13,15,0,15};
  private int[] frontsheetOpen = new int[] {2,11,15,11,13,15,0,15};
  private int[] shape = new int[] {7,3,4,3,4,4,2,4,4,4,4,8,2,8,4,8,4,9,7,9};

  public TreeIcon(boolean closed) {
    this.closed = closed;
  }

  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.yellow.darker().darker());
    GeneralPath path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(backsheet[0]), AppPreferences.getScaled(backsheet[1]));
    for (int i = 2 ; i < backsheet.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(backsheet[i]), AppPreferences.getScaled(backsheet[i+1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(Color.LIGHT_GRAY.brighter());
    g2.fill3DRect(AppPreferences.getScaled(paper.x), AppPreferences.getScaled(paper.y), 
        AppPreferences.getScaled(paper.width), AppPreferences.getScaled(paper.height), true);
    g2.setColor(Color.BLACK);
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(shape[0]), AppPreferences.getScaled(shape[1]));
    for (int i = 2 ; i < shape.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(shape[i]), AppPreferences.getScaled(shape[i+1]));
    g2.draw(path);
    g2.drawArc(AppPreferences.getScaled(4), AppPreferences.getScaled(3), 
        AppPreferences.getScaled(6), AppPreferences.getScaled(6), -90, 180);
    g2.drawLine(AppPreferences.getScaled(10), AppPreferences.getScaled(6), 
        AppPreferences.getScaled(11), AppPreferences.getScaled(6));
    path = new GeneralPath();
    int[] frontsheet = closed ? frontsheetClosed : frontsheetOpen;
    path.moveTo(AppPreferences.getScaled(frontsheet[0]), AppPreferences.getScaled(frontsheet[1]));
    for (int i = 2 ; i < frontsheet.length ; i +=2)
        path.lineTo(AppPreferences.getScaled(frontsheet[i]), AppPreferences.getScaled(frontsheet[i+1]));
    path.closePath();
    g2.setColor(Color.yellow.darker());
    g2.fill(path);
  }

}
