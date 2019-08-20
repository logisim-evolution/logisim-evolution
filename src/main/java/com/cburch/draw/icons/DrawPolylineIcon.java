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
import java.awt.geom.GeneralPath;

import com.cburch.logisim.gui.icons.AnnimatedIcon;
import com.cburch.logisim.prefs.AppPreferences;

public class DrawPolylineIcon extends AnnimatedIcon {

  private static int[] points = {1,14,1,1,7,8,13,4,10,13};
  private boolean closed = false;
  private int state = points.length;
  
  public DrawPolylineIcon(boolean closed) {
    this.closed = closed;  
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    g2.setColor(Color.BLUE.darker());
    GeneralPath p = new GeneralPath();
    p.moveTo(AppPreferences.getScaled(points[0]), AppPreferences.getScaled(points[1]));
    for (int i = 2 ; i < state-1 ; i+=2)
      p.lineTo(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i+1]));
    if (closed&&state==points.length)
      p.closePath();
    g2.draw(p);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.setColor(Color.GRAY);
    int wh = AppPreferences.getScaled(3);
    for (int i = 0 ; i <= state-1 ; i+=2)
      g2.drawRect(AppPreferences.getScaled(points[i]-1), AppPreferences.getScaled(points[i+1]-1), wh, wh);
  }

  @Override
  public void annimationUpdate() {
    state++;
    if (state == 2)
      state++;
    state %= (points.length+1);
  }

  @Override
  public void resetToStatic() {
    state = points.length;
  }

}
