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
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class DrawShapeIcon extends BaseIcon {

  public static final int RECTANGLE = 0;
  public static final int ROUNDED_RECTANGLE = 1;
  public static final int ELIPSE = 2;

  private static final int[] points = {3, 2, 8, 4, 14, 8};
  private final int shapeType;

  public DrawShapeIcon(int type) {
    this.shapeType = type;
  }

  @Override
  protected void paintIcon(Graphics2D gfx) {
    final var state = 3;

    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
    final var x = scale(1);
    final var y = scale(3);
    final var width = scale(points[(state - 1) * 2]);
    final var height = scale(points[(state - 1) * 2 + 1]);

    switch (shapeType) {
      case RECTANGLE:
        gfx.setColor(Color.BLUE.darker());
        gfx.drawRect(x, y, width, height);
        break;
      case ROUNDED_RECTANGLE:
        gfx.setColor(Color.RED.darker());
        // FIXME: rounded rect shape looks almost as regular rectangle on smaller zoom factor
        gfx.drawRoundRect(x, y, width, height, y, y);
        break;
      case ELIPSE:
      default:
        gfx.setColor(Color.BLUE);
        gfx.drawOval(x, y, width, height);
        break;
    }

    gfx.setColor(Color.GRAY);
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    gfx.drawRect(0, y - x, y, y);
    gfx.drawRect(width - x, y + height - x, y, y);
  }

}
