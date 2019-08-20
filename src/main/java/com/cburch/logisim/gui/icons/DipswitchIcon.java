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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class DipswitchIcon extends AnnimatedIcon {

  private int state = 0;

  @Override
  public void annimationUpdate() {
    state = (state+1)&3;
  }

  @Override
  public void resetToStatic() {
    state = 0;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLUE);
    g2.fillRect(0, 0, getIconWidth(), getIconHeight());
    int w = scale(8);
    int h = scale(5);
    g2.setColor(Color.WHITE);
    Font f = g2.getFont().deriveFont((float)(getIconWidth()/2.5));
    TextLayout t = new TextLayout("1",f,g2.getFontRenderContext());
    t.draw(g2, (float)((3*getIconWidth())/4-t.getBounds().getCenterX()), 
    		(float)(getIconHeight()/4-t.getBounds().getCenterY()));
    t = new TextLayout("2",f,g2.getFontRenderContext());
    t.draw(g2, (float)((3*getIconWidth())/4-t.getBounds().getCenterX()), 
    		(float)((3*getIconHeight())/4-t.getBounds().getCenterY()));
    g2.fillRect(scale(2), scale(2), w, h);
    g2.fillRect(scale(2), scale(9), w, h);
    g2.setColor(Color.gray);
    int x1,x2;
    switch (state) {
      case 0 : 
        x1 = x2 = scale(2);
        break;
      case 1 :
        x1 = scale(2)+w>>1;
        x2 = scale(2);
        break;
      case 3 :
        x2 = scale(2)+w>>1;
        x1 = scale(2);
        break;
      default:
    	x1 = x2 = scale(2)+w>>1;
    }
    g2.fillRect(x1, scale(2), w>>1, h);
    g2.fillRect(x2, scale(9), w>>1, h);
  }

}
