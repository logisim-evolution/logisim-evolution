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

import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.prefs.AppPreferences;

public class AppearEditIcon extends AbstractIcon {

  private int[] tip = {0,14,1,15,0,15};
  private int[] body = {0,13,13,0,15,2,2,15};
  private int[] extendedtip = {0,13,1,12,3,15,2,15};
  private int[] cleantip = {12,1,13,0,15,2,14,3};
  @Override
  protected void paintIcon(Graphics2D g2) {
    Graphics2D g = (Graphics2D)g2.create();
    SubcircuitFactory.paintClasicIcon(g);
    g.dispose();
    g2.setColor(Color.MAGENTA);
    GeneralPath path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(tip[0]), AppPreferences.getScaled(tip[1]));
    for (int i = 2 ; i < tip.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(tip[i]), AppPreferences.getScaled(tip[i+1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(new Color(139,69,19));
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(body[0]), AppPreferences.getScaled(body[1]));
    for (int i = 2 ; i < body.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(body[i]), AppPreferences.getScaled(body[i+1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(new Color(210,180,140));
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(extendedtip[0]), AppPreferences.getScaled(extendedtip[1]));
    for (int i = 2 ; i < extendedtip.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(extendedtip[i]), AppPreferences.getScaled(extendedtip[i+1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(Color.GRAY);
    path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(cleantip[0]), AppPreferences.getScaled(cleantip[1]));
    for (int i = 2 ; i < cleantip.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(cleantip[i]), AppPreferences.getScaled(cleantip[i+1]));
    path.closePath();
    g2.fill(path);
  }

}
