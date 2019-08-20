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
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

import javax.swing.Icon;

import com.cburch.logisim.prefs.AppPreferences;

public class ErrorIcon implements Icon {

  private int wh;
  
  public ErrorIcon() {
	  wh = AppPreferences.getIconSize();
  }

  public ErrorIcon(double scale) {
	  wh = (int)AppPreferences.getScaled(scale*AppPreferences.getIconSize());
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    double trd = wh/3;
    int[] xpos = {0,(int)trd,(int)(2*trd),wh-1,wh-1,(int)(2*trd),(int)trd,0};
    int[] ypos = {(int)trd,0,0,(int)trd,(int)(2*trd),wh-1,wh-1,(int)(2*trd)};
    g2.setColor(Color.RED.brighter().brighter());
    g2.fillPolygon(xpos, ypos, 8);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.RED.darker().darker());
    g2.drawPolygon(xpos, ypos, 8);
    g2.setColor(Color.WHITE);
    Font f = g2.getFont().deriveFont((float)wh/(float)1.3).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout("X",f,g2.getFontRenderContext());
    float xc = (float)wh/(float)2-(float)t.getBounds().getCenterX();
    float yc = (float)wh/(float)2-(float)t.getBounds().getCenterY();
    t.draw(g2, xc, yc);
    g2.dispose();
  }
  
  public static int scale(int v) {
    return AppPreferences.getScaled(v);
  }
  
  @Override
  public int getIconWidth() {
    return wh;
  }

  @Override
  public int getIconHeight() {
    return wh;
  }

}
