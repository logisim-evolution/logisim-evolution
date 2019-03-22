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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.Graphics;

public class CounterPoker extends RegisterPoker {

  @Override
  public void paint(InstancePainter painter) {
    Bounds bds = painter.getBounds();
    BitWidth dataWidth = painter.getAttributeValue(StdAttr.WIDTH);
    int width = dataWidth == null ? 8 : dataWidth.getWidth();
    int len = (width + 3) / 4;

    Graphics g = painter.getGraphics();
    g.setColor(Color.RED);
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      if (len > 4) {
        g.drawRect(bds.getX(), bds.getY() + 3, bds.getWidth(), 25);
      } else {
        int wid = 7 * len + 2;
        g.drawRect(bds.getX() + (bds.getWidth() - wid) / 2, bds.getY() + 4, wid, 15);
      }
    } else {
      int xcenter = Counter.SymbolWidth(width) - 25;
      g.drawRect(bds.getX() + xcenter - len * 4, bds.getY() + 22, len * 8, 16);
    }
    g.setColor(Color.BLACK);
  }
}
