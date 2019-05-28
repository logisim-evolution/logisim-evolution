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

public class ArithmeticIcon extends AbstractIcon {

  private String Opp;
  private boolean invalid;
  private int NrOfChars = 2;

  public ArithmeticIcon( String Operation ) {
    Opp = Operation;
    invalid = false;
  }
  
  public ArithmeticIcon( String Operation, int CharsPerLine) {
    Opp = Operation;
    invalid = false;
    NrOfChars = CharsPerLine;
  }
	  
  public void setInvalid(boolean invalid) {
    this.invalid = invalid;
  }

  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.BLACK);
    float scale = Opp.length() >= NrOfChars ? NrOfChars : 1;
    int yoff = Opp.length() > NrOfChars ? getIconHeight()>>2 : getIconHeight()>>1; 
    Font f = g2.getFont().deriveFont((float)getIconWidth()/scale).deriveFont(Font.BOLD);
    g2.drawRect(scale(1), scale(1), getIconWidth()-scale(2), getIconHeight()-scale(2));
    TextLayout t = new TextLayout(Opp.length() > NrOfChars ? Opp.substring(0, NrOfChars) : Opp,f,g2.getFontRenderContext());
    t.draw(g2, (float)(getIconWidth()/2-t.getBounds().getCenterX()),(float)(yoff-t.getBounds().getCenterY()));
    if (Opp.length() > NrOfChars) {
      t = new TextLayout(Opp.length() > 2*NrOfChars ? Opp.substring(NrOfChars, 2*NrOfChars) : 
    	  Opp.substring(NrOfChars),f,g2.getFontRenderContext());
      t.draw(g2, (float)(getIconWidth()/2-t.getBounds().getCenterX()),(float)(3*yoff-t.getBounds().getCenterY()));
    }
    if (invalid) {
      g2.setColor(Color.RED);
      g2.fillOval(0, getIconHeight()/2, getIconWidth()/2, getIconHeight()/2);
      f = g2.getFont().deriveFont(scale((float)getIconWidth()/(float)(2.8))).deriveFont(Font.BOLD);
      t = new TextLayout("!",f,g2.getFontRenderContext());
      g2.setColor(Color.WHITE);
      t.draw(g2, (float)(getIconWidth()/4-t.getBounds().getCenterX()),(float)((3*getIconHeight())/4-t.getBounds().getCenterY()));
    }
  };
}
