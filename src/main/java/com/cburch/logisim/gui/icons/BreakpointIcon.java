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
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.Icon;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

public class BreakpointIcon implements Icon {

  private int wh = AppPreferences.getScaled(12);

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.setColor(Color.RED);
    g.fillOval(x, y, wh, wh);
    g.setColor(Color.DARK_GRAY);
    g.drawOval(x, y, wh, wh);
    g.setColor(Color.YELLOW);
    Font f = g.getFont();
    g.setFont(f.deriveFont(Font.BOLD,(f.getSize()*6)/10));
    GraphicsUtil.drawCenteredText(g, "B", x+wh/2, y+wh/2);
    g.setFont(f);
  }

  @Override
  public int getIconWidth() { return wh; }

  @Override
  public int getIconHeight() { return wh; }

}
