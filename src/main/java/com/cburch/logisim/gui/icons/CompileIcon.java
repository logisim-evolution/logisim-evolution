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
import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

public class CompileIcon extends AbstractIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    int[] page = new int[] {0,0,0,15,15,15,15,5,10,5,10,0,15,5,10,0,0,0};
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1F)));
    int[] xpos = new int[9];
    int[] ypos = new int[9];
    for (int i = 0 ; i < 9 ; i++) {
      xpos[i] = AppPreferences.getScaled(page[i*2]);
      ypos[i] = AppPreferences.getScaled(page[i*2+1]);
    }
    g2.drawPolygon(xpos, ypos, 9);
    Font f = g2.getFont();
    g2.setFont(f.deriveFont(AppPreferences.getScaled(4F)));
    g2.setColor(Color.BLUE);
    GraphicsUtil.drawCenteredText(g2, "j r9", AppPreferences.getScaled(7), AppPreferences.getScaled(3));
    GraphicsUtil.drawCenteredText(g2, "nop", AppPreferences.getScaled(7), AppPreferences.getScaled(7));
    g2.setColor(Color.MAGENTA);
    GraphicsUtil.drawCenteredText(g2, "101..", AppPreferences.getScaled(7), AppPreferences.getScaled(11));
  }

}
