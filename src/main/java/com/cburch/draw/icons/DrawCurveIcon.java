/*
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

package com.cburch.draw.icons;

import com.cburch.logisim.gui.icons.BaseIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class DrawCurveIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D gfx) {
    final var wh = scale(3);
    gfx.setStroke(new BasicStroke(scale(1)));
    gfx.setColor(Color.GRAY);

    gfx.drawRect(scale(9), scale(0), wh, wh);

    gfx.setStroke(new BasicStroke(scale(2)));
    gfx.setColor(Color.BLUE.darker());

    final var p = new GeneralPath();
    p.moveTo(scale(1), scale(5));
    p.quadTo(scale(10), scale(1), scale(14), scale(14));
    gfx.draw(p);

    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(scale(1)));
    gfx.drawRect(scale(13), scale(13), wh, wh);
    gfx.drawRect(scale(0), scale(5), wh, wh);
  }
}
