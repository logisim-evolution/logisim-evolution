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
import java.util.concurrent.ThreadLocalRandom;

public class ShifterIcon extends AnnimatedIcon {

   private int state = -1;

  @Override
  public void annimationUpdate() {
    state >>= 1;
    int val = ThreadLocalRandom.current().nextInt(0, 2) << 2;
    state = (state | val)&7;
  }

  @Override
  public void resetToStatic() {
    state = -1;
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    StringBuffer s = new StringBuffer();
    if (state < 0) 
      s.append("\u25b6"+"\u25b6"+"\u25b6");
    else {
      int mask = 4;
      while (mask > 0) {
        s.append((state&mask)==0 ? "0" : "1");
        mask >>=1;
      }
    }
    Font f = g2.getFont().deriveFont(scale((float)6)).deriveFont(Font.BOLD);
    g2.setColor(Color.BLACK);
    g2.drawRect(0, scale(4), scale(16), scale(8));
    TextLayout t = new TextLayout(s.toString(),f,g2.getFontRenderContext());
    float x = scale((float)8)-(float)t.getBounds().getCenterX();
    float y = scale((float)8)-(float)t.getBounds().getCenterY();
    t.draw(g2, x, y);
  }

}
