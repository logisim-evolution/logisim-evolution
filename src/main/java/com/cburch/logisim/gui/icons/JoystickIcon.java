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
import java.awt.Graphics2D;

public class JoystickIcon extends AnnimatedIcon {

  private int state = 0;

  @Override
  public void annimationUpdate() {
    state = (state+1)%6;
  }

  @Override
  public void resetToStatic() {
    state = 0;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLUE.darker().darker());
    int[] xpos = {0,scale(6),scale(4),scale(2)};
    int[] ypos = {scale(13),scale(13),scale(15),scale(15)};
    g2.fillPolygon(xpos, ypos, 4);
    for (int i = 0 ; i < 4 ; i++)
      xpos[i] += scale(10);
    g2.fillPolygon(xpos, ypos, 4);
    if ((state&1) == 0) {
      g2.setColor(Color.RED);
      g2.fillRect(scale(2), scale(9), scale(4), scale(2));
    }
    int xbase = scale(9);
    int ybase = scale(11);
    int[] xcerc = {scale(9),scale(13),scale(9),scale(5),scale(2),scale(5)};
    int[] ycerc = {scale(2),scale(3),scale(2),scale(3),scale(5),scale(3)};
    int xtop = xcerc[state];
    int ytop = ycerc[state];
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.BLACK);
    g2.drawLine(xtop, ytop, xbase, ybase);
    xtop -= scale(2);
    ytop -= scale(2);
    g2.setStroke(new BasicStroke(scale(1)));
    g2.setColor(Color.RED);
    g2.fillOval(xtop, ytop, scale(4), scale(4));
    g2.drawOval(xtop, ytop, scale(4), scale(4));
    g2.setColor(Color.BLUE);
    g2.fillRoundRect(0, scale(10), scale(16), scale(4), scale(2), scale(2));
    g2.drawRoundRect(0, scale(10), scale(16), scale(4), scale(2), scale(2));
  }

}
