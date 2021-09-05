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

package com.cburch.logisim.gui.icons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class KeyboardIcon extends BaseIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    final int state = 0;

    final var wh = scale(12);
    var x = scale(state);
    var y = scale(11) + scale(state);
    final int[] xpos = {x, x + wh, scale(14), scale(3)};
    final int[] ypos = {y, y, scale(14), scale(14)};
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillPolygon(xpos, ypos, 4);
    g2.setColor(Color.BLACK);
    g2.drawPolygon(xpos, ypos, 4);
    x = wh + scale(state);
    y = scale(state);
    final int[] xpos1 = {x, x, scale(14), scale(14)};
    final int[] ypos1 = {y, y + wh, scale(14), scale(3)};
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillPolygon(xpos1, ypos1, 4);
    g2.setColor(Color.BLACK);
    g2.drawPolygon(xpos1, ypos1, 4);
    g2.setColor(Color.WHITE);
    g2.fillRect(scale(state), scale(state), wh, wh);
    g2.setColor(Color.BLACK);
    g2.drawRect(scale(state), scale(state), wh, wh);

    final var s = "K";
    final var f = g2.getFont().deriveFont((float) wh);
    final var t = new TextLayout(s, f, g2.getFontRenderContext());
    g2.setColor(Color.BLUE);
    final var center = scale(state) + wh / 2;
    t.draw(g2, center - (float) t.getBounds().getCenterX(), center - (float) t.getBounds().getCenterY());
  }
}
