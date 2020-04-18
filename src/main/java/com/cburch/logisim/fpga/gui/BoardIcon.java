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

package com.cburch.logisim.fpga.gui;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

public class BoardIcon implements Icon {
  private Image image;
  private int icon_width = 240;
  private int icon_height = 130;

  public BoardIcon(BufferedImage BoardImage) {
    if (BoardImage == null) image = null;
    else
      image =
          BoardImage.getScaledInstance(
              this.getIconWidth(), this.getIconHeight(), BufferedImage.SCALE_SMOOTH);
  }

  public int getIconHeight() {
    return AppPreferences.getScaled(icon_height);
  }

  public int getIconWidth() {
    return AppPreferences.getScaled(icon_width);
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (image != null) g.drawImage(image, x, y, null);
    else {
      g.setColor(Color.gray);
      g.fillRect(0, 0, this.getIconWidth(), this.getIconHeight());
    }
  }

  public void SetImage(BufferedImage BoardImage) {
    if (BoardImage == null) image = null;
    else
      image =
          BoardImage.getScaledInstance(
              this.getIconWidth(), this.getIconHeight(), BufferedImage.SCALE_SMOOTH);
  }
}
