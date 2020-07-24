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
import java.awt.font.TextLayout;

import com.cburch.logisim.prefs.AppPreferences;

public class ShowStateIcon extends AnnimatedIcon {

  private boolean pressed;
  private int state;
  
  public ShowStateIcon( boolean pressed ) {
    this.pressed = pressed;
    state = 5;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    if (pressed) {
      g2.setColor(Color.MAGENTA.brighter().brighter().brighter());
      g2.fillRect(0, 0, getIconWidth(), getIconHeight());
    }
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 0, getIconWidth(), getIconHeight()/2);
    Font f = g2.getFont().deriveFont((float)getIconWidth()/(float)2);
    String str = Integer.toBinaryString(state);
    while (str.length()<3) {
    	str = "0"+str;
    }
    TextLayout l = new TextLayout(str,f,g2.getFontRenderContext());
    l.draw(g2, (float)((double)getIconWidth()/2.0-l.getBounds().getCenterX()), 
    		(float)((double)getIconHeight()/4.0-l.getBounds().getCenterY()));
    int wh = AppPreferences.getScaled(AppPreferences.IconSize/2-AppPreferences.IconBorder);
    int offset = AppPreferences.getScaled(AppPreferences.IconBorder);
    g2.setColor((state&4) != 0 ? Color.RED : Color.DARK_GRAY);
    g2.fillOval(offset, offset+getIconHeight()/2, wh, wh);
    g2.setColor((state&1) != 0 ? Color.GREEN : Color.DARK_GRAY);
    g2.fillOval(offset+getIconWidth()/2, offset+getIconHeight()/2, wh, wh);
    g2.setColor(Color.BLACK);
    g2.drawOval(offset, offset+getIconHeight()/2, wh, wh);
    g2.drawOval(offset+getIconWidth()/2, offset+getIconHeight()/2, wh, wh);
  }

  @Override
  public void annimationUpdate() {
    state = (state+1)&7;
  }

  @Override
  public void resetToStatic() {
    state = 5;
  }

}
