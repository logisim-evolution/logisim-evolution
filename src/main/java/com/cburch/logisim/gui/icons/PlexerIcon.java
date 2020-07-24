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
import java.awt.Graphics2D;

import com.cburch.logisim.data.Value;

public class PlexerIcon extends AbstractIcon {

  private boolean inverted;
  private boolean singleInput;
  private static int[] xpos = {4,4,10,10};
  private static int[] ypos = {0,14,9,5};

  public PlexerIcon(boolean demux, boolean singleInput) {
    inverted = demux;
    this.singleInput = singleInput;
  }
  
  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setStroke(new BasicStroke(scale(2)));
    int[] realXpos = new int[4];
    int[] realYpos = new int[4];
    int xoff = inverted? 2 : 0;
    for (int i = 0 ; i < 4 ; i++) {
      realXpos[i] = scale(xpos[(i+xoff)&3]);
      realYpos[i] = scale(ypos[i]);
    }
    g2.drawPolygon(realXpos, realYpos, 4);
    xoff = inverted? scale(7):scale(8);
    g2.drawLine(xoff, scale(11), xoff, scale(15));
    /* draw output */
    xoff = inverted ? scale(xpos[0]-1) : scale(xpos[2]-1);
    int yoff = scale(ypos[3]+1);
    g2.setColor(Value.TRUE_COLOR);
    g2.fillOval(xoff, yoff, scale(3), scale(3));
    xoff = inverted ? scale(xpos[2]-1) : scale(xpos[0]-1);
    if (singleInput) {
      g2.fillOval(xoff, yoff, scale(3), scale(3));
    } else {
      g2.fillOval(xoff, scale(1), scale(3), scale(3));
      g2.fillOval(xoff, scale(11), scale(3), scale(3));
    }
  }

}
