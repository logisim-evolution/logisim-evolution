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

package com.cburch.draw.toolbar;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class ToolbarSeparator implements ToolbarItem {
  private final int size;

  public ToolbarSeparator(int size) {
    this.size = size;
  }

  @Override
  public Dimension getDimension(Object orientation) {
    return new Dimension(size, size);
  }

  @Override
  public String getToolTip() {
    return null;
  }

  @Override
  public boolean isSelectable() {
    return false;
  }

  @Override
  public void paintIcon(Component destination, Graphics gfx) {
    final var dim = destination.getSize();
    var x = 0;
    var y = 0;
    var w = dim.width;
    var h = dim.height;
    final var width = AppPreferences.getScaled(2);
    if (h >= w) { // separator is a vertical line in horizontal toolbar
      x = (w - width - 2) / 2;
      w = width;
    } else { // separator is a horizontal line in vertical toolbar
      y = (h - width - 2) / 2;
      h = width;
    }
    gfx.setColor(Color.GRAY);
    gfx.fillRect(x, y, w, h);
  }
}
