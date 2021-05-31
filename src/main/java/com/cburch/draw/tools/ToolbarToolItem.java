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

package com.cburch.draw.tools;

import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Icon;

public class ToolbarToolItem implements ToolbarItem {
  private final AbstractTool tool;
  private final Icon icon;

  public ToolbarToolItem(AbstractTool tool) {
    this.tool = tool;
    this.icon = tool.getIcon();
  }

  public Dimension getDimension(Object orientation) {
    if (icon == null) {
      return new Dimension(
          AppPreferences.getScaled(AppPreferences.IconSize),
          AppPreferences.getScaled(AppPreferences.IconSize));
    } else {
      return new Dimension(
          icon.getIconWidth() + 4 * AppPreferences.IconBorder,
          icon.getIconHeight() + 4 * AppPreferences.IconBorder);
    }
  }

  public AbstractTool getTool() {
    return tool;
  }

  public String getToolTip() {
    return tool.getDescription();
  }

  public boolean isSelectable() {
    return true;
  }

  public void paintIcon(Component destination, Graphics g) {
    if (icon == null) {
      g.setColor(new Color(255, 128, 128));
      g.fillRect(4, 4, 8, 8);
      g.setColor(Color.BLACK);
      g.drawLine(4, 4, 12, 12);
      g.drawLine(4, 12, 12, 4);
      g.drawRect(4, 4, 8, 8);
    } else {
      icon.paintIcon(destination, g, 4, 4);
    }
  }
}
