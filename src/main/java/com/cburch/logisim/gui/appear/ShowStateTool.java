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

package com.cburch.logisim.gui.appear;

import com.cburch.draw.toolbar.ToolbarClickableItem;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.logisim.gui.icons.ShowStateIcon;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.Icon;

public class ShowStateTool implements ToolbarClickableItem {

  private AppearanceView view;
  private AppearanceCanvas canvas;
  private DrawingAttributeSet attrs;
  private Icon icon, pressed;

  public ShowStateTool(AppearanceView view, AppearanceCanvas canvas, DrawingAttributeSet attrs) {
    this.view = view;
    this.canvas = canvas;
    this.attrs = attrs;
    icon = new ShowStateIcon(false);
    pressed = new ShowStateIcon(true);
  }

  public Dimension getDimension(Object orientation) {
    return new Dimension(icon.getIconWidth() + 8, icon.getIconHeight() + 8);
  }

  public String getToolTip() {
    return "Select state to be shown";
  }

  public boolean isSelectable() {
    return false;
  }

  public void clicked() {
    ShowStateDialog w = new ShowStateDialog(view.getFrame(), canvas);
    Point p = view.getFrame().getLocation();
    p.translate(80, 50);
    w.setLocation(p);
    w.setVisible(true);
  }

  public void paintIcon(java.awt.Component destination, Graphics g) {
    icon.paintIcon(destination, g, 4, 4);
  }

  public void paintPressedIcon(java.awt.Component destination, Graphics g) {
    pressed.paintIcon(destination, g, 4, 4);
  }
}
