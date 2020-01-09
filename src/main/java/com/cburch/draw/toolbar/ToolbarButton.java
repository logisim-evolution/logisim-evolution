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

package com.cburch.draw.toolbar;

import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;

class ToolbarButton extends JComponent implements MouseListener {
  private static final long serialVersionUID = 1L;

  private static final int BORDER = 2;

  private Toolbar toolbar;
  private ToolbarItem item;

  ToolbarButton(Toolbar toolbar, ToolbarItem item) {
    this.toolbar = toolbar;
    this.item = item;
    addMouseListener(this);
    setFocusable(true);
    setToolTipText("");
  }

  public ToolbarItem getItem() {
    return item;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension dim = item.getDimension(toolbar.getOrientation());
    dim.width += 2 * BORDER;
    dim.height += 2 * BORDER;
    return dim;
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    return item.getToolTip();
  }

  public void mouseClicked(MouseEvent e) {}

  public void mouseEntered(MouseEvent e) {}

  public void mouseExited(MouseEvent e) {
    toolbar.setPressed(null);
  }

  public void mousePressed(MouseEvent e) {
    if (item != null && (item.isSelectable() || (item instanceof ToolbarClickableItem))) {
      toolbar.setPressed(this);
    }
  }

  public void mouseReleased(MouseEvent e) {
    if (toolbar.getPressed() == this) {
      toolbar.setPressed(null);
      if (item != null && item.isSelectable()) {
        toolbar.getToolbarModel().itemSelected(item);
      } else if (item != null && item instanceof ToolbarClickableItem) {
        ((ToolbarClickableItem) item).clicked();
      }
    }
  }

  @Override
  public void paintComponent(Graphics g) {
    if (toolbar.getPressed() == this) {
      if (item instanceof ToolbarClickableItem) {
        Graphics g2 = g.create();
        g2.translate(BORDER, BORDER);
        ((ToolbarClickableItem) item).paintPressedIcon(ToolbarButton.this, g2);
        g2.dispose();
        return;
      }
      Dimension dim = item.getDimension(toolbar.getOrientation());
      Color defaultColor = g.getColor();
      GraphicsUtil.switchToWidth(g, 2);
      g.setColor(Color.GRAY);
      g.fillRect(BORDER, BORDER, dim.width, dim.height);
      GraphicsUtil.switchToWidth(g, 1);
      g.setColor(defaultColor);
    }

    Graphics g2 = g.create();
    g2.translate(BORDER, BORDER);
    item.paintIcon(ToolbarButton.this, g2);
    g2.dispose();

    // draw selection indicator
    if (toolbar.getToolbarModel().isSelected(item)) {
      Dimension dim = item.getDimension(toolbar.getOrientation());
      GraphicsUtil.switchToWidth(g, 2);
      g.setColor(Color.BLACK);
      g.drawRect(BORDER, BORDER, dim.width, dim.height);
      GraphicsUtil.switchToWidth(g, 1);
    }
  }
}
