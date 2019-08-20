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

import com.cburch.logisim.std.io.Tty;

public class TtyIcon extends AnnimatedIcon {

  private static String display = "__Hello World!__";
  private int index = 0;

  @Override
  public void annimationUpdate() {
    index = (index+1)%(display.length()-2);
  }

  @Override
  public void resetToStatic() {
    index = 0;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLUE);
    g2.fillRoundRect(0, scale(3), scale(16), scale(10), scale(3), scale(3));
    g2.setColor(Color.BLACK);
    g2.drawRoundRect(0, scale(3), scale(16), scale(10), scale(3), scale(3));
    Font f = Tty.DEFAULT_FONT.deriveFont(scale((float)5)).deriveFont(Font.BOLD);
    TextLayout t = new TextLayout(display.substring(index, index+3),f,g2.getFontRenderContext());
    g2.setColor(Color.yellow);
    t.draw(g2, (float)(getIconWidth()/2-t.getBounds().getCenterX()), 
    		(float)(getIconHeight()/2-t.getBounds().getCenterY()));
  }

}
