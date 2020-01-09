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
import java.awt.Graphics2D;
import java.util.concurrent.ThreadLocalRandom;

public class DiceIcon extends AnnimatedIcon {

  private int state = ThreadLocalRandom.current().nextInt(0, 6);

  @Override
  public void annimationUpdate() {
    state = ThreadLocalRandom.current().nextInt(0, 6);
  }

  @Override
  public void resetToStatic() {
    state = ThreadLocalRandom.current().nextInt(0, 6);
  }

  @Override
  protected void paintIcon(Graphics2D g2) {
    g2.setColor(Color.BLACK);
    g2.drawRoundRect(0, 0, scale(16), scale(16), scale(5), scale(5));
    if (state == 1 || state > 2)
      g2.fillOval(scale(2), scale(2), scale(3), scale(3));
    if (state == 5) {
      g2.fillOval(scale(2), scale(6), scale(3), scale(3));
      g2.fillOval(scale(10), scale(6), scale(3), scale(3));
    }
    if (state == 0 || state == 4 || state == 2)
      g2.fillOval(scale(6), scale(6), scale(3), scale(3));
    if (state > 1) {
      g2.fillOval(scale(2), scale(10), scale(3), scale(3));
      g2.fillOval(scale(10), scale(2), scale(3), scale(3));
    }
    if (state > 2 || state == 1) 
      g2.fillOval(scale(10), scale(10), scale(3), scale(3));
  }

}
