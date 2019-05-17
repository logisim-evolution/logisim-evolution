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
import java.awt.geom.GeneralPath;

public class DrcIcon extends AnnimatedIcon {

  public boolean empty;
  public int state = -1;
  
  public DrcIcon( boolean isDrcError ) {
    empty = !isDrcError;
  }

  @Override
  public void annimationUpdate() {
    state = (state+1)&3;
  }

  @Override
  public void resetToStatic() {
    state = -1;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    if (empty)
      return;
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, getIconWidth(), getIconHeight());
    switch (state) {
       case -1 : 
         paintStatic(g2);
         break;
       case 0 :
         paintText(g2,"!!");
         break;
       case 1 :
         paintText(g2);
         break;
       case 2 :
         paintText(g2,"??");
         break;
       default:
    	 SelectIcon.paint(g2);
    }
    
  }
  
  private void paintStatic(Graphics2D g2) {
    GeneralPath p = new GeneralPath();
    p.moveTo(scale(15), 0);
    p.quadTo(scale(7), scale(7), 0, scale(6));
    p.lineTo(0, scale(8));
    p.quadTo(scale(7), scale(9), scale(15), scale(2));
    p.closePath();
    g2.setColor(Color.RED.brighter().brighter());
    g2.fill(p);
    paintText(g2);
    p = new GeneralPath();
    p.moveTo(0, scale(6));
    p.quadTo(scale(3), scale(14), scale(12), scale(11));
    p.lineTo(scale(12), scale(9));;
    p.lineTo(scale(15), scale(12));
    p.lineTo(scale(12), scale(15));
    p.lineTo(scale(12), scale(13));
    p.quadTo(scale(3), scale(16), 0, scale(8));
    p.closePath();
    g2.setColor(Color.RED.darker());
    g2.fill(p);
  }
  
  private void paintText(Graphics2D g2) {
    Font f = g2.getFont().deriveFont(scale((float)getIconWidth()/3)).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout("DRC",f,g2.getFontRenderContext());
    g2.setColor(Color.BLUE.darker().darker());
    t.draw(g2, getIconWidth()/2-(float)t.getBounds().getCenterX(), getIconHeight()/2-(float)t.getBounds().getCenterY());
  }

  private void paintText(Graphics2D g2, String s) {
    Font f = g2.getFont().deriveFont(scale((float)getIconWidth()/(float)s.length())).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout(s,f,g2.getFontRenderContext());
    g2.setColor(Color.RED.darker().darker());
    t.draw(g2, getIconWidth()/2-(float)t.getBounds().getCenterX(), getIconHeight()/2-(float)t.getBounds().getCenterY());
  }

}
