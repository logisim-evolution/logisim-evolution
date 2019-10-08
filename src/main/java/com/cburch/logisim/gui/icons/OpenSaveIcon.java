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

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.prefs.AppPreferences;

public class OpenSaveIcon extends AbstractIcon {

  public static final int FILE_OPEN = 0;
  public static final int FILE_SAVE = 1;
  public static final int FILE_SAVE_AS = 2;
  
  private int myType;
  private static final int[] Arrowup = new int[] {4,3,7,0,10,3,8,3,8,6,6,6,6,3};
  private static final int[] Arrowdown = new int[] {6,0,8,0,8,3,10,3,7,6,4,3,6,3};
  
  public OpenSaveIcon(int type) {
    myType = type;
  }
  
  private Bounds getScaled(int x , int y , int width , int height) {
    return Bounds.create(AppPreferences.getScaled(x), AppPreferences.getScaled(y), 
            AppPreferences.getScaled(width), AppPreferences.getScaled(height));
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    Color DiscCol = myType == FILE_SAVE_AS ? Color.GRAY : Color.BLUE;
    Bounds bds = getScaled(2,2,12,12);
    g2.setColor(DiscCol);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.YELLOW);
    bds = getScaled(4,2,8,7);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.LIGHT_GRAY);
    bds = getScaled(6,10,4,4);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    bds = getScaled(8,11,1,2);
    g2.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    g2.setColor(Color.MAGENTA);
    int[] xpoints,ypoints;
    switch (myType) {
      case FILE_OPEN    : xpoints = new int[7];
                          ypoints = new int[7];
                          for (int i = 0 ; i < 7 ; i++) {
                            xpoints[i] = AppPreferences.getScaled(Arrowup[i*2]);
                            ypoints[i] = AppPreferences.getScaled(Arrowup[i*2+1]);
                          }
                          g2.fillPolygon(xpoints, ypoints, 7);
                          break;
      case FILE_SAVE_AS : 
      case FILE_SAVE    : xpoints = new int[7];
                          ypoints = new int[7];
                          for (int i = 0 ; i < 7 ; i++) {
                            xpoints[i] = AppPreferences.getScaled(Arrowdown[i*2]);
                            ypoints[i] = AppPreferences.getScaled(Arrowdown[i*2+1]);
                          }
                          g2.fillPolygon(xpoints, ypoints, 7);
                          break;
    }
  }

}
