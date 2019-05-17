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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.cburch.logisim.gui.icons.AnnimationTimer.AnnimationListener;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.prefs.AppPreferences;

public abstract class AnnimatedIcon implements Icon,AnnimationListener {

  public AnnimatedIcon() {
    Frame.ANNIMATIONICONTIMER.registerListener(this);
  }

  public void registerParrent(Component parrent) {
    Frame.ANNIMATIONICONTIMER.addParrent(parrent);
  }
  
  public static int scale(int v) {
    return AppPreferences.getScaled(v);
  }
  
  public static double scale(double v) {
    return AppPreferences.getScaled(v);
  }
	  
  public static float scale(float v) {
    return AppPreferences.getScaled(v);
  }
		  
  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    g2.translate(x, y);
    paintIcon(g2);
    g2.dispose();
  }

  protected abstract void paintIcon(Graphics2D g2);

  @Override
  public int getIconWidth() {
    return AppPreferences.getIconSize();
  }

  @Override
  public int getIconHeight() {
    return AppPreferences.getIconSize();
  }
    
}
