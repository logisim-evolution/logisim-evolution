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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;

import com.cburch.logisim.prefs.AppPreferences;

public class ProjectAddIcon  extends AbstractIcon{

  private boolean removeIcon = false;
  private boolean vhdl = false;
  private boolean deselect = false;
  private static int[] points = {2,6,6,6,6,2,9,2,9,6,13,6,13,9,9,9,9,13,6,13,6,9,2,9};
  
  public ProjectAddIcon() {
    vhdl = true;  
  }
  
  public ProjectAddIcon(boolean removeSymbol) {
    removeIcon = removeSymbol;
  }
  
  public void setDeselect(boolean val) {deselect = val;}

  protected void paintIcon(Graphics2D g2) {
	if (deselect) {
      Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
      g2.setComposite(c);
	}
    if (vhdl) {
      g2.setColor(Color.GREEN.darker().darker());
      Font f = g2.getFont().deriveFont((float)AppPreferences.getIconSize()/(float)1.6).deriveFont(Font.BOLD);
      TextLayout l1 = new TextLayout("VH",f,g2.getFontRenderContext());
      double top = AppPreferences.getIconSize()/4-l1.getBounds().getCenterY();
      double left = AppPreferences.getIconSize()/2-l1.getBounds().getCenterX();
      l1.draw(g2, (float)left, (float)top);
      l1 = new TextLayout("DL",f,g2.getFontRenderContext());
      top = (3*AppPreferences.getIconSize())/4-l1.getBounds().getCenterY();
      left = AppPreferences.getIconSize()/2-l1.getBounds().getCenterX();
      l1.draw(g2, (float)left, (float)top);
      g2.dispose();
      return;
    }
    if (removeIcon) {
       g2.setColor(Color.RED);
       g2.translate(AppPreferences.getIconSize()/2, AppPreferences.getIconSize()/2);
       g2.rotate(Math.PI/4);
       g2.translate(-AppPreferences.getIconSize()/2, -AppPreferences.getIconSize()/2);
    } else
      g2.setColor(Color.GREEN.darker());
    GeneralPath path = new GeneralPath();
    path.moveTo(AppPreferences.getScaled(points[0]), AppPreferences.getScaled(points[1]));
    for (int i = 2 ; i < points.length ; i +=2)
      path.lineTo(AppPreferences.getScaled(points[i]), AppPreferences.getScaled(points[i+1]));
    path.closePath();
    g2.fill(path);
    g2.setColor(removeIcon ? Color.RED.darker() : Color.GREEN.darker().darker());
    g2.draw(path);
  }

}
