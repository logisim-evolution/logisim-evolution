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

import com.cburch.logisim.std.io.HexDigit;

public class SevenSegmentIcon extends AnnimatedIcon {

  private boolean isHexDisplay;
  private int state;
  
  public SevenSegmentIcon( boolean HexDisplay ) {
    isHexDisplay = HexDisplay;
    state = (HexDisplay) ? -1 : 3;
  }

  @Override
  public void annimationUpdate() {
    state++;
    state %= isHexDisplay ? 16 : 10;
  }

  @Override
  public void resetToStatic() {
    state = isHexDisplay ? -1 : 3;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    int segson = HexDigit.getSegs(state);
    g2.setStroke(new BasicStroke(scale(2)));
    g2.setColor(Color.WHITE);
    g2.fillRect(scale(2), 0, scale(10), scale(16));
    g2.setColor(Color.BLACK);
    g2.drawRect(scale(2), 0, scale(10), scale(16));
    g2.setColor((segson&HexDigit.SEG_A_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(5), scale(3), scale(8), scale(3));
    g2.setColor((segson&HexDigit.SEG_B_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(9), scale(4), scale(9), scale(7));
    g2.setColor((segson&HexDigit.SEG_C_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(9), scale(9), scale(9), scale(12));
    g2.setColor((segson&HexDigit.SEG_D_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(5), scale(13), scale(8), scale(13));
    g2.setColor((segson&HexDigit.SEG_F_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(4), scale(4), scale(4), scale(7));
    g2.setColor((segson&HexDigit.SEG_E_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(4), scale(9), scale(4), scale(12));
    g2.setColor((segson&HexDigit.SEG_G_MASK)!=0 ? Color.RED : Color.LIGHT_GRAY);
    g2.drawLine(scale(5), scale(8), scale(8), scale(8));
  }

}
