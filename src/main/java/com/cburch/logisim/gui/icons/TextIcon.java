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

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class TextIcon extends AbstractIcon {

  @Override
  protected void paintIcon(Graphics2D g2) {
    Font f = StdAttr.DEFAULT_LABEL_FONT.deriveFont((float) AppPreferences.getIconSize());
    TextLayout l = new TextLayout("A", f, g2.getFontRenderContext());
    l.draw(
        g2,
        (float) (AppPreferences.getIconSize() / 2 - l.getBounds().getCenterX()),
        (float) (AppPreferences.getIconSize() / 2 - l.getBounds().getCenterY()));
  }
}
