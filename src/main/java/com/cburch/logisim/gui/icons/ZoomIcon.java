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
import java.awt.geom.GeneralPath;

import com.cburch.logisim.prefs.AppPreferences;

public class ZoomIcon  extends AbstractIcon {
  
  public static int ZOOMIN = 0;
  public static int ZOOMOUT = 1;
  public static int NOZOOM = 2;
  
  private int zoomType;
  
  public ZoomIcon() {
    zoomType = NOZOOM;
  }
  
  public ZoomIcon(int type) {
    zoomType = type;
  }

  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke((int)AppPreferences.getScaled(1.5)));
    g2.setColor(g2.getBackground().darker());
    int scaledOne = AppPreferences.getScaled(1);
    int scaledEleven = AppPreferences.getScaled(10);
    g2.fillOval(scaledOne, scaledOne, scaledEleven, scaledEleven);
    g2.setColor(g2.getBackground().darker().darker().darker());
    if (zoomType != NOZOOM) {
      g2.drawLine(AppPreferences.getScaled(4), AppPreferences.getScaled(6), AppPreferences.getScaled(8), AppPreferences.getScaled(6));
      if (zoomType == ZOOMIN)
        g2.drawLine(AppPreferences.getScaled(6), AppPreferences.getScaled(4), AppPreferences.getScaled(6), AppPreferences.getScaled(8));
    }
    g2.setColor(Color.BLACK);
    g2.drawOval(scaledOne, scaledOne, scaledEleven, scaledEleven);
    double xyPoint = AppPreferences.getScaled(6.0+Math.sqrt(12.5));
    GeneralPath path = new GeneralPath();
    path.moveTo(xyPoint, xyPoint);
    path.lineTo(AppPreferences.getScaled(15), AppPreferences.getScaled(13));
    path.lineTo(AppPreferences.getScaled(13), AppPreferences.getScaled(15));
    path.closePath();
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.setColor(new Color(139,69,19));
    g2.fill(path);
  }

}
