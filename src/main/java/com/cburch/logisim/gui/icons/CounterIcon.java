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

public class CounterIcon extends AnnimatedIcon {

  private int state = 1;

  public void annimationUpdate() {
    state = (state+1)%100;
  };
  
  public void resetToStatic() {
    state = 1;
  };
    
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLACK);
    g2.fillRect(scale(1), 0, scale(6), scale(16));
    g2.fillRect(scale(9), 0, scale(6), scale(16));
    g2.drawRect(scale(1), 0, scale(6), scale(16));
    g2.drawRect(scale(9), 0, scale(6), scale(16));
    Font f = g2.getFont().deriveFont(scale((float)6));
    int tens = state/10;
    int ones = state%10;
    g2.setColor(Color.WHITE);
    for (int i = -1 ; i < 2 ; i++) {
      int val = Math.abs((ones+i)%10);
      char c = (char) ('0'+val);
      TextLayout t = new TextLayout(Character.toString(c),f,g2.getFontRenderContext());
      float x = scale((float)11.5) - (float)t.getBounds().getCenterX();
      float y = scale((float)(8.5 + i*7)) - (float)t.getBounds().getCenterY();
      t.draw(g2, x, y);
    }
    for (int i = -1 ; i < 2 ; i++) {
      int val = Math.abs((tens+i)%10);
      char c = (char) ('0'+val);
      TextLayout t = new TextLayout(Character.toString(c),f,g2.getFontRenderContext());
      float x = scale((float)3.5) - (float)t.getBounds().getCenterX();
      float y = scale((float)(8.5 + i*7)) - (float)t.getBounds().getCenterY();
      t.draw(g2, x, y);
    }
  };
}
