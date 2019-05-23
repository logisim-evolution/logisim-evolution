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

import com.cburch.logisim.vhdl.gui.HdlToolbarModel;

public class HdlIcon extends AbstractIcon {

  private String type;

  public HdlIcon(String type) {
    this.type = type;
  }

  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.WHITE);
    g2.fillRect(0, scale(4), scale(16), scale(12));
    g2.setColor(Color.BLACK);
    g2.drawRect(0, scale(4), scale(16), scale(12));
    g2.setColor(Color.LIGHT_GRAY);
    Font f = g2.getFont().deriveFont((float)getIconWidth()/(float)4.5);
    TextLayout t = new TextLayout("LIBRARY",f,g2.getFontRenderContext());
    t.draw(g2, (float)(getIconWidth()/2-t.getBounds().getCenterX()), 
    		(float)((3*getIconHeight())/8-t.getBounds().getCenterY()));
    t = new TextLayout("USE iee",f,g2.getFontRenderContext());
    t.draw(g2, (float)(getIconWidth()/2-t.getBounds().getCenterX()), 
    		(float)((5*getIconHeight())/8-t.getBounds().getCenterY()));
    t = new TextLayout("ENTITY ",f,g2.getFontRenderContext());
    t.draw(g2, (float)(getIconWidth()/2-t.getBounds().getCenterX()), 
    		(float)((7*getIconHeight())/8-t.getBounds().getCenterY()));
    if (type.equals(HdlToolbarModel.HDL_VALIDATE)) {
      g2.setColor(Color.RED);
      g2.setStroke(new BasicStroke(scale(2)));
      int[] xpos = {scale(3),scale(6),scale(15)};
      int[] ypos = {scale(3),scale(11),0};
      g2.drawPolyline(xpos, ypos, 3);
    } else {
      if (type.equals(HdlToolbarModel.HDL_EXPORT)) {
        g2.translate(scale(8), scale(4));
        g2.rotate(Math.PI);
        g2.translate(-scale(8), -scale(4));
      }
      g2.setColor(Color.MAGENTA.darker());
      int[] x = {scale(7),scale(7),scale(5),scale(8),scale(11),scale(9),scale(9)};
      int[] y = {0,scale(5),scale(5),scale(8),scale(5),scale(5),0};
      g2.fillPolygon(x, y, 7);
      g2.drawPolygon(x, y, 7);
    }
  };
}
